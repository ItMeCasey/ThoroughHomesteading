---
title: Settling recipes
summary: The recipe type that says which workstation makes which profession, with the 13 recipes that ship.
infobox:
  Recipe type: thoroughhomesteading:settling
  Shipped: 13
---
A settling recipe connects one workstation block to one villager profession. A workstation with no settling recipe is ignored by the Hearth. Settling recipes are normal datapack recipes, so datapacks, ModJS and KubeJS can add, change and remove them.

## Format

```json
{
  "type": "thoroughhomesteading:settling",
  "workstation": "minecraft:composter",
  "profession": "minecraft:farmer",
  "min_tier": 1,
  "food": 20,
  "extra": [ { "item": "minecraft:emerald", "count": 1 } ]
}
```

| Field | Required | Meaning |
|---|---|---|
| `workstation` | yes | The block id of the workstation. |
| `profession` | yes | The villager profession id. It must exist in the game. |
| `min_tier` | no, default 1 | The lowest Hearth tier that may use the recipe: 1 Stone, 2 Iron, 3 Golden. |
| `food` | no, default 0 | Nutrition of food the arrival spends. 0 or missing means the `food_required` setting. |
| `extra` | no | Extra items spent with the food. Each entry has an `item` and a `count` (default 1). |

The Hearth accepts the extra items as input, the same way it accepts food.

If two recipes name the same workstation, the one with the lower recipe id (sorted as text) wins.

## Examples

A mod's workstation and profession:

```json
{
  "type": "thoroughhomesteading:settling",
  "workstation": "examplemod:alchemy_bench",
  "profession": "examplemod:alchemist"
}
```

A workstation only a Golden Hearth can use, with a bigger meal:

```json
{
  "type": "thoroughhomesteading:settling",
  "workstation": "minecraft:lectern",
  "profession": "minecraft:librarian",
  "min_tier": 3,
  "food": 40
}
```

Replace a shipped recipe by adding a file with the same id, `data/thoroughhomesteading/recipe/settle_composter.json`, in your datapack.

## The 13 shipped recipes

All of them use tier 1 and the default food amount.

| Workstation | Profession | Recipe file |
|---|---|---|
| `minecraft:composter` | `minecraft:farmer` | `settle_composter` |
| `minecraft:barrel` | `minecraft:fisherman` | `settle_barrel` |
| `minecraft:blast_furnace` | `minecraft:armorer` | `settle_blast_furnace` |
| `minecraft:brewing_stand` | `minecraft:cleric` | `settle_brewing_stand` |
| `minecraft:cartography_table` | `minecraft:cartographer` | `settle_cartography_table` |
| `minecraft:fletching_table` | `minecraft:fletcher` | `settle_fletching_table` |
| `minecraft:grindstone` | `minecraft:weaponsmith` | `settle_grindstone` |
| `minecraft:lectern` | `minecraft:librarian` | `settle_lectern` |
| `minecraft:loom` | `minecraft:shepherd` | `settle_loom` |
| `minecraft:smithing_table` | `minecraft:toolsmith` | `settle_smithing_table` |
| `minecraft:smoker` | `minecraft:butcher` | `settle_smoker` |
| `minecraft:stonecutter` | `minecraft:mason` | `settle_stonecutter` |
| `minecraft:cauldron` | `minecraft:leatherworker` | `settle_cauldron` |

These are the vanilla pairs. Settling recipes show in BEI, JEI and REI under the Settling category; see [[compatibility]].
