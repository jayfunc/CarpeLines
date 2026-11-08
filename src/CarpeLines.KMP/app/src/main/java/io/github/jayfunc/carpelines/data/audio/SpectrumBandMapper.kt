package io.github.jayfunc.carpelines.data.audio

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Turns a [android.media.audiofx.Visualizer] FFT capture into the normalized bar values the
 * spectrum overlay draws.
 *
 * The capture is a signed 8-bit, mono, real/imaginary FFT whose byte layout is
 * `[R0, R(n/2), R1, I1, R2, I2, ...]`: the first and last points carry only a real part, every
 * other point interleaves the two components. The frequency of bin `k` is `k * sampleRate / n`.
 *
 * The bands are logarithmic because a linear split spends almost every bar on the top octave,
 * where music has little energy. A mild high-frequency compensation curve keeps the upper bands
 * visible, mirroring the curve BetterLyrics applies to its WASAPI magnitudes.
 */
internal object SpectrumBandMapper {

    private const val MIN_FREQUENCY_HZ = 40f
    private const val MAX_FREQUENCY_HZ = 16_000f

    private val COMPENSATION_FREQUENCIES = floatArrayOf(
        20f, 50f, 100f, 200f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f, 20_000f,
    )
    private val COMPENSATION_GAINS = floatArrayOf(
        1f, 1.05f, 1.1f, 1.15f, 1.25f, 1.35f, 1.6f, 2.2f, 3.0f, 4.0f, 4.5f,
    )

    /** Magnitude per FFT bin, normalized to 0..1. */
    fun magnitudes(fft: ByteArray): FloatArray {
        val n = fft.size
        if (n < 4) return FloatArray(0)

        val bins = n / 2 + 1
        val result = FloatArray(bins)
        result[0] = abs(signedValue(fft[0]))
        result[bins - 1] = abs(signedValue(fft[1]))
        for (k in 1 until bins - 1) {
            val real = signedValue(fft[2 * k])
            val imaginary = signedValue(fft[2 * k + 1])
            result[k] = sqrt(real * real + imaginary * imaginary).coerceAtMost(1f)
        }
        return result
    }

    /**
     * Maps [magnitudes] onto [barCount] logarithmically spaced bands.
     *
     * @param sampleRateHz sampling rate in Hz (not milliHertz).
     */
    fun mapToBands(magnitudes: FloatArray, sampleRateHz: Int, barCount: Int): FloatArray {
        if (barCount <= 0) return FloatArray(0)
        if (magnitudes.size <= 2) return FloatArray(barCount)

        val n = (magnitudes.size - 1) * 2
        val sampleRate = sampleRateHz.takeIf { it > 0 } ?: 48_000
        val ratio = (MAX_FREQUENCY_HZ / MIN_FREQUENCY_HZ).toDouble()
        val lastBin = magnitudes.lastIndex

        return FloatArray(barCount) { index ->
            val lowFrequency = MIN_FREQUENCY_HZ *
                ratio.pow(index.toDouble() / barCount).toFloat()
            val highFrequency = MIN_FREQUENCY_HZ *
                ratio.pow((index + 1).toDouble() / barCount).toFloat()

            val lowBin = ceil(lowFrequency * n / sampleRate).toInt().coerceIn(1, lastBin)
            val highBin = floor(highFrequency * n / sampleRate).toInt().coerceIn(lowBin, lastBin)

            var peak = 0f
            for (bin in lowBin..highBin) {
                if (magnitudes[bin] > peak) peak = magnitudes[bin]
            }

            val centerFrequency = (lowFrequency + highFrequency) / 2f
            val compensated = (peak * compensation(centerFrequency)).coerceIn(0f, 1f)
            // Linear mapping matches BetterLyrics' renderer and keeps the bars readable; the
            // previous-decibel mapping compressed most of the Visualizer output against full scale.
            if (compensated < 0.02f) 0f else compensated
        }
    }

    /** Average of the lowest three bands, used by the bass-breathing effect. */
    fun bassEnergy(bands: FloatArray): Float {
        if (bands.isEmpty()) return 0f
        val count = minOf(3, bands.size)
        var sum = 0f
        for (index in 0 until count) sum += bands[index]
        return (sum / count).coerceIn(0f, 1f)
    }

    private fun signedValue(value: Byte): Float =
        value.toInt() / 127f

    private fun compensation(frequency: Float): Float {
        if (frequency <= COMPENSATION_FREQUENCIES.first()) return COMPENSATION_GAINS.first()
        if (frequency >= COMPENSATION_FREQUENCIES.last()) return COMPENSATION_GAINS.last()

        var index = 0
        while (frequency > COMPENSATION_FREQUENCIES[index + 1]) index++

        val x1 = COMPENSATION_FREQUENCIES[index]
        val y1 = COMPENSATION_GAINS[index]
        val x2 = COMPENSATION_FREQUENCIES[index + 1]
        val y2 = COMPENSATION_GAINS[index + 1]
        return y1 + (frequency - x1) * ((y2 - y1) / (x2 - x1))
    }
}
