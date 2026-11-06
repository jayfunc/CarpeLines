package io.github.jayfunc.carpelines.presentation.components.lyrics.card

import android.content.Context

/** Remembers the chosen style and each style's font, mirroring BetterLyrics' per-style config. */
internal object LyricsCardStyleStore {

    private const val PREFS = "lyrics_card_preferences"
    private const val STYLE_KEY = "selected_style"
    private const val FONT_PREFIX = "font_"

    fun style(context: Context): LyricsCardStyle =
        LyricsCardStyle.fromKey(prefs(context).getString(STYLE_KEY, null))

    fun saveStyle(context: Context, style: LyricsCardStyle) {
        prefs(context).edit().putString(STYLE_KEY, style.key).apply()
    }

    fun font(context: Context, style: LyricsCardStyle): LyricsCardFont {
        val stored = prefs(context).getString(FONT_PREFIX + style.key, null)
        return LyricsCardFont.entries.firstOrNull { it.name == stored } ?: style.defaultFont
    }

    fun saveFont(context: Context, style: LyricsCardStyle, font: LyricsCardFont) {
        prefs(context).edit().putString(FONT_PREFIX + style.key, font.name).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
