---
title: Versions
summary: The six builds, their loaders, the Java version, known differences and what has not been verified yet.
---
Version `2026.11.1`. Each build is one jar for one loader and one Minecraft version. All of them need Java 25, the version Minecraft 26.x ships with.

## Builds

| Minecraft | NeoForge | Forge |
|---|---|---|
| 26.1.2 | Yes | Yes |
| 26.2 | Yes | Yes |
| 26.3 | Yes | Yes |

The mod is needed on both the server and the clients.

## Differences between builds

| Area | NeoForge | Forge |
|---|---|---|
| Settling recipes sent to clients | Yes, viewers show the live recipes | No, viewers show the 13 shipped defaults |
| JEI and REI plugins | Yes | No JEI or REI build exists for these versions |
| Jade plugin | Yes | No Jade build exists for these versions |
| BEI plugin | Yes | Yes |
| Show Me What This Is plugin | Yes | Yes |
| Config file | `config/thoroughhomesteading-common.toml` | `config/thoroughhomesteading-common.toml` |

The 26.1.2, 26.2 and 26.3 builds behave the same for players. Their code differs only where Minecraft's own code changed between those versions. EMI has no build for 26.1.2, 26.2 or 26.3, so there is no EMI plugin.

## Not verified yet

- Nothing was run on a real game client. The headless server self test covers logic only.
- The BEI, JEI, REI, Show Me What This Is and Jade plugins compile but were not opened in a game.
- The Hearth models, textures and lit effect were not seen in a game.
- Lighting, feeding by hand, hopper feeding, upgrading and the comparator were not played by hand.
- ModJS, KubeJS and Barely Questing bridges were not run against live installs.
- Forge builds were not run with any recipe viewer or look-at mod.
