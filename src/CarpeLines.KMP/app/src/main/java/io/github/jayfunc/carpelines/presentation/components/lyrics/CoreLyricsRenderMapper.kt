package io.github.jayfunc.carpelines.presentation.components.lyrics

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import io.github.jayfunc.carpelines.data.model.SyncedLine

/** One rendered core line and the player line it came from, used to route taps back to a seek. */
internal data class RenderedLyricsLine(
    val core: ISyncedLine,
    val source: SyncedLine,
)

internal data class RenderedLyrics(
    val lyrics: SyncedLyrics,
    val lines: List<RenderedLyricsLine>,
)

/**
 * Rebuilds the accompanist-lyrics-core model from the player model. Lines with real syllable
 * timing keep their syllables; a plain LRC line becomes a single syllable spanning the line. Both
 * are karaoke lines, so the core UI can sweep them and both honour the line alignment.
 */
internal object CoreLyricsRenderMapper {

    private const val FALLBACK_LINE_DURATION_MS = 5_000

    /** Window used for the only syllable of a line that has no word-level timing of its own. */
    private const val UNTIMED_SYLLABLE_START = 0
    private const val UNTIMED_SYLLABLE_END = 1

    fun toRenderedLyrics(
        lines: List<SyncedLine>,
        showBackgroundVocals: Boolean,
        duetLayoutEnabled: Boolean,
        alignment: LyricsAlignment = LyricsAlignment.DEFAULT,
    ): RenderedLyrics {
        val rendered = mutableListOf<RenderedLyricsLine>()
        val coreLines = mutableListOf<ISyncedLine>()

        lines.forEachIndexed { index, line ->
            val nextStart = lines
                .asSequence()
                .drop(index + 1)
                .firstOrNull { it.time > line.time }
                ?.time
            val lineEnd = resolveLineEnd(line, nextStart)
            val lineAlignment = alignmentOf(line, duetLayoutEnabled, alignment)
            val background = line.background
                ?.takeIf { showBackgroundVocals && it.text.isNotBlank() }

            if (line.line.isBlank()) {
                if (background != null) {
                    val accompaniment = accompanimentLine(
                        background = background,
                        start = line.time,
                        end = lineEnd,
                        alignment = lineAlignment,
                    )
                    coreLines += accompaniment
                    rendered += RenderedLyricsLine(accompaniment, line)
                }
                return@forEachIndexed
            }

            val words = line.words.orEmpty()
            if (line.hasSyllableTiming && words.isNotEmpty()) {
                val syllables = words.toKaraokeSyllables(line, lineEnd)
                if (syllables.isEmpty()) return@forEachIndexed
                val accompaniment = background?.let {
                    accompanimentLine(
                        background = it,
                        start = line.time,
                        end = lineEnd,
                        alignment = lineAlignment,
                    )
                }
                val main = KaraokeLine.MainKaraokeLine(
                    syllables = syllables,
                    translation = line.translation,
                    alignment = lineAlignment,
                    start = syllables.first().start,
                    end = maxOf(syllables.last().end, lineEnd),
                    phonetic = line.romanization,
                    accompanimentLines = accompaniment?.let { listOf(it) },
                )
                coreLines += main
                rendered += RenderedLyricsLine(main, line)
            } else {
                // A line with no syllable timing is still one karaoke line, because only karaoke
                // lines carry an alignment — the core view pins a non-karaoke `SyncedLine` to the
                // leading edge. Its single syllable is timed before the song starts on purpose:
                // the core view then paints the row as a whole line (no sweep, no unlit state
                // while the line waits its turn), which is how an LRC line reads today. Real
                // per-line timings would turn every LRC line into a slowly wiping karaoke line.
                val main = KaraokeLine.MainKaraokeLine(
                    syllables = listOf(
                        KaraokeSyllable(
                            content = line.line,
                            start = UNTIMED_SYLLABLE_START,
                            end = UNTIMED_SYLLABLE_END,
                        )
                    ),
                    translation = line.translation,
                    alignment = lineAlignment,
                    start = line.time,
                    end = lineEnd,
                    phonetic = line.romanization,
                )
                coreLines += main
                rendered += RenderedLyricsLine(main, line)

                // A plain line has no karaoke container, so its backing vocals become the next
                // top-level line and still follow the same background-vocals setting.
                if (background != null) {
                    val accompaniment = accompanimentLine(
                        background = background,
                        start = line.time,
                        end = lineEnd,
                        alignment = lineAlignment,
                    )
                    coreLines += accompaniment
                    rendered += RenderedLyricsLine(accompaniment, line)
                }
            }
        }

        return RenderedLyrics(
            lyrics = SyncedLyrics(lines = coreLines),
            lines = rendered,
        )
    }

    private fun resolveLineEnd(line: SyncedLine, nextStart: Int?): Int {
        val explicitEnd = line.endTime?.takeIf { it > line.time }
        val wordsEnd = line.words.orEmpty()
            .maxOfOrNull { it.endTime?.coerceAtLeast(it.time) ?: it.time }
        val backgroundEnd = line.background?.words.orEmpty()
            .maxOfOrNull { it.endTime?.coerceAtLeast(it.time) ?: it.time }

        if (explicitEnd != null) {
            return maxOf(explicitEnd, wordsEnd ?: 0, backgroundEnd ?: 0)
        }
        return maxOf(
            wordsEnd ?: 0,
            backgroundEnd ?: 0,
            nextStart ?: (line.time + FALLBACK_LINE_DURATION_MS),
            line.time + 1,
        )
    }

    /**
     * Duet lines keep their side first — the two singers are the layout — and every other line
     * follows the user's choice. The karaoke renderer has no centred variant, so `LEFT` is left
     * to its natural leading edge instead of forcing a `Start` that would flip on RTL songs.
     */
    private fun alignmentOf(
        line: SyncedLine,
        duetLayoutEnabled: Boolean,
        userAlignment: LyricsAlignment,
    ): KaraokeAlignment {
        if (duetLayoutEnabled) {
            when (line.voice) {
                1 -> return KaraokeAlignment.Start
                2 -> return KaraokeAlignment.End
            }
        }
        return when (userAlignment) {
            LyricsAlignment.LEFT -> KaraokeAlignment.Unspecified
            LyricsAlignment.RIGHT -> KaraokeAlignment.End
        }
    }

    private fun accompanimentLine(
        background: io.github.jayfunc.carpelines.data.model.BackgroundVocals,
        start: Int,
        end: Int,
        alignment: KaraokeAlignment,
    ): KaraokeLine.AccompanimentKaraokeLine {
        val words = background.words.orEmpty()
        val syllables = if (words.isEmpty()) {
            listOf(
                KaraokeSyllable(
                    content = background.text,
                    start = start,
                    end = maxOf(end, start + 1),
                )
            )
        } else {
            words.toKaraokeSyllables(
                text = background.text,
                lineStart = start,
                lineEnd = end,
                startsNewWord = { it.startsNewWord },
            )
        }
        return KaraokeLine.AccompanimentKaraokeLine(
            syllables = syllables,
            translation = null,
            alignment = alignment,
            start = syllables.firstOrNull()?.start ?: start,
            end = maxOf(syllables.lastOrNull()?.end ?: start, end, start + 1),
        )
    }

    private fun List<io.github.jayfunc.carpelines.data.model.SyncedWord>.toKaraokeSyllables(
        line: SyncedLine,
        lineEnd: Int,
    ): List<KaraokeSyllable> = toKaraokeSyllables(
        text = line.line,
        lineStart = line.time,
        lineEnd = lineEnd,
        startsNewWord = { it.startsNewWord },
    )

    private fun List<io.github.jayfunc.carpelines.data.model.SyncedWord>.toKaraokeSyllables(
        text: String,
        lineStart: Int,
        lineEnd: Int,
        startsNewWord: (io.github.jayfunc.carpelines.data.model.SyncedWord) -> Boolean,
    ): List<KaraokeSyllable> {
        val result = mutableListOf<KaraokeSyllable>()
        this.forEachIndexed { index, word ->
            if (word.word.isBlank()) return@forEachIndexed
            val start = word.time.coerceAtLeast(lineStart)
            val nextStart = this.getOrNull(index + 1)?.time
            val end = word.endTime
                ?.coerceAtLeast(start)
                ?: nextStart?.coerceAtLeast(start)
                ?: lineEnd.coerceAtLeast(start + 1)

            val raw = word.sourceStartIndex
                ?.takeIf { it >= 0 }
                ?.let { offset ->
                    val length = word.sourceLength ?: word.word.length
                    text.substring(offset, (offset + length).coerceAtMost(text.length))
                        .takeIf { it.isNotBlank() }
                }
            val content = raw ?: buildString {
                if (result.isNotEmpty() && startsNewWord(word)) append(' ')
                append(word.word.trim())
            }

            result += KaraokeSyllable(
                content = content,
                start = start,
                end = maxOf(end, start),
            )
        }
        return result
    }
}
