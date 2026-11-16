# Scripting reference

Thorough Homesteading gives scripts two globals, `Homesteading` (reads, one action, settings) and `HomesteadingEvents` (event listeners). ModJS is the primary target. KubeJS gets the same two objects through a small helper script. Recipes, trades and Hearth tiers are plain datapack JSON, so scripts and datapacks can add, change and remove them without any API.

Typed declarations for both globals are in [thoroughhomesteading.d.ts](thoroughhomesteading.d.ts).

## How the globals reach a script

| Script host | How the globals appear |
|---|---|
| ModJS | The mod registers a binding with ModJS's `ExtraBindings` (by reflection, see `ScriptBridge`). `Homesteading` and `HomesteadingEvents` are put into every script scope that is not a client script. Hearths live on the server, so client scripts do not get them. |
| KubeJS | When KubeJS is installed, the mod writes `kubejs/startup_scripts/thoroughhomesteading.js` once (an existing file is never overwritten). It sets `global.Homesteading` and `global.HomesteadingEvents` (see `KubeJsBridge`). |
| Neither | Nothing is loaded and nothing fails. |

The helper script KubeJS receives is:

```js
global.Homesteading = Java.loadClass('com.itmecasey.thoroughhomesteading.script.Bridges').API
global.HomesteadingEvents = Java.loadClass('com.itmecasey.thoroughhomesteading.HomesteadingEvents').INSTANCE
```

Everything the API takes or returns is a string, number, boolean, list or map. A listener that throws is logged on one line and skipped, so a script cannot break the server tick.

## Homesteading

| Call | Returns | What it does |
|---|---|---|
| `Homesteading.tiers()` | `string[]` | `["stone", "iron", "golden"]`. |
| `Homesteading.events()` | `string[]` | The internal event names: `hearth_lit`, `hearth_extinguished`, `settler_arrived`, `settling_blocked`, `before_settle`. |
| `Homesteading.recipes()` | list of maps | Every settling recipe the running server has. Each map has `workstation`, `profession`, `minTier`, `food` (nutrition, with the `food_required` setting filled in when the recipe names none) and `extra` (a map of item id to count). Empty before a server runs. |
| `Homesteading.report(x, y, z)` | map or `null` | The report of the Hearth at that position (see below). Looks in the overworld first, then every dimension. `null` when there is no Hearth. |
| `Homesteading.report(dimension, x, y, z)` | map or `null` | The same for one dimension, such as `"minecraft:overworld"`. |
| `Homesteading.settlersOf(x, y, z)` | `string[]` | UUIDs (as text) of the settlers the Hearth has brought and still counts. Empty when there is no Hearth. |
| `Homesteading.settleNow(x, y, z, profession)` | `boolean` | Skips the countdown and brings a settler, if the Hearth is lit and the home check passes. `profession` (for example `"minecraft:farmer"`) limits which workstation is used; an empty string takes the nearest one. Returns `false` when nothing happened, including when a `beforeSettle` listener cancelled it. |
| `Homesteading.settingKeys()` | `string[]` | Every setting key, sorted. |
| `Homesteading.setting(key)` | `string` | The current value as text, for example `Homesteading.setting("radius.stone")`. Unknown keys throw. |
| `Homesteading.setSetting(key, value)` | nothing | Changes a setting until the game closes. The config file is not touched. Unknown keys throw. |

`settlersOf` and `settleNow` find the Hearth by position alone (overworld first, then every dimension).

A datapack Hearth file for a tier (see the wiki page Trades) wins over a config value for `radius.<tier>`, `population.<tier>` and `settle_ticks.<tier>`, so `setSetting` on those keys has no visible effect while such a file sets that value.

### The report map

`Homesteading.report` returns these keys, in this order.

| Key | Type | Meaning |
|---|---|---|
| `tier` | string | `stone`, `iron` or `golden`. |
| `lit` | boolean | Whether the Hearth is lit. |
| `owner` | string | UUID of the owner, or an empty string. |
| `settlers` | number | Settlers the Hearth counts as alive and at home. |
| `population` | number | Most settlers this tier keeps. |
| `beds` | number | Free beds found. |
| `litBeds` | number | Free beds whose head block has at least `light_min` light. |
| `light` | number | Best light level at a free bed's head. |
| `lightMin` | number | The `light_min` setting. |
| `workstations` | number | Free workstations that have a settling recipe this tier may use. |
| `food` | number | Nutrition of the food in the Hearth. |
| `foodRequired` | number | Nutrition the next recipe needs. |
| `progress` | number | Ticks of the countdown done. |
| `settleTicks` | number | Ticks the countdown needs for this tier. |
| `blocked` | string | The blocking reason (`population`, `beds`, `workstations`, `light`, `food`) or an empty string when the check passes. |
| `profession` | string | Profession of the workstation the next settler would take, or an empty string. |
| `settlerIds` | string[] | The same list `settlersOf` returns. |

## HomesteadingEvents

Each method takes one function. The function receives the event object.

| Method | Internal name | Fires when |
|---|---|---|
| `hearthLit(fn)` | `hearth_lit` | A Hearth is lit with flint and steel or a fire charge. |
| `hearthExtinguished(fn)` | `hearth_extinguished` | A Hearth is put out with a shovel. |
| `settlerArrived(fn)` | `settler_arrived` | A settler has been created and placed. |
| `settlingBlocked(fn)` | `settling_blocked` | A scan of a lit Hearth finds a blocking reason that differs from the last one. |
| `beforeSettle(fn)` | `before_settle` | Just before a settler is created. `event.cancel()` stops the arrival. |

### The event object

The event is a Java object. Read values with its methods:

| Method | Returns |
|---|---|
| `event.type()` | The internal event name. |
| `event.get(key)` | The raw value, or `null`. |
| `event.string(key)` | The value as text, or an empty string. |
| `event.number(key)` | The value as a number, or `0`. |
| `event.keys()` | The keys this event carries. |
| `event.cancel()` | Stops the arrival (only `before_settle` honors it). |
| `event.cancelled()` | Whether a listener cancelled it. |

### Keys per event

| Event | Keys |
|---|---|
| `hearth_lit` | `x`, `y`, `z`, `dimension`, `tier` |
| `hearth_extinguished` | `x`, `y`, `z`, `dimension`, `tier` |
| `settling_blocked` | `x`, `y`, `z`, `dimension`, `tier`, `reason` |
| `before_settle` | `x`, `y`, `z`, `dimension`, `tier`, `profession` |
| `settler_arrived` | `settler`, `x`, `y`, `z`, `dimension`, `tier`, `profession`, `owner` |

`x`, `y` and `z` are the position of the Hearth. `dimension` is an id such as `minecraft:overworld`. `tier` is `stone`, `iron` or `golden`. `reason` is `beds`, `workstations`, `light`, `food` or `population`. `profession` is an id such as `minecraft:farmer`. `settler` is the new villager's UUID as text. `owner` is the Hearth owner's UUID as text, or an empty string.

## Examples

The examples log with `console.log`, as the KubeJS helper script's own comment does. Use whatever output your script host offers.

### Welcome message

```js
HomesteadingEvents.settlerArrived(event => {
  console.log('A ' + event.string('profession') + ' moved in at '
    + event.number('x') + ' ' + event.number('y') + ' ' + event.number('z'))
})
```

### Cap one profession

Stops a third librarian. The counter lives in the script, so it starts again at zero when scripts reload.

```js
let librarians = 0

HomesteadingEvents.settlerArrived(event => {
  if (event.string('profession') === 'minecraft:librarian') librarians++
})

HomesteadingEvents.beforeSettle(event => {
  if (event.string('profession') === 'minecraft:librarian' && librarians >= 2) {
    event.cancel()
  }
})
```

### Log why a home is blocked

```js
HomesteadingEvents.settlingBlocked(event => {
  console.log('Hearth ' + event.number('x') + ' ' + event.number('y') + ' ' + event.number('z')
    + ' is waiting for: ' + event.string('reason'))
})
```

### Bring a librarian on demand

`settleNow` only works on a lit Hearth whose home check passes, so it returns `false` otherwise.

```js
HomesteadingEvents.hearthLit(event => {
  if (event.string('tier') === 'golden') {
    const ok = Homesteading.settleNow(event.number('x'), event.number('y'), event.number('z'), 'minecraft:librarian')
    console.log(ok ? 'Librarian arrived' : 'Home is not ready')
  }
})
```

### Read a Hearth

```js
const r = Homesteading.report(10, 64, 10)
if (r) {
  console.log(r.settlers + ' of ' + r.population + ' settlers, food ' + r.food + ' of ' + r.foodRequired
    + ', countdown ' + r.progress + ' of ' + r.settleTicks + ', blocked by ' + (r.blocked || 'nothing'))
}
```

### Change a setting

```js
console.log('Stone countdown was ' + Homesteading.setting('settle_ticks.stone'))
Homesteading.setSetting('settle_ticks.stone', '1200')
Homesteading.setSetting('light_min', '4')
```

### List the settling recipes

```js
Homesteading.recipes().forEach(r => {
  console.log(r.workstation + ' makes a ' + r.profession + ' (tier ' + r.minTier + ', food ' + r.food + ')')
})
```

### KubeJS

In KubeJS the same objects are on `global`, set by the helper startup script:

```js
global.HomesteadingEvents.settlerArrived(e => {
  console.log('Settled: ' + e.string('profession'))
})
```

## Datapack JSON that scripts can change

| What | Where | Notes |
|---|---|---|
| Settling recipes | recipe type `thoroughhomesteading:settling` | A normal recipe, so KubeJS and datapacks add, change and remove them. `Homesteading.recipes()` reads what is loaded. |
| Hearth crafting and upgrade recipes | `thoroughhomesteading:stone_hearth`, `iron_hearth_upgrade`, `golden_hearth_upgrade` | Normal shaped crafting recipes. |
| Trade files | `data/<namespace>/thoroughhomesteading/trades/<profession>[/<anything>].json` | Read again on `/reload`. |
| Hearth tier files | `data/<namespace>/thoroughhomesteading/hearth/<tier>.json` | Read again on `/reload`. |

## Barely Questing

With Barely Questing installed, the mod adds a quest task type `settler`: `{"type": "settler", "count": 3, "profession": "minecraft:farmer"}` (`profession` is optional). It counts settlers the player's Hearths have brought, kept in a ledger file in the world folder. The mod does not need Barely Questing to run.
