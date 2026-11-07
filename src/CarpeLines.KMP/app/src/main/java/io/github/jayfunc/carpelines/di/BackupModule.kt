package io.github.jayfunc.carpelines.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.jayfunc.carpelines.data.backup.format.BackupFormatDetector
import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.backup.module.ArtistImagesModuleHandler
import io.github.jayfunc.carpelines.data.backup.module.BackupModuleHandler
import io.github.jayfunc.carpelines.data.backup.module.EngagementStatsModuleHandler
import io.github.jayfunc.carpelines.data.backup.module.GlobalSettingsModuleHandler
import io.github.jayfunc.carpelines.data.backup.module.LyricsModuleHandler
import io.github.jayfunc.carpelines.data.backup.module.PlaybackHistoryModuleHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    @BackupGson
    fun provideBackupGson(): Gson {
        return GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
    }

    @Provides
    @Singleton
    fun provideBackupFormatDetector(): BackupFormatDetector {
        return BackupFormatDetector()
    }

    @Provides
    @Singleton
    fun provideModuleHandlers(
        globalSettingsHandler: GlobalSettingsModuleHandler,
        lyricsHandler: LyricsModuleHandler,
        engagementStatsHandler: EngagementStatsModuleHandler,
        playbackHistoryHandler: PlaybackHistoryModuleHandler,
        artistImagesHandler: ArtistImagesModuleHandler
    ): Map<BackupSection, BackupModuleHandler> {
        return mapOf(
            BackupSection.GLOBAL_SETTINGS to globalSettingsHandler,
            BackupSection.LYRICS to lyricsHandler,
            BackupSection.ENGAGEMENT_STATS to engagementStatsHandler,
            BackupSection.PLAYBACK_HISTORY to playbackHistoryHandler,
            BackupSection.ARTIST_IMAGES to artistImagesHandler
        )
    }
}
