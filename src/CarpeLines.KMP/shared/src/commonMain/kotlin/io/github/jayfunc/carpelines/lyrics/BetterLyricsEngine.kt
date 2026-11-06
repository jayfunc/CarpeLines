package io.github.jayfunc.carpelines.lyrics

import io.github.jayfunc.carpelines.lyrics.match.MetadataMatcher
import io.github.jayfunc.carpelines.lyrics.model.LyricsDocument
import io.github.jayfunc.carpelines.lyrics.model.LyricsFormat
import io.github.jayfunc.carpelines.lyrics.model.LyricsLine
import io.github.jayfunc.carpelines.lyrics.model.LyricsParseOptions
import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.model.LyricsSearchResult
import io.github.jayfunc.carpelines.lyrics.model.LyricsTrackType
import io.github.jayfunc.carpelines.lyrics.parse.CoreLyricsMapper
import io.github.jayfunc.carpelines.lyrics.parse.CoreLyricsParser
import io.github.jayfunc.carpelines.lyrics.parse.LyricsFormatDetector
import io.github.jayfunc.carpelines.lyrics.parse.ParsedLyrics
import io.github.jayfunc.carpelines.lyrics.pipeline.LyricsMerger
import io.github.jayfunc.carpelines.lyrics.pipeline.LyricsNormalizer
import io.github.jayfunc.carpelines.lyrics.provider.LyricsSource
import io.github.jayfunc.carpelines.lyrics.text.LanguageTags
import io.github.jayfunc.carpelines.lyrics.translate.LyricsTranslationClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

fun interface LyricsTranslationClientFactory {
    fun create(serverUrl: String): LyricsTranslationClient
}

/**
 * The BetterLyrics lyrics engine for Android.
 *
 * It keeps the Windows app's flow: query every enabled provider, score candidates with
 * [MetadataMatcher], parse the winner, merge its translation/transliteration tracks (or ask
 * LibreTranslate), then normalize timings for the renderer.
 */
class BetterLyricsEngine(
    private val sources: List<LyricsSource>,
    private val translationClientFactory: LyricsTranslationClientFactory? = null,
) {

    /** Searches sources in the requested provider order, or all sources when [providerOrder] is null. */
    suspend fun search(
        query: LyricsQuery,
        perSourceLimit: Int = 5,
        providerOrder: List<LyricsProvider>? = null,
    ): List<LyricsSearchResult> = coroutineScope {
        val orderedSources = if (providerOrder == null) {
            sources
        } else {
            providerOrder.mapNotNull { provider ->
                sources.firstOrNull { it.provider == provider }
            }.distinctBy { it.provider }
        }
        orderedSources.map { source ->
            async {
                try {
                    // One unreachable provider must not hold the whole result set hostage: the
                    // engine waits for every source, so a dead host would otherwise discard the
                    // answers the healthy providers already produced.
                    withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                        source.search(query, perSourceLimit)
                    }
                        ?.map { result ->
                            if (result.matchPercentage > 0) {
                                result
                            } else {
                                result.copy(matchPercentage = MetadataMatcher.score(query, result))
                            }
                        }
                        .orEmpty()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    currentCoroutineContext().ensureActive()
                    emptyList()
                }
            }
        }.awaitAll().flatten().sortedByDescending { it.matchPercentage }
    }

    /** Returns the best parsed lyrics above [minMatchPercentage], or null when nothing matches. */
    suspend fun load(
        query: LyricsQuery,
        options: LyricsParseOptions = LyricsParseOptions.Default,
        minMatchPercentage: Int = 60,
        providerOrder: List<LyricsProvider>? = null,
    ): LyricsDocument? {
        val results = search(query, providerOrder = providerOrder)
        val best = results.firstOrNull { it.matchPercentage >= minMatchPercentage } ?: return null
        return parse(best, options, query.durationMs)
    }

    /** Parses a provider answer. Translation/romanization tracks are attached when available. */
    suspend fun parse(
        result: LyricsSearchResult,
        options: LyricsParseOptions = LyricsParseOptions.Default,
        durationMs: Long? = null,
    ): LyricsDocument {
        val raw = result.raw.orEmpty()
        val format = LyricsFormatDetector.detect(raw)
        val parsed = parseRawPayload(raw, format)
        val originalLines = if (parsed.lines.isEmpty() && format == LyricsFormat.PLAIN) {
            raw.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { LyricsLine(startMs = 0L, text = it) }
                .toList()
        } else {
            parsed.lines
        }

        val effectiveDuration = durationMs ?: result.durationMs
        val detectedLanguage = parsed.languageTag
            ?: LanguageTags.detect(originalLines.joinToString(" ") { it.text })

        val translation = selectTranslation(parsed, result, options)
        val romanization = selectRomanization(parsed, result, options)

        var merged = if (options.translationEnabled && translation != null) {
            LyricsMerger.merge(originalLines, translation.second)
        } else {
            originalLines
        }
        if (options.romanizationEnabled && romanization != null) {
            merged = LyricsMerger.mergeRomanization(merged, romanization.second)
        }

        // No provider translation: fall back to LibreTranslate when configured.
        if (
            options.translationEnabled &&
            merged.none { !it.translation.isNullOrBlank() } &&
            options.libreTranslateEnabled &&
            !options.targetTranslationLanguage.isNullOrBlank() &&
            translationClientFactory != null
        ) {
            val generated = generateTranslation(merged, options)
            if (generated != null) merged = generated
        }

        val normalized = LyricsNormalizer.normalize(merged, effectiveDuration, options)
        val translationLanguage = translation?.first
            ?: options.targetTranslationLanguage?.takeIf { options.libreTranslateEnabled }
        val romanizationLanguage = romanization?.first

        return LyricsDocument(
            lines = normalized,
            format = format,
            provider = result.provider,
            languageTag = detectedLanguage,
            translationLanguageTag = translationLanguage,
            romanizationLanguageTag = romanizationLanguage,
            reference = result.reference,
            matchPercentage = result.matchPercentage,
            isSynced = parsed.lines.isNotEmpty(),
            durationMs = effectiveDuration,
        )
    }

    /** Parses raw text (imported file, embedded tag, pasted lyrics) without any provider call. */
    suspend fun parseRaw(
        raw: String,
        provider: LyricsProvider = LyricsProvider.LOCAL_FILE,
        options: LyricsParseOptions = LyricsParseOptions.Default,
        reference: String? = null,
        durationMs: Long? = null,
        matchPercentage: Int = 0,
    ): LyricsDocument = parse(
        result = LyricsSearchResult(
            provider = provider,
            raw = raw,
            reference = reference,
            durationMs = durationMs,
            matchPercentage = matchPercentage,
        ),
        options = options,
        durationMs = durationMs,
    )

    private fun parseRawPayload(raw: String, format: LyricsFormat): ParsedLyrics =
        ParsedLyrics(
            format = format,
            lines = CoreLyricsMapper.toLines(
                CoreLyricsParser.parse(raw),
                insertMissingLatinSpaces = KUGOU_WORD_TAG.containsMatchIn(raw),
            ),
        )

    /** Parses an auxiliary track (translation/romanization) with the same core engine. */
    private fun parseAuxiliary(raw: String): List<LyricsLine> =
        CoreLyricsMapper.toLines(CoreLyricsParser.parse(raw))

    private fun selectTranslation(
        parsed: ParsedLyrics,
        result: LyricsSearchResult,
        options: LyricsParseOptions,
    ): Pair<String?, List<LyricsLine>>? {
        if (!options.translationEnabled) return null

        val candidates = mutableListOf<Pair<String?, List<LyricsLine>>>()
        result.translationRaw
            ?.takeIf { it.isNotBlank() }
            ?.let { candidates += null to parseAuxiliary(it) }
        parsed.translations.forEach { (language, lines) ->
            if (lines.isNotEmpty()) candidates += language to lines
        }
        if (candidates.isEmpty()) return null

        val target = options.targetTranslationLanguage
        if (!target.isNullOrBlank()) {
            candidates.firstOrNull { LanguageTags.matches(it.first, target) }?.let { return it }
        }
        return candidates.first()
    }

    private fun selectRomanization(
        parsed: ParsedLyrics,
        result: LyricsSearchResult,
        options: LyricsParseOptions,
    ): Pair<String?, List<LyricsLine>>? {
        if (!options.romanizationEnabled) return null

        // A provider-supplied transliteration track wins over the ones embedded in the payload.
        result.transliterationRaw
            ?.takeIf { it.isNotBlank() }
            ?.let { raw -> return null to parseAuxiliary(raw) }

        val candidates = parsed.transliterations
            .map { (language, lines) -> language to lines }
            .filter { it.second.isNotEmpty() }
        if (candidates.isEmpty()) return null

        // Prefer a real phonetic track over a language-tagged plain copy.
        return candidates.firstOrNull { LanguageTags.isPhonetic(it.first) } ?: candidates.first()
    }

    private suspend fun generateTranslation(
        lines: List<LyricsLine>,
        options: LyricsParseOptions,
    ): List<LyricsLine>? {
        val target = options.targetTranslationLanguage ?: return null
        val serverUrl = options.libreTranslateServer?.takeIf { it.isNotBlank() } ?: return null
        val factory = translationClientFactory ?: return null
        val client = factory.create(serverUrl)

        val texts = lines.map { it.text }
        val translatedTexts = client.translate(texts, target) ?: return null
        val generated = lines.mapIndexed { index, line ->
            val text = translatedTexts.getOrNull(index).orEmpty()
            if (text.isBlank()) line else line.copy(translation = text)
        }
        return generated
    }

    /** Convenience for UIs that only need to know whether one track is a translation. */
    fun trackTypeOf(languageTag: String?, originalLanguageTag: String?): LyricsTrackType = when {
        LanguageTags.isPhonetic(languageTag) -> LyricsTrackType.TRANSLITERATION
        LanguageTags.matches(languageTag, originalLanguageTag) -> LyricsTrackType.TRANSLATION
        else -> LyricsTrackType.TRANSLATION
    }

    private companion object {
        val KUGOU_WORD_TAG = Regex("""<\d+,\d+,\d+>[^<]""")

        /** Per-provider ceiling, so one slow or dead source cannot stall the merged result. */
        const val PROVIDER_TIMEOUT_MS = 8_000L
    }
}
