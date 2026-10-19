"""Generate legacy launcher PNGs and the README icon from a single design.

Run with Pillow available:
    python tools/make_icons.py
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
SIZE = 1024
S = 4  # supersampling
W = SIZE * S


def lerp(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))  # type: ignore[return-value]


def cubic(p0, p1, p2, p3, steps=80):
    points = []
    for i in range(steps + 1):
        t = i / steps
        mt = 1 - t
        x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
        y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
        points.append((x * S, y * S))
    return points


def droplet_mask() -> Image.Image:
    mask = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(mask)
    pts = []
    pts += cubic((512, 330), (584, 428), (630, 500), (630, 565))[:-1]
    pts += cubic((630, 565), (630, 641), (577, 688), (512, 688))[:-1]
    pts += cubic((512, 688), (447, 688), (394, 641), (394, 565))[:-1]
    pts += cubic((394, 565), (394, 500), (440, 428), (512, 330))
    d.polygon(pts, fill=255)
    return mask


def main() -> None:
    bg = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    # Vertical deep-teal gradient.
    px = bg.load()
    for y in range(W):
        t = y / (W - 1)
        c = lerp((12, 44, 52), (3, 12, 16), t)
        for x in range(W):
            px[x, y] = (*c, 255)

    glow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    cx = cy = W / 2
    for r, alpha in ((430 * S, 44), (360 * S, 54), (310 * S, 50)):
        gd.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(40, 130, 128, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(55 * S))
    bg = Image.alpha_composite(bg, glow)

    layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    center = W / 2
    radius = 300 * S
    width = 48 * S
    box = (center - radius, center - radius, center + radius, center + radius)
    d.arc(box, 0, 360, fill=(46, 106, 115, 255), width=width)

    # Sweep the progress arc in small segments so it can carry a gradient.
    start = -90
    end = 250
    steps = 160
    for i in range(steps):
        a0 = start + (end - start) * i / steps
        a1 = start + (end - start) * (i + 1) / steps + 0.6
        t = i / max(1, steps - 1)
        color = lerp((99, 201, 255), (200, 255, 232), min(1.0, t * 1.15))
        d.arc(box, a0, a1, fill=(*color, 255), width=width)
    # Rounded caps.
    for angle, color in ((start, (99, 201, 255)), (end, (200, 255, 232))):
        r = math.radians(angle)
        x = center + math.cos(r) * radius
        y = center + math.sin(r) * radius
        d.ellipse((x - width / 2, y - width / 2, x + width / 2, y + width / 2),
                  fill=(*color, 255))

    # Outer bloom for the arc.
    arc_glow = layer.filter(ImageFilter.GaussianBlur(18 * S))
    bg = Image.alpha_composite(bg, arc_glow)
    bg = Image.alpha_composite(bg, layer)

    # Droplet with a soft vertical gradient.
    mask = droplet_mask()
    drop_grad = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    dp = drop_grad.load()
    for y in range(W):
        t = max(0.0, min(1.0, (y - 330 * S) / (688 * S - 330 * S)))
        c = lerp((200, 255, 232), (74, 225, 198), t)
        for x in range(W):
            dp[x, y] = (*c, 255)
    drop = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    drop.paste(drop_grad, (0, 0), mask)
    drop = drop.filter(ImageFilter.GaussianBlur(0.7 * S))
    bg = Image.alpha_composite(bg, drop)

    # Highlight inside the droplet.
    hi = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hi)
    hd.ellipse((453 * S, 490 * S, 493 * S, 575 * S), fill=(255, 255, 255, 70))
    hi = hi.filter(ImageFilter.GaussianBlur(14 * S))
    hi.putalpha(Image.composite(hi.getchannel("A"), Image.new("L", (W, W), 0), mask))
    bg = Image.alpha_composite(bg, hi)

    bg = bg.resize((SIZE, SIZE), Image.LANCZOS)

    square = bg.copy()
    # Legacy square icon with gently rounded corners.
    rounded = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    mask_square = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(mask_square).rounded_rectangle((0, 0, SIZE - 1, SIZE - 1),
                                                  radius=210, fill=255)
    rounded.paste(square, (0, 0), mask_square)

    round_icon = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    mask_round = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(mask_round).ellipse((0, 0, SIZE - 1, SIZE - 1), fill=255)
    round_icon.paste(square, (0, 0), mask_round)

    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for density, px in sizes.items():
        out = RES / f"mipmap-{density}"
        out.mkdir(parents=True, exist_ok=True)
        rounded.resize((px, px), Image.LANCZOS).save(out / "ic_launcher.png")
        round_icon.resize((px, px), Image.LANCZOS).save(out / "ic_launcher_round.png")

    preview = ROOT / "preview"
    preview.mkdir(exist_ok=True)
    square.save(preview / "icon.png")
    print(f"Icons written to {RES} and {preview / 'icon.png'}")


if __name__ == "__main__":
    main()
