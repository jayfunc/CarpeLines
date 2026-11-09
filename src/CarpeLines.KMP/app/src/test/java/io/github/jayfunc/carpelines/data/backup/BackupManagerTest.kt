package io.github.jayfunc.carpelines.data.backup

import android.content.Context
import android.net.Uri
import io.github.jayfunc.carpelines.data.backup.format.BackupReader
import io.github.jayfunc.carpelines.data.backup.format.BackupFormatDetector
import io.github.jayfunc.carpelines.data.backup.format.BackupWriter
import io.github.jayfunc.carpelines.data.backup.history.BackupHistoryRepository
import io.github.jayfunc.carpelines.data.backup.model.BackupManifest
import io.github.jayfunc.carpelines.data.backup.model.BackupModuleInfo
import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.backup.model.BackupValidationResult
import io.github.jayfunc.carpelines.data.backup.model.DeviceInfo
import io.github.jayfunc.carpelines.data.backup.model.ModuleRestoreDetail
import io.github.jayfunc.carpelines.data.backup.model.RestorePlan
import io.github.jayfunc.carpelines.data.backup.model.ValidationError
import io.github.jayfunc.carpelines.data.backup.module.BackupModuleHandler
import io.github.jayfunc.carpelines.data.backup.restore.RestoreExecutor
import io.github.jayfunc.carpelines.data.backup.restore.RestorePlanner
import io.github.jayfunc.carpelines.data.backup.validation.ValidationPipeline
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupManagerTest {

    private val context: Context = mockk(relaxed = true)
    private val backupWriter: BackupWriter = mockk()
    private val backupReader: BackupReader = mockk()
    private val restorePlanner: RestorePlanner = mockk()
    private val restoreExecutor: RestoreExecutor = mockk()
    private val validationPipeline: ValidationPipeline = mockk()
    private val backupHistoryRepository: BackupHistoryRepository = mockk(relaxed = true)

    private val manager = BackupManager(
        context = context,
        backupWriter = backupWriter,
        backupReader = backupReader,
        restorePlanner = restorePlanner,
        restoreExecutor = restoreExecutor,
        validationPipeline = validationPipeline,
        backupHistoryRepository = backupHistoryRepository,
        handlers = emptyMap<BackupSection, BackupModuleHandler>()
    )

    private val backupUri: Uri = mockk(relaxed = true)

    init {
        coEvery { backupReader.detectFormat(backupUri) } returns Result.success(
            BackupFormatDetector.Format.PXPL_V3_ZIP
        )
        every { context.getString(any<Int>()) } answers {
            when (firstArg<Int>()) {
                BackupSection.ENGAGEMENT_STATS.labelRes -> "Engagement Stats"
                else -> "text"
            }
        }
    }

    @Test
    fun `inspectBackup surfaces file and module warnings in the restore plan`() = runTest {
        val plan = restorePlan(selectedModules = setOf(BackupSection.ENGAGEMENT_STATS))

        every { validationPipeline.validateFile(backupUri) } returns BackupValidationResult.Invalid(
            listOf(
                ValidationError(
                    code = "FILE_EXTENSION",
                    message = "File extension is not .pxpl.",
                    severity = io.github.jayfunc.carpelines.data.backup.model.Severity.WARNING
                )
            )
        )
        coEvery { restorePlanner.buildRestorePlan(backupUri) } returns Result.success(plan)
        every { validationPipeline.validateManifest(plan.manifest) } returns BackupValidationResult.Valid
        coEvery {
            backupReader.readModulePayload(backupUri, BackupSection.ENGAGEMENT_STATS.key)
        } returns Result.success("""[{"playCount": 1}]""")
        every {
            validationPipeline.validateModulePayload(
                BackupSection.ENGAGEMENT_STATS,
                """[{"playCount": 1}]""",
                plan.manifest
            )
        } returns BackupValidationResult.Invalid(
            listOf(
                ValidationError(
                    code = "MISSING_SONG_ID",
                    message = "EngagementStats[0]: missing songId",
                    module = BackupSection.ENGAGEMENT_STATS.key,
                    severity = io.github.jayfunc.carpelines.data.backup.model.Severity.WARNING
                )
            )
        )

        val result = manager.inspectBackup(backupUri).getOrThrow()

        assertEquals(
            listOf(
                "File extension is not .pxpl.",
                "Engagement Stats: EngagementStats[0]: missing songId"
            ),
            result.warnings
        )
    }

    @Test
    fun `inspectBackup fails when the manifest lists a module without payload`() = runTest {
        val plan = restorePlan(selectedModules = setOf(BackupSection.LYRICS))

        every { validationPipeline.validateFile(backupUri) } returns BackupValidationResult.Valid
        coEvery { restorePlanner.buildRestorePlan(backupUri) } returns Result.success(plan)
        every { validationPipeline.validateManifest(plan.manifest) } returns BackupValidationResult.Valid
        coEvery {
            backupReader.readModulePayload(backupUri, BackupSection.LYRICS.key)
        } returns Result.failure(IllegalArgumentException("Module 'lyrics' not found in backup"))

        val result = manager.inspectBackup(backupUri)

        assertTrue(result.isFailure)
        assertEquals(
            "Module 'lyrics' not found in backup",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `inspectBackup never previews modules the planner dropped for size`() = runTest {
        // RestorePlanner removes payloads above the in-memory limit from the plan, so the
        // inspector must not try to load them into memory either.
        val oversizedModule = BackupReader.MAX_MODULE_PAYLOAD_BYTES + 1L
        val warning = "Playback History: payload exceeds the restore safety limit"
        val plan = restorePlan(
            selectedModules = setOf(BackupSection.PLAYBACK_HISTORY),
            moduleSizeBytes = oversizedModule
        ).copy(
            availableModules = emptySet(),
            selectedModules = emptySet(),
            moduleDetails = emptyMap(),
            warnings = listOf(warning)
        )

        every { validationPipeline.validateFile(backupUri) } returns BackupValidationResult.Valid
        coEvery { restorePlanner.buildRestorePlan(backupUri) } returns Result.success(plan)
        every { validationPipeline.validateManifest(plan.manifest) } returns BackupValidationResult.Valid

        val result = manager.inspectBackup(backupUri).getOrThrow()

        assertTrue(result.availableModules.isEmpty())
        assertTrue(result.warnings.contains(warning))
        coVerify(exactly = 0) { backupReader.readModulePayload(any(), any()) }
    }

    private fun restorePlan(
        selectedModules: Set<BackupSection>,
        moduleSizeBytes: Long = 32
    ): RestorePlan {
        val modules = selectedModules.associate { section ->
            section.key to BackupModuleInfo(
                checksum = "sha256:test",
                entryCount = 1,
                sizeBytes = moduleSizeBytes
            )
        }
        return RestorePlan(
            manifest = BackupManifest(
                schemaVersion = BackupManifest.CURRENT_SCHEMA_VERSION,
                appVersion = "test",
                appVersionCode = 1,
                createdAt = 1_700_000_000_000,
                deviceInfo = DeviceInfo(),
                modules = modules
            ),
            backupUri = "content://carpelines/test-backup",
            availableModules = selectedModules,
            selectedModules = selectedModules,
            moduleDetails = selectedModules.associateWith {
                ModuleRestoreDetail(
                    entryCount = 1,
                    sizeBytes = moduleSizeBytes,
                    willOverwrite = true
                )
            }
        )
    }
}
