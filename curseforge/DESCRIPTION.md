**A skyblock home for villagers: build a bed and a workstation around a lit Hearth, feed it, and a settler walks in. For Forge and NeoForge.**

In a void or skyblock world there is no village, so there are no villagers and no trading. Thorough Homesteading adds a deliberate way to get them. Place a Hearth, light it, build a home around it, and put food in. When everything is right, a countdown runs, and at its end a vanilla villager walks onto your island. No villager ever appears from nothing: you build the conditions.

# Features

- **Three Hearths:** Stone, Iron and Golden. Radius 8, 12 and 16 blocks, room for 2, 4 and 8 settlers, and arrival after 6000, 4000 and 2400 ticks by default. Golden settlers arrive with trade level 2 already unlocked.
- **Upgrades:** an Iron or Golden Hearth Upgrade raises a Hearth one tier. Contents, owner, settlers and the lit state stay.
- **The home check:** a free bed, a free workstation, enough light at the bed, food, and room for another settler. When one is missing the countdown waits and the look-at lines show which.
- **Profession from the workstation:** a composter makes a farmer, a smithing table a toolsmith. The nearest free workstation wins, and nothing is random. Workstations of other mods work as soon as a settling recipe names them.
- **A plain vanilla villager:** the settler claims the nearest free bed and the workstation, and follows vanilla rules from then on. Breeding, curing and trading are untouched, so every other mod that works with villagers keeps working.
- **No screen:** food goes in by hand or hopper, and a comparator reads how full the Hearth is.
- **Ownership:** the player who places a Hearth owns it. On a server with Barely Questing, the owner's team shares it.
- **Data driven:** settling recipes, trades and Hearth tiers are datapack JSON. Add pack-specific trades per profession and level, or replace them.
- **Scripting:** ModJS scripts get a `Homesteading` global and a `HomesteadingEvents` global. KubeJS gets the same through a helper script, and every recipe is plain JSON.
- **Quests:** Barely Questing gets a `settler` task type.

# Settings

Scan interval, countdown, radius and population per tier, light level, food, and the standing room check are in `config/thoroughhomesteading-common.toml`. A datapack can change a tier's radius, population and countdown too.

# Compatibility

- Recipe viewers show the Settling category (workstation in, profession out, with food and tier), the Hearth Upgrade category and an information page for every Hearth and workstation: BEI on Forge and NeoForge, JEI and REI on NeoForge.
- Show Me What This Is shows settlers, beds, workstations and food on Forge and NeoForge. Jade does the same on NeoForge.
- ModJS scripts on Forge and NeoForge. KubeJS can add, change and remove every recipe, trade and tier through datapacks.
- Barely Questing: a `settler` task type and shared team ownership.
- Works in any world. The Void Island world type from Prima Materia is the intended home.
- EMI has no build for these Minecraft versions yet.

# FAQ

**Does it need to be on the server?** Yes, on the server and on every client.

**Does it need another mod?** No. Recipe viewers, info overlays, script mods and Barely Questing are optional.

**Can I change the numbers?** Yes, in the config file or with a datapack.

**Does the villager stay?** A settler is a normal villager. If its bed or workstation is gone it stays a normal villager and looks for a new one by vanilla rules, and the Hearth may bring a replacement.

**Can I add my own professions or trades?** Yes. A settling recipe names a workstation and a profession, and trade files add or replace trades per level.

**Fabric?** Not at this time.

**Can I use it in my modpack?** Yes. It is MIT licensed.

Made by ItMeCasey Studios. Source and issues on [GitHub](https://github.com/ItMeCasey/ThoroughHomesteading).
