package io.github.jayfunc.carpelines.data.betterlyrics

import io.github.jayfunc.carpelines.lyrics.model.LyricsDocument
import io.github.jayfunc.carpelines.lyrics.model.LyricsFormat
import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.parse.LyricsTime
import io.github.jayfunc.carpelines.data.model.BackgroundVocals
import io.github.jayfunc.carpelines.data.model.Lyrics
import io.github.jayfunc.carpelines.data.model.Song
import io.github.jayfunc.carpelines.data.model.SyncedLine
import io.github.jayfunc.carpelines.data.model.SyncedWord

/**
 * Translates between the shared BetterLyrics document and CarpeLines's renderer model.
 *
 * Syllables are re-clustered for CarpeLines: a syllable that touches whitespace starts a new
 * word, so `SyncedWordFlow` can wrap Latin lines at real word boundaries while CJK lines keep
 * one continuous cluster and still animate per character.
 */
object BetterLyricsMapper {

    fun toQuery(song: Song): LyricsQuery = LyricsQuery(
        title = song.title,
        artist = song.displayArtist,
        album = song.album,
        durationMs = song.duration.takeIf { it > 0 },
        songId = song.id,
    )

    fun toPlayerLyrics(document: LyricsDocument, fromRemote: Boolean = true): Lyrics {
        if (!document.isSynced || document.lines.isEmpty()) {
            val plain = document.lines.map { it.text }.filter { it.isNotBlank() }
            return Lyrics(plain = plain.takeIf { it.isNotEmpty() }, areFromRemote = fromRemote)
        }

        val synced = document.lines.map { line ->
            SyncedLine(
                time = line.startMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                line = line.text,
                words = line.syllables.toSyncedWords(),
                translation = line.translation,
                romanization = line.romanization,
                voice = line.voice,
                endTime = line.endMs?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt(),
                hasSyllableTiming = line.hasRealSyllableTiming,
                background = line.backgroundText?.let { text ->
                    BackgroundVocals(
                        text = text,
                        words = line.backgroundSyllables.toSyncedWords(),
                    )
                },
            )
        }
        return Lyrics(synced = synced, areFromRemote = fromRemote)
    }

    /** Writes the enhanced-LRC the player persists, keeping syllable timing and voices. */
    fun toEnhancedLrc(document: LyricsDocument): String = buildString {
        document.lines.forEach { line ->
            if (line.text.isBlank() && !line.hasBackgroundVocals && line.translation.isNullOrBlank()) {
                return@forEach
            }
            append(LyricsTime.formatLrcTimestamp(line.startMs))
            line.voice?.let { append("v$it:") }
            if (line.syllables.isNotEmpty() && line.hasRealSyllableTiming) {
                line.syllables.forEach { syllable ->
                    append('<')
                    append(formatWordTag(syllable.startMs))
                    append('>')
                    append(syllable.text.replace('\n', ' '))
                }
            } else {
                append(line.text.replace('\n', ' '))
            }
            append('\n')

            if (line.hasBackgroundVocals) {
                append("[bg:")
                if (line.backgroundSyllables.isNotEmpty()) {
                    line.backgroundSyllables.forEach { syllable ->
                        append('<')
                        append(formatWordTag(syllable.startMs))
                        append('>')
                        append(syllable.text.replace('\n', ' '))
                    }
                } else {
                    append(line.backgroundText.orEmpty().replace('\n', ' '))
                }
                append("]\n")
            }

            // Translations travel with the line they translate: a second line at the same
            // timestamp is read back as its translation by the parser. A line that only has
            // backing vocals cannot take that slot — the next sung line shares its time — so it
            // uses the explicit tag instead. A translation identical to the original is skipped:
            // the parser refuses to pair it and would draw it as a duplicated lyric line.
            line.translation?.let { translation ->
                if (translation.trim() == line.text.trim()) return@let
                translation.lines().filter { it.isNotBlank() }.forEach { part ->
                    if (line.text.isBlank()) {
                        append("[tr:").append(part).append("]\n")
                    } else {
                        append(LyricsTime.formatLrcTimestamp(line.startMs)).append(part).append('\n')
                    }
                }
            }
        }
    }.trimEnd()

    fun lyricsFormatOf(document: LyricsDocument): String =
        when (document.format) {
            LyricsFormat.TTML -> "ttml"
            LyricsFormat.ESLRC, LyricsFormat.LRC -> "lrc"
            LyricsFormat.QRC -> "qrc"
            LyricsFormat.PLAIN -> "txt"
            LyricsFormat.UNKNOWN -> "txt"
        }

    fun isWordTimed(document: LyricsDocument): Boolean =
        document.lines.any { it.hasRealSyllableTiming }

    fun providerLabel(provider: LyricsProvider): String = provider.displayName

    private fun formatWordTag(timeMs: Long): String = LyricsTime
        .formatLrcTimestamp(timeMs)
        .removeSurrounding("[", "]")

    private fun List<io.github.jayfunc.carpelines.lyrics.model.LyricsSyllable>.toSyncedWords(): List<SyncedWord>? {
        if (isEmpty()) return null
        val words = mutableListOf<SyncedWord>()
        var previousEndedWithSpace = true
        forEach { syllable ->
            val text = syllable.text
            val trimmed = text.trim()
            val startsWithSpace = text.startsWith(' ') || text.startsWith('\t')
            if (trimmed.isNotEmpty()) {
                words += SyncedWord(
                    time = syllable.startMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    word = trimmed,
                    startsNewWord = words.isEmpty() || startsWithSpace || previousEndedWithSpace,
                    endTime = syllable.endMs?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt(),
                    sourceStartIndex = syllable.startIndex,
                    sourceLength = text.length,
                )
            }
            previousEndedWithSpace = text.endsWith(' ') || text.endsWith('\t')
        }
        return words.takeIf { it.isNotEmpty() }
    }
}
