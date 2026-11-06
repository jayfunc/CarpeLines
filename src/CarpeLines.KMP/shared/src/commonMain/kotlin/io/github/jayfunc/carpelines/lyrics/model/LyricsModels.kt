package io.github.jayfunc.carpelines.lyrics.model

import kotlinx.serialization.Serializable

/**
 * How a raw lyrics payload is encoded. Mirrors the formats BetterLyrics understands on Windows.
 *
 * KRC is the encrypted Kugou container and is intentionally not listed: it needs a platform
 * inflater and is handled by the provider that owns it before reaching the parser.
 */
@Serializable
enum class LyricsFormat {
    PLAIN,
    LRC,
    ESLRC,
    QRC,
    TTML,
    UNKNOWN;

    val isWordTimed: Boolean
        get() = this == ESLRC || this == QRC || this == TTML
}

/** Where a lyrics payload came from, shown in the "provided by" footer. */
@Serializable
enum class LyricsProvider(val displayName: String) {
    LRCLIB("LRCLIB"),
    APPLE_MUSIC("Apple Music"),
    AMLL_TTML_DB("AMLL TTML DB"),
    QQ_MUSIC("QQ Music"),
    NETEASE("NetEase Cloud Music"),
    KUGOU("Kugou Music"),
    EMBEDDED("Embedded lyrics"),
    LOCAL_LRC("Local .lrc"),
    LOCAL_FILE("Local file"),
    BETTER_LYRICS("BetterLyrics"),
    LIBRE_TRANSLATE("LibreTranslate"),
    UNKNOWN("Unknown"),
}

/** One timed syllable. [startIndex] is its offset inside the owning line's text. */
@Serializable
data class LyricsSyllable(
    val text: String,
    val startMs: Long,
    val endMs: Long? = null,
    val startIndex: Int = 0,
) {
    val durationMs: Long?
        get() = endMs?.let { end -> (end - startMs).takeIf { it > 0L } }
}

/**
 * One lyric line. Main text and syllables are always present; translation and romanization are
 * attached after the auxiliary tracks have been aligned by [io.github.jayfunc.carpelines.lyrics.pipeline.LyricsMerger].
 */
@Serializable
data class LyricsLine(
    val startMs: Long,
    val endMs: Long? = null,
    val text: String,
    val syllables: List<LyricsSyllable> = emptyList(),
    val hasRealSyllableTiming: Boolean = false,
    val voice: Int? = null,
    val backgroundText: String? = null,
    val backgroundSyllables: List<LyricsSyllable> = emptyList(),
    val translation: String? = null,
    val romanization: String? = null,
) {
    val isInstrumental: Boolean
        get() = text.isBlank() && backgroundText.isNullOrBlank()

    val hasBackgroundVocals: Boolean
        get() = !backgroundText.isNullOrBlank() || backgroundSyllables.isNotEmpty()
}

/** A whole track of lines: the original, its translation or its transliteration. */
@Serializable
data class LyricsTrack(
    val type: LyricsTrackType,
    val provider: LyricsProvider,
    val languageTag: String? = null,
    val lines: List<LyricsLine> = emptyList(),
)

@Serializable
enum class LyricsTrackType {
    ORIGINAL,
    TRANSLATION,
    TRANSLITERATION,
}

/**
 * A parsed, normalized document ready for rendering. [lines] carry the merged translation and
 * romanization so a renderer only has to walk one list.
 */
@Serializable
data class LyricsDocument(
    val lines: List<LyricsLine>,
    val format: LyricsFormat,
    val provider: LyricsProvider,
    val languageTag: String? = null,
    val translationLanguageTag: String? = null,
    val romanizationLanguageTag: String? = null,
    val reference: String? = null,
    val matchPercentage: Int = 0,
    val isSynced: Boolean = true,
    val durationMs: Long? = null,
) {
    val isEmpty: Boolean
        get() = lines.isEmpty()

    val hasTranslation: Boolean
        get() = lines.any { !it.translation.isNullOrBlank() }

    val hasRomanization: Boolean
        get() = lines.any { !it.romanization.isNullOrBlank() }

    companion object {
        val Empty = LyricsDocument(
            lines = emptyList(),
            format = LyricsFormat.UNKNOWN,
            provider = LyricsProvider.UNKNOWN,
            isSynced = false,
        )
    }
}

/** Metadata a provider needs to identify a song, independent from any player's song model. */
data class LyricsQuery(
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long? = null,
    val songId: String? = null,
)

/** A provider's raw answer before it is parsed into a [LyricsDocument]. */
data class LyricsSearchResult(
    val provider: LyricsProvider,
    val raw: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    /** Translation in LRC form, when the provider ships one next to the original. */
    val translationRaw: String? = null,
    /** Transliteration in LRC form, when the provider ships one next to the original. */
    val transliterationRaw: String? = null,
    val reference: String? = null,
    val matchPercentage: Int = 0,
) {
    val isFound: Boolean
        get() = !raw.isNullOrBlank()
}

/** Rules that decide which auxiliary tracks are kept and produced. */
data class LyricsParseOptions(
    val translationEnabled: Boolean = true,
    val romanizationEnabled: Boolean = true,
    /** BCP-47 tag of the language the translation should be in; null keeps provider tracks only. */
    val targetTranslationLanguage: String? = null,
    /** Allows the LibreTranslate fallback when no provider translation exists. */
    val libreTranslateEnabled: Boolean = false,
    val libreTranslateServer: String? = null,
    /** Filters credit lines such as `作词 : …` when true. */
    val filterInfoLines: Boolean = true,
    /** Builder/credit info lines that should never be shown as lyrics. */
    val infoLineKeywords: Set<String> = DEFAULT_INFO_LINE_KEYWORDS,
) {
    companion object {
        val DEFAULT_INFO_LINE_KEYWORDS = setOf(
            "作词", "作曲", "编曲", "制作", "监制", "出品", "统筹", "混音", "母带",
            "录音", "和声", "封面", "词", "曲", "唱", "演唱", "原唱", "翻唱",
            "lyrics by", "composed by", "produced by", "written by", "arranged by",
            "mixed by", "mastered by", "recorded by", "vocals by", "performed by",
        )

        val Default = LyricsParseOptions()
    }
}
