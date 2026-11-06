# Lyrics engine reference: BetterLyrics → CarpeLines

Parts of the lyrics engine are adapted from the WinUI 3 app [BetterLyrics](https://github.com/jayfunc/BetterLyrics).
This file maps each adopted component to where it lives here and records where CarpeLines deliberately
behaves differently, so a later change can be checked against the source instead of drifting into a look
that is merely "similar".

## Where the lyrics code lives

Paths are relative to `app/src/main/java/io/github/jayfunc/carpelines/` unless marked otherwise.

| Concern | File |
| --- | --- |
| Renderer entry point: maps the player model, configures the karaoke view | `presentation/components/SyncedLyricsList.kt` |
| Player model → core model (syllables, translations, romanization, duet sides, backing vocals) | `presentation/components/lyrics/CoreLyricsRenderMapper.kt` |
| WinUI-derived defaults (blur, opacities, translation/romanization font ratios) | `presentation/components/lyrics/WinLyricsVisuals.kt` |
| Display settings shared by the lyrics page and chrome | `presentation/components/lyrics/LyricsDisplaySettings.kt`, `LyricsArtworkPalette.kt` |
| Immersive shell, auto-hiding chrome, offset store, share card | `presentation/screens/NowPlayingLyricsScreen.kt`, `presentation/components/lyrics/LyricsChrome.kt`, `LyricsOffsetStore.kt`, `LyricsImmersiveEffects.kt`, `card/LyricsShareSheet.kt`, `card/LyricsCardExporter.kt` |
| Ambient backgrounds (fluid / album blur) | `presentation/components/carpelines/BetterLyricsAmbientBackground.kt` (package `components.betterlyrics`) |
| Ambient overlays (raindrops / snow) | `presentation/components/carpelines/BetterLyricsAmbientShaderOverlay.kt` + `app/src/main/assets/shaders/*.agsl` |
| Playback-clock interpolation and copy helpers | `presentation/components/LyricsPlaybackTiming.kt`, `LyricsClipboardText.kt` |

The line list, word sweep, per-character emphasis, translation layers and gap dots are rendered by
`com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView` (the `accompanist-lyrics`
KMP library). `SyncedLyricsList` owns the mapping into that view; there is no app-local per-character
canvas renderer any more. `WinLyricsVisualDefaults` keeps the WinUI default opacities, blur and font
ratios used by the settings and palette code.

## Mapping

| WinUI | Android |
| --- | --- |
| `PlayingLineTopOffset = 50` | `playingLineTopOffset`, default `0.5` on the immersive page; adjustable under Lyrics → Playing line position |
| `_defaultScale = 0.75`, `_highlightedScale = 1.0` | The karaoke view's line focus; the app no longer animates scale itself |
| `IsLyricsOutOfSightEffectEnabled`, fade, blur | `LyricsVisualSettings.animatedLyricsBlurEnabled` / `animatedLyricsBlurStrengthDp` passed to `KaraokeLyricsView(useBlurEffect = …)` |
| `LyricsAnimator` line transitions and stagger delay | The karaoke view's line transition; there is no app-side stagger animator |
| `DrawSubLineRegion` gradient stops | The karaoke view's syllable sweep |
| `GapIndicatorRenderer` / `LyricsGapHelper` | The karaoke view's gap indicators |
| `EdgeFadeMaskRenderer` | `LyricsFade.Fraction(topFadeFraction)` / `LyricsFade.Fraction(bottomFadeFraction)` in `SyncedLyricsList.kt`, plus the `edgeFeatheringEffectEnabled` switch |
| `Shaders/FluidBackgroundEffect.cs` (`FluidBackgroundRenderer`) | `BetterLyricsAmbientBackground`'s `FLUID` style: the same noise rotation, sine warp and four-colour two-layer composite, evaluated on the CPU into a small bitmap so it also runs on Android 11 (AGSL needs API 33). The shader's optional `enableLightWave` HSV pass is not ported; dithering is skipped because the buffer is upscaled |
| `Shaders/RaindropEffect.cs` (`RaindropRenderer`) | `RAINDROP` style: `RaindropOverlay` runs the same HLSL as an AGSL runtime shader over the blurred cover. `RaindropSpeed`/`Size`/`Density`/`LightAngle`/`ShadowIntensity` keep the WinUI defaults (100 % / 100 % / 40 % / 135° / 0 %), because the Android settings only pick the style |
| `Shaders/SnowEffect.cs` (`SnowRenderer`) | `SNOW` style: `SnowOverlay`, also AGSL, with `SnowFlakeOverlayAmount = 10 %` and `SnowFlakeOverlaySpeed = 1` as in WinUI |
| Win2D album blur | `BetterLyricsAmbientBackground`'s `ALBUM_BLUR` style (`Modifier.blur`) |
| `UnplayedOriginalLyricsOpacity`, `TranslatedLyricsOpacity`, `PhoneticLyricsOpacity` | `WinLyricsVisualDefaults` + `LyricsVisualSettings` (Lyrics → Text style) |
| `LyricsLineOverallSpacingFactor`, `LyricsLineInnerSpacingFactor` | `LyricsVisualSettings.lineSpacingFactor` (Lyrics → Text style) |
| `LyricsFontWeight`, CJK/Western font families | `LyricsFontWeight` / `LyricsFontFamily` (Lyrics → Text style) |

The renderer follows WinUI's data model rather than rebuilding a line per word:

- **One model per line.** `CoreLyricsRenderMapper` keeps each line's text, translation, romanization
  and timing together, and only emits a karaoke line when the source has real syllable timing.
  Plain LRC lines become whole-line highlights instead of fake per-character sweeps.
- **Syllables stay source-backed.** `SyncedWord.sourceStartIndex` / `sourceLength` keep the original
  character ranges, so punctuation and spaces are not duplicated or dropped when the karaoke line is
  built.
- **Duet and backing vocals.** `voice == 1/2` maps to the karaoke alignment start/end; background
  vocals become accompaniment lines (or a following line when the main line is plain), so they use
  the same settings as WinUI.
- **Gaps.** Gaps come from explicit line ends or the next line start; the karaoke view paints the
  breathing dots at the boundary. Blank marker rows are not rendered.
- **Edge fades.** Both fades are split at `playingLineTopOffset`; a single full-height fade made the
  lines above the playing line look dimmer than the ones below it.

## Deliberate differences

- **Colours.** WinUI's default palette is a neutral grey over a flat gradient. On a phone the
  background is the album cover, so the lyric colour comes from the artwork palette and a scrim
  sits under the whole column. The opacity values match WinUI's defaults (unplayed 30%,
  translation/romanization 60%) and are adjustable under Lyrics → Text style instead of being
  baked into the renderer.
- **Type.** WinUI renders Arial through DirectWrite; Android uses the platform typeface unless the
  user picks one. Glyph metrics, shaping and fallback therefore differ even when the layout and
  animation math are identical.
- **Immersive shell.** The Windows app is a desktop window with hover-revealed controls. On a phone
  the chrome hides after a few seconds and comes back from a bottom grip or a tap on the title.
- **No local playback.** CarpeLines does not own the audio session, so there is no audio
  visualiser, and transport goes through the system media session (`ExternalMediaMonitor`).
- **Optional desktop effects not ported.** 3D parallax, fan lyrics, audio-reactive breathing and
  the vertical layout are off by default in WinUI and are not exposed on Android; stroke width and
  non-auto-wrap marquee are likewise omitted (stroke defaults to 0 and auto-wrap to on).
- **Raindrops and snow need Android 13.** They are AGSL runtime shaders (see the pitfalls), so
  below API 33 the style sheet lists them as unavailable and the page keeps its backdrop. WinUI
  can afford them everywhere because Direct2D has no such floor.
- **Overlays sit below the lyrics.** WinUI paints snow and rain over the lyric text. On a phone the
  words are the page, so here they are part of the ambient layer and the lyric scrim stays on top
  of them: the drops read as light on the backdrop instead of noise across the lyrics.

## Pitfalls found while porting

- Reading `LazyListState.layoutInfo` inside a graphics layer is fine (draw phase), but writing the
  distance into a `TextStyle` is not: a shadow that changes per frame relayouts the text on every
  playback frame. Glow is drawn, not styled.
- Snapping to an item index for auto-scroll jumps whenever item heights differ (wrapped lines,
  translation layers). `SyncedLyricsList` opens at the line being sung instead of starting from the
  first line.
- The edge fade is masked from the screen edges, so both fade fractions must split at the playing
  line. `LyricsFade.ToAnchor` / `Fraction(0.5f)` put every line above the current one inside the
  top mask while the lines below were still fully opaque.
- The fluid gradient's CPU sample works because its output is a smooth field; raindrops and snow
  are not. `RaindropEffect` evaluates four cubic-noise gradients per pixel and `SnowEffect` loops
  over 66 cells, which is far past a phone CPU at any resolution where the drops are not mush, so
  both became AGSL programs in `assets/shaders`. Translating them means: no implicit matrix
  constructors (`Hlsl.Mul(map, v)` and `Hlsl.Mul(v, map)` differ — the map is not symmetric, so
  both are written as dot products), `mod()` instead of HLSL `%`, and an explicit `smoothstep`
  helper because several calls pass `edge0 > edge1`, where GLSL's version is undefined.
