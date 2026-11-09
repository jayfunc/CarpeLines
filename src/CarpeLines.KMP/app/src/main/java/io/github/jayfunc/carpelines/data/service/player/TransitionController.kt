package io.github.jayfunc.carpelines.data.service.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import io.github.jayfunc.carpelines.data.media.TrackBpmRepository
import io.github.jayfunc.carpelines.data.model.CrossfadeSettings
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the crossfade between consecutive songs by observing the player state and
 * commanding the DualPlayerEngine.
 *
 * The crossfade is a global playback setting (enabled + duration + smart mode); per-playlist
 * and per-pair rules no longer exist.
 */
@OptIn(UnstableApi::class)
@Singleton
class TransitionController @Inject constructor(
    private val engine: DualPlayerEngine,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val trackBpmRepository: TrackBpmRepository,
) {
    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var transitionListener: Player.Listener? = null
    private var transitionSchedulerJob: Job? = null
    private var currentObservedPlayer: Player? = null

    private val swapListener: (Player) -> Unit = { newPlayer ->
        Timber.tag("TransitionDebug").d("Controller detected player swap. Moving listener.")
        transitionListener?.let { listener ->
            currentObservedPlayer?.removeListener(listener)
            currentObservedPlayer = newPlayer
            newPlayer.addListener(listener)

            if (newPlayer.isPlaying) {
                val item = newPlayer.currentMediaItem
                if (item != null) {
                    scope.launch {
                        delay(1_000L)
                        if (currentObservedPlayer === newPlayer &&
                            newPlayer.currentMediaItem?.mediaId == item.mediaId
                        ) {
                            scheduleTransitionFor(item)
                        }
                    }
                }
            }
        }
    }

    /**
     * Attaches the controller to the player engine to start listening for state changes.
     */
    fun initialize() {
        if (transitionListener != null) return

        Timber.tag("TransitionDebug").d("Initializing TransitionController...")

        if (scope.coroutineContext[Job]?.isActive != true) {
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        transitionListener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Timber.tag("TransitionDebug").d("onMediaItemTransition: %s (reason=%d)", mediaItem?.mediaId, reason)
                engine.setPauseAtEndOfMediaItems(shouldPause = false)

                mediaItem?.let { scheduleTransitionFor(it) }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val job = transitionSchedulerJob
                if (isPlaying && (job == null || job.isCompleted)) {
                    Timber.tag("TransitionDebug").d("Playback resumed. Checking if transition needs scheduling.")
                    engine.masterPlayer.currentMediaItem?.let { scheduleTransitionFor(it) }
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    Timber.tag("TransitionDebug").d("Timeline changed (reason=%d). Cancelling pending transition.", reason)
                    transitionSchedulerJob?.cancel()
                    engine.cancelNext()

                    engine.masterPlayer.currentMediaItem?.let { scheduleTransitionFor(it) }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                Timber.tag("TransitionDebug").d("Repeat mode changed to %d. Rescheduling transition.", repeatMode)
                transitionSchedulerJob?.cancel()
                engine.cancelNext()
                engine.masterPlayer.currentMediaItem?.let { scheduleTransitionFor(it) }
            }
        }

        currentObservedPlayer = engine.masterPlayer
        currentObservedPlayer?.addListener(transitionListener!!)
        engine.addPlayerSwapListener(swapListener)

        // A sleep timer that stops at the end of the track must not crossfade into the next
        // one; once it clears, the current track gets its transition back. A crossfade that
        // is already running is left alone: the swap reschedules for the incoming track.
        scope.launch {
            engine.sleepTimerHold
                .map { it.suppressCrossfade }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    if (engine.isTransitionRunning()) return@collect
                    engine.masterPlayer.currentMediaItem?.let { scheduleTransitionFor(it) }
                }
        }
    }

    private fun scheduleTransitionFor(currentMediaItem: MediaItem) {
        transitionSchedulerJob?.cancel()
        engine.setPauseAtEndOfMediaItems(shouldPause = false)

        if (engine.sleepTimerHold.value.suppressCrossfade) {
            Timber.tag("TransitionDebug").d("Sleep timer holds playback at track end. No transition.")
            if (!engine.isTransitionRunning()) engine.cancelNext()
            return
        }

        transitionSchedulerJob = scope.launch {
            if (engine.isTransitionRunning()) {
                Timber.tag("TransitionDebug").d("Cancelling active transition to schedule next...")
                engine.cancelNext()
            }

            delay(1500)

            val player = engine.masterPlayer
            val repeatMode = player.repeatMode
            val transitionTarget = engine.getNextTransitionTarget(currentMediaItem, repeatMode)

            if (transitionTarget == null) {
                Timber.tag("TransitionDebug").d(
                    "No next track (currentIndex=%d, count=%d, repeatMode=%d). No transition.",
                    player.currentMediaItemIndex,
                    player.mediaItemCount,
                    repeatMode,
                )
                engine.cancelNext()
                return@launch
            }

            val nextMediaItem = transitionTarget.mediaItem

            combine(
                userPreferencesRepository.crossfadeDurationFlow,
                userPreferencesRepository.isCrossfadeEnabledFlow,
                userPreferencesRepository.smartCrossfadeEnabledFlow,
            ) { durationMs, isEnabled, isSmartEnabled ->
                Triple(durationMs, isEnabled, isSmartEnabled)
            }.distinctUntilChanged()
            .collectLatest { (durationMs, isEnabled, isSmartEnabled) ->
                if (!isEnabled) {
                    Timber.tag("TransitionDebug").d("Crossfade globally disabled. Using default gap.")
                    engine.cancelNext()
                    engine.setPauseAtEndOfMediaItems(shouldPause = false)
                    return@collectLatest
                }

                if (durationMs <= 0) {
                    Timber.tag("TransitionDebug").d("Crossfade duration is zero.")
                    engine.cancelNext()
                    engine.setPauseAtEndOfMediaItems(shouldPause = false)
                    return@collectLatest
                }

                val settings = CrossfadeSettings(durationMs = durationMs)

                // Smart crossfade: tag-embedded tempo for both sides of the transition
                // (cached in-memory after the first read — never decodes audio).
                val outgoingBpm = if (isSmartEnabled) trackBpmRepository.bpmFor(currentMediaItem) else null
                val incomingBpm = if (isSmartEnabled) trackBpmRepository.bpmFor(nextMediaItem) else null

                Timber.tag("TransitionDebug").d("Preparing next track for overlap: %s", nextMediaItem.mediaId)
                engine.prepareNext(transitionTarget)

                var duration = player.duration
                while ((duration == C.TIME_UNSET || duration <= 0) && isActive) {
                    delay(500)
                    duration = player.duration
                    Timber.tag("TransitionDebug").v("Waiting for duration... (%d)", duration)
                }

                if (!isActive) return@collectLatest

                val minFade = 500L
                val guardWindow = 150L

                if (duration < minFade + guardWindow) {
                    Timber.tag("TransitionDebug").w("Track too short for crossfade (duration=%d).", duration)
                    engine.cancelNext()
                    engine.setPauseAtEndOfMediaItems(false)
                    return@collectLatest
                }

                val smartPlan = if (isSmartEnabled) {
                    SmartCrossfadePlanner.plan(
                        base = settings,
                        trackDurationMs = duration,
                        outgoingBpm = outgoingBpm,
                        incomingBpm = incomingBpm,
                        minFadeMs = minFade,
                        guardWindowMs = guardWindow,
                    )
                } else {
                    null
                }

                val maxFadeDuration = (duration - guardWindow).coerceAtLeast(minFade)
                val effectiveSettings = smartPlan?.settings ?: settings
                val effectiveDuration = effectiveSettings.durationMs.toLong()
                    .coerceAtLeast(minFade)
                    .coerceAtMost(maxFadeDuration)

                val transitionPoint = smartPlan?.transitionPointMs ?: (duration - effectiveDuration)

                Timber.tag("TransitionDebug").d(
                    "Scheduled crossfade at %d ms (SongDur: %d). Fade duration: %d ms (smart=%s)",
                    transitionPoint, duration, effectiveDuration, smartPlan?.compatibility
                )

                engine.setPauseAtEndOfMediaItems(shouldPause = true)
                Timber.tag("TransitionDebug").d("Enabled pauseAtEndOfMediaItems to prevent auto-skip.")

                if (transitionPoint <= player.currentPosition) {
                    val remaining = (duration - player.currentPosition).coerceAtLeast(0L)
                    if (remaining > 0L) {
                        val adjustedDuration = remaining.coerceAtMost(effectiveDuration)
                        Timber.tag("TransitionDebug").w("Already past transition point! Triggering immediately.")
                        engine.performTransition(effectiveSettings.copy(durationMs = adjustedDuration.toInt()))
                    } else {
                        Timber.tag("TransitionDebug").w("Too close to end (%d ms left). Skipping to avoid glitch.", remaining)
                        engine.cancelNext()
                        engine.setPauseAtEndOfMediaItems(shouldPause = false)
                    }
                    return@collectLatest
                }

                while (player.currentPosition < transitionPoint && isActive) {
                    val remaining = transitionPoint - player.currentPosition
                    val sleep = when {
                        remaining > 5000 -> 1000L
                        remaining > 1000 -> 250L
                        else -> 50L
                    }.coerceAtMost(remaining).coerceAtLeast(1L)
                    if (remaining < 2000 && remaining % 500 < 50) {
                        Timber.tag("TransitionDebug").v("Countdown: %d ms to transition", remaining)
                    }
                    delay(sleep)
                }

                if (isActive) {
                    val remaining = (duration - player.currentPosition).coerceAtLeast(0L)
                    if (remaining > 0L) {
                        val adjustedDuration = remaining.coerceAtMost(effectiveDuration)
                        Timber.tag("TransitionDebug").d("FIRING TRANSITION NOW!")
                        engine.performTransition(effectiveSettings.copy(durationMs = adjustedDuration.toInt()))
                    } else {
                        Timber.tag("TransitionDebug").w("Too close to end (%d ms left). Skipping to avoid glitch.", remaining)
                        engine.cancelNext()
                        engine.setPauseAtEndOfMediaItems(shouldPause = false)
                    }
                } else {
                    Timber.tag("TransitionDebug").d("Job cancelled before firing.")
                    engine.setPauseAtEndOfMediaItems(shouldPause = false)
                }
            }
        }
    }

    /**
     * Cleans up resources and listeners.
     */
    fun release() {
        Timber.tag("TransitionDebug").d("Releasing controller.")
        transitionSchedulerJob?.cancel()
        engine.removePlayerSwapListener(swapListener)
        transitionListener?.let { currentObservedPlayer?.removeListener(it) }
        transitionListener = null
        currentObservedPlayer = null
        scope.cancel()
    }
}
