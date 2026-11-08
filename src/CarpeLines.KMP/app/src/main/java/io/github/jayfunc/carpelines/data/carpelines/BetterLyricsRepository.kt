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
import io.github.jayfunc.carpelines.lyrics.parse.CoreLyricsMapper
import io.github.jayfunc.carpelines.lyrics.parse.CoreLyricsParser
import io.github.jayfunc.carpelines.lyrics.parse.LyricsFormatDetector
import io.github.jayfunc.carpelines.data.model.Lyrics
import io.github.jayfunc.carpelines.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class BetterLyricsCandidate(
    val provider: LyricsProvider,
    val providerLabel: String,
    val result: LyricsSearchResult,
    val wordTimed: Boolean,
    /** First sung lines of the payload, parsed locally so list previews never show raw tags. */
    val previewLines: List<String> = emptyList(),
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
    @ApplicationContext private val context: Context,
    private val json: Json,
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

    /**
     * Provider work runs here rather than in the caller's scope: the lyrics page cancels its job
     * whenever the track changes, and a cancelled search used to throw away every answer it had
     * already collected. A request that outlives its page still fills the cache, so the next visit
     * to that track paints from it.
     */
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** One running provider search per cache key; later callers for the same key await it. */
    private val inFlightLoads = LinkedHashMap<String, InFlightLoad>()

    private data class InFlightLoad(
        val identity: String,
        val deferred: Deferred<LyricsDocument?>,
    )

    private data class CachedLoad(
        /** Normalized title/artist/album, used to drop every variant of one track at once. */
        val identity: String,
        val document: LyricsDocument?,
        val storedAtEpochMs: Long,
    )

    /** One provider answer as it survives process death; [key] guards against stale options. */
    @Serializable
    private data class PersistedLyricsLoad(
        val key: String,
        val storedAtEpochMs: Long,
        val document: LyricsDocument? = null,
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
     * How a provider payload is parsed. Both tracks are always attached so a stored selection
     * keeps them for later: the switches are presentation rules and are applied by
     * [toPlayerLyrics]. Only the LibreTranslate fallback, which costs a network call, stays
     * disabled while the translation switch is off.
     */
    fun parseOptions(): LyricsParseOptions {
        val server = libreTranslateServer
        return LyricsParseOptions(
            translationEnabled = true,
            romanizationEnabled = true,
            targetTranslationLanguage = translationTargetLanguage,
            libreTranslateEnabled = translationEnabled && !server.isNullOrBlank(),
            libreTranslateServer = server,
        )
    }

    /** Changes whenever a track option changes; lyrics pages key their reload on this. */
    val parseOptionsKey: Int
        get() = parseOptions().hashCode() * 31 +
            translationEnabled.hashCode() * 17 +
            romanizationEnabled.hashCode()

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
                val raw = result.raw.orEmpty()
                val format = LyricsFormatDetector.detect(raw)
                BetterLyricsCandidate(
                    provider = result.provider,
                    providerLabel = BetterLyricsMapper.providerLabel(result.provider),
                    result = result,
                    // Format detection is cheap and lets the UI show a "word timed" badge.
                    wordTimed = format.isWordTimed,
                    previewLines = previewLinesOf(raw),
                )
            }
    }

    /**
     * The first few sung lines of a raw payload, used by search-result previews.
     *
     * Parsing goes through the same core parser the lyrics page uses, so the list never shows
     * LRC/QRC timestamps or word-timing tags as if they were lyrics text.
     */
    suspend fun previewLines(raw: String, limit: Int = PREVIEW_LINE_COUNT): List<String> =
        withContext(Dispatchers.Default) { previewLinesOf(raw, limit) }

    private fun previewLinesOf(raw: String, limit: Int = PREVIEW_LINE_COUNT): List<String> {
        if (raw.isBlank()) return emptyList()
        val parsed = CoreLyricsMapper.toLines(
            CoreLyricsParser.parse(raw),
            insertMissingLatinSpaces = KUGOU_WORD_TAG.containsMatchIn(raw),
        )
            .asSequence()
            .map { it.text.trim() }
            .filter { it.isNotEmpty() }
            .take(limit)
            .toList()
        if (parsed.isNotEmpty()) return parsed

        // Unsynchronised payloads carry no tags for the parser to anchor on; they are already
        // plain text.
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(limit)
            .toList()
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
     * misses expire after [MISS_TTL_MS] so a provider that was briefly down is asked again. The
     * last answer also outlives the process: it is read back from disk when memory is cold.
     */
    suspend fun peek(query: LyricsQuery, minMatchPercentage: Int = 60): CachedLyricsLoad? =
        withContext(Dispatchers.IO) {
            val identity = identityOf(query)
            val key = cacheKey(query, minMatchPercentage, parseOptions())
            lookup(key)?.let { return@withContext CachedLyricsLoad(it.document) }

            val persisted = readDiskLoad(key, identity) ?: return@withContext null
            cacheInMemory(key, identity, persisted.storedAtEpochMs, persisted.document)
            CachedLyricsLoad(persisted.document)
        }

    /** Drops every cached answer for [query]; used when the user explicitly asks to look again. */
    fun invalidate(query: LyricsQuery) {
        val identity = identityOf(query)
        synchronized(recentLoads) {
            recentLoads.entries.removeAll { (_, entry) -> entry.identity == identity }
        }
        val cancelled = mutableListOf<Deferred<LyricsDocument?>>()
        synchronized(inFlightLoads) {
            val iterator = inFlightLoads.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.identity == identity) {
                    cancelled += entry.value.deferred
                    iterator.remove()
                }
            }
        }
        cancelled.forEach { it.cancel() }
        runCatching { diskFileFor(identity).delete() }
    }

    /**
     * Loads lyrics for any metadata query, used by the external media listener.
     *
     * With [useCache] the previous answer for the same query is reused instead of searching, which
     * is what keeps a re-entered lyrics page from hitting every provider again. A search already
     * running for the same query is awaited rather than started twice, and it is owned by
     * [cacheScope], so changing tracks cancels only the page's wait — not the search itself.
     */
    suspend fun load(
        query: LyricsQuery,
        minMatchPercentage: Int = 60,
        useCache: Boolean = true,
    ): LyricsDocument? = withContext(Dispatchers.IO) {
        val options = parseOptions()
        val identity = identityOf(query)
        val key = cacheKey(query, minMatchPercentage, options)
        if (useCache) {
            lookup(key)?.let { return@withContext it.document }
            readDiskLoad(key, identity)?.let { persisted ->
                cacheInMemory(key, identity, persisted.storedAtEpochMs, persisted.document)
                return@withContext persisted.document
            }
        }

        val deferred = synchronized(inFlightLoads) {
            inFlightLoads[key]?.let { return@synchronized it.deferred }
            cacheScope.async(start = CoroutineStart.LAZY) {
                val document = engine.load(
                    query = query,
                    options = options,
                    minMatchPercentage = minMatchPercentage,
                    providerOrder = onlineProviderOrder,
                )
                val storedAt = System.currentTimeMillis()
                cacheInMemory(key, identity, storedAt, document)
                writeDiskLoad(key, identity, storedAt, document)
                document
            }.also { started ->
                inFlightLoads[key] = InFlightLoad(identity, started)
                started.invokeOnCompletion {
                    synchronized(inFlightLoads) {
                        if (inFlightLoads[key]?.deferred === started) inFlightLoads.remove(key)
                    }
                }
            }
        }
        deferred.start()
        deferred.await()
    }

    private fun identityOf(query: LyricsQuery): String = listOf(query.title, query.artist, query.album)
        .joinToString("\u0001") { it.trim().lowercase() }

    private fun cacheInMemory(
        key: String,
        identity: String,
        storedAtEpochMs: Long,
        document: LyricsDocument?,
    ) {
        synchronized(recentLoads) {
            recentLoads[key] = CachedLoad(identity, document, storedAtEpochMs)
        }
    }

    /** A live cache entry, dropping "nothing found" answers once they are old enough to retry. */
    private fun lookup(key: String): CachedLoad? = synchronized(recentLoads) {
        val entry = recentLoads[key] ?: return@synchronized null
        if (!isFresh(entry.document, entry.storedAtEpochMs)) {
            recentLoads.remove(key)
            return@synchronized null
        }
        entry
    }

    private fun isFresh(document: LyricsDocument?, storedAtEpochMs: Long): Boolean =
        document != null || System.currentTimeMillis() - storedAtEpochMs <= MISS_TTL_MS

    /**
     * Provider answers are persisted one file per track identity, with the exact cache key inside:
     * an answer parsed under another source order, track option or duration cannot be mistaken for
     * this one. The directory is bounded so a long library does not accumulate files forever.
     */
    private fun readDiskLoad(key: String, identity: String): PersistedLyricsLoad? {
        val file = diskFileFor(identity)
        if (!file.isFile) return null
        return try {
            val persisted = json.decodeFromString<PersistedLyricsLoad>(file.readText())
            when {
                persisted.key != key -> null
                !isFresh(persisted.document, persisted.storedAtEpochMs) -> {
                    file.delete()
                    null
                }
                else -> persisted
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeDiskLoad(
        key: String,
        identity: String,
        storedAtEpochMs: Long,
        document: LyricsDocument?,
    ) {
        try {
            val dir = File(context.filesDir, DISK_CACHE_DIR)
            dir.mkdirs()
            val file = diskFileFor(identity)
            file.writeText(
                json.encodeToString(
                    PersistedLyricsLoad(
                        key = key,
                        storedAtEpochMs = storedAtEpochMs,
                        document = document,
                    )
                )
            )
            pruneDiskLoads(dir)
        } catch (_: Exception) {
            // A cache that cannot be written must not fail the lyrics page.
        }
    }

    private fun pruneDiskLoads(dir: File) {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        if (files.size <= MAX_DISK_LOADS) return
        files.sortedBy { it.lastModified() }
            .take(files.size - MAX_DISK_LOADS)
            .forEach { it.delete() }
    }

    private fun diskFileFor(identity: String): File =
        File(File(context.filesDir, DISK_CACHE_DIR), sha256Hex(identity))

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { byte -> "%02x".format(Locale.US, byte.toInt() and 0xFF) }

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

    /**
     * Converts a shared document into the model the existing player renderer consumes.
     *
     * The two track switches are applied here as well, not only while parsing: a payload the core
     * parser already paired with a translation, or a document persisted before the switch was
     * turned off, would otherwise keep painting a layer the user asked not to use.
     */
    fun toPlayerLyrics(document: LyricsDocument, fromRemote: Boolean = true): Lyrics {
        val showTranslation = translationEnabled
        val showRomanization = romanizationEnabled
        val visible = if (showTranslation && showRomanization) {
            document
        } else {
            document.copy(
                lines = document.lines.map { line ->
                    line.copy(
                        translation = line.translation.takeIf { showTranslation },
                        romanization = line.romanization.takeIf { showRomanization },
                    )
                }
            )
        }
        return BetterLyricsMapper.toPlayerLyrics(visible, fromRemote)
    }

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

        /** Provider answers on disk keep the last visited tracks warm across process death. */
        const val DISK_CACHE_DIR = "lyrics_provider_cache"
        const val MAX_DISK_LOADS = 100

        /** A "nothing found" answer is a hint, not a verdict: ask again after a while. */
        const val MISS_TTL_MS = 10 * 60 * 1000L

        /** Duration is matching input, not identity; this keeps small wobbles on one cache entry. */
        const val DURATION_BUCKET_MS = 5_000L

        /** Three lines fit a search-result card without turning it into a lyrics page. */
        const val PREVIEW_LINE_COUNT = 3

        /** Kugou word tags omit the spaces between Latin tokens; the parser can restore them. */
        val KUGOU_WORD_TAG = Regex("""<\d+,\d+,\d+>[^<]""")
    }
}
