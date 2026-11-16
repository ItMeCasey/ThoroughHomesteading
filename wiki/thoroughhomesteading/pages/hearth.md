---
title: The Hearth
summary: The block that runs the home check, holds the food and brings the settlers. Three tiers, upgraded one at a time.
infobox:
  Ids: stone_hearth, iron_hearth, golden_hearth
  Screen: None
  Holds: 9 stacks of food
  Comparator: Fullness of the 9 slots
---
The Hearth is a low block, 10 pixels high. It checks its surroundings, keeps the food, remembers its settlers and runs the countdown. A Hearth only works while its chunk is loaded.

## Tiers

| Tier | Block | Radius | Settlers | Countdown | Start level |
|---|---|---|---|---|---|
| Stone | `thoroughhomesteading:stone_hearth` | 8 | 2 | 6000 ticks | 1 |
| Iron | `thoroughhomesteading:iron_hearth` | 12 | 4 | 4000 ticks | 1 |
| Golden | `thoroughhomesteading:golden_hearth` | 16 | 8 | 2400 ticks | 2 |

The radius is horizontal; the scan also reaches 6 blocks up and 6 blocks down. The start level is the trade level a new settler begins at, so Golden settlers arrive with the second trade level already open. Every number can be changed in the config ([[settings]]) or in a datapack tier file ([[trades]]).

## Crafting

| Item | Recipe |
|---|---|
| Stone Hearth | A campfire in the center, stone bricks in the other eight slots. |
| Iron Hearth Upgrade | A bell in the center, iron ingots in the other eight slots. |
| Golden Hearth Upgrade | A bell in the center, gold ingots in the other eight slots. |

The recipes are normal datapack JSON, so packs can change them.

## Lighting

Use flint and steel or a fire charge on the Hearth. A shovel puts it out. An unlit Hearth does nothing unless `allow_unlit_hearth_scan` is true, and even then the countdown only runs while lit. A lit Hearth gives off a crackling sound and particles.

## Feeding

Right-click with food, or put food in from a hopper. Hoppers insert from above and from the sides, and nothing comes out of a Hearth. The Hearth takes any item that has food nutrition, plus any extra item a [[settling]] recipe asks for. It holds 9 stacks.

A comparator next to the Hearth reads how full the 9 slots are.

## Upgrading

Use an upgrade item on a Hearth. It raises the Hearth exactly one tier:

- Stone to Iron with the Iron Hearth Upgrade.
- Iron to Golden with the Golden Hearth Upgrade.

An upgrade never skips a tier. The food, the owner, the settlers, the countdown and the lit state all stay. The upgrade item is used up (not in creative mode).

## Ownership

The player who places a Hearth owns it. The owner can feed it, light it, put it out, upgrade it and break it. When Barely Questing is installed, the owner's Barely Questing team has the same rights. Players in creative mode can always manage a Hearth.

| Who | Can feed, light, upgrade and break |
|---|---|
| Owner | Yes |
| Owner's Barely Questing team | Yes, when Barely Questing is installed |
| Anyone else | No |
| Anyone, on a Hearth with no owner | Yes |

Everyone can trade with the settlers. Hoppers can always insert food. In single player none of this changes anything.

## Breaking

A broken Hearth drops itself and its food. Settlers stay villagers.

## Look-at lines

Show Me What This Is (all builds) and Jade (NeoForge builds) show the Hearth's state as names and values. See [[troubleshooting]] for what each line means.
