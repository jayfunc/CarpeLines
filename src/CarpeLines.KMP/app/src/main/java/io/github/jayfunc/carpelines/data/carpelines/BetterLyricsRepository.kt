package io.github.jayfunc.carpelines.data.betterlyrics

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jayfunc.carpelines.lyrics.BetterLyricsAndroid
import io.github.jayfunc.carpelines.lyrics.model.LyricsDocument
import io.github.jayfunc.carpelines.lyrics.model.LyricsParseOptions
import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider
import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.model.LyricsSearchResult
import io.github.jayfunc.carpelines.data.model.Lyrics
import io.github.jayfunc.carpelines.data.model.LyricsSourcePreference
import io.github.jayfunc.carpelines.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class BetterLyricsCandidate(
    val provider: LyricsProvider,
    val providerLabel: String,
    val result: LyricsSearchResult,
    val wordTimed: Boolean,
) {
    val score: Int get() = result.matchPercentage
}

/** A provider answer this repository already collected; [document] is null when nothing matched. */
data class CachedLyricsLoad(val document: LyricsDocument?)

/**
 * Android-facing facade over the shared BetterLyrics engine.
 *
 * Providers are searched in parallel and scored by [io.github.jayfunc.carpelines.lyrics.match.MetadataMatcher];
 * candidate payloads are parsed lazily so the search list stays responsive. Settings live in a
 * dedicated SharedPreferences file until the settings screen gains proper controls.
 */
@Singleton
class BetterLyricsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val preferences: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse {
        // Keystore access can fail on some OEM builds; keep the app usable instead of crashing.
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private val engine by lazy {
        BetterLyricsAndroid.engine { appleMusicToken }
    }

    /**
     * The last answer each query got, keyed by metadata, duration, source order and parse options.
     *
     * The lyrics page lives in a back-stack entry whose ViewModel is gone once the page is popped,
     * so without this every visit started the whole provider search again for a track that had
     * already been resolved seconds earlier. Entries are cheap (a parsed document) and bounded;
     * anything that changes the result — a reordered source list, a translation setting — changes
     * the key and therefore resolves afresh.
     */
    private val recentLoads =
        object : LinkedHashMap<String, CachedLoad>(INITIAL_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedLoad>?): Boolean =
                size > MAX_CACHED_LOADS
        }

    private data class CachedLoad(
        /** Normalized title/artist/album, used to drop every variant of one track at once. */
        val identity: String,
        val document: LyricsDocument?,
        val storedAtEpochMs: Long,
    )

    var appleMusicToken: String?
        get() = preferences.getString(KEY_APPLE_MUSIC_TOKEN, null)
        set(value) {
            preferences.edit().putString(KEY_APPLE_MUSIC_TOKEN, value?.trim()).apply()
        }

    var libreTranslateServer: String?
        get() = preferences.getString(KEY_LIBRE_TRANSLATE_SERVER, null)
        set(value) {
            preferences.edit().putString(KEY_LIBRE_TRANSLATE_SERVER, value?.trim()).apply()
        }

    var translationEnabled: Boolean
        get() = preferences.getBoolean(KEY_TRANSLATION_ENABLED, true)
        set(value) {
            preferences.edit().putBoolean(KEY_TRANSLATION_ENABLED, value).apply()
        }

    var romanizationEnabled: Boolean
        get() = preferences.getBoolean(KEY_ROMANIZATION_ENABLED, true)
        set(value) {
            preferences.edit().putBoolean(KEY_ROMANIZATION_ENABLED, value).apply()
        }

    /** Target for the LibreTranslate fallback; defaults to the device language. */
    var translationTargetLanguage: String
        get() = preferences.getString(KEY_TRANSLATION_TARGET, null)
            ?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().language
        set(value) {
            preferences.edit().putString(KEY_TRANSLATION_TARGET, value).apply()
        }

    /** The ordered source list shared by internal and external playback. */
    var sourceOrder: List<LyricsSourceId>
        get() {
            val stored = preferences.getString(KEY_SOURCE_ORDER, null)
            if (stored.isNullOrBlank()) return LyricsSourceId.DefaultOrder
            return stored.split(',')
                .mapNotNull { name -> LyricsSourceId.entries.firstOrNull { it.name == name } }
                .distinct()
                .ifEmpty { LyricsSourceId.DefaultOrder }
        }
        set(value) {
            preferences.edit()
                .putString(KEY_SOURCE_ORDER, value.distinct().joinToString(",") { it.name })
                .apply()
        }

    val onlineProviderOrder: List<LyricsProvider>
        get() = sourceOrder.mapNotNull { it.provider }

    val localSourcesFirst: Boolean
        get() {
            val firstLocal = sourceOrder.indexOfFirst { it.isLocal }
            val firstOnline = sourceOrder.indexOfFirst { !it.isLocal }
            return firstLocal >= 0 && (firstOnline < 0 || firstLocal < firstOnline)
        }

    fun isLocalSourceEnabled(source: LyricsSourceId): Boolean = sourceOrder.contains(source)

    /**
     * Re-applies one of the legacy three-way priority presets on top of the user's enabled
     * sources, so the preset dropdown and the reorderable source list never disagree.
     */
    fun applyPreset(preset: LyricsSourcePreference) {
        val enabled = sourceOrder.toSet()
        val available = LyricsSourceId.DefaultOrder.filter { it in enabled }
        val local = available.filter { it.isLocal }
        val online = available.filterNot { it.isLocal }
        val orderedLocals = when (preset) {
            LyricsSourcePreference.LOCAL_FIRST -> local.sortedBy { if (it == LyricsSourceId.LOCAL_LRC) 0 else 1 }
            else -> local.sortedBy { if (it == LyricsSourceId.EMBEDDED) 0 else 1 }
        }
        sourceOrder = when (preset) {
            LyricsSourcePreference.API_FIRST -> online + orderedLocals
            else -> orderedLocals + online
        }
    }

    fun parseOptions(): LyricsParseOptions {
        val server = libreTranslateServer
        return LyricsParseOptions(
            translationEnabled = translationEnabled,
            romanizationEnabled = romanizationEnabled,
            targetTranslationLanguage = translationTargetLanguage,
            libreTranslateEnabled = !server.isNullOrBlank(),
            libreTranslateServer = server,
        )
    }

    /** Searches every configured provider and returns scored, unparsed candidates. */
    suspend fun search(song: Song, perProviderLimit: Int = 5): List<BetterLyricsCandidate> =
        search(BetterLyricsMapper.toQuery(song), perProviderLimit)

    /** Searches every configured provider for an arbitrary query (manual search). */
    suspend fun search(
        query: LyricsQuery,
        perProviderLimit: Int = 5,
    ): List<BetterLyricsCandidate> = withContext(Dispatchers.IO) {
        engine.search(
            query,
            perProviderLimit,
            providerOrder = onlineProviderOrder,
        )
            .filter { it.isFound }
            .map { result ->
                BetterLyricsCandidate(
                    provider = result.provider,
                    providerLabel = BetterLyricsMapper.providerLabel(result.provider),
                    result = result,
                    // Format detection is cheap and lets the UI show a "word timed" badge.
                    wordTimed = result.raw.orEmpty().let { raw ->
                        io.github.jayfunc.carpelines.lyrics.parse.LyricsFormatDetector.detect(raw).isWordTimed
                    },
                )
            }
    }

    suspend fun parse(candidate: BetterLyricsCandidate, song: Song): LyricsDocument =
        parse(candidate, song.duration.takeIf { it > 0 })

    suspend fun parse(candidate: BetterLyricsCandidate, durationMs: Long?): LyricsDocument =
        withContext(Dispatchers.IO) {
            engine.parse(
                result = candidate.result,
                options = parseOptions(),
                durationMs = durationMs,
            )
        }

    suspend fun loadBest(song: Song, minMatchPercentage: Int = 60): LyricsDocument? =
        withContext(Dispatchers.IO) {
            engine.load(
                query = BetterLyricsMapper.toQuery(song),
                options = parseOptions(),
                minMatchPercentage = minMatchPercentage,
                providerOrder = onlineProviderOrder,
            )
        }

    /**
     * The answer [load] already produced for [query], without touching the network.
     *
     * Returns null while the query is still unknown. A miss (`document == null`) is reported as a
     * hit so a page that repaints does not search again for a track that genuinely has no lyrics;
     * misses expire after [MISS_TTL_MS] so a provider that was briefly down is asked again.
     */
    fun peek(query: LyricsQuery, minMatchPercentage: Int = 60): CachedLyricsLoad? {
        val entry = lookup(cacheKey(query, minMatchPercentage, parseOptions())) ?: return null
        return CachedLyricsLoad(entry.document)
    }

    /** Drops every cached answer for [query]; used when the user explicitly asks to look again. */
    fun invalidate(query: LyricsQuery) {
        val identity = identityOf(query)
        synchronized(recentLoads) {
            recentLoads.entries.removeAll { (_, entry) -> entry.identity == identity }
        }
    }

    /**
     * Loads lyrics for any metadata query, used by the external media listener.
     *
     * With [useCache] the previous answer for the same query is reused instead of searching, which
     * is what keeps a re-entered lyrics page from hitting every provider again.
     */
    suspend fun load(
        query: LyricsQuery,
        minMatchPercentage: Int = 60,
        useCache: Boolean = true,
    ): LyricsDocument? = withContext(Dispatchers.IO) {
        val options = parseOptions()
        val key = cacheKey(query, minMatchPercentage, options)
        if (useCache) {
            lookup(key)?.let { return@withContext it.document }
        }
        val document = engine.load(
            query = query,
            options = options,
            minMatchPercentage = minMatchPercentage,
            providerOrder = onlineProviderOrder,
        )
        synchronized(recentLoads) {
            recentLoads[key] = CachedLoad(
                identity = identityOf(query),
                document = document,
                storedAtEpochMs = System.currentTimeMillis(),
            )
        }
        document
    }

    private fun identityOf(query: LyricsQuery): String = listOf(query.title, query.artist, query.album)
        .joinToString("\u0001") { it.trim().lowercase() }

    /** A live cache entry, dropping "nothing found" answers once they are old enough to retry. */
    private fun lookup(key: String): CachedLoad? = synchronized(recentLoads) {
        val entry = recentLoads[key] ?: return@synchronized null
        if (entry.document == null &&
            System.currentTimeMillis() - entry.storedAtEpochMs > MISS_TTL_MS
        ) {
            recentLoads.remove(key)
            return@synchronized null
        }
        entry
    }

    private fun cacheKey(
        query: LyricsQuery,
        minMatchPercentage: Int,
        options: LyricsParseOptions,
    ): String = buildString {
        append(identityOf(query)).append('\u0001')
        // Durations wobble by a beat between what a session publishes and what a scanner reads;
        // bucketing keeps those from looking like a different query.
        append((query.durationMs ?: 0L) / DURATION_BUCKET_MS).append('\u0001')
        append(minMatchPercentage).append('\u0001')
        append(onlineProviderOrder.joinToString(",") { it.name }).append('\u0001')
        append(options.hashCode())
    }

    /** Parses local/embedded lyrics with the same options as provider payloads. */
    suspend fun parseRawLyrics(
        raw: String,
        provider: LyricsProvider = LyricsProvider.LOCAL_FILE,
        durationMs: Long? = null,
        reference: String? = null,
    ): LyricsDocument = withContext(Dispatchers.IO) {
        engine.parseRaw(
            raw = raw,
            provider = provider,
            options = parseOptions(),
            reference = reference,
            durationMs = durationMs,
        )
    }

    /** Converts a shared document into the model the existing player renderer consumes. */
    fun toPlayerLyrics(document: LyricsDocument, fromRemote: Boolean = true): Lyrics =
        BetterLyricsMapper.toPlayerLyrics(document, fromRemote)

    /** Serializes a document back to enhanced LRC for persistence / export. */
    fun toEnhancedLrc(document: LyricsDocument): String =
        BetterLyricsMapper.toEnhancedLrc(document)

    private companion object {
        const val PREFERENCES_NAME = "betterlyrics_settings"
        const val KEY_APPLE_MUSIC_TOKEN = "apple_music_media_user_token"
        const val KEY_LIBRE_TRANSLATE_SERVER = "libre_translate_server"
        const val KEY_TRANSLATION_ENABLED = "translation_enabled"
        const val KEY_ROMANIZATION_ENABLED = "romanization_enabled"
        const val KEY_TRANSLATION_TARGET = "translation_target_language"
        const val KEY_SOURCE_ORDER = "lyrics_source_order"

        /** One document per track is small; a couple of dozen covers a listening session. */
        const val MAX_CACHED_LOADS = 24
        const val INITIAL_CACHE_SIZE = 8

        /** A "nothing found" answer is a hint, not a verdict: ask again after a while. */
        const val MISS_TTL_MS = 10 * 60 * 1000L

        /** Duration is matching input, not identity; this keeps small wobbles on one cache entry. */
        const val DURATION_BUCKET_MS = 5_000L
    }
}
