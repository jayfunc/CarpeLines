package io.github.jayfunc.carpelines.presentation.floatinglyrics

import android.content.Context

/**
 * Shared geometry for the two hosts of the floating lyric view: the desktop overlay window and
 * the lock-screen activity. Both must land at the same width and position, otherwise toggling
 * the lock screen would make the lyrics jump.
 */
internal object FloatingLyricsPlacement {

    private const val POSITION_PREFERENCES = "floating_lyrics_position"
    private const val POSITION_X = "x"
    private const val POSITION_Y = "y"
    private const val NO_POSITION = Int.MIN_VALUE
    private const val MIN_WIDTH_DP = 220
    private const val MAX_WIDTH_DP = 380
    private const val DEFAULT_TOP_DP = 96

    fun widthPx(context: Context): Int = minOf(
        context.resources.displayMetrics.widthPixels - context.dp(32),
        context.dp(MAX_WIDTH_DP),
    ).coerceAtLeast(context.dp(MIN_WIDTH_DP))

    fun restoreX(context: Context, widthPx: Int): Int {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val saved = preferences(context).getInt(POSITION_X, NO_POSITION)
        if (saved != NO_POSITION && saved <= screenWidth - widthPx) return saved
        return ((screenWidth - widthPx) / 2).coerceAtLeast(0)
    }

    fun restoreY(context: Context): Int {
        val screenHeight = context.resources.displayMetrics.heightPixels
        val saved = preferences(context).getInt(POSITION_Y, NO_POSITION)
        if (saved != NO_POSITION && saved < screenHeight) return saved
        return context.dp(DEFAULT_TOP_DP)
    }

    fun remember(context: Context, x: Int, y: Int) {
        preferences(context).edit()
            .putInt(POSITION_X, x)
            .putInt(POSITION_Y, y)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(POSITION_PREFERENCES, Context.MODE_PRIVATE)

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
