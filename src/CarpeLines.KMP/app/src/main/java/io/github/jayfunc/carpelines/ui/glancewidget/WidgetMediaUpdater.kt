package io.github.jayfunc.carpelines.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.compose.ui.graphics.toArgb
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.jayfunc.carpelines.data.carpelines.ExternalLyricsResolver
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import io.github.jayfunc.carpelines.data.external.ExternalMediaState
import io.github.jayfunc.carpelines.data.model.lineAt
import io.github.jayfunc.carpelines.ui.theme.extractSeedColor
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
    private const val ARTWORK_SEED_PX = 96

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private val lyricsInFlight = ConcurrentHashMap.newKeySet<String>()
    private val artworkSeedCache = ConcurrentHashMap<String, Int>()

    @Volatile
    private var lyricsResolver: ExternalLyricsResolver? = null

    private var lastState: WidgetMediaState? = null
    private var lastWriteAtMs = 0L

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LyricsEntryPoint {
        fun externalLyricsResolver(): ExternalLyricsResolver
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
            previous.artworkUri != state.artworkUri ||
            previous.lyricLine != state.lyricLine ||
            previous.lyricTranslation != state.lyricTranslation ||
            previous.artworkSeedArgb != state.artworkSeedArgb
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
        val line = lyricsResolver(context)
            ?.peek(media)
            ?.lineAt(position)
        val secondary = line?.translation ?: line?.romanization ?: line?.nextText

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
            artworkSeedArgb = resolveArtworkSeed(context, media),
            lyricLine = line?.text.orEmpty(),
            lyricTranslation = secondary.orEmpty(),
        )
    }

    /**
     * The M3E widgets tone themselves with the cover palette. The seed is extracted once per
     * track; without artwork the state keeps 0 and the layouts fall back to the system theme.
     */
    private fun resolveArtworkSeed(context: Context, media: ExternalMediaState): Int {
        // Keyed by the artwork source as well: players often publish the new track's title before
        // its cover, and an identity-only key would pin the widget to "no artwork" for the track.
        val cacheKey = buildString {
            append(media.identity)
            append('|')
            append(media.artworkUri?.hashCode() ?: 0)
            append('|')
            append(media.artworkBitmap?.hashCode() ?: 0)
        }
        artworkSeedCache[cacheKey]?.let { return it }
        val bitmap = media.artworkBitmap
            ?: media.artworkUri?.let { uri ->
                decodeWidgetAlbumArtBitmap(
                    context = context,
                    rawUri = uri,
                    targetWidthPx = ARTWORK_SEED_PX,
                    targetHeightPx = ARTWORK_SEED_PX,
                )
            }
        val seed = bitmap?.let { artwork ->
            runCatching { extractSeedColor(artwork).toArgb() }.getOrDefault(0)
        } ?: 0
        artworkSeedCache[cacheKey] = seed
        if (artworkSeedCache.size > 32) artworkSeedCache.clear()
        return seed
    }

    private fun lyricsResolver(context: Context): ExternalLyricsResolver? =
        lyricsResolver ?: runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                LyricsEntryPoint::class.java,
            ).externalLyricsResolver()
        }.getOrNull()?.also { lyricsResolver = it }

    private fun ensureLyrics(context: Context, media: ExternalMediaState) {
        val resolver = lyricsResolver(context) ?: return
        if (resolver.isResolved(media) || !lyricsInFlight.add(media.identity)) return

        scope.launch {
            runCatching {
                withTimeoutOrNull(LYRICS_TIMEOUT_MS) {
                    resolver.resolve(media)
                }
            }.onFailure {
                Timber.tag("WidgetMediaUpdater").d(it, "Widget lyrics lookup failed")
            }

            lyricsInFlight.remove(media.identity)
            lastState = null
            refresh(context)
        }
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
