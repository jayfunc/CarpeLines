# Third-Party Notices

CarpeLines as a whole is licensed under the GNU General Public License v3.0 or later (GPL-3.0-or-later) and includes third-party open-source components.

CarpeLines is derived from PixelPlayerOSS (GPL-3.0) and adapts several components of the BetterLyrics desktop app (GPL-3.0) and of the Zenith digital-wellbeing app (GPL-3.0); each adapted component is listed separately below. Code carried over from the pre-fork public PixelPlayer repository is MIT-licensed: that repository shipped an MIT License from commit `b7a6710` (2025-08-30) until commit `ceec1bb9` (2026-05-12) switched it to a proprietary license, and copies obtained in that window stay under MIT. `licenses/PIXELPLAYER_MIT_NOTICE.txt` reproduces that upstream notice, so those portions stay under MIT. The MIT row below covers only those inherited portions — it is not a license grant for CarpeLines itself. BetterLyrics code remains GPL-3.0 and is not covered by the MIT notice.

The full text of every license listed here is bundled in `licenses/texts/`, except the preserved PixelPlayer MIT notice, which is `licenses/PIXELPLAYER_MIT_NOTICE.txt`.

## Contributors

| Component | Copyright | License |
| --- | --- | --- |
| PixelPlayer (MIT-era portions) | MIT-era code through 2026-05-12 · Theo Vilardo and PixelPlayer contributors | MIT License |
| PixelPlayerOSS (upstream fork base) | Theo Vilardo · maintained by lostf1sh | GPL-3.0 |

## Adapted components

| Component | Source | License |
| --- | --- | --- |
| Karaoke timing model, lyric card templates, fluid / raindrop / snow ambient shaders (`FluidBackgroundEffect`, `RaindropEffect`, `SnowEffect`) | BetterLyrics · `https://github.com/jayfunc/BetterLyrics` · jayfunc and BetterLyrics contributors | GPL-3.0 |
| App shell: media-session monitoring, remote transport, settings tree, Home preview | PixelPlayerOSS · `https://github.com/PixelPlayerHQ/PixelPlayerOSS` | GPL-3.0 |
| Bottom navigation bar (short navigation bar with expanding labels) and the activity heatmap wall (`LongTermSection` calendar) | Zenith · `https://github.com/1372Slash/Zenith` · 1372Slash and Zenith contributors | GPL-3.0 |

## Libraries

| Component | Copyright | License |
| --- | --- | --- |
| AndroidX / Jetpack (Compose, Material 3, Media3, Room, Paging, Navigation, Lifecycle, WorkManager, Glance, Security, Graphics Shapes) | The Android Open Source Project | Apache 2.0 |
| Material Components for Android | Google | Apache 2.0 |
| Kotlin standard library and kotlinx (coroutines, serialization, collections-immutable) | JetBrains s.r.o. and Kotlin contributors | Apache 2.0 |
| Dagger / Hilt | The Dagger Authors (Google) | Apache 2.0 |
| Accompanist (permissions, drawable-painter) | Google | Apache 2.0 |
| Coil | Coil Contributors | Apache 2.0 |
| Gson | Google | Apache 2.0 |
| Retrofit | Square, Inc. | Apache 2.0 |
| OkHttp | Square, Inc. | Apache 2.0 |
| Timber | Jake Wharton | Apache 2.0 |
| Ktor | JetBrains s.r.o. | Apache 2.0 |
| Netty | The Netty Project | Apache 2.0 |
| SnakeYAML | Andrey Somov and SnakeYAML contributors | Apache 2.0 |
| Apache Commons Lang | The Apache Software Foundation | Apache 2.0 |
| Apache HttpComponents HttpClient | The Apache Software Foundation | Apache 2.0 |
| jose4j | Brian Campbell | Apache 2.0 |
| Kuromoji | Atilika Inc. | Apache 2.0 |
| TagLib Android bindings (`libtaglib.so`) | Kyant0 · `https://github.com/Kyant0/taglib` | Apache 2.0 |
| Vorbis Java | Nick Burch (Gagravarr) | Apache 2.0 |
| JSyn (via Media3 MIDI) | Phil Burk · `https://github.com/philburk/jsyn` | Apache 2.0 |
| Wavy Slider | Mahdi Hosseinzadeh (Mahozad) | Apache 2.0 |
| Compose Reorderable | Calvin Liang | Apache 2.0 |
| Accompanist Lyrics (`lyrics-core`, `lyrics-ui`) | Simon Scholz (6xingyv) · `https://github.com/6xingyv/accompanist-lyrics-core` | Apache 2.0 |
| Capturable | Shreyas Patil | MIT License |
| Smooth Corner Rect Android Compose | racra · `https://github.com/racra/smooth-corner-rect-android-compose` | MIT License |
| Bouncy Castle (bcprov, bcpkix) | The Legion of the Bouncy Castle Inc. | MIT License |
| TagLib (native audio metadata library) | TagLib authors · `https://taglib.org` | LGPL-2.1 |
| JAudioTagger | Paul Taylor and JAudioTagger contributors | LGPL-2.1 |
| Jellyfin FFmpeg decoder (`libffmpegJNI.so`) | Jellyfin · `https://github.com/jellyfin/jellyfin-androidx-media` | GPL-3.0 |
| pinyin4j | Li Min and pinyin4j contributors | BSD 3-Clause |
| JDOM 2 | Jason Hunter & Brett McLaughlin | JDOM License |
| Lyricify-Lyrics-Helper (QQ Music, NetEase Cloud Music and Kugou lyrics protocols, ported to Kotlin) | WXRIW · `https://github.com/WXRIW/Lyricify-Lyrics-Helper` | Apache 2.0 |

## Bundled fonts

| Asset | Copyright | License |
| --- | --- | --- |
| Google Sans Flex (`gflex_variable.ttf`) | Google LLC · `https://fonts.google.com/specimen/Google+Sans+Flex` | SIL Open Font License 1.1 |
| Roboto Flex (`genre_variable.ttf`) | The Roboto Flex Project Authors · `https://github.com/googlefonts/roboto-flex` | SIL Open Font License 1.1 |

## Bundled data assets

| Asset | Source | License |
| --- | --- | --- |
| Artist genres (`nlp/artist-genres.txt`) | Artist-to-genre labels derived from Wikidata, `https://www.wikidata.org` | CC0 1.0 |
| Smart Mix concepts (`nlp/concepts.txt`) | Word-to-genre-family/mood concept table generated offline | GPL-3.0 |

See `docs/DEPENDENCY_LICENSES.md` in the source repository for the fuller dependency review notes.
