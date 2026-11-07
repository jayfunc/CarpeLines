"""CarpeLines logo -- geometry + contrast reference.

Prints the Android vector `pathData` for every shape in the mark, plus the WCAG
contrast numbers quoted in README.md and index.html, so both stay reproducible.

    python logo/tools/generate.py

The SVG files use <rect rx> / <circle> because that is far easier to hand-tune
than path data; this script is the source of truth for the numbers.
"""

# ---------------------------------------------------------------- geometry ---
# 108 x 108 = Android adaptive-icon canvas. Everything is a multiple of 4.
L1 = (25, 34, 75, 42)          # finished line
L2 = (25, 50, 84, 58)          # active line
L3 = (25, 66, 55, 74)          # upcoming line
KNOB = (60, 54, 7)             # cx, cy, r  -> Ø14, at 59% of L2
GAP = 2                        # clearance on both sides of the knob
R = 4                          # line radius = half the line height (a pill)
CONTAINER_R = 30               # 27.8% of the canvas


def sweep():
    """Sung segment end / knob centre / unsung segment start."""
    cx = KNOB[0]
    return cx - KNOB[2] - GAP, cx, cx + KNOB[2] + GAP


def rrect(x0, y0, x1, y1, r):
    """Rounded rect as pathData -- Android vector drawables have no <rect>."""
    r = min(r, (y1 - y0) / 2, (x1 - x0) / 2)
    k = 0.5522847498 * r
    return (
        f"M{x0},{y0 + r} C{x0},{y0 + r - k} {x0 + r - k},{y0} {x0 + r},{y0} "
        f"L{x1 - r},{y0} C{x1 - r + k},{y0} {x1},{y0 + r - k} {x1},{y0 + r} "
        f"L{x1},{y1 - r} C{x1},{y1 - r + k} {x1 - r + k},{y1} {x1 - r},{y1} "
        f"L{x0 + r},{y1} C{x0 + r - k},{y1} {x0},{y1 - r + k} {x0},{y1 - r} Z"
    )


def circle(cx, cy, r):
    k = 0.5522847498 * r
    return (
        f"M{cx},{cy - r} C{cx + k},{cy - r} {cx + r},{cy - k} {cx + r},{cy} "
        f"C{cx + r},{cy + k} {cx + k},{cy + r} {cx},{cy + r} "
        f"C{cx - k},{cy + r} {cx - r},{cy + k} {cx - r},{cy} "
        f"C{cx - r},{cy - k} {cx - k},{cy - r} {cx},{cy - r} Z"
    )


# ---------------------------------------------------------------- contrast ---
def lum(hexs):
    r, g, b = (int(hexs[i:i + 2], 16) / 255 for i in (1, 3, 5))
    f = lambda c: c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)


def contrast(a, b):
    la, lb = lum(a), lum(b)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


def composite(fg, alpha, bg):
    f = [int(fg[i:i + 2], 16) for i in (1, 3, 5)]
    b = [int(bg[i:i + 2], 16) for i in (1, 3, 5)]
    return "#" + "".join(f"{round(alpha * x + (1 - alpha) * y):02X}"
                         for x, y in zip(f, b))


# Container gradient, top-left -> bottom-right. Keep in sync with the SVG / drawable.
# Near-white warm paper, top-left light -> bottom-right faint apricot.
BG_STOPS = ((0.0, "#FFF6EA"), (1.0, "#FFE4C6"))


def bg_at(x, y):
    """Container colour under a canvas point (diagonal gradient over 0..108)."""
    t = max(0.0, min(1.0, (x + y) / 216))
    for (o0, c0), (o1, c1) in zip(BG_STOPS, BG_STOPS[1:]):
        if t <= o1:
            k = (t - o0) / (o1 - o0)
            return "#" + "".join(
                f"{round(int(c0[i:i + 2], 16) + (int(c1[i:i + 2], 16) - int(c0[i:i + 2], 16)) * k):02X}"
                for i in (1, 3, 5)
            )
    return BG_STOPS[-1][1]


def ink_backgrounds():
    """Points just outside each stroke: the colours the ink actually sits on.

    Contrast has to be measured here, not at the gradient ends — the lightest
    corner is decoration only and never carries a glyph.
    """
    pts = []
    for x0, y0, x1, y1 in (L1, L2, L3):
        mid = (y0 + y1) / 2
        pts += [(x0 + 4, y0 - 3), (x1 - 4, y0 - 3), (x0 - 3, mid), (x0 + 4, y1 + 3)]
    cx, cy, r = KNOB
    pts += [(cx, cy - r - 2), (cx, cy + r + 2)]
    return pts

if __name__ == "__main__":
    sung_end, cx, dim_start = sweep()

    print("== Android pathData ==")
    print(f"container   {rrect(0, 0, 108, 108, CONTAINER_R)}")
    print(f"L1          {rrect(*L1, R)}")
    print(f"L2 sung     {rrect(L2[0], L2[1], sung_end, L2[3], R)}")
    print(f"L2 unsung   {rrect(dim_start, L2[1], L2[2], L2[3], R)}")
    print(f"L3          {rrect(*L3, R)}")
    print(f"knob        {circle(*KNOB)}")
    print()
    print(f"knob at {(cx - L2[0]) / (L2[2] - L2[0]):.0%} of L2, "
          f"sung {sung_end - L2[0]:.0f} / unsung {L2[2] - dim_start:.0f}")
    print()

    print("== contrast, light surface (where the ink sits) ==")
    bgs = [bg_at(x, y) for x, y in ink_backgrounds()]
    for name, fg in (
        ("sung     #D9480F", "#D9480F"),
        ("unsung   #8A7663", "#8A7663"),
        ("knob     #D9480F", "#D9480F"),
        ("wordmark #1C1917", "#1C1917"),
    ):
        vals = [contrast(fg, b) for b in bgs]
        print(f"{name}  {min(vals):.2f} - {max(vals):.2f} : 1")
    lightest = max(bgs, key=lum)
    print(f"lightest ink bg {lightest}; 38% alpha would give "
          f"{composite('#1C1917', .38, lightest)} = "
          f"{contrast(composite('#1C1917', .38, lightest), lightest):.2f} : 1  (fails 3:1)")
    print(f"gradient ends {BG_STOPS[0][1]} / {BG_STOPS[-1][1]}: "
          f"unsung {contrast('#8A7663', BG_STOPS[0][1]):.2f} / "
          f"{contrast('#8A7663', BG_STOPS[-1][1]):.2f} : 1  (corners, decoration only)")
