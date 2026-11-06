package io.github.jayfunc.carpelines.data.model

import kotlinx.serialization.Serializable

/**
 * Data model for song lyrics.
 *
 * @param plain List of plain-text lines (unsynced).
 * @param synced List of (milliseconds, line) pairs for synced lyrics.
 * @param areFromRemote Indicates whether the lyrics were fetched from a remote source.
 */
@Serializable
data class Lyrics(
    val plain: List<String>? = null,
    val synced: List<SyncedLine>? = null,
    val areFromRemote: Boolean = false
)

@Serializable
data class SyncedLine(
    val time: Int,
    val line: String,
    val words: List<SyncedWord>? = null,
    val translation: String? = null,
    val romanization: String? = null,
    /** Singer number from an enhanced-LRC `vN:` prefix or a TTML `ttm:agent`; null when unmarked. */
    val voice: Int? = null,
    /**
     * True when [words] carry real per-syllable timings. Plain LRC lines get evenly distributed
     * syllables from the normalizer; those are not real timings and WinUI's Auto mode fills the
     * whole line instead of sweeping character by character.
     */
    val hasSyllableTiming: Boolean = false,
    /** Backing vocals sung under this line, from a `[bg: …]` line or TTML `x-bg` spans. */
    val background: BackgroundVocals? = null,
    /**
     * Explicit end of the sung line (TTML paragraph end). Null means the line simply runs until
     * the next one, which is how LRC behaves. WinUI ends the line at this time, so a long
     * instrumental break after a TTML paragraph is a real gap, not part of the line.
     */
    val endTime: Int? = null
)

@Serializable
data class BackgroundVocals(
    val text: String,
    val words: List<SyncedWord>? = null
)

@Serializable
data class SyncedWord(
    val time: Int,
    val word: String,
    val startsNewWord: Boolean = true,
    /** When the syllable stops, from a trailing empty LRC tag or a Kugou duration; null when unknown. */
    val endTime: Int? = null,
    /**
     * Offset of this syllable inside the owning line's text. BetterLyrics lays the original line
     * out once and maps every syllable back to its characters, so keeping the offset lets the
     * renderer reproduce that mapping instead of rebuilding the text from trimmed words.
     *
     * Null for lyrics written before this field existed; the renderer then finds the word in the
     * line text sequentially.
     */
    val sourceStartIndex: Int? = null,
    /**
     * Length of the untrimmed syllable (including separating whitespace), taken from the parsed
     * syllable rather than [word]. WinUI measures syllable length with that whitespace, which is
     * what keeps the karaoke sweep in step with the rendered line.
     */
    val sourceLength: Int? = null
)
