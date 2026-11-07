# CarpeLines brand mark

The mark for word-by-word lyrics: three lyric lines and one time cursor.

The cursor sits at **59%** of the line width, marking the character being sung
right now. The three lines are uneven (50 / 59 / 30) and the right edge is cut —
it reads as a line in progress, not as a menu icon.

The full specification lives in **[index.html](index.html)**: open it in a browser
for the construction diagram, sizes, colour values, motion and the self-check
table.

---

## Files

| File | Purpose |
| --- | --- |
| `carpe-icon.svg` | App icon, light background. The default and only colour version; it draws the 72dp visible area launchers keep |
| `carpe-icon.png` | 512 px bitmap. For README headers and other places that need raster |
| `carpe-icon-monochrome.svg` | Flat silhouette. Android 13+ themed icons, notification bar, single-colour print |
| `carpe-mark.svg` | Bare mark, no container, ink on light |
| `carpe-mark-mono.svg` | Bare mark, single colour (`currentColor`), inherits the text colour |
| `carpe-lockup-horizontal.svg` | Horizontal lockup, transparent background (README header) |
| `carpe-lockup-vertical.svg` | Vertical lockup, transparent background, splash screen / store art |
| `android/res/` | Adaptive icon resources, ready to drop into the app |
| `tools/generate.py` | Reproducible geometry and contrast script |
| `tools/rasterize.py` | Redraws every PNG in the repository from the same geometry |

Every icon shares a `108 × 108` canvas — the Android adaptive-icon coordinate
space. Scale other sizes proportionally; do not redraw them.

**There is exactly one colour version: the light-background one.** Dark UI reuses
the same icon instead of maintaining a separate dark variant. Where true single
colour is required (themed icons, notification bar, single-colour print), use
Mono.

## Metrics

```
Canvas         108 × 108        (multiple of 4, Android technical canvas)
Visible area   72 × 72          launchers crop 18 on each side
Output         glyph at 2/3     measured ink 39 × 27
Safe zone      Ø66 centred      (91.7% of the visible area)
Line height / gap   8 / 8  → 5.3 / 5.3   radius r4 → r2.7 (= half height, capsule)
Line width     50 / 59 / 30 → 33 / 39 / 20   left edge at x=25
Cursor         Ø14 → Ø9.3       centre (60, 54), overshoots 3 above and below, 2 clearance on each side
Container      r30 → r20        (27.8% of the canvas)
```

**Why 2/3.** Android adaptive icons are drawn on a 108dp canvas but launchers only
keep the middle 72dp. At full size the glyph would fill 82% of the visible area,
leaving 3–4dp on each edge; scaled to 2/3 the ink lands at 39 × 27 with 16dp of
breathing room, matching the whitespace the mark itself is drawn with.
`carpe-icon.svg`, the in-app preview and the 512 px bitmap are already cropped to
that 72dp.

## Colour

| Role | Value | Contrast |
| --- | --- | --- |
| Container (top → bottom) | `#FFF6EA` → `#FFE4C6` | baseline |
| Sung / current character, time cursor | `#D9480F` | 3.5–4.0 : 1 |
| Unsung | `#8A7663` | 3.5–4.0 : 1 |
| Wordmark | `#1C1917` | 14.3–16.3 : 1 |

**Push the background close to white and keep colour for the sung line alone.**
Once the background drifts yellow, the strokes drift brown and the whole surface
slides towards "caramel on old paper" — the easiest way for this mark to look
dated. The container carries a single faint gradient of the same hue and no glow;
orange and warm grey are pulled down to just clear the 3 : 1 floor for graphics,
and deep ink is reserved for the wordmark. The cursor wears the same vermilion as
the sung line: at Ø14 it is a large graphic, so 3.5 : 1 clears the floor, and the
circle's shape plus 2 units of clearance are what keep it from reading as a bulge
in the stroke.

Lockups carry **no background rectangle** — they sit directly on whatever surface
embeds them. The wordmark is `#1C1917` ink on light and flips to the `#F7F2E9`
paper tone through a `prefers-color-scheme: dark` rule inside the file; the mark
itself keeps its light container in both modes, so the icon never inverts.

Contrast is measured on **the background the strokes actually land on**
(`ink_backgrounds()` in `tools/generate.py`), not at the two ends of the
gradient: the brightest corner is decoration and never carries a stroke, so a
number derived from it means nothing.

**The mark does not take part in dynamic colour.** At runtime the UI derives 48
semantic roles from the album artwork, but brand assets must stay constant. The
table above mirrors the relationship between `primary` (sung / cursor), `primary`
dark (unsung) and `onSurface` (wordmark), yet the values are hard-coded in
resources and never read from `ColorScheme`.

## Android integration

The resources are already wired up in the app (2026-11). If you change the
geometry or the background, update them the same way:

1. Vector layer in `app/src/main/res/drawable/`:
   `ic_launcher_background.xml`, `ic_launcher_foreground.xml`,
   `ic_launcher_monochrome.xml`
2. Adaptive icon declarations `mipmap-anydpi-v26/ic_launcher{,_round}.xml`
   reference those three layers
3. Bitmaps (density fallbacks + store art + README header) are redrawn by
   `tools/rasterize.py`:

   ```sh
   python logo/tools/rasterize.py
   ```

4. Splash silhouette `drawable/splash_icon.xml` (segment 1 = the three lyric
   lines, segment 2 = the cursor, which `CarpeLinesSplash` animates in that
   order) and the `values[-night]/splash_colors.xml` palette

All foreground content stays inside the Ø66 safe circle (measured 39 × 27), so no
launcher mask can clip the cursor.

## Relationship to the M3E design system

It follows the conventions of
[`docs/M3E-DESIGN-SYSTEM.md`](../docs/M3E-DESIGN-SYSTEM.md):
one strong expressive point (the cursor), tonal rather than shadowed hierarchy,
and token discipline. The point-by-point comparison of expressive levers is in
section 9 of `index.html`.

**One deliberate deviation:** the design system forbids content colours below 38%
alpha, but the "unsung" stroke uses the solid `#8A7663` — 38% deep ink on paper
white falls below the 3 : 1 floor for graphics, while the solid colour stays at
≥ 3.5 : 1 across the gradient.

## Regenerating

```sh
python logo/tools/generate.py
```

It prints the squircle container path, the Android `pathData`, and every contrast
value in the table above. The shapes in the SVGs and in `index.html` are written
by hand (`<rect rx>` / `<circle>` are easier to read than `pathData`); the script
only verifies those numbers and never overwrites files.
