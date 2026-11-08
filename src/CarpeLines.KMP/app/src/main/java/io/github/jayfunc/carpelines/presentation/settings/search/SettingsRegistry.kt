package io.github.jayfunc.carpelines.presentation.settings.search

import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.presentation.model.SettingsCategory
import io.github.jayfunc.carpelines.presentation.navigation.Screen
import io.github.jayfunc.carpelines.presentation.navigation.navigateSafely

object SettingsRegistry {

    val allSettings: List<SettingSpec> by lazy {
        listOf(
            // --- LYRICS SUBSCREEN ---
            SettingSpec(
                id = "playback_lyrics_subscreen",
                itemKey = "item_playback_lyrics_subscreen",
                titleRes = R.string.setcat_lyrics_title,
                subtitleRes = R.string.setcat_lyrics_subtitle,
                category = SettingsCategory.LYRICS,
                subscreenRoute = Screen.LyricsSettings.createRoute(),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("lyrics", "words", "text", "font", "lrc", "synced lyrics")
            ),
            SettingSpec(
                id = "lyrics_font_size",
                itemKey = "item_lyrics_font_size",
                titleRes = R.string.setcat_lyrics_font_size_title,
                subtitleRes = R.string.setcat_lyrics_font_size_subtitle,
                category = SettingsCategory.LYRICS,
                subscreenRoute = Screen.LyricsSettings.createRoute(),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("lyrics size", "text size", "font size", "bigger lyrics", "accessibility")
            ),
            SettingSpec(
                id = "lyrics_alignment",
                itemKey = "item_lyrics_alignment",
                titleRes = R.string.lyrics_alignment_title,
                subtitleRes = R.string.lyrics_alignment_subtitle,
                category = SettingsCategory.LYRICS,
                subscreenRoute = Screen.LyricsSettings.createRoute(),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf(
                    "lyrics alignment", "align", "left", "right", "position", "justify",
                    "歌词对齐", "对齐", "左对齐", "右对齐",
                )
            ),
            SettingSpec(
                id = "lyrics_duet_layout",
                itemKey = "item_lyrics_duet_layout",
                titleRes = R.string.lyrics_duet_layout_title,
                subtitleRes = R.string.lyrics_duet_layout_subtitle,
                category = SettingsCategory.LYRICS,
                subscreenRoute = Screen.LyricsSettings.createRoute(),
                type = SettingType.SWITCH,
                keywordsStatic = listOf("duet", "singer", "voices", "v1", "v2", "alignment"),
                getValue = { it.lyricsDuetLayoutEnabled },
                onToggle = { viewModel, checked -> viewModel.setLyricsDuetLayoutEnabled(checked) }
            ),
            SettingSpec(
                id = "lyrics_background_vocals",
                itemKey = "item_lyrics_background_vocals",
                titleRes = R.string.lyrics_background_vocals_title,
                subtitleRes = R.string.lyrics_background_vocals_subtitle,
                category = SettingsCategory.LYRICS,
                subscreenRoute = Screen.LyricsSettings.createRoute(),
                type = SettingType.SWITCH,
                keywordsStatic = listOf("background vocals", "backing vocals", "bg", "adlib", "harmony"),
                getValue = { it.showLyricsBackgroundVocals },
                onToggle = { viewModel, checked -> viewModel.setShowLyricsBackgroundVocals(checked) }
            ),
            SettingSpec(
                id = "lyrics_animated_blur",
                itemKey = "item_lyrics_animated_blur",
                titleRes = R.string.presentation_batch_f_exp_lyric_blur_title,
                subtitleRes = R.string.presentation_batch_f_exp_lyric_blur_subtitle,
                category = SettingsCategory.LYRICS,
                subscreenRoute = Screen.LyricsSettings.createRoute(),
                type = SettingType.SWITCH,
                keywordsStatic = listOf("lyrics blur", "animated blur", "depth of field", "blur strength"),
                getValue = { it.animatedLyricsBlurEnabled },
                onToggle = { viewModel, checked -> viewModel.setAnimatedLyricsBlurEnabled(checked) }
            ),
            SettingSpec(
                id = "lyrics_provider_settings",
                itemKey = "item_lyrics_provider_settings",
                titleRes = R.string.betterlyrics_settings_title,
                subtitleRes = R.string.betterlyrics_settings_subtitle,
                category = SettingsCategory.LYRICS_MANAGEMENT,
                subscreenRoute = Screen.LyricsManagement.createRoute(),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf(
                    "apple music", "token", "media-user-token", "libretranslate", "translate server",
                    "translation", "romanization", "transliteration",
                    "歌词源设置", "令牌", "翻译", "音译", "服务器",
                )
            ),
            SettingSpec(
                id = "lyrics_source_order",
                itemKey = "item_lyrics_source_order",
                titleRes = R.string.lyrics_source_order_title,
                subtitleRes = R.string.lyrics_source_order_subtitle,
                category = SettingsCategory.LYRICS_MANAGEMENT,
                subscreenRoute = Screen.LyricsManagement.createRoute(),
                type = SettingType.ACTION,
                onAction = {
                    it.navigateSafely(
                        Screen.LyricsManagement.createRoute("item_lyrics_source_order")
                    )
                },
                keywordsStatic = listOf(
                    "lyrics source", "source order", "priority", "embedded", "lrc", "local",
                    "qq", "netease", "kugou", "lrclib", "apple", "online lyrics", "external lyrics",
                    "歌词源", "顺序", "优先级", "本地", "内嵌", "在线", "在线歌词", "在线服务",
                )
            ),
            SettingSpec(
                id = "lyrics_ambient_background",
                itemKey = "item_lyrics_ambient_background",
                titleRes = R.string.betterlyrics_ambient_title,
                subtitleRes = R.string.betterlyrics_ambient_subtitle,
                category = SettingsCategory.LYRICS,
                subscreenRoute = Screen.LyricsSettings.createRoute(),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf(
                    "ambient", "fluid", "gradient", "album blur", "background",
                    "raindrop", "rain", "snow", "snowflake",
                    "氛围", "流体", "渐变", "模糊", "背景", "雨滴", "雨", "下雪", "雪",
                )
            ),
            SettingSpec(
                id = "lyrics_reset_defaults",
                itemKey = "item_lyrics_reset_defaults",
                titleRes = R.string.lyrics_reset_defaults_title,
                subtitleRes = R.string.lyrics_reset_defaults_subtitle,
                category = SettingsCategory.LYRICS,
                subscreenRoute = Screen.LyricsSettings.createRoute(),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf(
                    "reset lyrics settings", "reset appearance", "restore defaults", "default settings",
                    "恢复默认", "重置设置", "默认外观",
                )
            ),

            // --- APPEARANCE CATEGORY ---
            SettingSpec(
                id = "appearance_app_language",
                itemKey = "item_appearance_app_language",
                titleRes = R.string.setcat_app_language_label,
                subtitleRes = R.string.setcat_app_language_desc,
                category = SettingsCategory.APPEARANCE,
                subscreenRoute = Screen.SettingsCategory.createRoute("appearance"),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("language", "locale", "translation", "english", "region")
            ),
            SettingSpec(
                id = "appearance_app_theme",
                itemKey = "item_appearance_app_theme",
                titleRes = R.string.setcat_app_theme_label,
                subtitleRes = R.string.setcat_app_theme_desc,
                category = SettingsCategory.APPEARANCE,
                subscreenRoute = Screen.SettingsCategory.createRoute("appearance"),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("dark mode", "light mode", "theme", "system", "night")
            ),
            SettingSpec(
                id = "appearance_show_scrollbar",
                itemKey = "item_appearance_show_scrollbar",
                titleRes = R.string.setcat_show_scrollbar_title,
                subtitleRes = R.string.setcat_show_scrollbar_subtitle,
                category = SettingsCategory.APPEARANCE,
                subscreenRoute = Screen.SettingsCategory.createRoute("appearance"),
                type = SettingType.SWITCH,
                keywordsStatic = listOf("scrollbar", "scroll", "fast scroll", "bar"),
                getValue = { it.showScrollbar },
                onToggle = { viewModel, checked -> viewModel.setShowScrollbar(checked) }
            ),
            SettingSpec(
                id = "appearance_immersive_lyrics",
                itemKey = "item_appearance_immersive_lyrics",
                titleRes = R.string.setcat_immersive_lyrics_title,
                subtitleRes = R.string.setcat_immersive_lyrics_subtitle,
                category = SettingsCategory.APPEARANCE,
                subscreenRoute = Screen.SettingsCategory.createRoute("appearance"),
                type = SettingType.SWITCH,
                keywordsStatic = listOf("immersive", "lyrics", "fullscreen", "auto hide", "controls"),
                getValue = { it.immersiveLyricsEnabled },
                onToggle = { viewModel, checked -> viewModel.setImmersiveLyricsEnabled(checked) }
            ),
            // Shown only while immersive lyrics is on.
            SettingSpec(
                id = "appearance_lyrics_auto_hide_delay",
                itemKey = "item_appearance_lyrics_auto_hide_delay",
                titleRes = R.string.setcat_auto_hide_delay_label,
                subtitleRes = R.string.setcat_auto_hide_delay_desc,
                category = SettingsCategory.APPEARANCE,
                subscreenRoute = Screen.SettingsCategory.createRoute("appearance"),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("auto hide", "delay", "timeout", "lyrics", "controls")
            ),
            SettingSpec(
                id = "appearance_default_tab",
                itemKey = "item_appearance_default_tab",
                titleRes = R.string.setcat_default_tab_label,
                subtitleRes = R.string.setcat_default_tab_desc,
                category = SettingsCategory.APPEARANCE,
                subscreenRoute = Screen.SettingsCategory.createRoute("appearance"),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("launch", "start screen", "default tab", "startup")
            ),

            // --- LOCAL MUSIC CATEGORY ---
            SettingSpec(
                id = "local_music_library",
                itemKey = "item_local_music_library",
                titleRes = R.string.local_music_scan_title,
                subtitleRes = R.string.local_music_scan_subtitle,
                category = SettingsCategory.LOCAL_MUSIC,
                subscreenRoute = Screen.SettingsCategory.createRoute("local_music"),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf(
                    "local music", "import", "scan", "rescan", "library", "embedded lyrics",
                    "tag lyrics", "lrc file", "device music"
                )
            ),
            // --- COVER SOURCES CATEGORY ---
            SettingSpec(
                id = "cover_source_order",
                itemKey = "item_cover_source_order",
                titleRes = R.string.settings_category_cover_sources_title,
                subtitleRes = R.string.settings_category_cover_sources_subtitle,
                category = SettingsCategory.COVER_SOURCES,
                subscreenRoute = Screen.CoverSources.createRoute(),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf(
                    "cover", "cover art", "artwork", "album art", "cover source", "cover sources",
                    "media session", "deezer", "embedded artwork", "local artwork",
                    "封面", "封面源", "专辑封面", "内嵌封面", "封面来源"
                )
            ),
            SettingSpec(
                id = "cover_sources_deezer_artist_images",
                itemKey = "item_cover_sources_deezer_artist_images",
                titleRes = R.string.setcat_external_artist_images_title,
                subtitleRes = R.string.setcat_external_artist_images_subtitle,
                category = SettingsCategory.COVER_SOURCES,
                subscreenRoute = Screen.CoverSources.createRoute(),
                type = SettingType.SWITCH,
                keywordsStatic = listOf(
                    "deezer", "artist images", "artist artwork", "portraits",
                    "歌手封面", "艺术家图片"
                ),
                getValue = { it.externalArtistImagesEnabled },
                onToggle = { viewModel, checked -> viewModel.setExternalArtistImagesEnabled(checked) }
            ),

            // --- PLAYER FILTER CATEGORY ---
            SettingSpec(
                id = "player_filter",
                itemKey = "item_player_filter",
                titleRes = R.string.settings_category_player_filter_title,
                subtitleRes = R.string.settings_category_player_filter_subtitle,
                category = SettingsCategory.PLAYER_FILTER,
                subscreenRoute = Screen.SettingsCategory.createRoute(SettingsCategory.PLAYER_FILTER.id),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf(
                    "player", "player filter", "media session", "listening stats", "statistics",
                    "ignore player", "follow player", "spotify", "youtube music", "apple music",
                    "播放器", "播放器筛选", "媒体会话", "收听统计", "排除播放器",
                )
            ),

            // --- BACKUP & RESTORE CATEGORY ---
            SettingSpec(
                id = "backup_export",
                itemKey = "item_backup_export",
                titleRes = R.string.setcat_export_backup_title,
                category = SettingsCategory.BACKUP_RESTORE,
                subscreenRoute = Screen.SettingsCategory.createRoute("backup_restore"),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("backup", "export", "save", "zip", "transfer")
            ),
            SettingSpec(
                id = "backup_import",
                itemKey = "item_backup_import",
                titleRes = R.string.setcat_import_backup_title,
                subtitleRes = R.string.setcat_import_backup_subtitle,
                category = SettingsCategory.BACKUP_RESTORE,
                subscreenRoute = Screen.SettingsCategory.createRoute("backup_restore"),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("restore", "import", "backup", "recover")
            ),

            // --- ABOUT ---
            SettingSpec(
                id = "about_app",
                itemKey = "item_about_app",
                titleRes = R.string.setcat_about_carpelines_title,
                subtitleRes = R.string.setcat_about_carpelines_subtitle,
                category = SettingsCategory.ABOUT,
                subscreenRoute = Screen.SettingsCategory.createRoute("about"),
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("version", "credits", "about", "info", "app")
            ),
            SettingSpec(
                id = "updates",
                itemKey = "item_updates",
                titleRes = R.string.updates_title,
                subtitleRes = R.string.updates_settings_subtitle,
                category = SettingsCategory.ABOUT,
                subscreenRoute = Screen.Updates.route,
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("update", "upgrade", "version", "alpha", "beta", "stable", "github", "channel")
            ),
            SettingSpec(
                id = "about_licenses",
                itemKey = "item_about_licenses",
                titleRes = R.string.about_licenses_title,
                subtitleRes = R.string.about_licenses_subtitle,
                category = SettingsCategory.ABOUT,
                subscreenRoute = Screen.Licenses.route,
                type = SettingType.NAVIGABLE_CARD,
                keywordsStatic = listOf("licenses", "open source", "third party", "notices", "attribution", "fonts")
            )
        )
    }
}
