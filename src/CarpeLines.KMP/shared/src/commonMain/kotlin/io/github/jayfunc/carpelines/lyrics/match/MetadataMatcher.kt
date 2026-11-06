package io.github.jayfunc.carpelines.lyrics.match

import io.github.jayfunc.carpelines.lyrics.model.LyricsQuery
import io.github.jayfunc.carpelines.lyrics.model.LyricsSearchResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Port of BetterLyrics' `MetadataComparer.CalculateScore`.
 *
 * Titles, artists and albums are compared with Jaro-Winkler (well suited to short strings such as
 * song names); duration is scored with a 1 s perfect / 10 s impossible window. Weights mirror the
 * Windows app: title 0.30, artist 0.30, album 0.10, duration 0.30.
 */
object MetadataMatcher {

    private const val WEIGHT_TITLE = 0.30
    private const val WEIGHT_ARTIST = 0.30
    private const val WEIGHT_ALBUM = 0.10
    private const val WEIGHT_DURATION = 0.30

    fun score(query: LyricsQuery, result: LyricsSearchResult): Int {
        val title = result.title
        val hasLocalMetadata = query.title.isNotBlank()
        val hasRemoteMetadata = !title.isNullOrBlank()

        val total = if (hasLocalMetadata && hasRemoteMetadata) {
            stringSimilarity(query.title, title) * WEIGHT_TITLE +
                stringSimilarity(query.artist, result.artist) * WEIGHT_ARTIST +
                stringSimilarity(query.album, result.album) * WEIGHT_ALBUM +
                durationSimilarity(query.durationMs, result.durationMs) * WEIGHT_DURATION
        } else {
            val local = "${query.title} ${query.artist}"
            val remote = "${title.orEmpty()} ${result.artist.orEmpty()}"
            val localFingerprint = fingerprint(local)
            val remoteFingerprint = fingerprint(remote)
            if (localFingerprint.isEmpty() || remoteFingerprint.isEmpty()) {
                0.0
            } else {
                jaroWinkler(localFingerprint, remoteFingerprint)
            }
        }

        return (total * 100.0).toInt().coerceIn(0, 100)
    }

    fun stringSimilarity(a: String?, b: String?): Double {
        val left = a?.trim()?.lowercase().orEmpty()
        val right = b?.trim()?.lowercase().orEmpty()
        if (left.isEmpty() && right.isEmpty()) return 1.0
        if (left.isEmpty() || right.isEmpty()) return 0.0
        return jaroWinkler(left, right)
    }

    fun durationSimilarity(localMs: Long?, remoteMs: Long?): Double {
        if (localMs == null || remoteMs == null || remoteMs <= 0L) return 0.0
        val diff = abs(localMs - remoteMs).toDouble()
        return when {
            diff <= 1_000.0 -> 1.0
            diff >= 10_000.0 -> 0.0
            else -> 1.0 - (diff - 1_000.0) / 9_000.0
        }
    }

    fun jaroWinkler(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0

        val matchDistance = max(a.length, b.length) / 2 - 1
        val aMatches = BooleanArray(a.length)
        val bMatches = BooleanArray(b.length)
        var matches = 0

        for (i in a.indices) {
            val start = max(0, i - matchDistance)
            val end = min(i + matchDistance + 1, b.length)
            for (j in start until end) {
                if (bMatches[j]) continue
                if (a[i] != b[j]) continue
                aMatches[i] = true
                bMatches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0
        var k = 0
        for (i in a.indices) {
            if (!aMatches[i]) continue
            while (!bMatches[k]) k++
            if (a[i] != b[k]) transpositions++
            k++
        }

        val jaro = (
            matches.toDouble() / a.length +
                matches.toDouble() / b.length +
                (matches - transpositions / 2.0) / matches
            ) / 3.0

        if (jaro < 0.7) return jaro

        var prefix = 0
        val maxPrefix = min(4, min(a.length, b.length))
        while (prefix < maxPrefix && a[prefix] == b[prefix]) prefix++

        return jaro + prefix * 0.1 * (1.0 - jaro)
    }

    private fun fingerprint(input: String): String {
        val tokens = input.lowercase()
            .map { char ->
                if (char.isLetterOrDigit() || char == ' ' || char.isWhitespace()) char else ' '
            }
            .joinToString("")
            .split(Regex("""\s+"""))
            .filter { it.isNotEmpty() }
            .sorted()
        return tokens.joinToString(" ")
    }
}
