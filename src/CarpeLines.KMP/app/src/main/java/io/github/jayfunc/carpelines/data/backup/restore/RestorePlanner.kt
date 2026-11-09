package io.github.jayfunc.carpelines.data.backup.restore

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.backup.format.BackupReader
import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.backup.model.ModuleRestoreDetail
import io.github.jayfunc.carpelines.data.backup.model.RestorePlan
import io.github.jayfunc.carpelines.data.backup.validation.ContentSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestorePlanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupReader: BackupReader,
    private val contentSanitizer: ContentSanitizer
) {

    /**
     * Reads the backup manifest and builds a RestorePlan showing what modules are available, their
     * sizes, and whether restoring them overwrites the current data.
     *
     * Modules the restore path refuses to load (payloads over the in-memory safety limit) are left
     * out of the plan instead of failing after the user has already chosen them.
     */
    suspend fun buildRestorePlan(uri: Uri): Result<RestorePlan> = withContext(Dispatchers.IO) {
        runCatching {
            val manifest = backupReader.readManifest(uri).getOrThrow()

            // Module keys reach handlers and zip entry names, so only plain module keys count.
            val availableModules = manifest.modules.keys
                .filter { contentSanitizer.isValidModuleKey(it) }
                .mapNotNull { key -> BackupSection.fromKey(key) }
                .toSet()

            val moduleDetails = mutableMapOf<BackupSection, ModuleRestoreDetail>()
            val oversizedModules = mutableListOf<BackupSection>()
            availableModules.forEach { section ->
                val info = manifest.modules[section.key]
                val sizeBytes = info?.sizeBytes ?: 0L
                if (sizeBytes > BackupReader.MAX_MODULE_PAYLOAD_BYTES) {
                    oversizedModules += section
                    return@forEach
                }
                moduleDetails[section] = ModuleRestoreDetail(
                    entryCount = info?.entryCount ?: 0,
                    sizeBytes = sizeBytes
                )
            }
            val restorableModules = availableModules - oversizedModules.toSet()

            val warnings = mutableListOf<String>()
            if (manifest.schemaVersion < 3) {
                warnings += context.getString(
                    R.string.backup_plan_warning_legacy_schema,
                    manifest.schemaVersion
                )
            }
            oversizedModules.sortedBy { it.key }.forEach { section ->
                val sizeMb = (manifest.modules[section.key]?.sizeBytes ?: 0L) / (1024 * 1024)
                warnings += context.getString(
                    R.string.backup_error_payload_too_large,
                    context.getString(section.labelRes),
                    sizeMb,
                    BackupReader.MAX_MODULE_PAYLOAD_BYTES / (1024 * 1024)
                )
            }

            RestorePlan(
                manifest = manifest,
                backupUri = uri.toString(),
                availableModules = restorableModules,
                selectedModules = restorableModules,
                moduleDetails = moduleDetails.filterKeys { it in restorableModules },
                warnings = warnings
            )
        }
    }
}
