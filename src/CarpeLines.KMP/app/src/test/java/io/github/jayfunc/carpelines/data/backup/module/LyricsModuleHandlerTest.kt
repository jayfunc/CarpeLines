package io.github.jayfunc.carpelines.data.backup.module

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.github.jayfunc.carpelines.data.carpelines.ManualLyricsStore
import io.github.jayfunc.carpelines.data.carpelines.RawManualLyrics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsModuleHandlerTest {

    private val store: ManualLyricsStore = mockk(relaxed = true)
    private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
    private val handler = LyricsModuleHandler(store, gson)

    @Test
    fun `export wraps stored selections with their keys`() = runTest {
        coEvery { store.exportRaw() } returns listOf(
            RawManualLyrics(
                key = "0123456789abcdef",
                json = """{"document":{"lines":[]},"mappedTitle":"A"}"""
            )
        )

        val array = JsonParser.parseString(handler.export()).asJsonArray

        assertEquals(1, array.size())
        val entry = array[0].asJsonObject
        assertEquals("0123456789abcdef", entry.get("key").asString)
        assertEquals("A", entry.getAsJsonObject("selection").get("mappedTitle").asString)
    }

    @Test
    fun `restore replaces the store with backed-up selections`() = runTest {
        val captured = slot<List<RawManualLyrics>>()
        coEvery { store.replaceRaw(capture(captured)) } returns Unit

        handler.restore(
            """[{"key":"0123456789abcdef","selection":{"document":{"lines":[]}}}]"""
        )

        assertEquals(listOf("0123456789abcdef"), captured.captured.map { it.key })
        assertTrue(captured.captured.single().json.contains("\"document\""))
    }

    @Test
    fun `legacy payload leaves manual selections untouched`() = runTest {
        handler.restore("""[{"songId":123,"content":"la la"}]""")

        coVerify(exactly = 0) { store.replaceRaw(any()) }
    }
}
