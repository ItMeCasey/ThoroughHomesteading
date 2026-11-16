---
title: Trades and tiers
summary: Datapack files that add or replace a profession's trades, and that change the radius, settler count and countdown of a Hearth tier.
---
No trade tables ship with the mod. Settlers trade like vanilla villagers of their profession until a pack adds trade files. Both kinds of file below are read from datapacks and read again on `/reload`.

## Trade files

Path: `data/<namespace>/thoroughhomesteading/trades/<profession>[/<anything>].json`

The profession is `<namespace>:<first path segment>`. The folder or file name after the first segment is free, so a pack can split one profession over many files.

| File | Profession |
|---|---|
| `data/minecraft/thoroughhomesteading/trades/farmer.json` | `minecraft:farmer` |
| `data/minecraft/thoroughhomesteading/trades/farmer/skyblock.json` | `minecraft:farmer` |
| `data/mypack/thoroughhomesteading/trades/alchemist.json` | `mypack:alchemist` |

To change a vanilla profession, put the file in the `minecraft` namespace folder.

| Field | Default | Meaning |
|---|---|---|
| `mode` | `add` | `add` appends the trades to the level's offers. `replace` swaps the level's offers for these. |
| `level` | 1 | The trade level, 1 to 5. The trades apply when a settler gets that level. |
| `trades` | none | A list of offers in vanilla's offer format. A trade that does not parse is logged and skipped. |

```json
{
  "mode": "add",
  "level": 1,
  "trades": [
    {
      "buy": { "id": "minecraft:emerald", "count": 1 },
      "sell": { "id": "minecraft:wheat_seeds", "count": 8 },
      "maxUses": 12,
      "xp": 2,
      "priceMultiplier": 0.05
    }
  ]
}
```

Several `replace` files for the same profession and level are combined. Add files are always applied after.

When a settler arrives, the files for its starting level apply (level 1, or level 2 for a Golden Hearth). Files for higher levels apply each time the settler reaches that level.

## Hearth tier files

Path: `data/<namespace>/thoroughhomesteading/hearth/<tier>.json`, where `<tier>` is `stone`, `iron` or `golden`.

```json
{
  "radius": 10,
  "population": 3,
  "settle_ticks": 3000
}
```

| Field | Config key it overrides | Range |
|---|---|---|
| `radius` | `radius.<tier>` | 2 to 64 |
| `population` | `population.<tier>` | 1 to 64 |
| `settle_ticks` | `settle_ticks.<tier>` | 20 to 1728000 |

Each field is optional. A value outside its range is limited to the range. A tier file wins over the config file, so a pack can set the numbers without asking players to edit anything. See [[settings]].
