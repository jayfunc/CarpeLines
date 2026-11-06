package io.github.jayfunc.carpelines.lyrics.translate

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * LibreTranslate client, ported from BetterLyrics' `TranslationService`.
 *
 * Lines are joined with newlines in one request; the service keeps the line structure, which
 * lets the result be mapped straight back onto the original lyric lines.
 */
class LibreTranslateClient(
    private val client: HttpClient,
    private val serverUrl: String,
    private val apiKey: String? = null,
) : LyricsTranslationClient {

    override suspend fun translate(lines: List<String>, targetLanguage: String): List<String>? {
        val payloadLines = lines.filter { it.isNotBlank() }
        if (payloadLines.isEmpty() || targetLanguage.isBlank()) return null

        val body = buildString {
            append("q=").append(payloadLines.joinToString("\n").encodeURLParameter())
            append("&source=auto")
            append("&target=").append(targetLanguage.encodeURLParameter())
            append("&format=text")
            if (!apiKey.isNullOrBlank()) append("&api_key=").append(apiKey.encodeURLParameter())
        }

        val response = runCatching {
            client.post("${serverUrl.trimEnd('/')}/translate") {
                header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(TextContent(body, ContentType.Application.FormUrlEncoded))
            }
        }.getOrNull() ?: return null
        if (!response.status.isSuccess()) return null

        val translated = runCatching {
            Json.parseToJsonElement(response.bodyAsText())
                .jsonObject["translatedText"]
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull() ?: return null

        val translatedLines = translated.split('\n')
        if (translatedLines.size != payloadLines.size) {
            // Newlines were not preserved; keep a single line rather than misaligning the lyrics.
            val joined = translated.replace('\n', ' ').trim()
            if (joined.isEmpty()) return null
            return lines.map { if (it.isBlank()) "" else joined }
        }

        var index = 0
        return lines.map { line ->
            if (line.isBlank()) "" else translatedLines[index++]
        }
    }
}
