package io.github.jayfunc.carpelines.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.os.SystemClock
import androidx.core.content.ContextCompat
import io.github.jayfunc.carpelines.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/** The normalized bar values and bass energy of one FFT capture. */
internal data class SpectrumFrame(
    val bars: FloatArray,
    val bass: Float,
)

internal enum class SpectrumStatus {
    IDLE,
    WAITING_FOR_SESSION,
    PERMISSION_REQUIRED,
    RUNNING,
    UNAVAILABLE,
}

/**
 * Attaches a [Visualizer] to an external player's audio session and publishes the bar values.
 *
 * The session id comes from [io.github.jayfunc.carpelines.data.external.ExternalAudioSessionRegistry];
 * the player must have announced it through the audio-effect control broadcast. Visualizer reads
 * only the captured FFT, never the microphone, and nothing is stored or sent anywhere.
 *
 * Capture callbacks and the band mapping run on the Visualizer callback thread; the Compose
 * renderer only reads the latest immutable snapshot, so it never touches the audio callback.
 */
internal class ExternalSpectrumAnalyzer(context: Context) {

    private companion object {
        private const val TAG = "ExternalSpectrum"
        private const val DEFAULT_SAMPLE_RATE_HZ = 48_000
        private const val DEFAULT_CAPTURE_SIZE = 1_024
    }

    private val appContext = context.applicationContext

    private val _frame = MutableStateFlow<SpectrumFrame?>(null)
    val frame: StateFlow<SpectrumFrame?> = _frame.asStateFlow()

    private val _status = MutableStateFlow(SpectrumStatus.IDLE)
    val status: StateFlow<SpectrumStatus> = _status.asStateFlow()

    @Volatile
    private var barCount: Int = 32

    @Volatile
    private var sensitivity: Float = 1f

    private val lock = Any()
    @Volatile
    private var visualizer: Visualizer? = null
    private var attachedSessionId: Int = 0
    private var agcPeak: Float = 0.05f
    private var lastCallbackLogAtMs: Long = 0L

    fun setConfig(barCount: Int, sensitivity: Float) {
        this.barCount = barCount.coerceIn(8, 64)
        this.sensitivity = sensitivity.coerceIn(0.25f, 4f)
    }

    @SuppressLint("MissingPermission")
    fun attach(sessionId: Int) {
        synchronized(lock) {
            if (sessionId <= 0) {
                detachInternal()
                _status.value = SpectrumStatus.WAITING_FOR_SESSION
                return
            }
            if (!hasRecordAudioPermission()) {
                detachInternal()
                _status.value = SpectrumStatus.PERMISSION_REQUIRED
                return
            }
            if (attachedSessionId == sessionId && visualizer != null) return

            detachInternal()
            var candidate: Visualizer? = null
            runCatching {
                candidate = Visualizer(sessionId).apply {
                    val range = Visualizer.getCaptureSizeRange()
                    captureSize = range
                        .lastOrNull()
                        ?.coerceIn(128, 2_048)
                        ?: DEFAULT_CAPTURE_SIZE
                    setScalingMode(Visualizer.SCALING_MODE_NORMALIZED)
                    val rate = Visualizer.getMaxCaptureRate().takeIf { it > 0 } ?: 20_000
                    setDataCaptureListener(listener, rate, false, true)
                }
                val enabled = candidate.setEnabled(true)
                check(enabled == Visualizer.SUCCESS) { "Visualizer.setEnabled returned $enabled" }
                visualizer = candidate
                attachedSessionId = sessionId
                agcPeak = 0.05f
                _status.value = SpectrumStatus.RUNNING
                Timber.tag(TAG).d("Attached to session %d", sessionId)
            }.onFailure { error ->
                Timber.tag(TAG).w(error, "Unable to attach to session %d", sessionId)
                candidate?.let { runCatching { it.release() } }
                visualizer = null
                attachedSessionId = 0
                _frame.value = null
                _status.value = SpectrumStatus.UNAVAILABLE
            }
        }
    }

    fun detach() {
        synchronized(lock) {
            detachInternal()
        }
        if (_status.value != SpectrumStatus.PERMISSION_REQUIRED &&
            _status.value != SpectrumStatus.UNAVAILABLE
        ) {
            _status.value = SpectrumStatus.IDLE
        }
    }

    fun release() {
        detach()
        _status.value = SpectrumStatus.IDLE
    }

    @SuppressLint("MissingPermission")
    private fun detachInternal() {
        val current = visualizer
        visualizer = null
        attachedSessionId = 0
        if (current != null) {
            runCatching { current.setEnabled(false) }
            runCatching { current.release() }
            Timber.tag(TAG).d("Detached")
        }
        _frame.value = null
    }

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    private val listener = object : Visualizer.OnDataCaptureListener {
        override fun onWaveFormDataCapture(
            visualizer: Visualizer?,
            waveform: ByteArray?,
            samplingRate: Int,
        ) = Unit

        override fun onFftDataCapture(
            visualizer: Visualizer?,
            fft: ByteArray?,
            samplingRate: Int,
        ) {
            if (fft == null || visualizer == null) return
            if (this@ExternalSpectrumAnalyzer.visualizer !== visualizer) return

            runCatching {
                publishFrame(fft, samplingRate)
            }.onFailure {
                Timber.tag(TAG).w(it, "Spectrum frame failed")
            }
        }
    }

    private fun publishFrame(fft: ByteArray, samplingRate: Int) {
        val magnitudes = SpectrumBandMapper.magnitudes(fft)
        val rawBands = SpectrumBandMapper.mapToBands(
            magnitudes = magnitudes,
            sampleRateHz = normalizeSampleRate(samplingRate),
            barCount = barCount,
        )
        if (rawBands.isEmpty()) return

        var framePeak = 0f
        for (value in rawBands) if (value > framePeak) framePeak = value

        if (BuildConfig.DEBUG) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastCallbackLogAtMs >= 1_000L) {
                lastCallbackLogAtMs = now
                var magnitudeMax = 0f
                var magnitudeSum = 0f
                for (value in magnitudes) {
                    if (value > magnitudeMax) magnitudeMax = value
                    magnitudeSum += value
                }
                val rawHead = buildString {
                    for (index in 0 until minOf(16, fft.size)) {
                        if (index > 0) append(',')
                        append(fft[index].toInt() and 0xFF)
                    }
                }
                Timber.tag(TAG).d(
                    "FFT %d bytes, rate %d, raw=[%s], magMax %.3f, magAvg %.3f, peak %.3f, bass %.3f",
                    fft.size,
                    samplingRate,
                    rawHead,
                    magnitudeMax,
                    magnitudeSum / magnitudes.size.coerceAtLeast(1),
                    framePeak,
                    SpectrumBandMapper.bassEnergy(rawBands),
                )
            }
        }

        if (framePeak < 0.01f) {
            agcPeak = (agcPeak * 0.985f).coerceAtLeast(0.02f)
        } else {
            agcPeak = if (framePeak > agcPeak) framePeak else (agcPeak * 0.985f).coerceAtLeast(0.02f)
        }

        val gain = (1f / agcPeak).coerceIn(1f, 4f) * sensitivity
        val bands = FloatArray(rawBands.size) { index ->
            (rawBands[index] * gain).coerceIn(0f, 1f)
        }

        _frame.value = SpectrumFrame(
            bars = bands,
            bass = SpectrumBandMapper.bassEnergy(bands),
        )
    }

    private fun normalizeSampleRate(value: Int): Int = when {
        value <= 0 -> DEFAULT_SAMPLE_RATE_HZ
        value > 192_000 -> value / 1_000
        else -> value
    }
}
