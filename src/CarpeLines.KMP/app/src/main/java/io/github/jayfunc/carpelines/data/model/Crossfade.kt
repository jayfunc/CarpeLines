package io.github.jayfunc.carpelines.data.model

/** Volume envelope used for the outgoing and incoming side of a crossfade. */
enum class Curve {
    LINEAR,
    S_CURVE,
    LOG,
    EXP
}

/**
 * The single crossfade applied between consecutive songs, read from the playback settings.
 * Per-playlist and per-track rules are gone, so this is the only shape the engine needs.
 */
data class CrossfadeSettings(
    val durationMs: Int,
    val curveIn: Curve = Curve.LINEAR,
    val curveOut: Curve = Curve.LINEAR
)
