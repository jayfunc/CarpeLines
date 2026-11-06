package io.github.jayfunc.carpelines.lyrics.parse

import io.github.jayfunc.carpelines.lyrics.model.LyricsFormat
import io.github.jayfunc.carpelines.lyrics.model.LyricsLine

/** Result of parsing one payload: a main track plus optional auxiliary tracks. */
data class ParsedLyrics(
    val format: LyricsFormat,
    val lines: List<LyricsLine>,
    val translations: Map<String, List<LyricsLine>> = emptyMap(),
    val transliterations: Map<String, List<LyricsLine>> = emptyMap(),
    val languageTag: String? = null,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}
