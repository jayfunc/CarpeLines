package io.github.jayfunc.carpelines.data.backup.module

import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.di.BackupGson
import io.github.jayfunc.carpelines.utils.LocalLibraryFolderFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs up the folders imported on the Local Music settings page.
 *
 * The paths are absolute and therefore device-specific, so this is kept out of the global
 * settings module: restoring a backup on another device can be opted out of on its own.
 */
@Singleton
class LocalMusicFoldersModuleHandler @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @BackupGson private val gson: Gson
) : BackupModuleHandler {

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        gson.toJson(userPreferencesRepository.localLibraryFoldersFlow.first().sorted())
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val array = JsonParser.parseString(payload).asJsonArray
        val folders = array
            .mapNotNull { element ->
                element
                    .takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                    ?.asString
            }
            .map { LocalLibraryFolderFilter.normalize(it) }
            .filter { it.isNotBlank() }
            .toSet()

        // A payload with entries that are all unusable cannot be applied; skipping reports it as
        // a warning and leaves the current folders untouched instead of clearing them silently.
        if (folders.isEmpty() && array.size() > 0) {
            throw ModuleRestoreSkippedException(
                "the local music folders payload is in a format this version cannot read"
            )
        }
        userPreferencesRepository.setLocalLibraryFolders(folders)
    }

    override suspend fun rollback(snapshot: String) = restore(snapshot)
}
