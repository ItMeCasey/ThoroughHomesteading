"""Builds every texture of Thorough Homesteading from vanilla Minecraft textures, so they can be regenerated at any time.

Usage: python scripts/make_textures.py [path to a Minecraft client jar or a folder with assets/minecraft/textures]

The vanilla textures are read from the jar (the default is the client jar Gradle leaves in ~/.gradle/caches/neoformruntime/artifacts);
nothing from it is copied into the repository except what is generated here.

Hearth blocks: the side takes its pixels from the vanilla block of the tier (stone bricks, iron block, gold block) with a sooty band at the
bottom and a lighter top row, the bottom is the same block darkened, the top has the block's rim around a pit of ash (unlit) or embers
(lit) in the colors of the vanilla campfire and coal block. Upgrade items are a bell drawn pixel by pixel in the palette of the vanilla iron
and gold ingots. The textures are not approved art.
"""
import glob
import io
import os
import random
import sys
import zipfile

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "thoroughhomesteading", "textures")
BASE_BLOCK = {"stone": "block/stone_bricks", "iron": "block/iron_block", "golden": "block/gold_block"}
INGOT = {"iron": "item/iron_ingot", "golden": "item/gold_ingot"}


class Vanilla:
    def __init__(self, source=None):
        self.zip = None
        self.folder = None
        if source is None:
            found = sorted(glob.glob(os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_26.2_client.jar")))
            source = found[0] if found else None
        if source is None:
            raise SystemExit("No Minecraft client jar found; pass its path")
        if os.path.isdir(source):
            self.folder = source
        else:
            self.zip = zipfile.ZipFile(source)

    def load(self, name):
        path = f"assets/minecraft/textures/{name}.png"
        if self.zip:
            data = self.zip.read(path)
            img = Image.open(io.BytesIO(data))
        else:
            img = Image.open(os.path.join(self.folder, path))
        img = img.convert("RGBA")
        if img.height > img.width:  # animated strip: first frame
            img = img.crop((0, 0, img.width, img.width))
        return img


def shade(color, factor):
    r, g, b, a = color
    return (max(0, min(255, int(r * factor))), max(0, min(255, int(g * factor))), max(0, min(255, int(b * factor))), a)


def save(img, *parts):
    path = os.path.join(OUT, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def palette(img, minimum_alpha=200):
    colors = []
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if p[3] >= minimum_alpha:
                colors.append(p)
    colors.sort(key=lambda c: c[0] * 3 + c[1] * 6 + c[2])
    return colors


def hearth(v, tier):
    base = v.load(BASE_BLOCK[tier])
    seed = {"stone": 11, "iron": 23, "golden": 37}[tier]
    rng = random.Random(seed)
    # Side: the block with a sooty band at the bottom and a lighter top row.
    side = base.copy()
    for x in range(16):
        side.putpixel((x, 0), shade(base.getpixel((x, 0)), 1.12))
        side.putpixel((x, 14), shade(base.getpixel((x, 14)), 0.78))
        side.putpixel((x, 15), shade(base.getpixel((x, 15)), 0.6))
    # The model shows the lower 10 pixels of the side; the band at row 14 and 15 sits at the bottom edge.
    save(side, "block", f"{tier}_hearth_side.png")
    save(darken(base, 0.8), "block", f"{tier}_hearth_bottom.png")

    coal = palette(v.load("block/coal_block"))
    flame = palette(v.load("block/campfire_fire"), 220)
    rim = base.copy()
    for lit in (False, True):
        top = rim.copy()
        for y in range(2, 14):
            for x in range(2, 14):
                edge = x in (2, 13) or y in (2, 13)
                if edge:
                    top.putpixel((x, y), shade(base.getpixel((x, y)), 0.55))
                else:
                    c = coal[rng.randrange(0, max(1, len(coal) // 2))]
                    top.putpixel((x, y), c)
        if not lit:
            for _ in range(10):
                x, y = rng.randrange(3, 13), rng.randrange(3, 13)
                grey = coal[min(len(coal) - 1, len(coal) * 3 // 4 + rng.randrange(0, max(1, len(coal) // 4)))]
                top.putpixel((x, y), grey)
        else:
            for _ in range(34):
                x, y = rng.randrange(3, 13), rng.randrange(3, 13)
                top.putpixel((x, y), flame[rng.randrange(len(flame) // 4, len(flame) * 5 // 6)])
            for _ in range(8):
                x, y = rng.randrange(4, 12), rng.randrange(4, 12)
                top.putpixel((x, y), flame[len(flame) * 5 // 6])
        save(top, "block", f"{tier}_hearth_top{'_lit' if lit else ''}.png")


def darken(img, factor):
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            out.putpixel((x, y), shade(img.getpixel((x, y)), factor))
    return out


BELL = [
    "................",
    "......####......",
    ".....#....#.....",
    ".....#....#.....",
    "......####......",
    ".......##.......",
    "......####......",
    ".....######.....",
    "....########....",
    "....########....",
    "....########....",
    "...##########...",
    "...##########...",
    "..############..",
    "..############..",
    "....########....",
]


def upgrade_icon(v, tier):
    pal = palette(v.load(INGOT[tier]))
    n = len(pal)
    dark, mid, light, high = pal[0], pal[n // 2], pal[(n * 3) // 4], pal[-1]
    outline = shade(pal[0], 0.55)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mask = [[ch == "#" for ch in row] for row in BELL]

    def inside(x, y):
        return 0 <= x < 16 and 0 <= y < 16 and mask[y][x]

    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            edge = not (inside(x - 1, y) and inside(x + 1, y) and inside(x, y - 1) and inside(x, y + 1))
            if edge:
                color = outline
            elif x <= 5 or y <= 7 and x <= 7:
                color = light
            elif x >= 11:
                color = dark
            else:
                color = mid
            img.putpixel((x, y), color)
    img.putpixel((5, 9), high)
    img.putpixel((5, 10), high)
    if tier == "golden":
        img.putpixel((13, 3), high)
        img.putpixel((12, 4), light)
        img.putpixel((14, 4), light)
        img.putpixel((13, 5), high)
    else:
        # A small arrow under the bell marks it as an upgrade.
        for x, y in ((8, 15), (7, 15), (9, 15)):
            img.putpixel((x, y), light)
    save(img, "item", f"{tier}_hearth_upgrade.png")


def logo(v):
    side = Image.open(os.path.join(OUT, "block", "stone_hearth_side.png")).convert("RGBA")
    top = Image.open(os.path.join(OUT, "block", "stone_hearth_top_lit.png")).convert("RGBA")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    img.paste(top.crop((0, 0, 16, 8)), (0, 2))
    img.paste(side.crop((0, 6, 16, 16)), (0, 6))
    big = img.resize((128, 128), Image.NEAREST)
    path = os.path.join(ROOT, "common", "src", "main", "resources", "thoroughhomesteading_logo.png")
    big.save(path)


def main():
    v = Vanilla(sys.argv[1] if len(sys.argv) > 1 else None)
    for tier in ("stone", "iron", "golden"):
        hearth(v, tier)
    for tier in ("iron", "golden"):
        upgrade_icon(v, tier)
    if not os.path.exists(os.path.join(ROOT, "branding", "icon", "icon_512.png")):
        logo(v)
    print("textures written")


if __name__ == "__main__":
    main()
