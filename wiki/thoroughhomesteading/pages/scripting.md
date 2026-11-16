---
title: Scripting
summary: The Homesteading and HomesteadingEvents globals for ModJS and KubeJS, every event key, the report map and short examples.
---
Thorough Homesteading gives scripts two globals: `Homesteading` (reads, one action, settings) and `HomesteadingEvents` (listeners). ModJS is the primary target. KubeJS gets the same two objects through a helper script. Settling recipes, trades and tier files are plain datapack JSON that scripts and datapacks can add, change and remove.

## How the globals reach a script

| Script host | How |
|---|---|
| ModJS | The mod registers a binding with ModJS's `ExtraBindings` by reflection. Both globals are put into every script scope that is not a client script, because Hearths live on the server. |
| KubeJS | The mod writes `kubejs/startup_scripts/thoroughhomesteading.js` once. It sets `global.Homesteading` and `global.HomesteadingEvents`. An existing file is never overwritten. |
| Neither | Nothing loads and nothing fails. |

A listener that throws is logged on one line and skipped, so a script cannot break the server tick.

## Homesteading

| Call | Returns | What it does |
|---|---|---|
| `tiers()` | list | `stone`, `iron`, `golden`. |
| `events()` | list | The internal event names. |
| `recipes()` | list of maps | Every settling recipe the server has: `workstation`, `profession`, `minTier`, `food`, `extra` (item id to count). Empty before a server runs. |
| `report(x, y, z)` | map or null | The report of the Hearth at that position. Null when there is none. |
| `report(dimension, x, y, z)` | map or null | The same in one dimension, such as `minecraft:overworld`. |
| `settlersOf(x, y, z)` | list | UUIDs (text) of the settlers the Hearth counts. |
| `settleNow(x, y, z, profession)` | boolean | Skips the countdown and brings a settler if the Hearth is lit and the home check passes. An empty profession takes the nearest workstation. |
| `settingKeys()` | list | Every setting key, sorted. |
| `setting(key)` | text | The current value. Unknown keys throw. |
| `setSetting(key, value)` | nothing | Changes a value until the game closes. The config file is not touched. Unknown keys throw. |

### The report map

| Key | Meaning |
|---|---|
| `tier` | `stone`, `iron` or `golden`. |
| `lit` | Whether the Hearth is lit. |
| `owner` | Owner UUID, or empty. |
| `settlers` | Settlers counted. |
| `population` | Most settlers for this tier. |
| `beds` | Free beds found. |
| `litBeds` | Free beds with enough light. |
| `light` | Best light level at a free bed. |
| `lightMin` | The `light_min` setting. |
| `workstations` | Free workstations usable by this tier. |
| `food` | Nutrition in the Hearth. |
| `foodRequired` | Nutrition the next recipe needs. |
| `progress` | Ticks of the countdown done. |
| `settleTicks` | Ticks the countdown needs. |
| `blocked` | `population`, `beds`, `workstations`, `light`, `food`, or empty. |
| `profession` | Profession of the next settler, or empty. |
| `settlerIds` | The UUIDs `settlersOf` returns. |

## HomesteadingEvents

| Method | Fires when |
|---|---|
| `hearthLit(fn)` | A Hearth is lit. |
| `hearthExtinguished(fn)` | A Hearth is put out. |
| `settlerArrived(fn)` | A settler has been placed. |
| `settlingBlocked(fn)` | A scan of a lit Hearth finds a blocking reason that differs from the last one. |
| `beforeSettle(fn)` | Just before a settler is created. `event.cancel()` stops it. |

An event is read with `event.string(key)`, `event.number(key)` and `event.get(key)`. `event.keys()` lists the keys, `event.type()` gives the internal name, and `event.cancel()` and `event.cancelled()` work on `beforeSettle`.

| Event | Keys |
|---|---|
| `hearthLit`, `hearthExtinguished` | `x`, `y`, `z`, `dimension`, `tier` |
| `settlingBlocked` | `x`, `y`, `z`, `dimension`, `tier`, `reason` |
| `beforeSettle` | `x`, `y`, `z`, `dimension`, `tier`, `profession` |
| `settlerArrived` | `settler`, `x`, `y`, `z`, `dimension`, `tier`, `profession`, `owner` |

`x`, `y`, `z` are the Hearth's position. `settler` is the new villager's UUID. `reason` is one of `beds`, `workstations`, `light`, `food`, `population`.

## Examples

The examples log with `console.log`. Use whatever output your script host offers.

Welcome message:

```js
HomesteadingEvents.settlerArrived(event => {
  console.log('A ' + event.string('profession') + ' moved in')
})
```

Cap one profession (the counter lives in the script and resets when scripts reload):

```js
let librarians = 0
HomesteadingEvents.settlerArrived(e => {
  if (e.string('profession') === 'minecraft:librarian') librarians++
})
HomesteadingEvents.beforeSettle(e => {
  if (e.string('profession') === 'minecraft:librarian' && librarians >= 2) e.cancel()
})
```

Log why a home is blocked:

```js
HomesteadingEvents.settlingBlocked(e => {
  console.log('Hearth at ' + e.number('x') + ' ' + e.number('y') + ' ' + e.number('z') + ' waits for ' + e.string('reason'))
})
```

Bring a librarian on demand:

```js
HomesteadingEvents.hearthLit(e => {
  if (e.string('tier') === 'golden') {
    Homesteading.settleNow(e.number('x'), e.number('y'), e.number('z'), 'minecraft:librarian')
  }
})
```

Read a Hearth:

```js
const r = Homesteading.report(10, 64, 10)
if (r) console.log(r.settlers + ' of ' + r.population + ', blocked by ' + (r.blocked || 'nothing'))
```

Change a setting:

```js
Homesteading.setSetting('settle_ticks.stone', '1200')
```

In KubeJS the same objects are on `global`:

```js
global.HomesteadingEvents.settlerArrived(e => console.log('Settled: ' + e.string('profession')))
```

## Datapack JSON

| What | Where |
|---|---|
| Settling recipes | Recipe type `thoroughhomesteading:settling`, see [[settling]]. |
| Hearth crafting and upgrade recipes | Normal shaped recipes `stone_hearth`, `iron_hearth_upgrade`, `golden_hearth_upgrade`. |
| Trades and tiers | See [[trades]]. |

## Barely Questing

With Barely Questing installed, quests can use the task type `settler`, for example `{"type": "settler", "count": 3, "profession": "minecraft:farmer"}`. The profession is optional. See [[compatibility]].
