---
title: The home check
summary: Every requirement a lit Hearth looks for, its default, how beds and workstations are counted, the countdown, arrival and lost homes.
---
A lit Hearth runs the home check every `scan_interval` ticks (default 200). The check never changes anything by itself. It decides whether the countdown moves.

## Requirements

The Hearth checks them in this order and reports the first one that fails.

| Order | Requirement | Default | Notes |
|---|---|---|---|
| 1 | Settlers below the limit | 2 / 4 / 8 by tier | Counts the settlers this Hearth brought that are still alive and at home. |
| 2 | A free bed | 1 | See below. |
| 3 | A free workstation | 1 | See below. |
| 4 | Light at a free bed | 8 (`light_min`) | At least one free bed must have this light level at its head block. |
| 5 | Food | 20 nutrition (`food_required`) | The sum of the nutrition of every food item in the Hearth. A recipe can name its own amount. |
| 6 | Standing room | on (`check_standing_room`) | A settler must be able to walk from the arrival point to a lit bed. |

Standing room is not a separate line in the look-at output. A bed that nobody can walk to counts as no usable bed, so it shows as `Beds 0 of 1`.

## Reach

| Tier | Horizontal radius | Vertical |
|---|---|---|
| Stone | 8 | 6 up and 6 down |
| Iron | 12 | 6 up and 6 down |
| Golden | 16 | 6 up and 6 down |

The horizontal reach is a circle around the Hearth. Only loaded chunks are looked at. Rooms do not need walls or a roof.

## What counts as a free bed

- The head half of a bed inside the reach, with its foot half present.
- Nobody sleeping in it.
- No villager has it as their home (the Hearth checks the villagers around it).
- An open block above both halves (no collision, no fluid).

## What counts as a free workstation

- A block that has a [[settling]] recipe whose `min_tier` is at or below the Hearth's tier.
- No villager has it as their job site.
- The recipe's profession exists in the game.

When there are several, the one nearest the Hearth wins. Nothing is random. Its profession is the profession of the next settler.

## Light

Light is read at the head block of each free bed. At least one free bed needs `light_min` or more.

## Food

Every item with food nutrition counts for its nutrition times its stack size. If the winning recipe lists extra items and the Hearth does not hold them, the food counts as short by at least one, so the check fails on food.

## The countdown

While the Hearth is lit and every requirement holds, the countdown moves up by `scan_interval` at each scan. When it reaches `settle_ticks` (6000 / 4000 / 2400 by tier) the settler arrives and the countdown starts over.

If a requirement stops holding, the countdown stands still. It does not reset. It is saved with the Hearth. An unlit Hearth does not count at all.

## Arrival

1. The arrival point is the nearest block to the Hearth with a solid floor, two open blocks above it and no fluid.
2. Script listeners for `before_settle` run. One of them can cancel the arrival.
3. A vanilla villager is created at the arrival point with the profession of the workstation. Its trade level is 1, or 2 for a Golden Hearth.
4. The bed becomes its home and the workstation its job site, so it works and sleeps where you built.
5. The Hearth spends food worth the recipe's nutrition, plus the recipe's extra items.
6. A trail of green sparkles runs from the Hearth to the arrival point, and the villager celebrates.
7. Script listeners for `settler_arrived` run.

Trade files ([[trades]]) are applied to the settler's offers at arrival and again each time it reaches a new trade level.

## Lost homes

A settler stays a normal villager for the rest of its life. If its bed or its workstation is broken, vanilla clears that memory and the villager looks for a new one by vanilla rules. The Hearth stops counting a settler that has died, or that has lost its bed or its workstation, so a replacement can come when the population limit allows it.

## Nothing happens when

- The Hearth is unlit (unless `allow_unlit_hearth_scan` is on, which only updates the look-at lines).
- The Hearth is broken.
- The Hearth's chunk is not loaded.
