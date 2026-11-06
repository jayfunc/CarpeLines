package io.github.jayfunc.carpelines.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.jayfunc.carpelines.data.betterlyrics.BetterLyricsRepository
import io.github.jayfunc.carpelines.data.carpelines.ManualLyricsStore
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import io.github.jayfunc.carpelines.data.external.ExternalMediaState
import io.github.jayfunc.carpelines.data.model.Lyrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Writes the current system media session into every placed widget.
 *
 * Lyrics are resolved once per track and cached in memory; the widget only ever receives the
 * line that matches the published position, so its layout can stay a plain Glance tree.
 */
object WidgetMediaUpdater {

    private const val MIN_PROGRESS_UPDATE_MS = 2_000L
    private const val LYRICS_TIMEOUT_MS = 8_000L
    private const val ARTWORK_MAX_BYTES = 256 * 1024

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private val lyricsCache = ConcurrentHashMap<String, CachedLyrics>()
    private val lyricsInFlight = ConcurrentHashMap.newKeySet<String>()

    private var lastState: WidgetMediaState? = null
    private var lastWriteAtMs = 0L

    private data class CachedLyrics(val lyrics: Lyrics?)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LyricsEntryPoint {
        fun betterLyricsRepository(): BetterLyricsRepository
        fun manualLyricsStore(): ManualLyricsStore
    }

    fun onPlaybackChanged(
        context: Context,
        media: ExternalMediaState?,
        positionMs: Long,
    ) {
        val appContext = context.applicationContext
        scope.launch {
            val state = buildState(appContext, media, positionMs)
            val now = SystemClock.elapsedRealtime()
            if (!shouldWrite(state, now)) return@launch

            writeMutex.withLock {
                if (!shouldWrite(state, now)) return@withLock
                lastState = state
                lastWriteAtMs = now
                val widgetCount = writeState(appContext, state)
                if (widgetCount > 0 && media != null && media.hasMetadata) {
                    ensureLyrics(appContext, media)
                }
            }
        }
    }

    fun refresh(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val state = buildState(
                appContext,
                ExternalMediaMonitor.state.value,
                ExternalMediaMonitor.positionMs.value,
            )
            writeMutex.withLock {
                lastState = state
                lastWriteAtMs = SystemClock.elapsedRealtime()
                writeState(appContext, state)
            }
        }
    }

    private fun shouldWrite(state: WidgetMediaState, now: Long): Boolean {
        val previous = lastState
        if (previous == null) return true
        if (
            previous.hasSession != state.hasSession ||
            previous.title != state.title ||
            previous.artist != state.artist ||
            previous.isPlaying != state.isPlaying ||
            previous.lyricLine != state.lyricLine ||
            previous.lyricTranslation != state.lyricTranslation
        ) {
            return true
        }
        return state.isPlaying &&
            state.durationMs > 0L &&
            now - lastWriteAtMs >= MIN_PROGRESS_UPDATE_MS
    }

    private suspend fun writeState(context: Context, state: WidgetMediaState): Int {
        val manager = GlanceAppWidgetManager(context)
        var count = 0
        manager.getGlanceIds(BarWidget4x1::class.java).forEach { glanceId ->
            updateAppWidgetState(context, WidgetMediaStateDefinition, glanceId) { state }
            BarWidget4x1().update(context, glanceId)
            count++
        }
        manager.getGlanceIds(ControlWidget4x2::class.java).forEach { glanceId ->
            updateAppWidgetState(context, WidgetMediaStateDefinition, glanceId) { state }
            ControlWidget4x2().update(context, glanceId)
            count++
        }
        manager.getGlanceIds(GridWidget2x2::class.java).forEach { glanceId ->
            updateAppWidgetState(context, WidgetMediaStateDefinition, glanceId) { state }
            GridWidget2x2().update(context, glanceId)
            count++
        }
        return count
    }

    private fun buildState(
        context: Context,
        media: ExternalMediaState?,
        positionMs: Long,
    ): WidgetMediaState {
        if (media == null || !media.hasMetadata) return WidgetMediaState()

        val duration = media.durationMs?.coerceAtLeast(0L) ?: 0L
        val position = positionMs.coerceIn(0L, duration.takeIf { it > 0L } ?: Long.MAX_VALUE)
        val lyrics = lyricsCache[media.identity]?.lyrics
        val line = lyrics?.let { currentLine(it, position) }

        return WidgetMediaState(
            hasSession = true,
            title = media.title,
            artist = media.artist,
            album = media.album,
            appLabel = media.appLabel,
            isPlaying = media.isPlaying,
            positionMs = position,
            durationMs = duration,
            artworkUri = media.artworkUri ?: media.artworkBitmap?.let {
                persistArtwork(context, it, media.identity)
            },
            lyricLine = line?.first.orEmpty(),
            lyricTranslation = line?.second.orEmpty(),
        )
    }

    private fun ensureLyrics(context: Context, media: ExternalMediaState) {
        val key = media.identity
        if (lyricsCache.containsKey(key) || !lyricsInFlight.add(key)) return

        scope.launch {
            val lyrics = runCatching {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    LyricsEntryPoint::class.java,
                )
                val manual = entryPoint.manualLyricsStore()
                    .load(media.title, media.artist, media.album)
                    ?.takeIf { !it.document.isEmpty }
                if (manual != null) {
                    entryPoint.betterLyricsRepository().toPlayerLyrics(manual.document)
                } else {
                    withTimeoutOrNull(LYRICS_TIMEOUT_MS) {
                        entryPoint.betterLyricsRepository()
                            .load(media.toLyricsQuery())
                            ?.takeIf { !it.isEmpty }
                            ?.let { entryPoint.betterLyricsRepository().toPlayerLyrics(it) }
                    }
                }
            }.onFailure {
                Timber.tag("WidgetMediaUpdater").d(it, "Widget lyrics lookup failed")
            }.getOrNull()

            lyricsCache[key] = CachedLyrics(lyrics)
            if (lyricsCache.size > 32) lyricsCache.clear()
            lyricsInFlight.remove(key)
            lastState = null
            refresh(context)
        }
    }

    private fun currentLine(lyrics: Lyrics, positionMs: Long): Pair<String, String>? {
        val synced = lyrics.synced
            ?.filter { it.line.isNotBlank() }
            .orEmpty()
        if (synced.isEmpty()) {
            val plain = lyrics.plain
                ?.firstOrNull { it.isNotBlank() }
                ?: return null
            return plain to ""
        }
        val index = synced.indexOfLast { it.time <= positionMs }.coerceAtLeast(0)
        val line = synced.getOrNull(index) ?: return null
        return line.line to line.translation.orEmpty()
    }

    private fun persistArtwork(context: Context, bitmap: Bitmap, identity: String): String? {
        val file = File(context.cacheDir, "widget_artwork_${identity.hashCode()}.jpg")
        if (file.isFile && file.length() > 0) return Uri.fromFile(file).toString()
        return runCatching {
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)
            }
            if (file.length() in 1..ARTWORK_MAX_BYTES.toLong()) {
                Uri.fromFile(file).toString()
            } else {
                file.delete()
                null
            }
        }.getOrNull()
    }
}
