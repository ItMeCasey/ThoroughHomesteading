"""Writes the resource files of Thorough Homesteading that are not textures: blockstates, models, item definitions, language, recipes,
loot tables and tags. Textures come from make_textures.py.

Shared files go to common/src/main/resources; the loot tables (whose format differs in 26.3) go to every version-<mc> folder. Run it
again after changing a recipe or a name; it overwrites what it writes.
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MOD = "thoroughhomesteading"
COMMON = os.path.join(ROOT, "common", "src", "main", "resources")
ASSETS = os.path.join(COMMON, "assets", MOD)
DATA = os.path.join(COMMON, "data", MOD)
VERSIONS = {"26.1.2": "old", "26.2": "old", "26.3": "new"}
TIERS = ["stone", "iron", "golden"]
PARTICLE = {"stone": "stone_bricks", "iron": "iron_block", "golden": "gold_block"}

# workstation, profession (kept equal to SettlingDefaults.java; a unit test compares them)
SETTLING = [
    ("composter", "farmer"), ("barrel", "fisherman"), ("blast_furnace", "armorer"), ("brewing_stand", "cleric"),
    ("cartography_table", "cartographer"), ("fletching_table", "fletcher"), ("grindstone", "weaponsmith"), ("lectern", "librarian"),
    ("loom", "shepherd"), ("smithing_table", "toolsmith"), ("smoker", "butcher"), ("stonecutter", "mason"), ("cauldron", "leatherworker"),
]


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        if isinstance(data, str):
            f.write(data)
        else:
            json.dump(data, f, indent=2)
            f.write("\n")


def loot(block, style):
    # A Hearth is two blocks tall: only the lower half drops the item (the upper half drops nothing), like a door.
    lower = {"block": f"{MOD}:{block}", "properties": {"half": "lower"}}
    if style == "old":
        pool = {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}"}],
                "conditions": [{"condition": "minecraft:block_state_property", **lower}, {"condition": "minecraft:survives_explosion"}]}
    else:
        pool = {"rolls": 1, "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:block_state_property", **lower},
                                                                                 {"type": "minecraft:survives_explosion"}]},
                "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}"}]}
    return {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{MOD}:blocks/{block}"}


LANG = {
    "itemGroup.thoroughhomesteading": "Thorough Homesteading",
    "block.thoroughhomesteading.stone_hearth": "Stone Hearth",
    "block.thoroughhomesteading.iron_hearth": "Iron Hearth",
    "block.thoroughhomesteading.golden_hearth": "Golden Hearth",
    "item.thoroughhomesteading.iron_hearth_upgrade": "Iron Hearth Upgrade",
    "item.thoroughhomesteading.golden_hearth_upgrade": "Golden Hearth Upgrade",
    # Look-at lines (Show Me What This Is and Jade): names and values only
    "smwti.thoroughhomesteading.settlers": "Settlers %s of %s",
    "smwti.thoroughhomesteading.beds": "Beds %s",
    "smwti.thoroughhomesteading.workstations": "Workstations %s",
    "smwti.thoroughhomesteading.food": "Food %s of %s",
    "smwti.thoroughhomesteading.need.population": "Settlers %s of %s",
    "smwti.thoroughhomesteading.need.beds": "Beds %s of %s",
    "smwti.thoroughhomesteading.need.workstations": "Workstations %s of %s",
    "smwti.thoroughhomesteading.need.light": "Light %s of %s",
    "smwti.thoroughhomesteading.need.food": "Food %s of %s",
    # Recipe viewers
    "viewer.thoroughhomesteading.settling": "Settling",
    "viewer.thoroughhomesteading.hearth_upgrade": "Hearth Upgrade",
    "viewer.thoroughhomesteading.info": "Hearth",
    "viewer.thoroughhomesteading.info.radius": "Radius %s",
    "viewer.thoroughhomesteading.info.population": "Settlers %s",
    "viewer.thoroughhomesteading.info.seconds": "Arrival %s s",
    "viewer.thoroughhomesteading.info.level": "Trade level %s",
    "viewer.thoroughhomesteading.info.profession": "Profession %s",
    "viewer.thoroughhomesteading.info.food": "Food %s",
    "viewer.thoroughhomesteading.info.tier": "Hearth tier %s",
    "viewer.thoroughhomesteading.tier": "%s Hearth",
}


def main():
    # The Hearth blockstates, models and item models come from scripts/make_hearth_models.py.
    for t in TIERS[1:]:
        name = f"{t}_hearth_upgrade"
        write(os.path.join(ASSETS, "models", "item", f"{name}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        write(os.path.join(ASSETS, "items", f"{name}.json"), {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}"}})

    # Crafting recipes
    def shaped(pattern, key, result):
        return {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                "key": {k: f"minecraft:{v}" for k, v in key.items()}, "result": {"id": f"{MOD}:{result}", "count": 1}}

    write(os.path.join(DATA, "recipe", "stone_hearth.json"), shaped(["SSS", "SCS", "SSS"], {"S": "stone_bricks", "C": "campfire"}, "stone_hearth"))
    write(os.path.join(DATA, "recipe", "iron_hearth_upgrade.json"), shaped(["III", "IBI", "III"], {"I": "iron_ingot", "B": "bell"}, "iron_hearth_upgrade"))
    write(os.path.join(DATA, "recipe", "golden_hearth_upgrade.json"), shaped(["GGG", "GBG", "GGG"], {"G": "gold_ingot", "B": "bell"}, "golden_hearth_upgrade"))
    # Settling recipes: one per workstation
    for ws, prof in SETTLING:
        write(os.path.join(DATA, "recipe", f"settle_{ws}.json"), {
            "type": f"{MOD}:settling", "workstation": f"minecraft:{ws}", "profession": f"minecraft:{prof}", "min_tier": 1})

    # Loot tables per version
    for version, style in VERSIONS.items():
        base = os.path.join(ROOT, f"version-{version}", "src", "main", "resources", "data", MOD)
        for t in TIERS:
            write(os.path.join(base, "loot_table", "blocks", f"{t}_hearth.json"), loot(f"{t}_hearth", style))

    # Tags: the Hearths need a pickaxe
    write(os.path.join(COMMON, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json"),
          {"replace": False, "values": [f"{MOD}:{t}_hearth" for t in TIERS]})
    write(os.path.join(COMMON, "data", "minecraft", "tags", "block", "needs_stone_tool.json"),
          {"replace": False, "values": [f"{MOD}:{t}_hearth" for t in TIERS]})

    # Language
    write(os.path.join(ASSETS, "lang", "en_us.json"), dict(sorted(LANG.items())))
    print("assets written")


if __name__ == "__main__":
    main()
