package io.github.jayfunc.carpelines.data.stats

import io.github.jayfunc.carpelines.data.external.ExternalPlaybackStore
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * The player filter every playback-statistics query has to apply.
 *
 * Returns null while the filter is off, so callers keep the unfiltered fast path. Legacy events
 * that predate the per-event source are attributed through the external playback snapshot's last
 * player for the track.
 */
@Singleton
class PlaybackPlayerFilterProvider @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val externalPlaybackStore: ExternalPlaybackStore,
) {

    suspend fun current(): PlaybackStatsRepository.PlaybackPlayerFilter? {
        if (!userPreferencesRepository.externalPlayerFilterEnabledFlow.first()) return null
        return PlaybackStatsRepository.PlaybackPlayerFilter(
            allowedPackages = userPreferencesRepository.externalPlayerAllowedPackagesFlow.first(),
            sourcePackageBySongId = externalPlaybackStore.sourcePackagesBySongId(),
        )
    }
}
