package io.github.jayfunc.carpelines.lyrics.pipeline

import io.github.jayfunc.carpelines.lyrics.model.LyricsLine
import io.github.jayfunc.carpelines.lyrics.model.LyricsParseOptions
import io.github.jayfunc.carpelines.lyrics.model.LyricsSyllable

/**
 * Fills the timing gaps every provider leaves behind, so a renderer can always animate:
 * missing line ends come from the next line, missing syllable ends from the next syllable,
 * and un-timed LRC lines get evenly distributed per-character syllables (BetterLyrics'
 * `EnsureSyllables`/`EnsureEndMs` behaviour).
 */
object LyricsNormalizer {

    private const val FALLBACK_LINE_DURATION_MS = 1_000L
    private const val FALLBACK_SYLLABLE_DURATION_MS = 1_000L

    fun normalize(
        lines: List<LyricsLine>,
        durationMs: Long?,
        options: LyricsParseOptions = LyricsParseOptions.Default,
    ): List<LyricsLine> {
        val sorted = lines.sortedBy { it.startMs }
        val filtered = if (options.filterInfoLines) {
            sorted.filterNot { isInfoLine(it.text, options.infoLineKeywords) }
        } else {
            sorted
        }

        val result = mutableListOf<LyricsLine>()
        for (index in filtered.indices) {
            val line = filtered[index]
            val nextStart = filtered.getOrNull(index + 1)?.startMs
                ?: durationMs?.takeIf { it > line.startMs }
                ?: (line.startMs + FALLBACK_LINE_DURATION_MS)

            val endMs = resolveLineEnd(line, nextStart)
            val syllables = ensureSyllables(line, endMs)
            val background = ensureBackground(line, endMs)

            result += line.copy(
                endMs = endMs,
                syllables = syllables,
                backgroundSyllables = background,
                backgroundText = line.backgroundText?.takeIf { it.isNotBlank() },
            )
        }
        return result
    }

    private fun resolveLineEnd(line: LyricsLine, nextStartMs: Long): Long {
        line.endMs?.takeIf { it > line.startMs }?.let { return it }

        val lastSyllableEnd = line.syllables.lastOrNull()?.endMs
            ?.takeIf { it > line.startMs }
        if (lastSyllableEnd != null) return lastSyllableEnd

        return if (line.startMs >= nextStartMs) {
            line.startMs + FALLBACK_LINE_DURATION_MS
        } else {
            nextStartMs.coerceAtLeast(line.startMs + 1L)
        }
    }

    private fun ensureSyllables(line: LyricsLine, lineEndMs: Long): List<LyricsSyllable> {
        if (line.syllables.isEmpty()) {
            // Only synthesize syllables for a timed line with text; instrumental markers stay bare.
            if (line.text.isBlank()) return emptyList()
            return synthesize(line.text, line.startMs, lineEndMs)
        }

        var offset = 0
        return line.syllables.mapIndexed { index, syllable ->
            val start = syllable.startMs.coerceAtLeast(line.startMs)
            val nextStart = line.syllables.getOrNull(index + 1)?.startMs
            val fallbackEnd = when {
                nextStart != null && nextStart > start -> nextStart
                start >= lineEndMs -> start + FALLBACK_SYLLABLE_DURATION_MS
                else -> lineEndMs
            }
            val end = syllable.endMs?.takeIf { it > start }
                ?: fallbackEnd.coerceAtLeast(start + 1L)
            val copy = syllable.copy(
                startMs = start,
                endMs = end,
                startIndex = offset,
            )
            offset += copy.text.length
            copy
        }
    }

    private fun ensureBackground(line: LyricsLine, lineEndMs: Long): List<LyricsSyllable> {
        if (line.backgroundSyllables.isEmpty()) return emptyList()
        var offset = 0
        return line.backgroundSyllables.mapIndexed { index, syllable ->
            val nextStart = line.backgroundSyllables.getOrNull(index + 1)?.startMs
            val end = syllable.endMs?.takeIf { it > syllable.startMs }
                ?: nextStart
                ?: lineEndMs.coerceAtLeast(syllable.startMs + 1L)
            syllable.copy(endMs = end, startIndex = offset).also {
                offset += it.text.length
            }
        }
    }

    /** Splits a line into evenly spaced per-character syllables, as BetterLyrics does for LRC. */
    private fun synthesize(text: String, startMs: Long, endMs: Long): List<LyricsSyllable> {
        val chars = text.toCharArray()
        if (chars.isEmpty()) return emptyList()
        val duration = (endMs - startMs).takeIf { it > 0 } ?: FALLBACK_LINE_DURATION_MS
        val step = duration.toDouble() / chars.size
        return chars.mapIndexed { index, char ->
            val syllableStart = startMs + (step * index).toLong()
            val syllableEnd = startMs + (step * (index + 1)).toLong()
            LyricsSyllable(
                text = char.toString(),
                startMs = syllableStart,
                endMs = syllableEnd,
                startIndex = index,
            )
        }
    }

    fun isInfoLine(text: String, keywords: Set<String>): Boolean {
        val line = text.trim()
        if (line.isEmpty()) return false
        val lower = line.lowercase()

        val colonIndex = lower.indexOfFirst { it == ':' || it == '：' }
        if (colonIndex > 0) {
            val key = lower.substring(0, colonIndex).trim()
            if (key in keywords) return true
        }

        keywords.forEach { keyword ->
            if (keyword.length >= 2 && lower.startsWith(keyword)) return true
        }
        return false
    }
}
