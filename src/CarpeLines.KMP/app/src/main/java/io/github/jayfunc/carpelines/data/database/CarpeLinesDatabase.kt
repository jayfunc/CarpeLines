package io.github.jayfunc.carpelines.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AlbumArtThemeEntity::class,
        SearchHistoryEntity::class,
        SongEntity::class,
        SongSearchFtsEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        SongArtistCrossRef::class,
        SongEngagementEntity::class,
        LyricsEntity::class,
        NavidromeSongEntity::class,
        NavidromePlaylistEntity::class,
        JellyfinSongEntity::class,
        JellyfinPlaylistEntity::class,
        ListenBrainzPendingListenEntity::class,
        AudioBookmarkEntity::class,
        OfflineTrackEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class CarpeLinesDatabase : RoomDatabase() {
    abstract fun albumArtThemeDao(): AlbumArtThemeDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun musicDao(): MusicDao
    abstract fun engagementDao(): EngagementDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun navidromeDao(): NavidromeDao
    abstract fun jellyfinDao(): JellyfinDao
    abstract fun listenBrainzDao(): ListenBrainzDao
    abstract fun audioBookmarkDao(): AudioBookmarkDao
    abstract fun offlineTrackDao(): OfflineTrackDao

    companion object {
        private fun createSongsSearchVirtualTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                    CREATE VIRTUAL TABLE IF NOT EXISTS songs_fts
                    USING fts4(
                        title,
                        artist_name,
                        tokenize=unicode61
                    )
                """.trimIndent()
            )
        }

        fun installSongsSearchSyncTriggers(db: SupportSQLiteDatabase) {
            createSongsSearchVirtualTable(db)

            db.execSQL("DROP TRIGGER IF EXISTS trg_songs_fts_insert")
            db.execSQL("DROP TRIGGER IF EXISTS trg_songs_fts_update")
            db.execSQL("DROP TRIGGER IF EXISTS trg_songs_fts_delete")

            db.execSQL(
                """
                    CREATE TRIGGER IF NOT EXISTS trg_songs_fts_insert
                    AFTER INSERT ON songs
                    BEGIN
                        INSERT INTO songs_fts(rowid, title, artist_name)
                        VALUES (NEW.id, NEW.title, NEW.artist_name);
                    END
                """.trimIndent()
            )

            db.execSQL(
                """
                    CREATE TRIGGER IF NOT EXISTS trg_songs_fts_update
                    AFTER UPDATE ON songs
                    BEGIN
                        DELETE FROM songs_fts WHERE rowid = OLD.id;
                        INSERT INTO songs_fts(rowid, title, artist_name)
                        VALUES (NEW.id, NEW.title, NEW.artist_name);
                    END
                """.trimIndent()
            )

            db.execSQL(
                """
                    CREATE TRIGGER IF NOT EXISTS trg_songs_fts_delete
                    AFTER DELETE ON songs
                    BEGIN
                        DELETE FROM songs_fts WHERE rowid = OLD.id;
                    END
                """.trimIndent()
            )
        }

        fun createRuntimeArtifactsCallback(): RoomDatabase.Callback {
            return object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    installSongsSearchSyncTriggers(db)
                }
            }
        }
    }
}
