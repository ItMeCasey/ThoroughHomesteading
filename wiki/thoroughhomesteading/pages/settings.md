---
title: Settings
summary: Every config key with its default and range.
infobox:
  Config file: config/thoroughhomesteading-common.toml
---
The settings live in `config/thoroughhomesteading-common.toml` on both loaders. Scripts can change a value until the game closes with `Homesteading.setSetting` ([[scripting]]).

A datapack tier file can override the `radius`, `population` and `settle_ticks` keys of a tier ([[trades]]), and then wins over the config.

## Scan and countdown

| Key | Default | Range | Meaning |
|---|---|---|---|
| `scan_interval` | 200 | 20 to 24000 | Ticks between home checks of a lit Hearth. The countdown moves by this many ticks at each passing check. |
| `settle_ticks.stone` | 6000 | 20 to 1728000 | Ticks the countdown of a Stone Hearth runs while the home check passes. |
| `settle_ticks.iron` | 4000 | 20 to 1728000 | The same for an Iron Hearth. |
| `settle_ticks.golden` | 2400 | 20 to 1728000 | The same for a Golden Hearth. |

## Reach and population

| Key | Default | Range | Meaning |
|---|---|---|---|
| `radius.stone` | 8 | 2 to 64 | Horizontal radius in blocks a Stone Hearth searches for beds and workstations (6 blocks up and down). |
| `radius.iron` | 12 | 2 to 64 | The same for an Iron Hearth. |
| `radius.golden` | 16 | 2 to 64 | The same for a Golden Hearth. |
| `population.stone` | 2 | 1 to 64 | Most living settlers a Stone Hearth keeps. |
| `population.iron` | 4 | 1 to 64 | The same for an Iron Hearth. |
| `population.golden` | 8 | 1 to 64 | The same for a Golden Hearth. |

## Requirements

| Key | Default | Range | Meaning |
|---|---|---|---|
| `light_min` | 8 | 0 to 15 | Light level needed at the head of a bed. |
| `food_required` | 20 | 1 to 10000 | Nutrition of food a settling recipe uses when it names no amount. |
| `check_standing_room` | true | true or false | The Hearth checks that a settler can walk from the arrival point to the bed. |
| `allow_unlit_hearth_scan` | false | true or false | An unlit Hearth runs the home check so the look-at lines update. The countdown only runs while lit. |
