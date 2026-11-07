package io.github.jayfunc.carpelines.data.backup.module

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.preferences.PreferenceBackupEntry
import io.github.jayfunc.carpelines.data.preferences.UserPreferencesRepository
import io.github.jayfunc.carpelines.di.BackupGson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalSettingsModuleHandler @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @BackupGson private val gson: Gson
) : BackupModuleHandler {

    override val section = BackupSection.GLOBAL_SETTINGS

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        gson.toJson(userPreferencesRepository.exportPreferencesForBackup())
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        userPreferencesRepository.exportPreferencesForBackup().size
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val type = TypeToken.getParameterized(List::class.java, PreferenceBackupEntry::class.java).type
        val entries: List<PreferenceBackupEntry> = gson.fromJson(payload, type)
        userPreferencesRepository.importPreferencesFromBackup(entries, clearExisting = true)
    }

    override suspend fun rollback(snapshot: String) = restore(snapshot)
}
