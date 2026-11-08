package io.github.jayfunc.carpelines.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

/** Whether the current lyric line is shown in a floating window above other apps. */
val FLOATING_LYRICS_ENABLED_KEY = booleanPreferencesKey("floating_lyrics_enabled")

/** Whether the floating window may also stay on top of the lock screen. */
val LOCK_SCREEN_LYRICS_ENABLED_KEY = booleanPreferencesKey("lock_screen_lyrics_enabled")

const val DEFAULT_FLOATING_LYRICS_ENABLED = false
const val DEFAULT_LOCK_SCREEN_LYRICS_ENABLED = false
