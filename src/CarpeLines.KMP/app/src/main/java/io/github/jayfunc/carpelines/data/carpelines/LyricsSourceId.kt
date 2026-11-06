package io.github.jayfunc.carpelines.data.betterlyrics

import io.github.jayfunc.carpelines.lyrics.model.LyricsProvider

/** User-reorderable lyrics sources. Local entries have no remote provider. */
enum class LyricsSourceId(val provider: LyricsProvider?) {
    EMBEDDED(null),
    LOCAL_LRC(null),
    QQ_MUSIC(LyricsProvider.QQ_MUSIC),
    NETEASE(LyricsProvider.NETEASE),
    KUGOU(LyricsProvider.KUGOU),
    LRCLIB(LyricsProvider.LRCLIB),
    APPLE_MUSIC(LyricsProvider.APPLE_MUSIC);

    val isLocal: Boolean
        get() = provider == null

    companion object {
        val DefaultOrder: List<LyricsSourceId> = listOf(
            EMBEDDED,
            LOCAL_LRC,
            QQ_MUSIC,
            NETEASE,
            KUGOU,
            LRCLIB,
            APPLE_MUSIC,
        )
    }
}
