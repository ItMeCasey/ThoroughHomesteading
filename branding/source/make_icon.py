"""Draws the Thorough Homesteading icon: a 32x32 pixel tile (night background, stepped corners, a lit stone Hearth, gold embers) and
writes icon_32 to icon_1024 and the mod list logo. Uses the Hearth textures of the mod, so run make_textures.py first.

Usage: python branding/source/make_icon.py
"""
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEX = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "thoroughhomesteading", "textures", "block")
OUT = os.path.join(ROOT, "branding", "icon")
NIGHT_TOP = (15, 14, 26, 255)
NIGHT_BOTTOM = (26, 24, 48, 255)
EDGE = (46, 42, 70, 255)
GOLD = (242, 179, 61, 255)
GOLD_LIGHT = (255, 226, 140, 255)
GOLD_DARK = (178, 116, 32, 255)


def tile():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        t = y / 31
        color = tuple(int(NIGHT_TOP[i] * (1 - t) + NIGHT_BOTTOM[i] * t) for i in range(3)) + (255,)
        for x in range(32):
            img.putpixel((x, y), color)
    # Stepped corners: clear the corner pixels, then draw the edge just inside.
    corners = [(0, 0), (1, 0), (0, 1), (31, 0), (30, 0), (31, 1), (0, 31), (1, 31), (0, 30), (31, 31), (30, 31), (31, 30)]
    for c in corners:
        img.putpixel(c, (0, 0, 0, 0))
    for x in range(2, 30):
        img.putpixel((x, 0), EDGE)
        img.putpixel((x, 31), EDGE)
    for y in range(2, 30):
        img.putpixel((0, y), EDGE)
        img.putpixel((31, y), EDGE)
    for c in [(1, 1), (30, 1), (1, 30), (30, 30)]:
        img.putpixel(c, EDGE)
    side = Image.open(os.path.join(TEX, "stone_hearth_side.png")).convert("RGBA")
    top = Image.open(os.path.join(TEX, "stone_hearth_top_lit.png")).convert("RGBA")
    # A hearth seen from the front and above: the lit top squashed to 7 rows over the lower 10 rows of the side.
    squashed = top.resize((16, 7), Image.NEAREST)
    hearth = Image.new("RGBA", (16, 17), (0, 0, 0, 0))
    hearth.paste(squashed, (0, 0))
    hearth.paste(side.crop((0, 6, 16, 16)), (0, 7))
    big = hearth.resize((24, 25), Image.NEAREST)
    img.paste(big, (4, 5), big)
    # Embers rising from the pit.
    for x, y, c in [(10, 4, GOLD_LIGHT), (16, 2, GOLD), (21, 4, GOLD_LIGHT), (13, 3, GOLD_DARK), (19, 1, GOLD_DARK), (7, 2, GOLD)]:
        img.putpixel((x, y), c)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    base = tile()
    for size in (32, 64, 128, 256, 512, 1024):
        base.resize((size, size), Image.NEAREST).save(os.path.join(OUT, f"icon_{size}.png"))
    base.resize((128, 128), Image.NEAREST).save(os.path.join(OUT, "thoroughhomesteading_logo.png"))
    base.resize((128, 128), Image.NEAREST).save(os.path.join(ROOT, "common", "src", "main", "resources", "thoroughhomesteading_logo.png"))
    print("icon written")


if __name__ == "__main__":
    main()
