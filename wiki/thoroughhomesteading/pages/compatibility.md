---
title: Compatibility
summary: Which recipe viewers, look-at mods, script hosts and quest mods work with Thorough Homesteading on which loader and version.
---
Every integration is optional. The mod runs the same without any of them.

## Recipe viewers

| Viewer | NeoForge (26.1.2, 26.2, 26.3) | Forge (26.1.2, 26.2, 26.3) |
|---|---|---|
| BEI | Yes | Yes |
| JEI | Yes | No JEI build exists |
| REI | Yes | No REI build exists |
| EMI | No EMI build exists for these versions | No EMI build exists for these versions |

In each viewer you get a Settling category (workstation and extra items in, profession out, food and tier as values), a Hearth Upgrade category, information pages for each Hearth and each workstation with a settling recipe, and the three crafting recipes in the viewer's own crafting category.

### Which settling recipes the viewers list

| Loader | Viewers list |
|---|---|
| NeoForge | The live settling recipes. The server sends them to the client. |
| Forge | The 13 shipped defaults. Recipes a datapack or script changes are used by the server but the viewer still lists the defaults. |

## Look-at

| Mod | NeoForge | Forge |
|---|---|---|
| Show Me What This Is | Yes | Yes |
| Jade | Yes | No Jade build exists |

Both show `Settlers 2 of 4`, `Beds 1`, `Workstations 0` and `Food 12 of 20`, plus one line for a missing bed, workstation or light as a value such as `Workstations 0 of 1`.

## Scripting

| Host | What you get | Builds |
|---|---|---|
| ModJS (primary) | `Homesteading` and `HomesteadingEvents` in server scripts | All six |
| KubeJS | The same objects as `global.Homesteading` and `global.HomesteadingEvents` from a helper startup script; settling recipes, trades and tiers as datapack JSON | All six |
| Datapacks | Settling recipes, trade files, tier files, crafting recipes | All six |

See [[scripting]].

## Barely Questing

| Feature | What it does |
|---|---|
| `settler` task type | A quest counts settlers the player's Hearths have brought, optionally by profession. |
| Team ownership | The owner's Barely Questing team can feed, upgrade and break the owner's Hearth. |

Both work on all six builds and only when Barely Questing is installed.

## Other mods

- A workstation or profession from any mod works once a [[settling]] recipe names it.
- Settlers are vanilla villagers, so other mods' villager features, trade screens and quests keep working.
- Village generation and villager behavior are not changed.

## Not verified on a real client

Nothing was run on a real game client. The headless server self test covers logic only. The viewer plugins and the look-at plugins compile but were not opened in a game, and the ModJS, KubeJS and Barely Questing bridges were not run against live installs. See [[versions]].
