package io.github.jayfunc.carpelines.presentation.viewmodel

import com.google.common.truth.Truth.assertThat
import io.github.jayfunc.carpelines.data.model.Lyrics
import io.github.jayfunc.carpelines.data.model.SyncedLine
import org.junit.jupiter.api.Test

/**
 * Regression guard for the "source says Local, page says loading" defect.
 *
 * A local payload — a stub `.lrc`, or one that only carries `[ti:]`/`[ar:]` tags — parses to a
 * document with no lines. That used to be published as a success, which left the page with a
 * provider label but no drawable lyrics, and the screen's catch-all branch spun forever.
 */
class LyricsContentStateTest {

    @Test
    fun `empty parsed payload has no content`() {
        assertThat(Lyrics().hasContent()).isFalse()
        assertThat(Lyrics(plain = emptyList(), synced = emptyList()).hasContent()).isFalse()
    }

    @Test
    fun `plain lyrics count as content`() {
        assertThat(Lyrics(plain = listOf("第一行")).hasContent()).isTrue()
    }

    @Test
    fun `synced lyrics count as content`() {
        assertThat(
            Lyrics(synced = listOf(SyncedLine(time = 0, line = "第一行"))).hasContent()
        ).isTrue()
    }
}
