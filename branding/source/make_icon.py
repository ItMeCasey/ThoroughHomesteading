"""Draws the Thorough Homesteading icon: a 32x32 pixel tile (night background, stepped corners, a villager bust with a warm hearth glow and gold embers) and
writes icon_32 to icon_1024 and the mod list logo.

Usage: python branding/source/make_icon.py
"""
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
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
    # A villager bust: bald head with the long nose and a single brow, green eyes, brown robe, warm hearth glow behind.
    SKIN = (196, 142, 108, 255)
    SKIN_LIGHT = (214, 164, 128, 255)
    SKIN_DARK = (160, 110, 82, 255)
    NOSE = (172, 118, 88, 255)
    NOSE_DARK = (140, 92, 68, 255)
    BROW = (62, 38, 22, 255)
    WHITE = (236, 236, 232, 255)
    GREEN = (68, 148, 58, 255)
    ROBE = (112, 74, 42, 255)
    ROBE_LIGHT = (136, 92, 54, 255)
    ROBE_DARK = (84, 54, 30, 255)
    GLOW = [(72, 36, 22, 255), (104, 52, 26, 255)]
    # Glow low behind the shoulders.
    for y in range(20, 30):
        for x in range(2, 30):
            if (x - 16) ** 2 / 196 + (y - 30) ** 2 / 64 < 1:
                img.putpixel((x, y), GLOW[0] if (x - 16) ** 2 / 100 + (y - 30) ** 2 / 36 > 1 else GLOW[1])

    def rect(x0, y0, x1, y1, c):
        for yy in range(y0, y1 + 1):
            for xx in range(x0, x1 + 1):
                img.putpixel((xx, yy), c)

    # Robe and shoulders.
    rect(6, 23, 25, 30, ROBE)
    rect(6, 23, 25, 23, ROBE_LIGHT)
    rect(15, 24, 16, 30, ROBE_DARK)
    rect(6, 27, 25, 27, ROBE_DARK)
    # Head.
    rect(9, 4, 22, 22, SKIN)
    rect(9, 4, 22, 5, SKIN_LIGHT)
    rect(9, 4, 9, 22, SKIN_DARK)
    rect(22, 4, 22, 22, SKIN_DARK)
    rect(9, 21, 22, 22, SKIN_DARK)
    # One brow across both eyes.
    rect(10, 10, 21, 10, BROW)
    rect(10, 9, 12, 9, BROW)
    rect(19, 9, 21, 9, BROW)
    # Eyes.
    rect(11, 12, 14, 13, WHITE)
    rect(17, 12, 20, 13, WHITE)
    rect(13, 12, 14, 13, GREEN)
    rect(17, 12, 18, 13, GREEN)
    # Nose.
    rect(14, 14, 17, 20, NOSE)
    rect(14, 14, 14, 20, SKIN_LIGHT)
    rect(14, 20, 17, 20, NOSE_DARK)
    rect(17, 14, 17, 19, NOSE_DARK)
    # Embers rising from the hearth below.
    for x, y, c in [(6, 20, GOLD_LIGHT), (25, 19, GOLD), (4, 24, GOLD), (27, 25, GOLD_LIGHT), (8, 17, GOLD_DARK), (24, 14, GOLD_DARK)]:
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
