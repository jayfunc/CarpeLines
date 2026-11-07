"""Rasterises the CarpeLines mark into every PNG the repo still carries.

    python logo/tools/rasterize.py

Outputs
    app/src/main/res/mipmap-*dpi/ic_launcher.png            48dp legacy square
    app/src/main/res/mipmap-*dpi/ic_launcher_round.png      48dp legacy circle
    app/src/main/res/mipmap-*dpi/ic_launcher_background.png 108dp adaptive layer
    app/src/main/res/mipmap-*dpi/ic_launcher_foreground.png 108dp adaptive layer
    logo/carpe-icon.png                                     512 brand icon (squircle)
    app/src/main/ic_launcher-playstore.png                  512 store art (full square)

Every PNG is a re-render of the same 108dp geometry, so the bitmaps can never
drift from drawable/ic_launcher_foreground.xml. Rendering goes through Chrome,
which is the only rasteriser guaranteed to be on this machine.
"""

from __future__ import annotations

import base64
import pathlib
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
RES = ROOT / "src/CarpeLines.KMP/app/src/main/res"

CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

# --- the mark, once -----------------------------------------------------------
# Same numbers as drawable/ic_launcher_foreground.xml.
GLYPH = """
<rect x="25" y="34" width="50" height="8" rx="4" fill="#D9480F"/>
<rect x="25" y="50" width="26" height="8" rx="4" fill="#D9480F"/>
<rect x="69" y="50" width="15" height="8" rx="4" fill="#8A7663"/>
<rect x="25" y="66" width="30" height="8" rx="4" fill="#8A7663"/>
<circle cx="60" cy="54" r="7" fill="#1C1917"/>
"""

# The launcher keeps only the middle 72dp of the 108dp canvas, so the mark is output
# at 2/3. Same transform as drawable/ic_launcher_foreground.xml.
SCALED_GLYPH = (
    '<g transform="translate(54 54) scale(.6667) translate(-54 -54)">' + GLYPH + "</g>"
)

# Same numbers as drawable/ic_launcher_background.xml: one near-white warm paper gradient.
SURFACE = """
<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="#FFF6EA"/>
    <stop offset="1" stop-color="#FFE4C6"/>
  </linearGradient>
</defs>
<rect width="108" height="108" fill="url(#bg)"/>
"""

# Store/brand bitmap: the 72dp visible area only, so the file matches carpe-icon.svg.
SURFACE_72 = """
<defs>
  <linearGradient id="bg" x1="18" y1="18" x2="90" y2="90" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="#FFF6EA"/>
    <stop offset="1" stop-color="#FFE4C6"/>
  </linearGradient>
</defs>
<rect width="108" height="108" fill="url(#bg)"/>
"""


def svg(viewbox: str, size: int, body: str, clip: str | None = None) -> str:
    inner = f'<g clip-path="url(#c)">{body}</g>' if clip else body
    clipdef = ""
    if clip == "squircle":
        clipdef = '<clipPath id="c"><rect width="108" height="108" rx="30"/></clipPath>'
    elif clip == "circle":
        clipdef = '<clipPath id="c"><rect width="108" height="108" rx="54"/></clipPath>'
    elif clip == "icon":
        clipdef = '<clipPath id="c"><rect x="18" y="18" width="72" height="72" rx="20"/></clipPath>'
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        f'viewBox="{viewbox}">{clipdef}{inner}</svg>'
    )


def render(markup: str, size: int, out: pathlib.Path) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    out.unlink(missing_ok=True)  # so a stale bitmap can never pass for a fresh one
    with tempfile.TemporaryDirectory() as tmp:
        page = pathlib.Path(tmp) / "p.html"
        page.write_text(
            "<!doctype html><meta charset=utf-8>"
            "<style>html,body{margin:0;padding:0;width:%dpx;height:%dpx;overflow:hidden}"
            "svg{display:block}</style>%s" % (size, size, markup),
            encoding="utf-8",
        )
        subprocess.run(
            [
                CHROME,
                "--headless=new",
                "--disable-gpu",
                "--hide-scrollbars",
                # A throwaway profile: without it the command is handed to the
                # already-running Chrome, which exits 0 and writes no screenshot.
                f"--user-data-dir={pathlib.Path(tmp) / 'profile'}",
                "--default-background-color=00000000",
                f"--window-size={size},{size}",
                f"--screenshot={out}",
                page.as_uri(),
            ],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
    if not out.exists():
        sys.exit(f"Chrome wrote no bitmap for {out.name}")
    print(f"  {size:>4}px  {out.relative_to(ROOT)}")


# The adaptive-icon canvas is 108dp but only the centre 66dp is guaranteed
# visible, so legacy icons are the safe zone blown up to fill the bitmap.
SAFE = "21 21 66 66"
FULL = "0 0 108 108"
ICON = "18 18 72 72"

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}


def main() -> None:
    if not pathlib.Path(CHROME).exists():
        sys.exit(f"Chrome not found at {CHROME}")

    print("adaptive layers (108dp):")
    for name, mult in DENSITIES.items():
        px = round(108 * mult)
        render(svg(FULL, px, SURFACE), px, RES / f"mipmap-{name}/ic_launcher_background.png")
        render(svg(FULL, px, SCALED_GLYPH), px, RES / f"mipmap-{name}/ic_launcher_foreground.png")

    print("legacy launcher icons (48dp, safe zone fills the bitmap):")
    for name, mult in DENSITIES.items():
        px = round(48 * mult)
        render(svg(SAFE, px, SURFACE + SCALED_GLYPH), px, RES / f"mipmap-{name}/ic_launcher.png")
        render(
            svg(SAFE, px, SURFACE + SCALED_GLYPH, clip="circle"),
            px,
            RES / f"mipmap-{name}/ic_launcher_round.png",
        )

    print("brand bitmap (72dp visible area, 512):")
    render(svg(ICON, 512, SURFACE_72 + SCALED_GLYPH, clip="icon"), 512, ROOT / "logo/carpe-icon.png")

    print("store art (512, unmasked square):")
    render(
        svg(ICON, 512, SURFACE_72 + SCALED_GLYPH),
        512,
        ROOT / "src/CarpeLines.KMP/app/src/main/ic_launcher-playstore.png",
    )


if __name__ == "__main__":
    main()
