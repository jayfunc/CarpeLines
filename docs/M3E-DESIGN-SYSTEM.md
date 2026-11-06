# Material 3 Expressive design system (CarpeLines)

This document is the design and implementation contract for the UI rewrite. It addresses
designers (for the reasoning) and developers (for the implementation) at once. Any later
interface change should first find its token or component here instead of inventing a new `dp`
value inside a composable.

Paths are relative to `src/CarpeLines.KMP/app/src/main/java/io/github/jayfunc/carpelines/`.

---

## 1. Design intent

| Dimension | Decision |
|------|------|
| Platform | Android phone / tablet / foldable / desktop window (adaptive) |
| Primary user goal | Follow the music the system is playing and read word-synced lyrics |
| Primary action | The Home hero opens the lyrics page for the current track |
| Top-level destinations | Home, Lyrics, Stats, Settings (4 — inside the M3 navigation bar constraint of 3–5) |
| Information density | Medium-high. Lists are artwork plus two lines of text |
| Expressive strength | **One** strong expressive point per screen (Home hero, empty-state hero art); everything else stays restrained |
| Dark first | Yes. Backgrounds are driven by artwork-derived colour |

M3E's expressive tools (colour / shape / scale / motion / container / type) are used only when they
strengthen hierarchy, usability or emotional clarity. "Everything stands out" is equivalent to
"nothing stands out", so the component library is quiet by default.

---

## 2. Token layer

Token dependencies point one way:

```
Material Color Utilities / system dynamic colour  →  ColorScheme semantic roles  →  components
M3 shape/type/spacing/motion scales              →  ui/theme token objects     →  components
```

The UI may depend only on **semantic roles** and **token objects**; literal colour values and
one-off numbers are not allowed.

### 2.1 Colour (semantic roles)

Colour generation reuses the existing implementation (`ColorRoles.kt`, built on Material Color
Utilities' `SchemeTonalSpot / SchemeExpressive / SchemeVibrant / SchemeFruitSalad /
SchemeMonochrome`), with the full set of 48 roles (including the five `surfaceContainer*` levels
and the `*Fixed` family). The generation logic is unchanged; this document only fixes how it is
used:

| Purpose | Role |
|------|------|
| Page background | `surface` |
| Card / list group | `surfaceContainer` (one level down with `surfaceContainerLow`, one up with `High/Highest`) |
| Primary text | `onSurface` |
| Secondary text / icons | `onSurfaceVariant` |
| Primary CTA | `primary` + `onPrimary`; emphasise the container with `primaryContainer` |
| Selected state (lists, navigation indicator) | `secondaryContainer` + `onSecondaryContainer` |
| Destructive / error | `errorContainer` + `onErrorContainer` |
| Dividers | `outlineVariant` |
| State layers (pressed / hovered / focused) | Derived automatically by the `Surface`/`clickable` ripple; hand-written alpha backgrounds are forbidden |

**Constraint**: colour alone must never carry state. Selected items change both container and
content colour; filter chips also show a check icon; the shuffle state also carries a text pill.

### 2.2 Type scale (`ui/theme/Type.kt`)

The 15 standard roles use the official M3 size / line height / tracking, with the product's
variable-weight rounded family (`RoundedSans`).

There are also **15 emphasized roles** (`MaterialTheme.typography.emphasized.*`): **identical
metrics, one weight step heavier**. Swapping a standard role for its emphasized counterpart
therefore causes no relayout at all — only the strength of the hierarchy changes.

```kotlin
// Standard: ordinary reading, settings rows, body copy
Text(text = song.title, style = MaterialTheme.typography.bodyLarge)

// Emphasized: the visual entry point of a screen — use exactly one
Text(text = pageTitle, style = MaterialTheme.typography.emphasized.headlineSmall)
```

Usage rules:

- Page title / hero: `emphasized.displaySmall` ~ `emphasized.displayLarge`
- Section header: `emphasized.titleLarge`
- List primary text: `bodyLarge`; secondary text: `bodyMedium` + `onSurfaceVariant`
- Button and chip labels: `labelLarge` / `labelMedium`

### 2.3 Shape (`ui/theme/Shape.kt` + `ShapeCache.kt`)

The M3 shape ladder plus M3E's increased levels:

| Token | Radius | Typical use |
|------|------|----------|
| `M3EShape.extraSmall` | 4dp | badges, very small containers |
| `M3EShape.small` | 8dp | chips, thumbnails, compact rows |
| `M3EShape.medium` | 12dp | list items, text fields |
| `M3EShape.large` | 16dp | cards, menus, FAB |
| `M3EShape.largeIncreased` | 20dp | emphasized cards |
| `M3EShape.extraLarge` | 28dp | dialogs, floating navigation bar |
| `M3EShape.extraLargeIncreased` | 32dp | pushed-up sheets, hero media |
| `M3EShape.extraExtraLarge` | 48dp | very large media containers |
| `M3EShape.full` | capsule | buttons, chips, indicators, avatars |

The five levels of `MaterialTheme.shapes` match the first five rows above, so default Material
component shapes never fight the app's own surfaces.

Grouped lists use `M3EShape.grouped(index, count)`: only the outer corners of the whole group are
rounded, and rows are separated by shape rather than each row being wrapped in its own card.

Horizontal "connected button groups" use `M3EShape.connected(index, count)` with a 2dp gap:
related actions on the same object share one outline, inner corners drop to 8dp, and primary vs.
secondary comes from emphasis (filled / tonal) instead of two independently floating capsules or
outlined buttons.

### 2.4 Spacing (`ui/theme/Spacing.kt`)

A 4dp baseline grid. Spacing expresses "grouping": small steps inside a group, large steps
between groups.

| Token | Value | Purpose |
|------|----|------|
| `M3ESpacing.extraSmall` | 4dp | between icon and text |
| `M3ESpacing.small` | 8dp | inside a control |
| `M3ESpacing.medium` | 12dp | between items of the same group |
| `M3ESpacing.large` | 16dp | card / row padding |
| `M3ESpacing.extraLarge` | 24dp | between groups, page margins |
| `M3ESpacing.extraExtraLarge` | 32dp | top-of-page whitespace |
| `M3ESpacing.huge` | 48dp | hero breathing room |

Size tokens (`M3ESize`): 48dp minimum touch target, list row heights 56/72/88dp, icons
16/24/32dp, leading icon container 40dp, empty-state hero 76dp.

### 2.5 Adaptive (`rememberM3ELayoutMetrics()`)

| Window | Margin | Gutter | Max content width | Navigation |
|------|------|------|--------------|------|
| Compact (<600dp) | 16dp | 16dp | unlimited | bottom navigation bar |
| Medium (600–839dp) | 24dp | 24dp | 600dp | `WideNavigationRail` |
| Expanded (≥840dp) | 24dp | 24dp | 840dp | `WideNavigationRail` |

A wide window should provide **more context** (a second pane, more columns), not a
proportionally scaled-up phone layout.

### 2.6 Motion (`ui/theme/Motion.kt`)

- Duration tokens: `short1..4`(50–200) / `medium1..4`(250–400) / `long1..4`(450–600) / `extraLong1..4`(700–1000)
- Easing tokens: three families, `standard*` and `emphasized*`
- Specification source: `M3EMotion.spatial()` / `spatialFast()` / `spatialSlow()` / `effects()`
  all read the theme's `MotionScheme` (currently `MotionScheme.expressive()`), so the whole motion
  language can be switched in one place.

Motion explains state changes and spatial relationships only; decorative animation is not used.

---

## 3. Component library (`ui/m3e`)

| Component | Responsibility | Key guarantee |
|------|------|----------|
| `M3ESurface` | Semantic container | Picks colour by `M3ESurfaceLevel`, correct `on*` content colour, built-in ripple state layer and `Role` when clickable |
| `M3EListItem` | List row | Row height 56/72/88 derived from the number of text lines; the whole row is one target; selection uses `secondaryContainer` + content colour |
| `M3EEmphasizedListItem` | The single emphasized row in a group | Uses the emphasized type scale so the whole list is not shouted |
| `M3ELeadingIconContainer` | Leading icon base | 40dp circle + container colour, so contrast holds on any surface |
| `M3EPrimaryButton` / `M3ETonalButton` / `M3ESecondaryButton` | Three levels of action strength | One filled button per view; height ≥48dp; capsule shape |
| `M3EButtonGroup` | Related actions as a group | Intra-group spacing is smaller than the page gutter; the group keeps its own semantics |
| `M3EFab` | The only creation action | Sizes 56/80/96; never used for navigation or destructive actions |
| `M3EFilterChip` | View filtering | Selection shows both a check icon and a container colour |
| `M3EEmptyState` | Empty state | Answers "what is missing / why / what next"; the hero art expresses shape through `SoftBurst` |
| `M3EErrorState` | Error state | `errorContainer` pairing + a recovery action + a polite live region |
| `M3ELoadingState` | Loading state | Reports `progressBarRangeInfo` when determinate; uses the M3E `LoadingIndicator` when indeterminate |
| `M3ESkeletonBlock` | Skeleton placeholder | Locks the final layout and is hidden from accessibility |
| `M3ENavigationBar` | Top-level navigation | `Role.Tab` + `selected` semantics; indicator width follows theme motion; labels always visible |

---

## 4. State coverage

Every data-driven screen must cover:

- **Default**: stable layout with content
- **Loading**: `M3ELoadingState` or a skeleton, with no layout jump
- **Empty**: `M3EEmptyState` with a next step
- **Error**: `M3EErrorState` with a retry path
- **Disabled**: content colour at 38% alpha and no click handling
- **Selected**: container colour + content colour + `selected` semantics
- **Pressed / focused / hovered**: provided by the ripple state layer, never hand-written

---

## 5. Accessibility (release requirement)

- Touch targets ≥ 48×48dp (`M3ESize.minimumTarget`)
- Text/background contrast: body ≥ 4.5:1, large text ≥ 3:1; state never depends on a single colour
- Every icon button has a `contentDescription`; decorative icons are explicitly `null`
- List rows expose `Role.Button` / `Role.Switch` / `Role.Tab`, and selected items carry `selected`
- Loading / error blocks are polite `liveRegion`s: announced when they appear without stealing focus
- System font scaling is supported: all sizes come from `sp` roles, fixed `dp` text heights are forbidden
- The system "remove animations" setting is respected: motion goes through `MotionScheme`, so it only has to be wired up there

---

## 6. Migration guide (moving the remaining screens to M3E)

1. **Look up the token first**: replace `12.dp` / `RoundedCornerShape(10.dp)` / `titleMedium + FontWeight.Bold`
   with `M3ESpacing.medium` / `M3EShape.medium` / `emphasized.titleMedium`.
2. **Swap the container**: `Surface(color = surfaceContainer, modifier = clip(shape).clickable(...))`
   → `M3ESurface(level = M3ESurfaceLevel.Container, shape = ..., onClick = ...)`.
   The latter brings its own content colour, bounded ripple and semantic role.
3. **Swap the list row**: `Row + manual padding` → `M3EListItem`, so height and padding come from one spec.
4. **Swap empty / error / loading**: → `M3EEmptyState` / `M3EErrorState` / `M3ELoadingState`.
5. **Converge expression**: keep one `emphasized` display-level element per screen and send the rest back to the standard scale.
6. **Fill in states**: make sure disabled, selected, loading, empty and error all have a representation.

### Completed migrations

| Scope | Files | Notes |
|------|------|------|
| Token layer | `ui/theme/{Type,Shape,Spacing,Motion,ShapeCache,Theme}.kt` | New M3 type scale + 15 emphasized roles; M3E shape ladder; spacing/size/motion tokens |
| Component library | `ui/m3e/*` | M3E components (see section 3) |
| App shell | `MainActivity.kt`, `PlayerInternalNavigationBar.kt` | Floating navigation bar became a semantic container driven by tokens; shadow-based fake hierarchy removed in favour of tonal elevation; 24dp between the floating capsule and the gesture bar instead of sitting flush |
| Home | `HomeScreen.kt` | Hero uses `emphasized.displayLarge`; empty / loading / list rows moved to M3E components; shuffle state became "colour + text" |
| Home collage | `AlbumArtCollage.kt` | Pattern switching went from whole-layer scale/fade to per-cover translation/size/rotation with separately cross-faded outlines, so it reads as "the same set of covers rearranged" |
| Settings | `SettingsComponents.kt`, `SettingsMainList.kt` | Row containers, type sizes, spacing and group shapes moved to tokens |
| Lyrics page | `NowPlayingLyricsScreen.kt`, `components/lyrics/LyricsChrome.kt` | Transport became an M3E connected button group (56dp tonal previous/next + 96dp filled play, with M3E shape morph on press); the immersive exit button moved to a tonal icon button in the top start and no longer sits at the bottom; tapping empty lyric space brings the chrome back; titles use the emphasized scale. The overview gained a "now singing" capsule row: per-syllable sweep that reads the playback position only during the draw phase and opens immersive mode on tap; source labels became small tonal capsules; the whole surface enters in stages — artwork → title → source → current line → progress → controls — with artwork and lyric dismissal using `M3EMotion.spatial()` and the hand-off between overview and lyrics becoming a translation relay rather than a double fade |
| Lyric line actions sheet | `components/lyrics/LyricsLineActionsSheet.kt` | Removed dividers and the settings-page feel: tonal header, standalone offset calibration control, `M3EShape.grouped` group actions, `M3ELeadingIconContainer` leading icons |
| Lyric card sharing | `components/lyrics/card/LyricsShareSheet.kt` | Font choice became an `Aa` capsule rendered in its own family plus an expressive menu group; lyric selection moved from tonal buttons to action-style list rows; export became `M3EConnectedActions` (horizontal) in a connected action bar; the selection sheet dropped per-row checkboxes and the "select all" toggle capsule in favour of container colour + a check mark, a title-bar text action and a right-aligned connected confirm key |
| Lyric style settings | `LyricsSettingsScreen.kt`, `components/lyrics/LyricsDisplaySettings.kt` | Aligned with BetterLyrics' transparency / line spacing / font-size ratio / word-by-word mode / depth, fade, blur, feathering, glow, scale, float and interlude-dot switches |
| Screen transitions | `ScreenWrapper.kt` | Duration/easing moved to `M3EMotion` tokens |

### Pending migrations (ordered by payoff)

1. `SettingsCategoryScreen.kt`: grouped cards, slider rows and switch rows made clickable
2. `LyricsSettingsScreen.kt` / `LyricsSourceOrderScreen.kt`: settings rows unified onto the shared components
3. `StatsScreen.kt` / `presentation/recap/*`: list and chart styling for stats and the yearly recap
4. `AboutScreen.kt` / `UpdatesScreen.kt`: information page typography

---

## 7. Self-assessment

| Category | Score | Notes |
|------|------|------|
| Task clarity | 2 | Home hero + a single primary CTA; 4 top-level destinations with labels always visible |
| Information hierarchy | 2 | Type scale and spacing carry the hierarchy; grouping no longer relies on stacked cards |
| Component semantics | 2 | Navigation uses `Role.Tab`, whole list rows are clickable, the FAB is creation-only |
| Token discipline | 2 | New and changed code uses only semantic roles and token objects |
| Adaptivity | 1 | Tokens and the rail are ready, but the lyrics and settings pages are still single-column |
| States and feedback | 2 | Empty/error/loading/selected/disabled all have shared components; remaining pages are being migrated |
| Accessibility | 1 | Target sizes, semantics and live regions are covered; no page-by-page TalkBack pass yet |
| Expressive restraint | 1 | Home and the empty state each have one expressive point; some older components still keep one-off shapes |

> Conclusion: **the foundation and the shell are at release quality**; continuing the checklist in
> section 6 can lift adaptivity and accessibility to 2.

---

## 8. Known deliberate deviations

| Deviation | Reason | Compensation |
|------|------|------|
| Backgrounds are sampled from album artwork (not pure system dynamic colour) | Core product differentiation | All roles still come from the full M3 role table, with consistent light/dark and accessible contrast |
| The bottom navigation bar is a floating capsule (M3 default is full-width and flush) | Coexists with the immersive lyrics page and the gradient bottom bar | Still `surfaceContainer` + tonal elevation, with interaction and semantics fully following M3 |
| Smooth corners (60% smoothness) instead of plain rounding | Matches the artwork-led look | Radii come strictly from the M3 shape ladder |
| Display-level type uses the product typeface | Brand recognition | Sizes, line heights and tracking are the official M3 values; emphasis comes from the emphasized roles |
