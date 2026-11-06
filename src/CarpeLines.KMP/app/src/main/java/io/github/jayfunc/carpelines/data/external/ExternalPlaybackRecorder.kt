package io.github.jayfunc.carpelines.data.external

import android.os.SystemClock
import io.github.jayfunc.carpelines.data.stats.PlaybackStatsRepository
import io.github.jayfunc.carpelines.presentation.viewmodel.ListeningStatsTracker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Feeds what other apps play into the listening statistics.
 *
 * CarpeLines deliberately does not play audio itself, so [MusicService]'s session tracking never
 * runs: the listening stats page was structurally empty even after hours of playback. This watcher
 * mirrors [ExternalMediaMonitor]'s session into the same event store the player used to write, and
 * keeps the per-player records the home screen needs.
 *
 * Time is measured on the wall clock while the foreign session reports itself as playing, exactly
 * like [ListeningStatsTracker] does for owned playback, so a paused track accrues nothing. Long
 * sessions are written in chunks; the repository merges contiguous events back into one segment,
 * and an unexpected process death then costs at most one chunk instead of the whole album.
 */
@Singleton
class ExternalPlaybackRecorder @Inject constructor(
    private val store: ExternalPlaybackStore,
    private val statsRepository: PlaybackStatsRepository,
    private val listeningStatsTracker: ListeningStatsTracker
) {

    private class Session(
        val identity: String,
        val songId: String,
        var accumulatedMs: Long,
        var lastTickRealtimeMs: Long,
        var wasPlaying: Boolean
    )

    private var job: Job? = null
    private var session: Session? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                runCatching { tick() }.onFailure { Timber.w(it, "External playback tick failed") }
                delay(TICK_MS)
            }
        }
    }

    /** Writes whatever has accrued so far; called when the app leaves the foreground. */
    fun flush() {
        ioScope.launch { finalizeSession() }
    }

    private suspend fun tick() {
        val media = ExternalMediaMonitor.state.value
        if (media == null || !media.hasMetadata) {
            finalizeSession()
            return
        }

        val nowRealtime = SystemClock.elapsedRealtime()
        val current = session
        if (current == null || current.identity != media.identity) {
            finalizeSession()
            // A paused track is not a play. Opening the app while the player sits paused used to
            // open a session, so the store counted a play before a single second was heard.
            if (media.isPlaying) startSession(media)
            return
        }

        if (media.isPlaying && current.wasPlaying) {
            val delta = (nowRealtime - current.lastTickRealtimeMs).coerceIn(0L, MAX_TICK_DELTA_MS)
            current.accumulatedMs += delta
        } else if (!media.isPlaying && current.wasPlaying) {
            // The user paused: bank what was heard before it can be lost to a session change.
            persistChunk(current)
        }
        current.lastTickRealtimeMs = nowRealtime
        current.wasPlaying = media.isPlaying

        if (current.accumulatedMs >= CHUNK_MS) {
            persistChunk(current)
        }
    }

    private suspend fun startSession(media: ExternalMediaState) {
        session = Session(
            identity = media.identity,
            songId = store.songIdFor(media),
            accumulatedMs = 0L,
            lastTickRealtimeMs = SystemClock.elapsedRealtime(),
            wasPlaying = media.isPlaying
        )
        runCatching { store.recordStart(media, media.artworkBitmap) }
            .onFailure { Timber.w(it, "Unable to record external player usage") }
    }

    private suspend fun finalizeSession() {
        val current = session ?: return
        persistChunk(current, force = true)
        session = null
    }

    /**
     * Writes the time accrued since the last write. [chunkStartMs] is `now - duration`, which makes
     * consecutive chunks of one track contiguous, so the aggregator joins them into a single
     * listening segment instead of inventing pauses between them.
     */
    private suspend fun persistChunk(current: Session, force: Boolean = false) {
        val duration = current.accumulatedMs
        if (duration < MIN_LISTEN_MS) {
            // A five second glance is not a play; drop it so it cannot grow a phantom segment.
            current.accumulatedMs = 0L
            return
        }
        if (!force && duration < CHUNK_MS) return
        current.accumulatedMs = 0L
        val timestamp = System.currentTimeMillis()
        runCatching {
            statsRepository.recordPlayback(
                songId = current.songId,
                durationMs = duration,
                timestamp = timestamp
            )
            listeningStatsTracker.reloadHistory()
        }.onFailure { Timber.w(it, "Unable to record external listening time") }
    }

    private companion object {
        const val TICK_MS = 1_000L

        /** A frozen process must not bank the seconds it was not actually listening. */
        const val MAX_TICK_DELTA_MS = 4_000L

        /** Long sessions are flushed periodically so a kill loses at most this much. */
        const val CHUNK_MS = 4 * 60 * 1_000L

        /** Matches the player's own threshold: a five second glance is not a play. */
        const val MIN_LISTEN_MS = 5_000L
    }
}
