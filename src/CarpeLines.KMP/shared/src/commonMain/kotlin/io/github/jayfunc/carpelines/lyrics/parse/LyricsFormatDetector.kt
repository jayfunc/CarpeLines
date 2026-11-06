package io.github.jayfunc.carpelines.lyrics.parse

import io.github.jayfunc.carpelines.lyrics.model.LyricsFormat

/** Detects the container of a raw lyrics payload before a concrete parser is chosen. */
object LyricsFormatDetector {

    private val TTML_DOCUMENT = Regex("""<tt\s|<ttml\s|xmlns:t="http://www\.w3\.org/ns/ttml"""", RegexOption.IGNORE_CASE)
    private val LRC_LINE = Regex("""^\[\d+:\d+(?:[.:]\d+)?]""")
    private val LRC_WORD_TAG = Regex("""<\d+:\d+(?:[.:]\d+)?>""")
    private val BRACKET_TIMESTAMP = Regex("""\[\d+:\d+(?:[.:]\d+)?]""")
    private val LEADING_TIMESTAMPS = Regex("""^(?:\[\d+:\d+(?:[.:]\d+)?])+""")
    private val QRC_LINE = Regex("""^\[\d{1,9},\d{1,9}]""")

    fun detect(raw: String?): LyricsFormat {
        val text = raw?.trimStart('\uFEFF', '\uFFFD')?.trimStart().orEmpty()
        if (text.isEmpty()) return LyricsFormat.UNKNOWN

        if (text.startsWith("{") && text.endsWith("}")) return LyricsFormat.UNKNOWN
        if (TTML_DOCUMENT.containsMatchIn(text.take(2_000)) || text.startsWith("<?xml", true)) {
            return LyricsFormat.TTML
        }

        val firstLines = text.lineSequence().take(80).map { it.trim() }.filter { it.isNotEmpty() }
        val hasLrc = firstLines.any { LRC_LINE.containsMatchIn(it) }
        if (!hasLrc) {
            // A QRC line can still carry a plain header before the timed body.
            if (firstLines.any { QRC_LINE.containsMatchIn(it) }) return LyricsFormat.QRC
            return if (text.hasTimedTag()) LyricsFormat.UNKNOWN else LyricsFormat.PLAIN
        }

        return when {
            text.contains("[bg:", true) -> LyricsFormat.ESLRC
            LRC_WORD_TAG.containsMatchIn(text) -> LyricsFormat.ESLRC
            text.hasInlineBracketWordTags() -> LyricsFormat.ESLRC
            firstLines.count { it.startsWith("[") && it.contains("(") && it.contains(")") } > 0 -> LyricsFormat.QRC
            else -> LyricsFormat.LRC
        }
    }

    /**
     * True when a line carries `[mm:ss.xx]` tags after its leading line timestamps, the
     * word-by-word variant some providers emit (`[00:10.00]背[00:10.40]对`).
     */
    private fun String.hasInlineBracketWordTags(): Boolean =
        lineSequence().take(200).any { line ->
            val withoutLeading = LEADING_TIMESTAMPS.replace(line.trim(), "")
            BRACKET_TIMESTAMP.containsMatchIn(withoutLeading)
        }

    private fun String.hasTimedTag(): Boolean =
        LRC_WORD_TAG.containsMatchIn(this) || LRC_LINE.find(this) != null
}
