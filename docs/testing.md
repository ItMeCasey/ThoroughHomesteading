# Testing

What ran, and what did not. Nothing here has been seen on a real game client.

## Unit tests (`./gradlew test`, 29 per module)
Home check order and countdown, settings defaults and ranges, the shipped settling recipes against the defaults table, assets and language keys (look-at lines are names and values only), the settler ledger, script events (cancel, a throwing listener), and the optional-mod bridges with none of those mods present.

## Server self test (`python scripts/server_check.py <layer>`, 149 checks)
Starts a dedicated server with `-PdevTools`, builds homes on stacked platforms and prints `SELFTEST PASS/FAIL` lines ending in `SELFTEST DONE failures=0`. It covers content registration, the settling recipe and its codecs, every blocking reason, hopper input and the comparator, arrival (profession, bed and workstation memories, food spent), population, cancel, the countdown and blocked events, lost and dead settlers, upgrades through the block and the items, ownership, saving and loading, trade files (injected), the look-at values and the scripting API. Run on all six layers; also with the ModJS 2026.11.31 jar (a script used both globals and received events) and with Barely Questing 2026.11.5 (the `settler` task type registered).

## Not covered
Rendering and screens, the viewer and look-at plugins in a game, trade and tier files from a real datapack, KubeJS, a quest completed through the task type, a standing-room path through real terrain.
The light rule is checked by comparing the setting with the bed's light; the light engine runs in the background, so a freshly built test home has no settled light and the self test sets `light_min` to 0 for the other checks.
