package io.github.jayfunc.carpelines.data.model

/**
 * One point on the lyric timeline, resolved for a playback position.
 *
 * [nextText] is the following sung line, so surfaces that only have room for a small second
 * line (widgets, the floating overlay) can preview what comes next instead of showing nothing.
 */
data class TimelineLine(
    val text: String,
    val translation: String?,
    val romanization: String?,
    val nextText: String?,
)

/** Whether there is anything a compact lyrics surface can draw. */
fun Lyrics.hasTimelineContent(): Boolean =
    !synced.isNullOrEmpty() || !plain.isNullOrEmpty()

/**
 * The line being sung at [positionMs]. Falls back to the first line for unsynced payloads, and
 * reports null only when the lyrics carry no text at all.
 */
fun Lyrics.lineAt(positionMs: Long): TimelineLine? {
    val syncedLines = synced.orEmpty().filter { it.line.isNotBlank() }
    if (syncedLines.isNotEmpty()) {
        val index = syncedLines.indexOfLast { it.time <= positionMs }.coerceAtLeast(0)
        val line = syncedLines[index]
        return TimelineLine(
            text = line.line,
            translation = line.translation?.takeIf { it.isNotBlank() },
            romanization = line.romanization?.takeIf { it.isNotBlank() },
            nextText = syncedLines.getOrNull(index + 1)?.line?.takeIf { it.isNotBlank() },
        )
    }

    val plainLines = plain.orEmpty().filter { it.isNotBlank() }
    val first = plainLines.firstOrNull() ?: return null
    return TimelineLine(
        text = first,
        translation = null,
        romanization = null,
        nextText = plainLines.getOrNull(1),
    )
}
