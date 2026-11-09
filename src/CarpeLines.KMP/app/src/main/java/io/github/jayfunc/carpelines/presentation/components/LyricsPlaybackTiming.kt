package io.github.jayfunc.carpelines.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import io.github.jayfunc.carpelines.data.model.SyncedLine
import io.github.jayfunc.carpelines.data.model.SyncedWord
import io.github.jayfunc.carpelines.utils.LyricsUtils
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToLong

private val LeadingTagRegex = Regex("^v\\d+:\\s*", RegexOption.IGNORE_CASE)

internal fun sanitizeLyricLineText(raw: String): String =
    LyricsUtils.stripLrcTimestamps(raw).replace(LeadingTagRegex, "").trimStart()

internal fun sanitizeSyncedWords(words: List<SyncedWord>): List<SyncedWord> =
    buildList {
        words.forEachIndexed { index, word ->
            val sanitized = if (index == 0) LeadingTagRegex.replace(word.word, "") else word.word
            val normalized = sanitized.trim()
            if (normalized.isEmpty()) return@forEachIndexed

            add(
                word.copy(
                    word = normalized,
                    startsNewWord = if (isEmpty()) true else word.startsNewWord,
                )
            )
        }
    }

@Composable
internal fun rememberInterpolatedPlaybackPosition(
    sampledPosition: State<Long>,
    isPlaying: Boolean,
    playbackSpeed: Float,
    positionOverrideMs: Long?,
): State<Long> {
    val interpolatedPosition = remember {
        mutableLongStateOf(positionOverrideMs ?: sampledPosition.value)
    }

    LaunchedEffect(sampledPosition, isPlaying, playbackSpeed, positionOverrideMs) {
        positionOverrideMs?.let {
            interpolatedPosition.longValue = it.coerceAtLeast(0L)
            return@LaunchedEffect
        }

        val safePlaybackSpeed = playbackSpeed.takeIf { it.isFinite() && it > 0f } ?: 1f
        snapshotFlow { sampledPosition.value }.collectLatest { sampledPositionMs ->
            val anchorPositionMs = sampledPositionMs.coerceAtLeast(0L)
            interpolatedPosition.longValue = anchorPositionMs
            if (!isPlaying) return@collectLatest

            val anchorFrameNanos = withFrameNanos { it }
            while (true) {
                val frameNanos = withFrameNanos { it }
                val elapsedMs = ((frameNanos - anchorFrameNanos) / 1_000_000L).coerceAtLeast(0L)
                interpolatedPosition.longValue = anchorPositionMs + (elapsedMs * safePlaybackSpeed).roundToLong()
            }
        }
    }

    return interpolatedPosition
}

internal fun resolveSeekPositionMs(
    lineTimeMs: Long,
    lyricsSyncOffsetMs: Int,
): Long = (lineTimeMs - lyricsSyncOffsetMs.toLong()).coerceAtLeast(0L)
