package io.github.jayfunc.carpelines.utils

/**
 * Brand wordmark "CarpeLines" split into its two halves, "Carpe" + "Lines".
 *
 * Kept in one place so the About hero and the empty artwork placeholder can't
 * drift apart. Latin only: the wordmark is never translated.
 */
object BrandName {
    const val FULL = "CarpeLines"
    const val HEAD = "Carpe"
    const val TAIL = "Lines"

    /** Code-unit index where [HEAD] ends and [TAIL] begins. */
    const val HEAD_LENGTH = 5
}
