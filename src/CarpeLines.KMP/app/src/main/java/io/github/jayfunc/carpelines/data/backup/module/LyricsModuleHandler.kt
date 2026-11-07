package io.github.jayfunc.carpelines.data.backup.module

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.carpelines.ManualLyricsStore
import io.github.jayfunc.carpelines.data.carpelines.RawManualLyrics
import io.github.jayfunc.carpelines.di.BackupGson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs up the lyrics the user picked by hand in the BetterLyrics search.
 *
 * Embedded tags, sibling `.lrc` files, and the legacy `lyrics` table are not included: the first
 * two already live in the audio files and the table is not read by the current lyrics page.
 */
@Singleton
class LyricsModuleHandler @Inject constructor(
    private val manualLyricsStore: ManualLyricsStore,
    @BackupGson private val gson: Gson
) : BackupModuleHandler {

    override val section = BackupSection.LYRICS

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val array = JsonArray()
        manualLyricsStore.exportRaw().forEach { entry ->
            val selection = runCatching { JsonParser.parseString(entry.json) }.getOrNull()
            if (selection == null || !selection.isJsonObject) return@forEach
            array.add(
                JsonObject().apply {
                    addProperty(KEY, entry.key)
                    add(SELECTION, selection)
                }
            )
        }
        gson.toJson(array)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        manualLyricsStore.exportRaw().size
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val array = JsonParser.parseString(payload).asJsonArray
        val entries = array.filter { it.isJsonObject }.mapNotNull { element ->
            val obj = element.asJsonObject
            val key = obj.get(KEY)
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString
                ?: return@mapNotNull null
            val selection = obj.get(SELECTION)?.takeIf { it.isJsonObject }
                ?: return@mapNotNull null
            RawManualLyrics(key = key, json = gson.toJson(selection))
        }
        // Older backups stored the legacy lyric table, which the current page never reads.
        // Leave manual selections untouched rather than clearing them for an unsupported payload.
        if (entries.isEmpty() && array.size() > 0) return@withContext
        manualLyricsStore.replaceRaw(entries)
    }

    override suspend fun rollback(snapshot: String) = restore(snapshot)

    private companion object {
        const val KEY = "key"
        const val SELECTION = "selection"
    }
}
