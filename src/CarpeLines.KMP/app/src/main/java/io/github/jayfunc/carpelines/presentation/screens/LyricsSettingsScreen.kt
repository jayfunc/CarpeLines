package io.github.jayfunc.carpelines.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatLineSpacing
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.preferences.dataStore
import io.github.jayfunc.carpelines.data.preferences.FLOATING_LYRICS_ENABLED_KEY
import io.github.jayfunc.carpelines.data.preferences.LOCK_SCREEN_LYRICS_ENABLED_KEY
import io.github.jayfunc.carpelines.presentation.floatinglyrics.FloatingLyricsService
import io.github.jayfunc.carpelines.presentation.components.betterlyrics.BetterLyricsAmbientStyle
import io.github.jayfunc.carpelines.presentation.components.betterlyrics.SPECTRUM_BAR_COUNTS
import io.github.jayfunc.carpelines.presentation.components.betterlyrics.SpectrumPlacement
import io.github.jayfunc.carpelines.presentation.components.betterlyrics.SpectrumStyle
import io.github.jayfunc.carpelines.presentation.components.betterlyrics.isAmbientShaderSupported
import io.github.jayfunc.carpelines.presentation.components.lyrics.AMBIENT_ENABLED_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.AMBIENT_STYLE_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.DEFAULT_PLAYING_LINE_TOP_OFFSET
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_ALIGNMENT_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_ANNOTATION_FONT_RATIO_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_EDGE_FADE_REACH_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_EDGE_FEATHERING_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_FLOAT_EFFECT_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_FOLLOW_SCROLL_DURATION_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_FONT_FAMILY_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_FONT_WEIGHT_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_GLOW_EFFECT_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_LINE_SPACING_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_PLAYING_LINE_OFFSET_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_SCALE_EFFECT_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_SCROLL_RESUME_DELAY_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LYRICS_TEXT_COLOR_MODE_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.SPECTRUM_BAR_COUNT_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.SPECTRUM_BREATHING_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.SPECTRUM_ENABLED_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.SPECTRUM_GLOW_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.SPECTRUM_OPACITY_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.SPECTRUM_PLACEMENT_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.SPECTRUM_SENSITIVITY_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.SPECTRUM_STYLE_KEY
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsAlignment
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsFontFamily
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsFontWeight
import io.github.jayfunc.carpelines.presentation.components.lyrics.LyricsTextColorMode
import io.github.jayfunc.carpelines.presentation.components.lyrics.rememberLyricsDisplaySettings
import io.github.jayfunc.carpelines.presentation.components.lyrics.resetLyricsDisplayPreferences
import io.github.jayfunc.carpelines.data.preferences.MAX_LYRICS_FONT_SIZE_SP
import io.github.jayfunc.carpelines.data.preferences.MIN_LYRICS_FONT_SIZE_SP
import io.github.jayfunc.carpelines.presentation.components.CollapsibleCommonTopBar
import io.github.jayfunc.carpelines.presentation.components.MiniPlayerHeight
import io.github.jayfunc.carpelines.presentation.components.rememberSyncedSliderState
import io.github.jayfunc.carpelines.presentation.settings.search.LocalSettingsHighlightTarget
import io.github.jayfunc.carpelines.presentation.settings.search.settingHighlight
import io.github.jayfunc.carpelines.presentation.viewmodel.SettingsViewModel
import io.github.jayfunc.carpelines.ui.theme.m3eContentWidth
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val MIN_PLAYING_LINE_OFFSET = 0.25f
private const val MAX_PLAYING_LINE_OFFSET = 0.6f

/**
 * The page's subsections, in display order. Each one owns the search-highlight keys of its rows,
 * so a search result can scroll the matching LazyColumn item into composition before that row
 * brings itself into view.
 */
private enum class LyricsSection(val itemKeys: Set<String>) {
    TYPOGRAPHY(
        setOf(
            "item_lyrics_font_size",
            "item_lyrics_font_family",
            "item_lyrics_font_weight",
            "item_lyrics_line_spacing",
            "item_lyrics_alignment"
        )
    ),
    COLOR(setOf("item_lyrics_text_color")),
    VOICES(
        setOf(
            "item_lyrics_annotation_font_ratio",
            "item_lyrics_duet_layout",
            "item_lyrics_background_vocals"
        )
    ),
    EDGE_FADE(
        setOf(
            "item_lyrics_edge_feathering",
            "item_lyrics_edge_fade_reach"
        )
    ),
    HIGHLIGHT_MOTION(
        setOf(
            "item_lyrics_animated_blur",
            "item_lyrics_glow",
            "item_lyrics_scale",
            "item_lyrics_float"
        )
    ),
    AMBIENT(setOf("item_lyrics_ambient_background")),
    SPECTRUM(
        setOf(
            "item_lyrics_spectrum_enabled",
            "item_lyrics_spectrum_style",
            "item_lyrics_spectrum_placement",
            "item_lyrics_spectrum_bars",
            "item_lyrics_spectrum_sensitivity",
            "item_lyrics_spectrum_opacity",
            "item_lyrics_spectrum_glow",
            "item_lyrics_spectrum_breathing",
        )
    ),
    PLAYING_LINE(setOf("item_lyrics_playing_line")),
    FOLLOW_SCROLL(
        setOf(
            "item_lyrics_scroll_duration",
            "item_lyrics_scroll_resume_delay"
        )
    ),
    FLOATING_OVERLAY(
        setOf(
            "item_lyrics_floating_enabled",
            "item_lyrics_lock_screen_enabled"
        )
    ),
    RESET(setOf("item_lyrics_reset_defaults"));

    companion object {
        /** Index of the LazyColumn item holding [itemKey]; 0 when the key is not mapped. */
        fun listIndex(itemKey: String): Int =
            LyricsSection.entries.indexOfFirst { itemKey in it.itemKeys }.coerceAtLeast(0)
    }
}

@Composable
fun LyricsSettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    highlightKey: String? = null
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var fontSizeDraft by remember(uiState.lyricsFontSizeSp) { mutableFloatStateOf(uiState.lyricsFontSizeSp) }
    var showResetDefaultsDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val displaySettings = rememberLyricsDisplaySettings()
    val visual = displaySettings.visual

    fun persistFloat(key: androidx.datastore.preferences.core.Preferences.Key<Float>, value: Float) {
        coroutineScope.launch { context.dataStore.edit { it[key] = value } }
    }

    fun persistBoolean(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        coroutineScope.launch { context.dataStore.edit { it[key] = value } }
    }

    fun persistString(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        coroutineScope.launch { context.dataStore.edit { it[key] = value } }
    }

    fun persistInt(key: androidx.datastore.preferences.core.Preferences.Key<Int>, value: Int) {
        coroutineScope.launch { context.dataStore.edit { it[key] = value } }
    }

    fun persistLong(key: androidx.datastore.preferences.core.Preferences.Key<Long>, value: Long) {
        coroutineScope.launch { context.dataStore.edit { it[key] = value } }
    }

    val ambientEnabledFlow = remember(context) {
        context.dataStore.data.map {
            it[AMBIENT_ENABLED_KEY] ?: true
        }
    }
    val playingLineOffsetFlow = remember(context) {
        context.dataStore.data.map {
            it[LYRICS_PLAYING_LINE_OFFSET_KEY] ?: DEFAULT_PLAYING_LINE_TOP_OFFSET
        }
    }
    val playingLineOffset by playingLineOffsetFlow.collectAsStateWithLifecycle(
        initialValue = DEFAULT_PLAYING_LINE_TOP_OFFSET
    )
    var lineOffsetDraft by remember(playingLineOffset) { mutableFloatStateOf(playingLineOffset) }
    val ambientEnabled by ambientEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val ambientStyleFlow = remember(context) {
        context.dataStore.data.map {
            BetterLyricsAmbientStyle.fromStorage(it[AMBIENT_STYLE_KEY])
        }
    }
    val ambientStyle by ambientStyleFlow.collectAsStateWithLifecycle(
        initialValue = BetterLyricsAmbientStyle.FLUID
    )

    val floatingLyricsEnabledFlow = remember(context) {
        context.dataStore.data.map { it[FLOATING_LYRICS_ENABLED_KEY] ?: false }
    }
    val floatingLyricsEnabled by floatingLyricsEnabledFlow.collectAsStateWithLifecycle(
        initialValue = false
    )
    val lockScreenLyricsEnabledFlow = remember(context) {
        context.dataStore.data.map { it[LOCK_SCREEN_LYRICS_ENABLED_KEY] ?: false }
    }
    val lockScreenLyricsEnabled by lockScreenLyricsEnabledFlow.collectAsStateWithLifecycle(
        initialValue = false
    )

    var pendingDesktopEnable by remember { mutableStateOf(false) }
    var pendingLockScreenEnable by remember { mutableStateOf(false) }
    var overlayPermissionDenied by remember { mutableStateOf(false) }
    var notificationPermissionDenied by remember { mutableStateOf(false) }
    var recordAudioPermissionDenied by remember { mutableStateOf(false) }
    var permissionRequestTick by remember { mutableIntStateOf(0) }

    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    // Both switches share one DataStore edit. Starting the service only after the edit commits
    // avoids the service reading the previous (both-off) values and stopping itself immediately.
    fun persistFloatingLyrics(desktop: Boolean, lockScreen: Boolean) {
        coroutineScope.launch {
            context.dataStore.edit { preferences ->
                preferences[FLOATING_LYRICS_ENABLED_KEY] = desktop
                preferences[LOCK_SCREEN_LYRICS_ENABLED_KEY] = lockScreen
            }
            if (desktop || lockScreen) {
                FloatingLyricsService.start(context)
            } else {
                FloatingLyricsService.stop(context)
            }
        }
    }

    fun applyPendingEnable() {
        val enableDesktop = pendingDesktopEnable
        val enableLockScreen = pendingLockScreenEnable
        if (!enableDesktop && !enableLockScreen) return
        pendingDesktopEnable = false
        pendingLockScreenEnable = false
        persistFloatingLyrics(
            desktop = floatingLyricsEnabled || enableDesktop,
            lockScreen = lockScreenLyricsEnabled || enableLockScreen,
        )
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        overlayPermissionDenied = !Settings.canDrawOverlays(context)
        if (!overlayPermissionDenied) {
            permissionRequestTick++
        } else {
            pendingDesktopEnable = false
            pendingLockScreenEnable = false
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionDenied = !granted
        if (granted) {
            permissionRequestTick++
        } else {
            pendingDesktopEnable = false
            pendingLockScreenEnable = false
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        recordAudioPermissionDenied = !granted
        if (granted) {
            persistBoolean(SPECTRUM_ENABLED_KEY, true)
        }
    }

    // Resolves the permission chain one step at a time: overlay (desktop only) → notifications →
    // commit the switch. Running it from a version tick lets each launcher request the next step
    // after it returns without forward references between local functions.
    LaunchedEffect(permissionRequestTick) {
        if (permissionRequestTick == 0) return@LaunchedEffect
        if (!pendingDesktopEnable && !pendingLockScreenEnable) return@LaunchedEffect

        if (pendingDesktopEnable && !Settings.canDrawOverlays(context)) {
            val packageUri = Uri.parse("package:${context.packageName}")
            val launched = runCatching {
                overlayPermissionLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
                )
            }.isSuccess
            if (!launched) {
                runCatching {
                    overlayPermissionLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    )
                }
            }
            return@LaunchedEffect
        }

        if (!hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return@LaunchedEffect
        }

        applyPendingEnable()
    }

    // Some ROMs return from the permission pages without an activity result, so re-check when the
    // app is in front again; the effect above only acts while a switch is waiting.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionRequestTick++
        if (displaySettings.spectrumEnabled && !hasRecordAudioPermission()) {
            recordAudioPermissionDenied = true
        } else if (hasRecordAudioPermission()) {
            recordAudioPermissionDenied = false
        }
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 180.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }
    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }

    LaunchedEffect(minTopBarHeightPx, maxTopBarHeightPx) {
        val clamped = topBarHeight.value.coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
        if (clamped != topBarHeight.value) topBarHeight.snapTo(clamped)
    }
    val collapseFraction = 1f - (
        (topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)
        ).coerceIn(0f, 1f)

    val nestedScrollConnection = remember(minTopBarHeightPx, maxTopBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val scrollingUp = available.y < 0f
                if (!scrollingUp && (
                        lazyListState.firstVisibleItemIndex > 0 ||
                            lazyListState.firstVisibleItemScrollOffset > 0
                        )
                ) return Offset.Zero

                val previous = topBarHeight.value
                val next = (previous + available.y).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = next - previous
                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(next) }
                }
                return if (scrollingUp && next == minTopBarHeightPx) Offset.Zero else Offset(0f, consumed)
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val canExpand = lazyListState.firstVisibleItemIndex == 0 &&
                lazyListState.firstVisibleItemScrollOffset == 0
            val expand = canExpand && topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2f
            topBarHeight.animateTo(
                targetValue = if (expand) maxTopBarHeightPx else minTopBarHeightPx,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    // The rows live in separate lazy items, so bring the target's item into composition
    // first; the row then scrolls itself clear of the bars. Skipped once the highlight was
    // consumed, so returning here or rotating does not scroll again.
    val highlightPending = LocalSettingsHighlightTarget.current != null
    LaunchedEffect(highlightKey) {
        if (highlightPending && !highlightKey.isNullOrBlank()) {
            delay(200)
            lazyListState.animateScrollToItem(LyricsSection.listIndex(highlightKey))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection),
        contentAlignment = Alignment.TopCenter
    ) {
        val currentTopBarHeight = with(density) { topBarHeight.value.toDp() }
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.m3eContentWidth().fillMaxSize(),
            contentPadding = PaddingValues(
                top = currentTopBarHeight + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = MiniPlayerHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        ) {
            item(key = LyricsSection.TYPOGRAPHY.name) {
                LyricsSettingsSubsection(title = stringResource(R.string.lyrics_style_section)) {
                    LyricsSliderItem(
                        title = stringResource(R.string.setcat_lyrics_font_size_title),
                        subtitle = stringResource(R.string.setcat_lyrics_font_size_subtitle),
                        valueText = stringResource(R.string.setcat_lyrics_font_size_value, fontSizeDraft),
                        icon = Icons.Rounded.FormatSize,
                        value = fontSizeDraft,
                        valueRange = MIN_LYRICS_FONT_SIZE_SP..MAX_LYRICS_FONT_SIZE_SP,
                        steps = (MAX_LYRICS_FONT_SIZE_SP - MIN_LYRICS_FONT_SIZE_SP).toInt() - 1,
                        onValueChange = { fontSizeDraft = it },
                        onValueChangeFinished = { settingsViewModel.setLyricsFontSize(fontSizeDraft) },
                        modifier = Modifier.settingHighlight("item_lyrics_font_size", highlightKey)
                    )

                    ThemeSelectorItem(
                        label = stringResource(R.string.lyrics_font_weight_title),
                        description = stringResource(R.string.lyrics_font_weight_subtitle),
                        options = LyricsFontWeight.entries.associate {
                            it.storageValue to stringResource(it.labelRes())
                        },
                        selectedKey = visual.fontWeight.storageValue,
                        onSelectionChanged = { value ->
                            persistString(LYRICS_FONT_WEIGHT_KEY, value)
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.FormatBold, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_font_weight", highlightKey)
                    )

                    ThemeSelectorItem(
                        label = stringResource(R.string.lyrics_font_family_title),
                        description = stringResource(R.string.lyrics_font_family_subtitle),
                        options = LyricsFontFamily.entries.associate {
                            it.storageValue to stringResource(it.labelRes())
                        },
                        selectedKey = visual.fontFamily.storageValue,
                        onSelectionChanged = { value ->
                            persistString(LYRICS_FONT_FAMILY_KEY, value)
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.FontDownload, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_font_family", highlightKey)
                    )

                    LyricsSettingSlider(
                        title = stringResource(R.string.lyrics_line_spacing_title),
                        subtitle = stringResource(R.string.lyrics_line_spacing_subtitle),
                        icon = Icons.Rounded.FormatLineSpacing,
                        value = visual.lineSpacingFactor,
                        valueRange = 0f..1.2f,
                        steps = 23,
                        valueText = { stringResource(R.string.lyrics_opacity_value, (it * 100f).roundToInt()) },
                        onValueCommit = { persistFloat(LYRICS_LINE_SPACING_KEY, it) },
                        modifier = Modifier.settingHighlight("item_lyrics_line_spacing", highlightKey)
                    )

                    ThemeSelectorItem(
                        label = stringResource(R.string.lyrics_alignment_title),
                        description = stringResource(R.string.lyrics_alignment_subtitle),
                        options = LyricsAlignment.entries.associate {
                            it.storageValue to stringResource(it.labelRes())
                        },
                        selectedKey = visual.alignment.storageValue,
                        onSelectionChanged = { value ->
                            persistString(LYRICS_ALIGNMENT_KEY, value)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Rounded.FormatAlignLeft,
                                null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_alignment", highlightKey)
                    )
                }
            }

            item(key = LyricsSection.COLOR.name) {
                LyricsSettingsSubsection(title = stringResource(R.string.lyrics_color_section)) {
                    ThemeSelectorItem(
                        label = stringResource(R.string.lyrics_text_color_title),
                        description = stringResource(R.string.lyrics_text_color_subtitle),
                        options = mapOf(
                            LyricsTextColorMode.ARTWORK.storageValue to
                                stringResource(R.string.lyrics_text_color_artwork),
                            LyricsTextColorMode.NEUTRAL.storageValue to
                                stringResource(R.string.lyrics_text_color_neutral),
                            LyricsTextColorMode.LIGHT.storageValue to
                                stringResource(R.string.lyrics_text_color_light),
                            LyricsTextColorMode.DARK.storageValue to
                                stringResource(R.string.lyrics_text_color_dark),
                        ),
                        selectedKey = visual.textColorMode.storageValue,
                        onSelectionChanged = { persistString(LYRICS_TEXT_COLOR_MODE_KEY, it) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Opacity, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_text_color", highlightKey)
                    )
                }
            }

            item(key = LyricsSection.VOICES.name) {
                LyricsSettingsSubsection(title = stringResource(R.string.lyrics_voices_section)) {
                    LyricsSettingSlider(
                        title = stringResource(R.string.lyrics_annotation_font_ratio_title),
                        subtitle = stringResource(R.string.lyrics_annotation_font_ratio_subtitle),
                        icon = Icons.Rounded.FormatSize,
                        value = visual.annotationFontRatio,
                        valueRange = 0.4f..1f,
                        steps = 11,
                        valueText = { stringResource(R.string.lyrics_ratio_value, (it * 100f).roundToInt()) },
                        onValueCommit = { persistFloat(LYRICS_ANNOTATION_FONT_RATIO_KEY, it) },
                        modifier = Modifier.settingHighlight("item_lyrics_annotation_font_ratio", highlightKey)
                    )

                    SwitchSettingItem(
                        title = stringResource(R.string.lyrics_duet_layout_title),
                        subtitle = stringResource(R.string.lyrics_duet_layout_subtitle),
                        checked = uiState.lyricsDuetLayoutEnabled,
                        onCheckedChange = settingsViewModel::setLyricsDuetLayoutEnabled,
                        leadingIcon = {
                            Icon(Icons.Rounded.Groups, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_duet_layout", highlightKey)
                    )

                    SwitchSettingItem(
                        title = stringResource(R.string.lyrics_background_vocals_title),
                        subtitle = stringResource(R.string.lyrics_background_vocals_subtitle),
                        checked = uiState.showLyricsBackgroundVocals,
                        onCheckedChange = settingsViewModel::setShowLyricsBackgroundVocals,
                        leadingIcon = {
                            Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_background_vocals", highlightKey)
                    )
                }
            }

            item(key = LyricsSection.EDGE_FADE.name) {
                LyricsSettingsSubsection(title = stringResource(R.string.lyrics_edge_fade_section)) {
                    SwitchSettingItem(
                        title = stringResource(R.string.lyrics_edge_feathering_title),
                        subtitle = stringResource(R.string.lyrics_edge_feathering_subtitle),
                        checked = visual.edgeFeatheringEffectEnabled,
                        onCheckedChange = { persistBoolean(LYRICS_EDGE_FEATHERING_KEY, it) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Gradient, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_edge_feathering", highlightKey)
                    )

                    AnimatedVisibility(
                        visible = visual.edgeFeatheringEffectEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        LyricsSettingSlider(
                            title = stringResource(R.string.lyrics_edge_fade_reach_title),
                            subtitle = stringResource(R.string.lyrics_edge_fade_reach_subtitle),
                            icon = Icons.Rounded.SwapVert,
                            value = visual.edgeFadeReach,
                            valueRange = 0f..1f,
                            steps = 19,
                            valueText = {
                                stringResource(
                                    R.string.lyrics_edge_fade_reach_value,
                                    (it * 100f).roundToInt()
                                )
                            },
                            onValueCommit = { persistFloat(LYRICS_EDGE_FADE_REACH_KEY, it) },
                            modifier = Modifier.settingHighlight("item_lyrics_edge_fade_reach", highlightKey)
                        )
                    }
                }
            }

            item(key = LyricsSection.HIGHLIGHT_MOTION.name) {
                LyricsSettingsSubsection(title = stringResource(R.string.lyrics_highlight_motion_section)) {
                    SwitchSettingItem(
                        title = stringResource(R.string.presentation_batch_f_exp_lyric_blur_title),
                        subtitle = stringResource(R.string.presentation_batch_f_exp_lyric_blur_subtitle),
                        checked = uiState.animatedLyricsBlurEnabled,
                        onCheckedChange = settingsViewModel::setAnimatedLyricsBlurEnabled,
                        leadingIcon = {
                            Icon(Icons.Rounded.BlurOn, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_animated_blur", highlightKey)
                    )

                    SwitchSettingItem(
                        title = stringResource(R.string.lyrics_glow_title),
                        subtitle = stringResource(R.string.lyrics_glow_subtitle),
                        checked = visual.glowEffectEnabled,
                        onCheckedChange = { persistBoolean(LYRICS_GLOW_EFFECT_KEY, it) },
                        leadingIcon = {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_glow", highlightKey)
                    )

                    SwitchSettingItem(
                        title = stringResource(R.string.lyrics_scale_effect_title),
                        subtitle = stringResource(R.string.lyrics_scale_effect_subtitle),
                        checked = visual.scaleEffectEnabled,
                        onCheckedChange = { persistBoolean(LYRICS_SCALE_EFFECT_KEY, it) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Opacity, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_scale", highlightKey)
                    )

                    SwitchSettingItem(
                        title = stringResource(R.string.lyrics_float_title),
                        subtitle = stringResource(R.string.lyrics_float_subtitle),
                        checked = visual.floatEffectEnabled,
                        onCheckedChange = { persistBoolean(LYRICS_FLOAT_EFFECT_KEY, it) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Animation, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_float", highlightKey)
                    )
                }
            }

            item(key = LyricsSection.AMBIENT.name) {
                LyricsSettingsSubsection(title = stringResource(R.string.lyrics_background_section)) {
                    SwitchSettingItem(
                        title = stringResource(R.string.betterlyrics_ambient_title),
                        subtitle = stringResource(R.string.betterlyrics_ambient_subtitle),
                        checked = ambientEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                context.dataStore.edit { preferences ->
                                    preferences[AMBIENT_ENABLED_KEY] = enabled
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Gradient, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_ambient_background", highlightKey)
                    )

                    AnimatedVisibility(
                        visible = ambientEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        ThemeSelectorItem(
                            label = stringResource(R.string.betterlyrics_ambient_style_title),
                            description = stringResource(R.string.betterlyrics_ambient_style_subtitle),
                            options = mapOf(
                                BetterLyricsAmbientStyle.FLUID.name to stringResource(R.string.betterlyrics_ambient_style_fluid),
                                BetterLyricsAmbientStyle.ALBUM_BLUR.name to stringResource(R.string.betterlyrics_ambient_style_album_blur),
                                BetterLyricsAmbientStyle.RAINDROP.name to stringResource(R.string.betterlyrics_ambient_style_raindrop),
                                BetterLyricsAmbientStyle.SNOW.name to stringResource(R.string.betterlyrics_ambient_style_snow),
                            ),
                            // The two overlay shaders are AGSL, so they need Android 13. Older pages
                            // keep the backdrop; the options are listed but cannot be picked.
                            optionDescriptions = if (isAmbientShaderSupported) {
                                emptyMap()
                            } else {
                                mapOf(
                                    BetterLyricsAmbientStyle.RAINDROP.name to
                                        stringResource(R.string.betterlyrics_ambient_style_needs_android13),
                                    BetterLyricsAmbientStyle.SNOW.name to
                                        stringResource(R.string.betterlyrics_ambient_style_needs_android13),
                                )
                            },
                            disabledOptionKeys = if (isAmbientShaderSupported) {
                                emptySet()
                            } else {
                                setOf(
                                    BetterLyricsAmbientStyle.RAINDROP.name,
                                    BetterLyricsAmbientStyle.SNOW.name,
                                )
                            },
                            selectedKey = ambientStyle.name,
                            onSelectionChanged = { key ->
                                val style = BetterLyricsAmbientStyle.entries
                                    .firstOrNull { it.name == key } ?: return@ThemeSelectorItem
                                coroutineScope.launch {
                                    context.dataStore.edit { preferences ->
                                        preferences[AMBIENT_STYLE_KEY] = style.name
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Gradient, null, tint = MaterialTheme.colorScheme.secondary)
                            }
                        )
                    }
                }
            }

            item(key = LyricsSection.SPECTRUM.name) {
                LyricsSettingsSubsection(title = stringResource(R.string.lyrics_spectrum_section)) {
                    SwitchSettingItem(
                        title = stringResource(R.string.betterlyrics_spectrum_title),
                        subtitle = stringResource(R.string.betterlyrics_spectrum_subtitle),
                        checked = displaySettings.spectrumEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                recordAudioPermissionDenied = false
                                persistBoolean(SPECTRUM_ENABLED_KEY, false)
                            } else if (hasRecordAudioPermission()) {
                                recordAudioPermissionDenied = false
                                persistBoolean(SPECTRUM_ENABLED_KEY, true)
                            } else {
                                recordAudioPermissionDenied = false
                                recordAudioPermissionLauncher.launch(
                                    Manifest.permission.RECORD_AUDIO
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Gradient,
                                null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        modifier = Modifier.settingHighlight(
                            "item_lyrics_spectrum_enabled",
                            highlightKey
                        )
                    )

                    if (recordAudioPermissionDenied) {
                        Text(
                            text = stringResource(R.string.betterlyrics_spectrum_permission_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = displaySettings.spectrumEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            ThemeSelectorItem(
                                label = stringResource(R.string.betterlyrics_spectrum_style_title),
                                description = stringResource(R.string.betterlyrics_spectrum_style_subtitle),
                                options = mapOf(
                                    SpectrumStyle.CURVE.name to stringResource(R.string.betterlyrics_spectrum_style_curve),
                                    SpectrumStyle.BAR.name to stringResource(R.string.betterlyrics_spectrum_style_bar),
                                ),
                                selectedKey = displaySettings.spectrumStyle.name,
                                onSelectionChanged = { key ->
                                    val style = SpectrumStyle.entries
                                        .firstOrNull { it.name == key } ?: return@ThemeSelectorItem
                                    persistString(SPECTRUM_STYLE_KEY, style.name)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Gradient,
                                        null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                modifier = Modifier.settingHighlight(
                                    "item_lyrics_spectrum_style",
                                    highlightKey
                                )
                            )

                            ThemeSelectorItem(
                                label = stringResource(R.string.betterlyrics_spectrum_placement_title),
                                description = stringResource(R.string.betterlyrics_spectrum_placement_subtitle),
                                options = mapOf(
                                    SpectrumPlacement.BOTTOM.name to stringResource(R.string.betterlyrics_spectrum_placement_bottom),
                                    SpectrumPlacement.TOP.name to stringResource(R.string.betterlyrics_spectrum_placement_top),
                                ),
                                selectedKey = displaySettings.spectrumPlacement.name,
                                onSelectionChanged = { key ->
                                    val placement = SpectrumPlacement.entries
                                        .firstOrNull { it.name == key } ?: return@ThemeSelectorItem
                                    persistString(SPECTRUM_PLACEMENT_KEY, placement.name)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.SwapVert,
                                        null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                modifier = Modifier.settingHighlight(
                                    "item_lyrics_spectrum_placement",
                                    highlightKey
                                )
                            )

                            ThemeSelectorItem(
                                label = stringResource(R.string.betterlyrics_spectrum_bars_title),
                                description = stringResource(R.string.betterlyrics_spectrum_bars_subtitle),
                                options = SPECTRUM_BAR_COUNTS.associate { it.toString() to it.toString() },
                                selectedKey = displaySettings.spectrumBarCount.toString(),
                                onSelectionChanged = { key ->
                                    key.toIntOrNull()?.let {
                                        persistInt(SPECTRUM_BAR_COUNT_KEY, it)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Gradient,
                                        null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                modifier = Modifier.settingHighlight(
                                    "item_lyrics_spectrum_bars",
                                    highlightKey
                                )
                            )

                            LyricsSettingSlider(
                                title = stringResource(R.string.betterlyrics_spectrum_sensitivity_title),
                                subtitle = stringResource(R.string.betterlyrics_spectrum_sensitivity_subtitle),
                                icon = Icons.Rounded.Animation,
                                value = displaySettings.spectrumSensitivity,
                                valueRange = 0.5f..2f,
                                steps = 15,
                                valueText = {
                                    stringResource(
                                        R.string.betterlyrics_spectrum_sensitivity_value,
                                        (it * 100f).roundToInt()
                                    )
                                },
                                onValueCommit = {
                                    persistFloat(SPECTRUM_SENSITIVITY_KEY, it)
                                },
                                modifier = Modifier.settingHighlight(
                                    "item_lyrics_spectrum_sensitivity",
                                    highlightKey
                                )
                            )

                            LyricsSettingSlider(
                                title = stringResource(R.string.betterlyrics_spectrum_opacity_title),
                                subtitle = stringResource(R.string.betterlyrics_spectrum_opacity_subtitle),
                                icon = Icons.Rounded.Opacity,
                                value = displaySettings.spectrumOpacity,
                                valueRange = 0.2f..1f,
                                steps = 15,
                                valueText = {
                                    stringResource(
                                        R.string.betterlyrics_spectrum_opacity_value,
                                        (it * 100f).roundToInt()
                                    )
                                },
                                onValueCommit = {
                                    persistFloat(SPECTRUM_OPACITY_KEY, it)
                                },
                                modifier = Modifier.settingHighlight(
                                    "item_lyrics_spectrum_opacity",
                                    highlightKey
                                )
                            )

                            SwitchSettingItem(
                                title = stringResource(R.string.betterlyrics_spectrum_glow_title),
                                subtitle = stringResource(R.string.betterlyrics_spectrum_glow_subtitle),
                                checked = displaySettings.spectrumGlowEnabled,
                                onCheckedChange = {
                                    persistBoolean(SPECTRUM_GLOW_KEY, it)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                modifier = Modifier.settingHighlight(
                                    "item_lyrics_spectrum_glow",
                                    highlightKey
                                )
                            )

                            SwitchSettingItem(
                                title = stringResource(R.string.betterlyrics_spectrum_breathing_title),
                                subtitle = stringResource(R.string.betterlyrics_spectrum_breathing_subtitle),
                                checked = displaySettings.spectrumBreathingEnabled,
                                onCheckedChange = {
                                    persistBoolean(SPECTRUM_BREATHING_KEY, it)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Animation,
                                        null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                modifier = Modifier.settingHighlight(
                                    "item_lyrics_spectrum_breathing",
                                    highlightKey
                                )
                            )

                            Text(
                                text = stringResource(R.string.betterlyrics_spectrum_player_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            item(key = LyricsSection.PLAYING_LINE.name) {
                LyricsSettingsSubsection(title = stringResource(R.string.lyrics_playing_line_section)) {
                    LyricsSliderItem(
                        title = stringResource(R.string.lyrics_playing_line_title),
                        subtitle = stringResource(R.string.lyrics_playing_line_subtitle),
                        valueText = stringResource(
                            R.string.lyrics_playing_line_value,
                            (lineOffsetDraft * 100f).roundToInt()
                        ),
                        icon = Icons.Rounded.SwapVert,
                        value = lineOffsetDraft,
                        valueRange = MIN_PLAYING_LINE_OFFSET..MAX_PLAYING_LINE_OFFSET,
                        steps = 34,
                        onValueChange = { lineOffsetDraft = it },
                        onValueChangeFinished = {
                            coroutineScope.launch {
                                context.dataStore.edit { preferences ->
                                    preferences[LYRICS_PLAYING_LINE_OFFSET_KEY] = lineOffsetDraft
                                }
                            }
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_playing_line", highlightKey)
                    )
                }
            }

            item(key = LyricsSection.FOLLOW_SCROLL.name) {
                LyricsSettingsSubsection(title = stringResource(R.string.lyrics_follow_scroll_section)) {
                    LyricsSettingSlider(
                        title = stringResource(R.string.lyrics_scroll_duration_title),
                        subtitle = stringResource(R.string.lyrics_scroll_duration_subtitle),
                        icon = Icons.Rounded.Animation,
                        value = visual.followScrollDurationMs.toFloat(),
                        valueRange = 0f..1_200f,
                        steps = 23,
                        valueText = {
                            stringResource(
                                R.string.lyrics_scroll_duration_value,
                                it.roundToInt()
                            )
                        },
                        onValueCommit = {
                            persistInt(LYRICS_FOLLOW_SCROLL_DURATION_KEY, it.roundToInt())
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_scroll_duration", highlightKey)
                    )

                    LyricsSettingSlider(
                        title = stringResource(R.string.lyrics_scroll_resume_delay_title),
                        subtitle = stringResource(R.string.lyrics_scroll_resume_delay_subtitle),
                        icon = Icons.Rounded.Animation,
                        value = visual.scrollResumeDelayMs.toFloat(),
                        valueRange = 0f..5_000f,
                        steps = 49,
                        valueText = {
                            stringResource(
                                R.string.lyrics_scroll_resume_delay_value,
                                it / 1_000f
                            )
                        },
                        onValueCommit = {
                            persistLong(LYRICS_SCROLL_RESUME_DELAY_KEY, it.roundToInt().toLong())
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_scroll_resume_delay", highlightKey)
                    )
                }
            }

            item(key = LyricsSection.FLOATING_OVERLAY.name) {
                LyricsSettingsSubsection(title = stringResource(R.string.floating_lyrics_section)) {
                    SwitchSettingItem(
                        title = stringResource(R.string.floating_lyrics_desktop_title),
                        subtitle = stringResource(R.string.floating_lyrics_desktop_subtitle),
                        checked = floatingLyricsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                pendingDesktopEnable = true
                                permissionRequestTick++
                            } else {
                                overlayPermissionDenied = false
                                persistFloatingLyrics(false, lockScreenLyricsEnabled)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.PictureInPictureAlt,
                                null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_floating_enabled", highlightKey)
                    )

                    SwitchSettingItem(
                        title = stringResource(R.string.floating_lyrics_lock_screen_title),
                        subtitle = stringResource(R.string.floating_lyrics_lock_screen_subtitle),
                        checked = lockScreenLyricsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                pendingLockScreenEnable = true
                                permissionRequestTick++
                            } else {
                                notificationPermissionDenied = false
                                persistFloatingLyrics(floatingLyricsEnabled, false)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Lock,
                                null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_lock_screen_enabled", highlightKey)
                    )

                    if (lockScreenLyricsEnabled) {
                        SettingsItem(
                            title = stringResource(R.string.floating_lyrics_lock_notification_title),
                            subtitle = stringResource(R.string.floating_lyrics_lock_notification_subtitle),
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            .putExtra(
                                                Settings.EXTRA_CHANNEL_ID,
                                                FloatingLyricsService.NOTIFICATION_CHANNEL_ID
                                            )
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        )
                    }

                    if (overlayPermissionDenied) {
                        Text(
                            text = stringResource(R.string.floating_lyrics_permission_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    if (notificationPermissionDenied) {
                        Text(
                            text = stringResource(R.string.floating_lyrics_notification_permission_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            item(key = LyricsSection.RESET.name) {
                LyricsSettingsSubsection(
                    title = stringResource(R.string.lyrics_reset_section),
                    addBottomSpace = false
                ) {
                    SettingsItem(
                        title = stringResource(R.string.lyrics_reset_defaults_title),
                        subtitle = stringResource(R.string.lyrics_reset_defaults_subtitle),
                        leadingIcon = {
                            Icon(Icons.Rounded.RestartAlt, null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.settingHighlight("item_lyrics_reset_defaults", highlightKey),
                        onClick = { showResetDefaultsDialog = true }
                    )
                }
            }
        }

        CollapsibleCommonTopBar(
            title = stringResource(R.string.setcat_lyrics_title),
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeight,
            onBackClick = { navController.popBackStack() }
        )
    }

    if (showResetDefaultsDialog) {
        AlertDialog(
            icon = { Icon(Icons.Rounded.RestartAlt, null) },
            title = { Text(stringResource(R.string.dialog_reset_lyrics_settings_title)) },
            text = { Text(stringResource(R.string.dialog_reset_lyrics_settings_body)) },
            onDismissRequest = { showResetDefaultsDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDefaultsDialog = false
                        coroutineScope.launch { resetLyricsDisplayPreferences(context) }
                    }
                ) {
                    Text(stringResource(R.string.confirm), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDefaultsDialog = false }) {
                    Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        )
    }
}

@Composable
private fun LyricsSettingsSubsection(
    title: String,
    addBottomSpace: Boolean = true,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        content()
    }
    if (addBottomSpace) {
        Spacer(modifier = Modifier.height(10.dp))
    }
}

/**
 * A slider whose draft lives next to the persisted value.
 *
 * The preference flow only updates after the drag ends, so binding the slider straight to it would
 * make the thumb jump back to the old value on every frame.
 */
@Composable
private fun LyricsSettingSlider(
    title: String,
    subtitle: String,
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: @Composable (Float) -> String,
    onValueCommit: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    LyricsSliderItem(
        title = title,
        subtitle = subtitle,
        valueText = valueText(draft),
        icon = icon,
        value = draft,
        valueRange = valueRange,
        steps = steps,
        onValueChange = { draft = it },
        onValueChangeFinished = { onValueCommit(draft) },
        modifier = modifier,
    )
}

private fun LyricsFontWeight.labelRes(): Int = when (this) {
    LyricsFontWeight.NORMAL -> R.string.lyrics_font_weight_normal
    LyricsFontWeight.MEDIUM -> R.string.lyrics_font_weight_medium
    LyricsFontWeight.BOLD -> R.string.lyrics_font_weight_bold
}

private fun LyricsFontFamily.labelRes(): Int = when (this) {
    LyricsFontFamily.DEFAULT -> R.string.lyrics_font_family_default
    LyricsFontFamily.SANS_SERIF -> R.string.lyrics_font_family_sans
    LyricsFontFamily.SERIF -> R.string.lyrics_font_family_serif
    LyricsFontFamily.MONOSPACE -> R.string.lyrics_font_family_monospace
    LyricsFontFamily.CURSIVE -> R.string.lyrics_font_family_cursive
}

private fun LyricsAlignment.labelRes(): Int = when (this) {
    LyricsAlignment.LEFT -> R.string.lyrics_alignment_left
    LyricsAlignment.RIGHT -> R.string.lyrics_alignment_right
}

@Composable
private fun LyricsSliderItem(
    title: String,
    subtitle: String,
    valueText: String,
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(end = 8.dp)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                text = valueText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Slider(
                state = rememberSyncedSliderState(
                    value = value,
                    steps = steps,
                    valueRange = valueRange
                ),
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,)
        }
    }
}
