package io.github.jayfunc.carpelines.lyrics.text

/** Small language-tag helpers; BetterLyrics uses NLanguageTag, which has no KMP equivalent. */
object LanguageTags {

    private val PHONETIC_MARKERS = listOf("latn", "pinyin", "jyutping", "romaji", "romanization")

    fun normalize(tag: String?): String? =
        tag?.trim()?.takeIf { it.isNotEmpty() }?.replace('_', '-')

    fun baseLanguage(tag: String?): String? =
        normalize(tag)?.substringBefore('-')?.lowercase()?.takeIf { it.isNotEmpty() }

    fun matches(a: String?, b: String?): Boolean {
        val left = baseLanguage(a) ?: return false
        val right = baseLanguage(b) ?: return false
        return left == right
    }

    fun isPhonetic(tag: String?): Boolean {
        val normalized = normalize(tag)?.lowercase() ?: return false
        return PHONETIC_MARKERS.any { it in normalized } || "-latn" in normalized
    }

    /** Best-effort script detection used when a provider does not report a language. */
    fun detect(text: String?): String? {
        if (text.isNullOrBlank()) return null
        var han = 0
        var kana = 0
        var hangul = 0
        var cyrillic = 0
        var latin = 0
        var total = 0
        for (char in text) {
            if (char.isWhitespace() || !char.isLetter()) continue
            total++
            when {
                char.code in 0x3040..0x30FF -> kana++
                char.code in 0xAC00..0xD7AF -> hangul++
                char.code in 0x4E00..0x9FFF || char.code in 0x3400..0x4DBF -> han++
                char.code in 0x0400..0x04FF -> cyrillic++
                char.code in 0x0041..0x024F -> latin++
            }
        }
        if (total == 0) return null
        return when {
            kana > 0 -> "ja"
            hangul > 0 -> "ko"
            han > 0 -> "zh"
            cyrillic > latin -> "ru"
            latin > 0 -> "und-Latn"
            else -> null
        }
    }

    fun displayName(tag: String?): String? =
        normalize(tag)?.substringBefore('-')?.uppercase()
}
