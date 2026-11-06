package io.github.jayfunc.carpelines.lyrics.translate

/** Abstraction over LibreTranslate/LLM translation so the engine stays testable. */
interface LyricsTranslationClient {
    /**
     * Translates a batch of lines while preserving their order. Returns null when the service is
     * unreachable or answered with something unusable.
     */
    suspend fun translate(lines: List<String>, targetLanguage: String): List<String>?
}
