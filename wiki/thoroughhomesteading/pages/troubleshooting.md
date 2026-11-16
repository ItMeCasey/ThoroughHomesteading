---
title: Troubleshooting
summary: How to read the Hearth's look-at lines when a settler does not arrive, and what each blocking reason means.
---
When no settler arrives, look at the Hearth with Show Me What This Is or Jade. The lines are values only.

## The look-at lines

| Line | Meaning |
|---|---|
| `Settlers 2 of 4` | Settlers this Hearth counts, and the most its tier keeps. |
| `Beds 1` | Free beds found. |
| `Workstations 0` | Free workstations this tier may use. |
| `Food 12 of 20` | Nutrition in the Hearth, and what the next recipe needs. |
| `Beds 0 of 1` | The home needs one more free bed. |
| `Workstations 0 of 1` | The home needs one more free workstation. |
| `Light 5 of 8` | The best light at a free bed, and the light needed. |

The last three appear only when that is the thing blocking the settler. Settlers and food already show `x of y` in their own lines.

## Blocking reasons

The Hearth reports the first requirement that fails, in this order.

| Reason | What it means | Where the numbers come from |
|---|---|---|
| `population` | The Hearth already has as many settlers as its tier keeps. | `Settlers 4 of 4`. A settler that dies or loses its bed or workstation stops counting. |
| `beds` | No usable free bed. A bed is not free if a villager has claimed it, someone is sleeping in it, a half is missing or a half has no open block above it. A bed nobody can walk to from the arrival point also counts as none while `check_standing_room` is on. | `Beds 0 of 1`. |
| `workstations` | No free workstation with a settling recipe this tier may use. A job site a villager has claimed does not count. | `Workstations 0 of 1`. See [[settling]]. |
| `light` | No free bed has enough light at its head. | `Light 5 of 8`. See `light_min` in [[settings]]. |
| `food` | Not enough nutrition in the Hearth, or the recipe's extra items are missing. | `Food 12 of 20`. |

See [[home-check]] for the full rules.

## Other things to check

- The Hearth must be lit. An unlit Hearth does not count down.
- The beds and workstations must be inside the reach of the tier (8, 12 or 16 blocks across, 6 up and down). See [[hearth]].
- The countdown stands still while a requirement fails. It keeps its progress and continues when the home is complete again.
- Scripts can stop an arrival with `beforeSettle`. A script that does so shows no look-at reason. See [[scripting]].
- Only the owner and their Barely Questing team can feed a Hearth by hand. Hoppers always work.
