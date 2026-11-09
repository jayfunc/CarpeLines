package io.github.jayfunc.carpelines.data.backup.restore

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.backup.format.BackupReader
import io.github.jayfunc.carpelines.data.backup.model.BackupOperationType
import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.backup.model.BackupTransferProgressUpdate
import io.github.jayfunc.carpelines.data.backup.model.BackupValidationResult
import io.github.jayfunc.carpelines.data.backup.model.RestorePlan
import io.github.jayfunc.carpelines.data.backup.model.RestoreResult
import io.github.jayfunc.carpelines.data.backup.module.BackupModuleHandler
import io.github.jayfunc.carpelines.data.backup.module.ModuleRestoreSkippedException
import io.github.jayfunc.carpelines.data.backup.validation.ValidationPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestoreExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupReader: BackupReader,
    private val validationPipeline: ValidationPipeline,
    private val handlers: Map<BackupSection, @JvmSuppressWildcards BackupModuleHandler>
) {
    companion object {
        private const val TAG = "RestoreExecutor"
    }

    suspend fun execute(
        uri: Uri,
        plan: RestorePlan,
        onProgress: (BackupTransferProgressUpdate) -> Unit
    ): RestoreResult = withContext(Dispatchers.IO) {
        val warnings = mutableListOf<String>()
        val selectedModules = plan.selectedModules.toList().sortedBy { it.key }
        val totalSteps = selectedModules.size * 2 + 3
        var step = 0

        reportProgress(
            onProgress, ++step, totalSteps,
            context.getString(R.string.backup_progress_snapshotting),
            context.getString(R.string.backup_progress_snapshotting_detail)
        )

        val snapshots = mutableMapOf<BackupSection, String>()
        try {
            selectedModules.forEach { section ->
                val handler = handlers[section]
                if (handler != null) {
                    snapshots[section] = handler.snapshot()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Snapshot phase failed")
            return@withContext RestoreResult.TotalFailure(
                context.getString(R.string.backup_error_snapshot_failed, e.message.orEmpty())
            )
        }

        reportProgress(
            onProgress, ++step, totalSteps,
            context.getString(R.string.backup_progress_preparing_restore),
            context.getString(R.string.backup_progress_preparing_restore_detail)
        )

        val restoredModules = mutableListOf<BackupSection>()
        var currentSection: BackupSection? = null
        try {
            selectedModules.forEach { section ->
                currentSection = section
                val moduleInfo = plan.manifest.modules[section.key]
                if (moduleInfo != null &&
                    moduleInfo.sizeBytes > BackupReader.MAX_MODULE_PAYLOAD_BYTES
                ) {
                    throw IllegalStateException(
                        context.getString(
                            R.string.backup_error_payload_too_large,
                            context.getString(section.labelRes),
                            moduleInfo.sizeBytes / (1024 * 1024),
                            BackupReader.MAX_MODULE_PAYLOAD_BYTES / (1024 * 1024)
                        )
                    )
                }

                val payload = backupReader.readModulePayload(uri, section.key).getOrThrow()

                val validationResult = validationPipeline.validateModulePayload(
                    section, payload, plan.manifest
                )
                if (validationResult is BackupValidationResult.Invalid && validationResult.fatalErrors.isNotEmpty()) {
                    throw IllegalStateException(
                        context.getString(
                            R.string.backup_error_module_validation,
                            context.getString(section.labelRes),
                            validationResult.fatalErrors.first().message
                        )
                    )
                }

                reportProgress(
                    onProgress, ++step, totalSteps,
                    context.getString(
                        R.string.backup_progress_validated_section,
                        context.getString(section.labelRes)
                    ),
                    context.getString(section.descriptionRes),
                    section
                )

                reportProgress(
                    onProgress, ++step, totalSteps,
                    context.getString(
                        R.string.backup_progress_restoring_section,
                        context.getString(section.labelRes)
                    ),
                    context.getString(section.descriptionRes),
                    section
                )

                val handler = handlers[section]
                    ?: throw IllegalStateException("No handler for module ${section.key}")
                try {
                    handler.restore(payload)
                    restoredModules.add(section)
                } catch (skipped: ModuleRestoreSkippedException) {
                    // The payload is fine but this library cannot take it (legacy format, or the
                    // library has not been scanned yet). Nothing was written, so no rollback.
                    Timber.tag(TAG).w("Skipped ${section.key}: ${skipped.message}")
                    warnings += context.getString(
                        R.string.backup_module_skipped_warning,
                        context.getString(section.labelRes),
                        skipped.message.orEmpty()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Restore failed while processing backup module, rolling back")
            var rollbackSuccess = true
            val rollbackOrder = (restoredModules + listOfNotNull(currentSection)).distinct().asReversed()
            rollbackOrder.forEach { section ->
                try {
                    val snapshot = snapshots[section]
                    if (snapshot != null) {
                        handlers[section]?.rollback(snapshot)
                    }
                } catch (rollbackError: Exception) {
                    Timber.tag(TAG).e(rollbackError, "Rollback failed for ${section.key}")
                    rollbackSuccess = false
                }
            }

            val failedReason = e.message ?: "Unknown error"
            val failedSection = currentSection

            if (rollbackSuccess) {
                val failedLabel = failedSection?.let { context.getString(it.labelRes) }
                    ?: context.getString(R.string.backup_error_unknown_module)
                return@withContext RestoreResult.TotalFailure(
                    context.getString(
                        R.string.backup_error_restore_failed_rolled_back,
                        failedLabel,
                        failedReason
                    )
                )
            }

            return@withContext RestoreResult.PartialFailure(
                succeeded = restoredModules.toSet(),
                failed = failedSection?.let { mapOf(it to failedReason) }.orEmpty(),
                rolledBack = false
            )
        }

        reportProgress(
            onProgress, ++step, totalSteps,
            context.getString(R.string.backup_progress_restore_complete),
            context.getString(R.string.backup_progress_restore_complete_detail)
        )

        RestoreResult.Success(warnings)
    }

    private fun reportProgress(
        onProgress: (BackupTransferProgressUpdate) -> Unit,
        step: Int,
        totalSteps: Int,
        title: String,
        detail: String,
        section: BackupSection? = null
    ) {
        onProgress(
            BackupTransferProgressUpdate(
                operation = BackupOperationType.IMPORT,
                step = step,
                totalSteps = totalSteps,
                title = title,
                detail = detail,
                section = section
            )
        )
    }
}
