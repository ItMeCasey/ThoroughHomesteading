# Thorough Homesteading

A skyblock home for villagers. Build a bed and a workstation around a lit Hearth, feed it, and a vanilla villager settles in with the profession of the workstation. For Minecraft on Forge and NeoForge (26.1.2, 26.2, 26.3), on both the client and the server.

Docs: https://itmecasey.com/wiki/thoroughhomesteading/ (not deployed yet). Listing text: [`curseforge/DESCRIPTION.md`](curseforge/DESCRIPTION.md). Design: [`docs/DESIGN.md`](docs/DESIGN.md).

## What it adds

| Block or item | What it does |
|---|---|
| Stone, Iron and Golden Hearth | A lit Hearth runs the home check: free beds, free workstations, light at the bed, food, and room for another settler. When it holds, a countdown runs, and at its end a settler walks in. Radius 8, 12 and 16; 2, 4 and 8 settlers; arrival after 6000, 4000 and 2400 ticks. Golden settlers start at trade level 2. No screen: food goes in by hand or hopper, a comparator reads how full it is |
| Iron and Golden Hearth Upgrade | Used on a Hearth, one tier at a time. Contents, owner, settlers and the lit state stay |
| `thoroughhomesteading:settling` recipe | One per workstation: the workstation block, the profession, the lowest Hearth tier, the food (nutrition) and optional extra items. 13 ship, one per vanilla workstation. Datapacks, ModJS and KubeJS add, change and remove them |
| Trade files and tier files | Datapack JSON to add or replace a profession's trades per level, and to change a tier's radius, population and countdown |

The settler is a plain vanilla villager: it takes the nearest free workstation, claims the nearest free bed, and follows vanilla rules from then on. A settler whose bed or workstation is gone stays a normal villager and stops counting toward the Hearth's population.

Ownership: the player who places a Hearth owns it; the owner (and their Barely Questing team when that mod is installed) feeds, upgrades and breaks it. Hoppers always insert.

Scripting: with ModJS, server scripts get a `Homesteading` global and a `HomesteadingEvents` global; see [docs/scripting.md](docs/scripting.md). KubeJS: recipes, trades and tiers are datapack JSON, plus a helper script that exposes the same globals. Barely Questing: a `settler` task type.

Optional integrations: BEI, JEI and REI categories (Settling, Hearth Upgrade, Hearth information), Show Me What This Is and Jade look-at lines. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for what exists per version.

## Build

One module per loader and Minecraft version (`neoforge-26.2`, `forge-26.2`, and so on), sharing `common/` (Minecraft-free logic and unit tests) and the master code in `version-26.2`. Never edit the copies in `version-26.1.2` and `version-26.3`; run `python scripts/sync_versions.py` after changing `version-26.2` (loader code lives in the 26.1.2 layers and is copied the same way).

Textures are generated from vanilla textures by `python scripts/make_textures.py`; other resources by `python scripts/make_assets.py`.

```
cd neoforge-26.2
./gradlew build
./gradlew test
```

Development tools (the self test) build only with `-PdevTools`, so release jars never contain them. `python scripts/server_check.py <layer>` runs the self test on a real dedicated server.

## License

MIT, see [LICENSE](LICENSE).
