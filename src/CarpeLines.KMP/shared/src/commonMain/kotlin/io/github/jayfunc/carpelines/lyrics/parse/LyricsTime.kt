package io.github.jayfunc.carpelines.lyrics.parse

/**
 * Parses the time expressions used by the lyrics formats we support.
 *
 * LRC uses `[mm:ss.xx]` / `[mm:ss:xx]`, TTML uses clock values (`hh:mm:ss.fff`, `mm:ss.fff`),
 * frame values (`hh:mm:ss:ff`) or offset values (`100ms`, `2.5s`, `1m`), and QRC uses plain
 * milliseconds.
 */
object LyricsTime {

    private const val DEFAULT_FRAME_RATE = 25.0

    /** Parses an LRC timestamp such as `01:23.45`, `01:23.456` or `01:23:45`. */
    fun parseLrcTimestamp(value: String, defaultFrameRate: Double = DEFAULT_FRAME_RATE): Long? {
        val parts = value.trim().split(':', '.')
        if (parts.size < 2) return null
        val minutes = parts[0].toIntOrNull() ?: return null
        val seconds = parts[1].toIntOrNull() ?: return null
        val fraction = parts.getOrNull(2)?.toLongOrNull() ?: 0L
        val fractionMs = when {
            parts.size < 3 -> 0L
            parts[2].length >= 3 -> parts[2].take(3).toLong()
            parts[2].length == 2 -> fraction * 10L
            else -> fraction * 100L
        }
        if (parts.size == 4) {
            // `hh:mm:ss:ff` style frame values are not valid LRC; reject rather than mis-time.
            return null
        }
        return minutes.toLong() * 60_000L + seconds.toLong() * 1_000L + fractionMs
    }

    /**
     * Parses a TTML clock, frame or offset time. Returns milliseconds or null when the expression
     * cannot be understood, which lets callers fall back to a parent/next value.
     */
    fun parseTtmlTime(value: String?, defaultFrameRate: Double = DEFAULT_FRAME_RATE): Long? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        val offset = parseOffset(text)
        if (offset != null) return offset

        val parts = text.split(':')
        return try {
            when (parts.size) {
                4 -> {
                    // hh:mm:ss:ff (frames at the document's frame rate).
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val seconds = parts[2].toDouble()
                    val frames = parts[3].toLong()
                    (hours * 3_600L + minutes * 60L) * 1_000L +
                        (seconds * 1_000.0).toLong() +
                        (frames * 1_000.0 / defaultFrameRate).toLong()
                }
                3 -> {
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val seconds = parts[2].toDouble()
                    (hours * 3_600L + minutes * 60L) * 1_000L + (seconds * 1_000.0).toLong()
                }
                2 -> {
                    val minutes = parts[0].toLong()
                    val seconds = parts[1].toDouble()
                    minutes * 60_000L + (seconds * 1_000.0).toLong()
                }
                1 -> (parts[0].toDouble() * 1_000.0).toLong()
                else -> null
            }
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun parseOffset(text: String): Long? {
        val match = Regex("""^([0-9]+(?:\.[0-9]+)?)(ms|h|m|s|f)$""", RegexOption.IGNORE_CASE)
            .matchEntire(text) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null
        return when (match.groupValues[2].lowercase()) {
            "h" -> (amount * 3_600_000.0).toLong()
            "m" -> (amount * 60_000.0).toLong()
            "s" -> (amount * 1_000.0).toLong()
            "ms" -> amount.toLong()
            "f" -> (amount * 1_000.0 / DEFAULT_FRAME_RATE).toLong()
            else -> null
        }
    }

    /** Formats milliseconds as an LRC `[mm:ss.xx]` timestamp (hundredths precision). */
    fun formatLrcTimestamp(timeMs: Long): String {
        val safe = timeMs.coerceAtLeast(0L)
        val totalSeconds = safe / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val hundredths = (safe % 1_000L) / 10L
        return "[${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${
            hundredths.toString().padStart(2, '0')}]"
    }
}
