# Compatibility

Thorough Homesteading 2026.11.1 ships six builds: Forge and NeoForge on Minecraft 26.1.2, 26.2 and 26.3 (Java 25). Every integration below is optional. The mod runs the same without any of them.

## Recipe viewers

| Viewer | NeoForge 26.1.2 | NeoForge 26.2 | NeoForge 26.3 | Forge 26.1.2 | Forge 26.2 | Forge 26.3 |
|---|---|---|---|---|---|---|
| BEI (`bei-api`) | yes | yes | yes | yes | yes | yes |
| JEI | yes | yes | yes | no build exists | no build exists | no build exists |
| REI | yes | yes | yes | no build exists | no build exists | no build exists |
| EMI | no EMI build exists for these versions | same | same | same | same | same |

What the viewers show (BEI, JEI and REI alike):

- A **Settling** category: the workstation (and any extra items) in, the profession out, with food and tier as values.
- A **Hearth Upgrade** category: a Hearth plus an upgrade item gives the next Hearth.
- Information pages for each Hearth tier (radius, settlers, arrival seconds, trade level) and for each workstation that has a settling recipe (profession, food).
- The three crafting recipes (Stone Hearth and the two upgrade items) in each viewer's own crafting category.

There is no EMI plugin because no EMI build exists for 26.1.2, 26.2 or 26.3.

### Which settling recipes the viewers show

| Loader | Viewers show |
|---|---|
| NeoForge | The live settling recipes. The server sends them to the client on datapack sync, and the client clears them when it leaves the world. |
| Forge | The shipped defaults (the 13 recipes in the mod). Recipes a datapack or script adds, changes or removes are used by the server but are not what the viewer lists. |

Before any recipes have arrived (for example in the title screen), both loaders show the shipped defaults (`SettlingRecipes.forViewers`).

## Look-at plugins

| Plugin | NeoForge 26.1.2 | NeoForge 26.2 | NeoForge 26.3 | Forge 26.1.2 | Forge 26.2 | Forge 26.3 |
|---|---|---|---|---|---|---|
| Show Me What This Is | yes | yes | yes | yes | yes | yes |
| Jade | yes | yes | yes | no build exists | no build exists | no build exists |

Both show the same lines: `Settlers 2 of 4`, `Beds 1`, `Workstations 0`, `Food 12 of 20`, plus one line for a missing bed, workstation or light as a value such as `Workstations 0 of 1`.

## Scripting

| Host | What it gets | Loaders and versions |
|---|---|---|
| ModJS (primary) | The `Homesteading` and `HomesteadingEvents` globals in every non-client script scope. | All six builds. |
| KubeJS | The same two objects as `global.Homesteading` and `global.HomesteadingEvents` from a helper startup script the mod writes to `kubejs/startup_scripts/thoroughhomesteading.js` (never overwrites an existing file). Recipes, trades and tiers are datapack JSON. | All six builds (the bridge uses no KubeJS classes at compile time). |
| Datapacks | Settling recipes (`thoroughhomesteading:settling`), trade files, Hearth tier files, the three crafting recipes. | All six builds. |

Reference: [scripting.md](scripting.md) and [thoroughhomesteading.d.ts](thoroughhomesteading.d.ts).

## Barely Questing

| Feature | What it does | Builds |
|---|---|---|
| `settler` task type | `{"type": "settler", "count": 3, "profession": "minecraft:farmer"}` (profession optional). Counts settlers the player's Hearths have brought, read from a ledger in the world folder. | All six |
| Team shared ownership | The placing player owns a Hearth. Members of the owner's Barely Questing team may also feed, upgrade and break it. Without Barely Questing, only the owner can. A Hearth with no owner is open to everyone. Hoppers can always insert food. | All six |

Both are reached by reflection. Without Barely Questing the mod runs the same and the team check is always false.

## Other mods

- Any mod that adds a workstation block and a villager profession works once a settling recipe names the block and the profession. The profession must exist in the registry, or the workstation is not counted.
- Vanilla villagers are the settlers, so other mods' trade screens, quests and villager changes keep working.
- Village generation and villager AI are not changed.
- Trades from a trade file are applied to the settler as it gains levels. Trades another mod adds to villagers are untouched.

## Not verified on a real client

- Nothing was run on a real game client. The headless server self test covers logic only (home check, arrival, upgrades, events, reads, trade and tier files).
- The BEI, JEI and REI plugins, the Show Me What This Is plugin and the Jade plugin compile against their APIs but were not opened in a game. How the Settling, Hearth Upgrade and information pages look and lay out is not checked.
- The Hearth models, textures and the lit effect were not seen in a game.
- Lighting, putting out, feeding by hand, upgrading and hopper feeding were not played by hand.
- The ModJS binding, the KubeJS helper script and the Barely Questing task type are reflection bridges. The mod's tests cover the Minecraft-free parts; a live ModJS, KubeJS or Barely Questing install was not run.
- The Forge builds were not checked together with any viewer or look-at mod other than BEI and Show Me What This Is, and those were not run.
