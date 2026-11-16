# Design note (as built)

This note says what the design spec decided, what the code does, and which defaults were picked for the points the spec left open. Where they differ, the code is the truth.

## What the spec decided

- A skyblock mod: a lit Hearth checks for free beds, workstations, light and food around it, and then brings a vanilla villager (a settler).
- Forge and NeoForge on Minecraft 26.1.2, 26.2 and 26.3, one jar per loader and version, shared code in `common/`.
- Settlers are vanilla villagers. Upgrades are items used on a Hearth, one tier at a time. The Hearth works in any world.
- Ownership is the placing player plus their Barely Questing team when that mod is installed.
- A settler whose home is gone stays a normal villager.
- Everything tunable is a config value or datapack JSON. No hints, tutorial text or screens.
- The Caravan (a Trade Post for wandering traders) is not in this release.

## What was built

| Area | As built |
|---|---|
| Blocks | `stone_hearth`, `iron_hearth`, `golden_hearth`. 16 by 10 by 16 shape. Lit with flint and steel or a fire charge, put out with a shovel. A comparator reads how full the 9 slots are. No screen. |
| Items | `iron_hearth_upgrade`, `golden_hearth_upgrade` (stack of 16). Each raises a Hearth exactly one tier and keeps contents, owner, settlers, countdown and lit state. |
| Food | Put in by hand or by hopper (from above and the sides; nothing comes out). Any item with food nutrition is accepted, plus the extra items a settling recipe names. |
| Home check | Every `scan_interval` ticks (200). Reach is the tier radius (8, 12, 16) horizontally and 6 blocks up and down. Checks in this order: population, free beds, free workstations, light at a bed, food. A standing-room check (a path from the arrival point to the bed) runs when everything else passes. |
| Countdown | Advances by the scan interval while the check passes and the Hearth is lit, then brings the settler. `settle_ticks` is 6000, 4000 and 2400. |
| Arrival | The nearest standable block to the Hearth. A vanilla villager with the workstation's profession, the bed as its home and the workstation as its job site, a green particle trail and a sound. Golden Hearths start settlers at trade level 2. |
| Recipes | Recipe type `thoroughhomesteading:settling` (workstation, profession, min_tier, food, extra). 13 shipped. |
| Trades and tiers | Datapack folders `thoroughhomesteading/trades` and `thoroughhomesteading/hearth`, read again on `/reload`. |
| Settings | 14 keys in `config/thoroughhomesteading-common.toml` (see the wiki Settings page). |
| Look-at | Show Me What This Is on all builds, Jade on NeoForge. |
| Viewers | BEI on all builds, JEI and REI on NeoForge. Categories Settling and Hearth Upgrade, plus information pages. |
| Scripting | ModJS globals `Homesteading` and `HomesteadingEvents`; KubeJS helper script and datapack JSON. |
| Barely Questing | `settler` task type and team shared ownership. |

## Defaults picked for the open items

| Open item | Choice |
|---|---|
| Hearth look | Two blocks tall, like a door (the upper half needs the block above free). Stone Hearth: a campfire in a stone ring with a bell hung from a wooden pole. Iron Hearth: an iron ring, a banded iron post with a brace and a bell. Golden Hearth: a gold gazebo with gem inlays, a stepped roof and a bell hanging over the fire. The fire shows when lit. Models and textures come from `scripts/make_hearth_models.py` and `make_hearth_textures.py`. |
| Tier names | Stone, Iron, Golden. |
| Trade tables | None shipped. Settlers trade like vanilla villagers of their profession until a pack adds trade files. |
| Settling recipes | The 13 vanilla workstation and profession pairs (below). All use tier 1 and the `food_required` default of 20. |

### The 13 shipped settling recipes

| Workstation | Profession |
|---|---|
| `minecraft:composter` | `minecraft:farmer` |
| `minecraft:barrel` | `minecraft:fisherman` |
| `minecraft:blast_furnace` | `minecraft:armorer` |
| `minecraft:brewing_stand` | `minecraft:cleric` |
| `minecraft:cartography_table` | `minecraft:cartographer` |
| `minecraft:fletching_table` | `minecraft:fletcher` |
| `minecraft:grindstone` | `minecraft:weaponsmith` |
| `minecraft:lectern` | `minecraft:librarian` |
| `minecraft:loom` | `minecraft:shepherd` |
| `minecraft:smithing_table` | `minecraft:toolsmith` |
| `minecraft:smoker` | `minecraft:butcher` |
| `minecraft:stonecutter` | `minecraft:mason` |
| `minecraft:cauldron` | `minecraft:leatherworker` |

The crafting recipes: Stone Hearth is a campfire surrounded by stone bricks. Each upgrade item is a bell surrounded by eight ingots (iron for the Iron upgrade, gold for the Golden upgrade).

## Where the code differs from the spec

| Spec | Code |
|---|---|
| Scripts read `event.settler`, `event.hearth`, `event.profession`. | An event is read with `event.string(key)`, `event.number(key)` and `event.get(key)`. There is no `event.hearth`; the Hearth position is the keys `x`, `y`, `z` and `dimension`. |
| `Homesteading.report` returns every requirement, the timer and the settlers. | It does, as a flat map of 17 keys (see the scripting reference). |
| The home check lists radius, beds, workstations, light, standing room, food and population as separate requirements. | The blocking reason is one of population, beds, workstations, light, food. Standing room is folded into beds: a bed nobody can walk to counts as no usable bed. |
| Information pages on each Hearth and workstation. | Built for BEI, JEI and REI. In BEI each page is a paper item named for the subject, with the value lines as its lore. |
| Unlit Hearth does nothing. | With `allow_unlit_hearth_scan` true an unlit Hearth runs the check so look-at lines update; the countdown still needs a lit Hearth. |
