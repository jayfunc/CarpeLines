package io.github.jayfunc.carpelines.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

/** Whether lines marked for different singers (`v1:`, `v2:`) sit on opposite sides. */
val LYRICS_DUET_LAYOUT_KEY = booleanPreferencesKey("lyrics_duet_layout_enabled")

/** Whether `[bg: …]` backing vocals are shown under their line. */
val SHOW_LYRICS_BACKGROUND_VOCALS_KEY = booleanPreferencesKey("show_lyrics_background_vocals")

const val DEFAULT_LYRICS_DUET_LAYOUT_ENABLED = true
const val DEFAULT_SHOW_LYRICS_BACKGROUND_VOCALS = true
