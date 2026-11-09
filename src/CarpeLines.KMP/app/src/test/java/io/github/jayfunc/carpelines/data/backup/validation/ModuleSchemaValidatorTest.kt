package io.github.jayfunc.carpelines.data.backup.validation

import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.backup.model.BackupValidationResult
import io.github.jayfunc.carpelines.data.backup.model.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModuleSchemaValidatorTest {

    private val validator = ModuleSchemaValidator()

    @Test
    fun `valid lyrics array passes validation`() {
        val payload = """[{"key": "0123456789abcdef", "selection": {"document": {}}}]"""
        val result = validator.validate(BackupSection.LYRICS, payload)
        assertTrue(result.isValid())
    }

    @Test
    fun `legacy lyrics payload emits warning`() {
        val payload = """[{"songId": 123, "content": "la la"}]"""
        val result = validator.validate(BackupSection.LYRICS, payload)
        assertTrue(result is BackupValidationResult.Invalid)
        val warnings = (result as BackupValidationResult.Invalid).warnings
        assertTrue(warnings.any { it.code == "LEGACY_LYRICS_FORMAT" })
    }

    @Test
    fun `invalid JSON fails validation`() {
        val payload = "not valid json{"
        val result = validator.validate(BackupSection.LYRICS, payload)
        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).fatalErrors
        assertTrue(errors.any { it.code == "INVALID_JSON" })
    }

    @Test
    fun `non-array module fails validation`() {
        val payload = """{"key": "value"}"""
        val result = validator.validate(BackupSection.LYRICS, payload)
        assertTrue(result is BackupValidationResult.Invalid)
        assertTrue((result as BackupValidationResult.Invalid).fatalErrors.any { it.code == "NOT_ARRAY" })
    }

    @Test
    fun `playback history with negative duration emits warning`() {
        val payload = """[{"songId": "123", "timestamp": 1700000000000, "durationMs": -500}]"""
        val result = validator.validate(BackupSection.PLAYBACK_HISTORY, payload)
        assertTrue(result is BackupValidationResult.Invalid)
        val warnings = (result as BackupValidationResult.Invalid).warnings
        assertTrue(warnings.any { it.code == "NEGATIVE_DURATION" })
    }

    @Test
    fun `preference entries with valid types pass`() {
        val payload = """[
            {"key": "theme", "type": "string", "stringValue": "dark"},
            {"key": "count", "type": "int", "intValue": 5},
            {"key": "enabled", "type": "boolean", "booleanValue": true}
        ]"""
        val result = validator.validate(BackupSection.GLOBAL_SETTINGS, payload)
        assertTrue(result.isValid())
    }

    @Test
    fun `preference entries with invalid type emit warning`() {
        val payload = """[{"key": "theme", "type": "invalid_type", "stringValue": "dark"}]"""
        val result = validator.validate(BackupSection.GLOBAL_SETTINGS, payload)
        assertTrue(result is BackupValidationResult.Invalid)
        val warnings = (result as BackupValidationResult.Invalid).warnings
        assertTrue(warnings.any { it.code == "INVALID_PREF_TYPE" })
    }

    @Test
    fun `engagement stats with negative play count emit warning`() {
        val payload = """[{"songId": "123", "playCount": -1, "totalDuration": 0}]"""
        val result = validator.validate(BackupSection.ENGAGEMENT_STATS, payload)
        assertTrue(result is BackupValidationResult.Invalid)
        val warnings = (result as BackupValidationResult.Invalid).warnings
        assertTrue(warnings.any { it.code == "NEGATIVE_PLAY_COUNT" })
    }

    @Test
    fun `engagement stats with missing song id and invalid numbers emit warnings`() {
        val payload = """[{"playCount": "oops", "totalDuration": -5, "lastPlayedTimestamp": "bad"}]"""
        val result = validator.validate(BackupSection.ENGAGEMENT_STATS, payload)
        assertTrue(result is BackupValidationResult.Invalid)
        val warnings = (result as BackupValidationResult.Invalid).warnings
        assertTrue(warnings.any { it.code == "MISSING_SONG_ID" })
        assertTrue(warnings.any { it.code == "INVALID_PLAY_COUNT" })
        assertTrue(warnings.any { it.code == "NEGATIVE_TOTAL_DURATION" })
        assertTrue(warnings.any { it.code == "INVALID_LAST_PLAYED_TIMESTAMP" })
    }

    @Test
    fun `engagement stats with duplicate song ids emit warning`() {
        val payload = """[
            {"songId": "123", "playCount": 1},
            {"songId": "123", "playCount": 2}
        ]"""
        val result = validator.validate(BackupSection.ENGAGEMENT_STATS, payload)
        assertTrue(result is BackupValidationResult.Invalid)
        val warnings = (result as BackupValidationResult.Invalid).warnings
        assertTrue(warnings.any { it.code == "DUPLICATE_SONG_ID" })
    }

    @Test
    fun `wrong field types are warnings instead of throwing`() {
        val history = validator.validate(
            BackupSection.PLAYBACK_HISTORY,
            """[{"songId":"123","durationMs":{"bad":1}}]"""
        )
        assertTrue(history is BackupValidationResult.Invalid)
        assertTrue((history as BackupValidationResult.Invalid).warnings.any { it.code == "INVALID_DURATION" })
    }

    @Test
    fun `local music folders with valid paths pass validation`() {
        val payload = """["/storage/emulated/0/Music", "/storage/emulated/0/Download/Music"]"""
        val result = validator.validate(BackupSection.LOCAL_MUSIC_FOLDERS, payload)
        assertTrue(result.isValid())
    }

    @Test
    fun `local music folders with invalid entries emit warnings`() {
        val payload = """["/storage/emulated/0/Music", 42, "  ", null]"""
        val result = validator.validate(BackupSection.LOCAL_MUSIC_FOLDERS, payload)
        assertTrue(result is BackupValidationResult.Invalid)
        val warnings = (result as BackupValidationResult.Invalid).warnings
        assertEquals(3, warnings.count { it.code == "INVALID_LOCAL_MUSIC_FOLDER" })
    }

    @Test
    fun `empty array passes validation`() {
        val payload = "[]"
        val result = validator.validate(BackupSection.LYRICS, payload)
        assertTrue(result.isValid())
    }
}
