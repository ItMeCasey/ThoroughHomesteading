# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-16
Summary: First release code is finished and passes unit tests and the headless server self test on all six builds; client-side parts (viewers, look-at overlays, models) are compile-checked only, and nothing is published yet.
Spend: none

## Foundation
- [x] Design approved, decisions answered (w:1)
- [x] Hearth blocks, tiers and upgrade recipes (w:3)
- [x] Home check and arrival, including vanilla job-site and bed memories (w:5)
- [~] `settling` recipe type, trade files and tier data (w:4)
- [x] Look-at lines and comparator output (w:1)

## Compatibility
- [~] BEI, JEI, REI and EMI categories and info pages (w:3)
- [x] ModJS global, events, reads, typed API, docs and tests (w:4)
- [~] KubeJS bridge and datapack coverage (w:2)
- [~] Barely Questing task type and bridge (w:2)

## Testing
- [~] Self-test and a play check on the loaders and versions the release targets (w:3)

## Documentation and listing
- [x] Wiki pages written: Compatibility, Scripting and Hearth (w:2)
- [x] CurseForge description (what it does, features, settings, compatibility, FAQ only) (w:1)
- [x] Banner and gallery images (w:1)

## Release
- [ ] GitHub repository, CI and release workflow (w:2)
- [ ] CurseForge project created and first release uploaded (w:2)
- [x] Wiki deployed live (w:1)

## Notes
- Verified headless: 29 unit tests per module; server self test `SELFTEST DONE failures=0` (149 checks) on Forge and NeoForge 26.1.2, 26.2 and 26.3, also with the ModJS jar installed (script global and events ran) and with Barely Questing installed (the `settler` task type registered).
- Not verified: any real game client (models, textures, viewer plugins, look-at overlays), trade and tier files read from a real datapack (the self test injects trade files), KubeJS, and a quest driven by the `settler` task. Step marks above follow this.
- No EMI build exists for 26.1.2, 26.2 or 26.3, and JEI, REI and Jade have no Forge build for these versions: not a gap.
