package io.github.jayfunc.carpelines.data.backup.format

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyPayloadAdapterTest {

    private val adapter = LegacyPayloadAdapter()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    @Test
    fun `adapts v2 backup with playlists folded into globalSettings`() {
        val json = """
        {
            "formatVersion": 2,
            "exportedAtEpochMs": 1700000000000,
            "availableSections": ["playlists", "global_settings"],
            "playlists": [
                {"key": "user_playlists_json_v1", "type": "string", "stringValue": "[]"}
            ],
            "globalSettings": [
                {"key": "app_theme", "type": "string", "stringValue": "dark"}
            ]
        }
        """.trimIndent()

        val (manifest, modules) = adapter.adapt(json, gson)

        assertEquals(2, manifest.schemaVersion)
        assertEquals(1700000000000, manifest.createdAt)
        assertEquals(1, modules.size)
        assertTrue(modules.containsKey("global_settings"))
        assertTrue(modules["global_settings"]!!.contains("user_playlists_json_v1"))
        assertTrue(modules["global_settings"]!!.contains("app_theme"))
    }

    @Test
    fun `adapts v1 backup with combined preferences field`() {
        val json = """
        {
            "formatVersion": 1,
            "exportedAtEpochMs": 1600000000000,
            "availableSections": ["playlists", "global_settings"],
            "preferences": [
                {"key": "user_playlists_json_v1", "type": "string", "stringValue": "[]"},
                {"key": "app_theme", "type": "string", "stringValue": "dark"},
                {"key": "crossfade_duration", "type": "int", "intValue": 6000}
            ]
        }
        """.trimIndent()

        val (manifest, modules) = adapter.adapt(json, gson)

        assertEquals(1, manifest.schemaVersion)
        assertEquals(1, modules.size)
        assertTrue(modules.containsKey("global_settings"))
        assertTrue(modules["global_settings"]!!.contains("user_playlists_json_v1"))
        assertTrue(modules["global_settings"]!!.contains("crossfade_duration"))
    }

    @Test
    fun `skips modules not in availableSections`() {
        val json = """
        {
            "formatVersion": 2,
            "exportedAtEpochMs": 1700000000000,
            "availableSections": ["playback_history"],
            "playlists": [
                {"key": "user_playlists_json_v1", "type": "string", "stringValue": "[]"}
            ],
            "playbackHistory": [
                {"songId": "123", "timestamp": 1700000000000, "durationMs": 1000}
            ]
        }
        """.trimIndent()

        val (_, modules) = adapter.adapt(json, gson)

        assertEquals(1, modules.size)
        assertTrue(modules.containsKey("playback_history"))
    }

    @Test
    fun `generates checksums in manifest module info`() {
        val json = """
        {
            "formatVersion": 2,
            "exportedAtEpochMs": 1700000000000,
            "availableSections": ["playback_history"],
            "playbackHistory": [
                {"songId": "123", "timestamp": 1700000000000, "durationMs": 1000}
            ]
        }
        """.trimIndent()

        val (manifest, _) = adapter.adapt(json, gson)

        val historyInfo = manifest.modules["playback_history"]
        assertTrue(historyInfo != null)
        assertTrue(historyInfo!!.checksum.startsWith("sha256:"))
        assertTrue(historyInfo.entryCount > 0)
        assertTrue(historyInfo.sizeBytes > 0)
    }

    @Test
    fun `handles empty backup gracefully`() {
        val json = """
        {
            "formatVersion": 2,
            "exportedAtEpochMs": 1700000000000,
            "availableSections": []
        }
        """.trimIndent()

        val (manifest, modules) = adapter.adapt(json, gson)

        assertEquals(2, manifest.schemaVersion)
        assertTrue(modules.isEmpty())
    }
}
