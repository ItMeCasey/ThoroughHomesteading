# Thorough Homesteading listing kit: CurseForge

Everything needed to fill in the CurseForge project form. The icon files are in `branding/icon/`. Banners and gallery images are not made yet.

## Project fields

| Field | CurseForge |
|---|---|
| Project ID | not created yet (Casey creates the project; put the id here and in the upload script) |
| Name | Thorough Homesteading |
| Slug / URL | `thoroughhomesteading` |
| Mod id | `thoroughhomesteading` |
| Icon / logo | `icon/icon_512.png` (square, at least 400x400) |
| Class | Mods |
| Main category | Utility & QoL |
| Loaders | Forge and NeoForge |
| Game versions | Minecraft 26.1.2, 26.2 and 26.3, one jar per loader and Minecraft version |
| Environment | Client and Server |
| License | MIT |
| Source | https://github.com/ItMeCasey/ThoroughHomesteading |
| Issues | https://github.com/ItMeCasey/ThoroughHomesteading/issues |
| Wiki | https://itmecasey.com/wiki/thoroughhomesteading/ (not deployed yet; add the link once it is live) |

### One-line description

> A skyblock home for villagers: build a bed and a workstation around a lit Hearth, feed it, and a vanilla villager settles in, for Forge and NeoForge.

Use it for the CurseForge summary (under 250 characters), social posts and the top of the description.

### Description

The CurseForge description is in [`curseforge/DESCRIPTION.md`](../curseforge/DESCRIPTION.md). Paste it with the description type set to **Markdown**. It describes the mod only.

## Checklist

1. Create the CurseForge project with `icon_512.png` as the logo, record the project id above and in the upload script, and add the `CURSEFORGE_TOKEN` repository secret.
2. Make the banner and gallery images (`banner`, `features`, `social`) from the Hearth textures and the in-game frames, push `branding/`, and add them to the description.
3. `icon/thoroughhomesteading_logo.png` is the mod list logo (a copy is in `common/src/main/resources`).
