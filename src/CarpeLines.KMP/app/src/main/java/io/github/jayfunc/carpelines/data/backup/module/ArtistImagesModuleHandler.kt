package io.github.jayfunc.carpelines.data.backup.module

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jayfunc.carpelines.data.backup.model.ArtistImageBackupEntry
import io.github.jayfunc.carpelines.data.backup.validation.ContentSanitizer
import io.github.jayfunc.carpelines.data.database.MusicDao
import io.github.jayfunc.carpelines.di.BackupGson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Artist artwork: the remote image URL plus the image the user picked themselves, inlined as
 * base64 so it survives a reinstall (the file it points at does not).
 *
 * Restore replaces the artwork of every artist the backup knows: an artist whose entry carries an
 * image URL but no custom image has its stale local custom image cleared, because that custom
 * image would otherwise keep hiding the restored URL. Artists absent from the backup are left
 * untouched.
 */
@Singleton
class ArtistImagesModuleHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
    private val contentSanitizer: ContentSanitizer,
    @BackupGson private val gson: Gson
) : BackupModuleHandler {

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val artists = musicDao.getAllArtistsListRaw()
        val entries = artists
            .mapNotNull { artist ->
                val imageUrl = artist.imageUrl?.takeIf { it.isNotBlank() }
                val customImageBase64 = artist.customImageUri
                    ?.takeIf { it.isNotBlank() }
                    ?.let { readFileAsBase64(it) }
                if (imageUrl == null && customImageBase64 == null) return@mapNotNull null
                ArtistImageBackupEntry(
                    artistName = artist.name,
                    imageUrl = imageUrl ?: "",
                    customImageBase64 = customImageBase64
                )
            }
        gson.toJson(entries)
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val type = TypeToken.getParameterized(List::class.java, ArtistImageBackupEntry::class.java).type
        val entries: List<ArtistImageBackupEntry> = gson.fromJson(payload, type) ?: emptyList()
        if (entries.isEmpty()) return@withContext

        val artists = musicDao.getAllArtistsListRaw()
        if (artists.isEmpty()) {
            throw ModuleRestoreSkippedException(
                "the music library has not been scanned, so artist images cannot be matched"
            )
        }
        val idByExactName = artists.associate { it.name to it.id }
        val idByNormalizedName = artists.associate { it.name.trim().lowercase() to it.id }

        var unmatched = 0
        var applied = 0
        entries.forEach { entry ->
            val artistId = idByExactName[entry.artistName]
                ?: idByNormalizedName[entry.artistName.trim().lowercase()]
            // A non-http(s) URL can only come from a hand-edited or tampered backup; the schema
            // validator warns about it, and writing it would leave the artist with a dead image.
            val imageUrl = contentSanitizer.sanitizeUrl(entry.imageUrl)
            val customBase64 = entry.customImageBase64
            if (artistId == null || (imageUrl.isEmpty() && customBase64 == null)) {
                unmatched++
                return@forEach
            }

            if (imageUrl.isNotEmpty()) {
                musicDao.updateArtistImageUrl(artistId, imageUrl)
            }
            val written = when {
                customBase64 != null -> writeCustomImage(artistId, customBase64)
                else -> {
                    clearCustomImage(artistId)
                    true
                }
            }
            if (written) applied++ else unmatched++
        }

        if (applied == 0) {
            throw ModuleRestoreSkippedException(
                "none of the ${entries.size} artist image entries matched an artist in this library"
            )
        }
        if (unmatched > 0) {
            Timber.tag(TAG).w("$unmatched of ${entries.size} artist image entries matched no artist")
        }
    }

    /**
     * restore() replaces artwork for artists in the snapshot and clears it for artists absent from
     * it, so replaying the snapshot undoes exactly what a failed restore may have written.
     */
    override suspend fun rollback(snapshot: String): Unit = withContext(Dispatchers.IO) {
        val type = TypeToken.getParameterized(List::class.java, ArtistImageBackupEntry::class.java).type
        val entries: List<ArtistImageBackupEntry> = gson.fromJson(snapshot, type) ?: emptyList()
        val snapshotByName = entries.associateBy { it.artistName.trim().lowercase() }

        musicDao.getAllArtistsListRaw().forEach { artist ->
            val entry = snapshotByName[artist.name.trim().lowercase()]
            musicDao.updateArtistImageUrl(
                artist.id,
                entry?.imageUrl?.let { contentSanitizer.sanitizeUrl(it) }.orEmpty()
            )

            val customBase64 = entry?.customImageBase64
            if (customBase64 != null) {
                writeCustomImage(artist.id, customBase64)
            } else {
                clearCustomImage(artist.id)
            }
        }
    }

    private suspend fun writeCustomImage(artistId: Long, base64: String): Boolean {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val file = customImageFile(artistId)
            file.writeBytes(bytes)
            musicDao.updateArtistCustomImage(artistId, file.absolutePath)
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to restore custom image for artist id=$artistId")
            false
        }
    }

    private suspend fun clearCustomImage(artistId: Long) {
        runCatching { customImageFile(artistId).delete() }
        musicDao.updateArtistCustomImage(artistId, null)
    }

    private fun customImageFile(artistId: Long): File =
        File(context.filesDir, "artist_art_$artistId.jpg")

    private fun readFileAsBase64(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists() || file.length() == 0L) return null
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to read artist image: $path")
            null
        }
    }

    companion object {
        private const val TAG = "ArtistImagesHandler"
    }
}
