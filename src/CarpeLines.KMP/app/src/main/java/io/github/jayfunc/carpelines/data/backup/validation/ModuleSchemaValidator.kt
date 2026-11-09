package io.github.jayfunc.carpelines.data.backup.validation

import com.google.gson.JsonParser
import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.backup.model.BackupValidationResult
import io.github.jayfunc.carpelines.data.backup.model.Severity
import io.github.jayfunc.carpelines.data.backup.model.ValidationError
import io.github.jayfunc.carpelines.data.carpelines.ManualLyricsStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-module payload checks.
 *
 * Everything here reads JSON defensively: a payload field of the wrong type is a warning about
 * that entry, never an exception, so one malformed entry cannot make the whole backup unreadable.
 */
@Singleton
class ModuleSchemaValidator @Inject constructor() {
    private data class NumericFieldResult(
        val present: Boolean,
        val value: Long?
    )

    companion object {
        const val MAX_ENTRIES_PER_MODULE = 100_000
        private const val MAX_ARTIST_IMAGE_URL_LENGTH = 2_000
        private val VALID_PREF_TYPES = setOf("string", "int", "long", "boolean", "float", "double", "string_set")
    }

    fun validate(section: BackupSection, payload: String): BackupValidationResult {
        val errors = mutableListOf<ValidationError>()

        val jsonElement = try {
            JsonParser.parseString(payload)
        } catch (e: Exception) {
            errors.add(ValidationError("INVALID_JSON", "Module '${section.key}' contains invalid JSON.", module = section.key))
            return BackupValidationResult.Invalid(errors)
        }

        if (!jsonElement.isJsonArray) {
            errors.add(ValidationError("NOT_ARRAY", "Module '${section.key}' should be a JSON array.", module = section.key))
            return BackupValidationResult.Invalid(errors)
        }
        val array = jsonElement.asJsonArray
        if (array.size() > MAX_ENTRIES_PER_MODULE) {
            errors.add(ValidationError("TOO_MANY_ENTRIES", "Module '${section.key}' has ${array.size()} entries (max $MAX_ENTRIES_PER_MODULE).", module = section.key))
            return BackupValidationResult.Invalid(errors)
        }

        when (section) {
            BackupSection.LYRICS -> validateLyrics(array, errors)
            BackupSection.ENGAGEMENT_STATS -> validateEngagementStats(array, errors)
            BackupSection.PLAYBACK_HISTORY -> validatePlaybackHistory(array, errors)
            BackupSection.ARTIST_IMAGES -> validateArtistImages(array, errors)
            BackupSection.GLOBAL_SETTINGS -> validatePreferenceEntries(jsonElement, section.key, errors)
        }

        return if (errors.isNotEmpty()) {
            BackupValidationResult.Invalid(errors)
        } else {
            BackupValidationResult.Valid
        }
    }

    private fun validateLyrics(array: com.google.gson.JsonArray, errors: MutableList<ValidationError>) {
        val isLegacyPayload = array.any { element ->
            element.isJsonObject &&
                element.asJsonObject.has("songId") &&
                !element.asJsonObject.has("selection")
        }
        if (isLegacyPayload) {
            errors.add(
                ValidationError(
                    "LEGACY_LYRICS_FORMAT",
                    "Lyrics payload uses the legacy format and will be skipped.",
                    module = "lyrics",
                    severity = Severity.WARNING
                )
            )
            return
        }

        array.forEachIndexed { i, element ->
            if (!element.isJsonObject) {
                errors.add(
                    ValidationError(
                        "INVALID_LYRICS_ENTRY",
                        "Lyrics[$i]: entry is not a JSON object",
                        module = "lyrics",
                        severity = Severity.WARNING
                    )
                )
                return@forEachIndexed
            }
            val obj = element.asJsonObject
            val key = obj.get("key")
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString
            if (key == null || !ManualLyricsStore.isValidKey(key)) {
                errors.add(
                    ValidationError(
                        "INVALID_LYRICS_KEY",
                        "Lyrics[$i]: invalid track key",
                        module = "lyrics",
                        severity = Severity.WARNING
                    )
                )
            }
            val selection = obj.get("selection")
            if (selection == null || !selection.isJsonObject) {
                errors.add(
                    ValidationError(
                        "MISSING_LYRICS_SELECTION",
                        "Lyrics[$i]: missing selection",
                        module = "lyrics",
                        severity = Severity.WARNING
                    )
                )
                return@forEachIndexed
            }
            val document = selection.asJsonObject.get("document")
            if (document == null || !document.isJsonObject) {
                errors.add(
                    ValidationError(
                        "MISSING_LYRICS_DOCUMENT",
                        "Lyrics[$i]: missing lyrics document",
                        module = "lyrics",
                        severity = Severity.WARNING
                    )
                )
            }
        }
    }

    private fun validateEngagementStats(array: com.google.gson.JsonArray, errors: MutableList<ValidationError>) {
        val seenSongIds = mutableSetOf<String>()
        array.forEachIndexed { i, element ->
            if (!element.isJsonObject) {
                errors.add(
                    ValidationError(
                        "INVALID_ENGAGEMENT_ENTRY",
                        "EngagementStats[$i]: entry is not a JSON object",
                        module = "engagement_stats",
                        severity = Severity.WARNING
                    )
                )
                return@forEachIndexed
            }
            val obj = element.asJsonObject
            val songId = readStringField(obj, "songId", "song_id")?.trim()
            if (songId.isNullOrEmpty()) {
                errors.add(
                    ValidationError(
                        "MISSING_SONG_ID",
                        "EngagementStats[$i]: missing songId",
                        module = "engagement_stats",
                        severity = Severity.WARNING
                    )
                )
            } else if (!seenSongIds.add(songId)) {
                errors.add(
                    ValidationError(
                        "DUPLICATE_SONG_ID",
                        "EngagementStats[$i]: duplicate songId '$songId'",
                        module = "engagement_stats",
                        severity = Severity.WARNING
                    )
                )
            }

            val playCount = readNumericField(obj, "play_count", "playCount", "score", "plays")
            if (playCount.present && playCount.value == null) {
                errors.add(
                    ValidationError(
                        "INVALID_PLAY_COUNT",
                        "EngagementStats[$i]: play count is not numeric",
                        module = "engagement_stats",
                        severity = Severity.WARNING
                    )
                )
            } else if ((playCount.value ?: 0L) < 0L) {
                errors.add(
                    ValidationError(
                        "NEGATIVE_PLAY_COUNT",
                        "EngagementStats[$i]: negative play count",
                        module = "engagement_stats",
                        severity = Severity.WARNING
                    )
                )
            }

            val totalDuration = readNumericField(
                obj,
                "total_play_duration_ms",
                "totalPlayDurationMs",
                "totalDuration",
                "total_duration",
                "durationMs",
                "duration_ms"
            )
            if (totalDuration.present && totalDuration.value == null) {
                errors.add(
                    ValidationError(
                        "INVALID_TOTAL_DURATION",
                        "EngagementStats[$i]: total duration is not numeric",
                        module = "engagement_stats",
                        severity = Severity.WARNING
                    )
                )
            } else if ((totalDuration.value ?: 0L) < 0L) {
                errors.add(
                    ValidationError(
                        "NEGATIVE_TOTAL_DURATION",
                        "EngagementStats[$i]: negative total duration",
                        module = "engagement_stats",
                        severity = Severity.WARNING
                    )
                )
            }

            val lastPlayed = readNumericField(
                obj,
                "last_played_timestamp",
                "lastPlayedTimestamp",
                "lastPlayedAt",
                "last_played_at",
                "timestamp"
            )
            if (lastPlayed.present && lastPlayed.value == null) {
                errors.add(
                    ValidationError(
                        "INVALID_LAST_PLAYED_TIMESTAMP",
                        "EngagementStats[$i]: last played timestamp is not numeric",
                        module = "engagement_stats",
                        severity = Severity.WARNING
                    )
                )
            } else if ((lastPlayed.value ?: 0L) < 0L) {
                errors.add(
                    ValidationError(
                        "NEGATIVE_LAST_PLAYED_TIMESTAMP",
                        "EngagementStats[$i]: negative last played timestamp",
                        module = "engagement_stats",
                        severity = Severity.WARNING
                    )
                )
            }
        }
    }

    private fun validatePlaybackHistory(array: com.google.gson.JsonArray, errors: MutableList<ValidationError>) {
        array.forEachIndexed { i, element ->
            if (!element.isJsonObject) {
                errors.add(
                    ValidationError(
                        "INVALID_HISTORY_ENTRY",
                        "PlaybackHistory[$i]: entry is not a JSON object",
                        module = "playback_history",
                        severity = Severity.WARNING
                    )
                )
                return@forEachIndexed
            }
            val obj = element.asJsonObject
            if (readStringField(obj, "songId", "song_id")?.trim().isNullOrEmpty()) {
                errors.add(
                    ValidationError(
                        "MISSING_SONG_ID",
                        "PlaybackHistory[$i]: missing songId",
                        module = "playback_history",
                        severity = Severity.WARNING
                    )
                )
            }

            val duration = readNumericField(obj, "durationMs", "duration_ms")
            when {
                duration.present && duration.value == null -> errors.add(
                    ValidationError(
                        "INVALID_DURATION",
                        "PlaybackHistory[$i]: duration is not numeric",
                        module = "playback_history",
                        severity = Severity.WARNING
                    )
                )
                (duration.value ?: 0L) < 0L -> errors.add(
                    ValidationError(
                        "NEGATIVE_DURATION",
                        "PlaybackHistory[$i]: negative duration",
                        module = "playback_history",
                        severity = Severity.WARNING
                    )
                )
            }

            val timestamp = readNumericField(obj, "timestamp")
            if (timestamp.present && timestamp.value == null) {
                errors.add(
                    ValidationError(
                        "INVALID_TIMESTAMP",
                        "PlaybackHistory[$i]: timestamp is not numeric",
                        module = "playback_history",
                        severity = Severity.WARNING
                    )
                )
            }
        }
    }

    private fun validateArtistImages(array: com.google.gson.JsonArray, errors: MutableList<ValidationError>) {
        array.forEachIndexed { i, element ->
            if (!element.isJsonObject) {
                errors.add(
                    ValidationError(
                        "INVALID_ARTIST_IMAGE_ENTRY",
                        "ArtistImages[$i]: entry is not a JSON object",
                        module = "artist_images",
                        severity = Severity.WARNING
                    )
                )
                return@forEachIndexed
            }
            val obj = element.asJsonObject
            if (readStringField(obj, "artistName", "artist_name")?.trim().isNullOrEmpty()) {
                errors.add(
                    ValidationError(
                        "MISSING_ARTIST_NAME",
                        "ArtistImages[$i]: missing artist name",
                        module = "artist_images",
                        severity = Severity.WARNING
                    )
                )
            }
            val urlElement = obj.get("imageUrl") ?: obj.get("image_url")
            if (urlElement != null && !urlElement.isJsonPrimitive) {
                errors.add(
                    ValidationError(
                        "INVALID_URL",
                        "ArtistImages[$i]: URL is not text and will be ignored",
                        module = "artist_images",
                        severity = Severity.WARNING
                    )
                )
            }
            val imageUrl = readStringField(obj, "imageUrl", "image_url").orEmpty()
            if (imageUrl.isNotEmpty() && !imageUrl.startsWith("https://")) {
                errors.add(
                    ValidationError(
                        "INSECURE_URL",
                        "ArtistImages[$i]: URL is not HTTPS",
                        module = "artist_images",
                        severity = Severity.WARNING
                    )
                )
            }
            if (imageUrl.length > MAX_ARTIST_IMAGE_URL_LENGTH) {
                errors.add(
                    ValidationError(
                        "URL_TOO_LONG",
                        "ArtistImages[$i]: URL exceeds $MAX_ARTIST_IMAGE_URL_LENGTH chars",
                        module = "artist_images",
                        severity = Severity.WARNING
                    )
                )
            }
        }
    }

    private fun validatePreferenceEntries(jsonElement: com.google.gson.JsonElement, moduleKey: String, errors: MutableList<ValidationError>) {
        jsonElement.asJsonArray.forEachIndexed { i, element ->
            if (!element.isJsonObject) return@forEachIndexed
            val obj = element.asJsonObject
            val key = readStringField(obj, "key")
            val type = readStringField(obj, "type")
            if (key.isNullOrBlank()) {
                errors.add(ValidationError("MISSING_PREF_KEY", "Preference[$i]: missing key", module = moduleKey, severity = Severity.WARNING))
            }
            if (type == null || type !in VALID_PREF_TYPES) {
                errors.add(ValidationError("INVALID_PREF_TYPE", "Preference[$i]: invalid type '$type'", module = moduleKey, severity = Severity.WARNING))
            }
        }
    }

    private fun readStringField(obj: com.google.gson.JsonObject, vararg keys: String): String? {
        return keys.asSequence()
            .mapNotNull { key ->
                obj.get(key)
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
            }
            .firstOrNull()
    }

    private fun readNumericField(obj: com.google.gson.JsonObject, vararg keys: String): NumericFieldResult {
        keys.forEach { key ->
            val element = obj.get(key) ?: return@forEach
            val primitive = element.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
                ?: return NumericFieldResult(present = true, value = null)
            return when {
                primitive.isNumber -> NumericFieldResult(present = true, value = primitive.asNumber.toLong())
                primitive.isString -> NumericFieldResult(present = true, value = primitive.asString.toLongOrNull())
                else -> NumericFieldResult(present = true, value = null)
            }
        }
        return NumericFieldResult(present = false, value = null)
    }
}
