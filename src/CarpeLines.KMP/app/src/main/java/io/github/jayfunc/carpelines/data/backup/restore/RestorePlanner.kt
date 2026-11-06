package io.github.jayfunc.carpelines.data.backup.restore

import android.net.Uri
import io.github.jayfunc.carpelines.data.backup.format.BackupReader
import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.backup.model.ModuleRestoreDetail
import io.github.jayfunc.carpelines.data.backup.model.RestorePlan
import io.github.jayfunc.carpelines.data.backup.module.BackupModuleHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestorePlanner @Inject constructor(
    private val backupReader: BackupReader,
    private val handlers: Map<BackupSection, @JvmSuppressWildcards BackupModuleHandler>
) {
    /**
     * Reads the backup manifest and builds a RestorePlan showing what modules
     * are available, their sizes, and what will be overwritten.
     */
    suspend fun buildRestorePlan(uri: Uri): Result<RestorePlan> = withContext(Dispatchers.IO) {
        runCatching {
            val manifest = backupReader.readManifest(uri).getOrThrow()

            val availableModules = manifest.modules.keys.mapNotNull { key ->
                BackupSection.fromKey(key)
            }.toSet()

            val moduleDetails = mutableMapOf<BackupSection, ModuleRestoreDetail>()
            availableModules.forEach { section ->
                val info = manifest.modules[section.key]
                moduleDetails[section] = ModuleRestoreDetail(
                    entryCount = info?.entryCount ?: 0,
                    sizeBytes = info?.sizeBytes ?: 0,
                    willOverwrite = true
                )
            }

            val warnings = mutableListOf<String>()
            if (manifest.schemaVersion < 3) {
                warnings.add("This is a legacy backup (v${manifest.schemaVersion}). Some new modules may not be available.")
            }

            RestorePlan(
                manifest = manifest,
                backupUri = uri.toString(),
                availableModules = availableModules,
                selectedModules = availableModules,
                moduleDetails = moduleDetails,
                warnings = warnings
            )
        }
    }
}
