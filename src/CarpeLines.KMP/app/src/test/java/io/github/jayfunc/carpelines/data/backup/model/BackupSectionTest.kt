package io.github.jayfunc.carpelines.data.backup.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackupSectionTest {

    @Test
    fun `all sections have unique keys`() {
        val keys = BackupSection.entries.map { it.key }
        assertEquals(keys.size, keys.distinct().size, "BackupSection keys must be unique")
    }

    @Test
    fun `all sections have unique label resources`() {
        val labels = BackupSection.entries.map { it.labelRes }
        assertEquals(labels.size, labels.distinct().size, "BackupSection labels must be unique")
    }

    @Test
    fun `fromKey returns correct section for known keys`() {
        assertEquals(BackupSection.GLOBAL_SETTINGS, BackupSection.fromKey("global_settings"))
        assertEquals(BackupSection.LYRICS, BackupSection.fromKey("lyrics"))
        assertEquals(BackupSection.ENGAGEMENT_STATS, BackupSection.fromKey("engagement_stats"))
        assertEquals(BackupSection.PLAYBACK_HISTORY, BackupSection.fromKey("playback_history"))
    }

    @Test
    fun `fromKey returns null for unknown key`() {
        assertNull(BackupSection.fromKey("nonexistent"))
        assertNull(BackupSection.fromKey(""))
    }

    @Test
    fun `defaultSelection contains all sections`() {
        assertEquals(BackupSection.entries.toSet(), BackupSection.defaultSelection)
    }

    @Test
    fun `there are exactly 4 backup sections`() {
        assertEquals(4, BackupSection.entries.size)
    }

    @Test
    fun `all sections have an icon`() {
        BackupSection.entries.forEach { section ->
            assertTrue(section.iconRes != 0, "${section.key} should have an icon resource")
        }
    }

    @Test
    fun `all sections have label and description resources`() {
        BackupSection.entries.forEach { section ->
            assertTrue(section.labelRes != 0, "${section.key} should have a label resource")
            assertTrue(section.descriptionRes != 0, "${section.key} should have a description resource")
        }
    }
}
