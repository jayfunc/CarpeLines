package io.github.jayfunc.carpelines.lyrics.pipeline

import io.github.jayfunc.carpelines.lyrics.model.LyricsLine
import kotlin.math.abs

/**
 * Aligns translation and transliteration tracks with the original lines.
 *
 * Providers either repeat the original timestamps (QQ/NetEase) or ship auxiliary tracks whose
 * start times are a few tens of milliseconds off (TTML head tracks, LibreTranslate). Exact
 * matches win; otherwise the nearest line inside [MATCH_WINDOW_MS] is used, and when the track
 * has the same number of lines as the original the index is used as a last resort.
 */
object LyricsMerger {

    private const val MATCH_WINDOW_MS = 900L

    fun merge(original: List<LyricsLine>, auxiliary: List<LyricsLine>): List<LyricsLine> {
        if (original.isEmpty() || auxiliary.isEmpty()) return original
        val sameLength = auxiliary.size == original.size
        val used = BooleanArray(auxiliary.size)

        return original.mapIndexed { index, line ->
            val matchIndex = findMatch(line, auxiliary, used, sameLength, index) ?: return@mapIndexed line
            used[matchIndex] = true
            val text = auxiliary[matchIndex].text.trim()
            if (text.isEmpty()) line else line.copy(translation = cleanMultiline(text))
        }
    }

    fun mergeRomanization(original: List<LyricsLine>, auxiliary: List<LyricsLine>): List<LyricsLine> {
        if (original.isEmpty() || auxiliary.isEmpty()) return original
        val sameLength = auxiliary.size == original.size
        val used = BooleanArray(auxiliary.size)

        return original.mapIndexed { index, line ->
            val matchIndex = findMatch(line, auxiliary, used, sameLength, index) ?: return@mapIndexed line
            used[matchIndex] = true
            val text = auxiliary[matchIndex].text.trim()
            if (text.isEmpty()) line else line.copy(romanization = cleanMultiline(text))
        }
    }

    private fun findMatch(
        line: LyricsLine,
        auxiliary: List<LyricsLine>,
        used: BooleanArray,
        sameLength: Boolean,
        index: Int,
    ): Int? {
        for (i in auxiliary.indices) {
            if (!used[i] && auxiliary[i].startMs == line.startMs) return i
        }

        var best = -1
        var bestDistance = Long.MAX_VALUE
        auxiliary.forEachIndexed { auxIndex, auxLine ->
            if (used[auxIndex]) return@forEachIndexed
            val distance = abs(auxLine.startMs - line.startMs)
            if (distance <= MATCH_WINDOW_MS && distance < bestDistance) {
                best = auxIndex
                bestDistance = distance
            }
        }
        if (best >= 0) return best
        return if (sameLength && index in auxiliary.indices && !used[index]) index else null
    }

    private fun cleanMultiline(text: String): String =
        text.replace('\r', ' ').replace('\n', ' ').trim()
}
