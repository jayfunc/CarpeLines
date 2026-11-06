package io.github.jayfunc.carpelines.lyrics

import io.github.jayfunc.carpelines.lyrics.provider.AppleMusicSource
import io.github.jayfunc.carpelines.lyrics.provider.KugouSource
import io.github.jayfunc.carpelines.lyrics.provider.LrcLibSource
import io.github.jayfunc.carpelines.lyrics.provider.NeteaseSource
import io.github.jayfunc.carpelines.lyrics.provider.QqMusicSource
import io.github.jayfunc.carpelines.lyrics.translate.LibreTranslateClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Android entry point for the shared engine. It hides the Ktor engine choice from `:app`, so the
 * app only depends on the Kotlin API and can swap providers without knowing the HTTP stack.
 */
object BetterLyricsAndroid {

    fun httpClient(): HttpClient = HttpClient(OkHttp)

    fun engine(appleMusicTokenProvider: () -> String? = { null }): BetterLyricsEngine {
        val client = httpClient()
        return BetterLyricsEngine(
            sources = listOf(
                LrcLibSource(client),
                AppleMusicSource(client, appleMusicTokenProvider),
                QqMusicSource(client),
                NeteaseSource(client),
                KugouSource(client),
            ),
            translationClientFactory = { serverUrl -> LibreTranslateClient(client, serverUrl) },
        )
    }
}
