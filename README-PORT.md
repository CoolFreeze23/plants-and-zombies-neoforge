# Plants & Zombies: NeoForge 1.21.1 port

Unofficial backport of [joshxviii/plants-and-zombies](https://github.com/joshxviii/plants-and-zombies) (Fabric, MC 26.1.2,
upstream commit `e92d5937`, mod version 1.5) to **NeoForge 21.1 / Minecraft 1.21.1**. Licensed MPL-2.0 like upstream.

## Install
Put `build/libs/plants-and-zombies-neoforge-1.5-neoforge.1.jar` in `mods/` together with:
- Kotlin for Forge 5.11.0 or newer (NeoForge build)
- TerraBlender for NeoForge 1.21.1 (4.1.0.x)

Optional, detected automatically:
- **Iris** (+ Sodium): with a shader pack active, the paint overlay renders through Iris' entity program, like the
  26.1 original's Iris hook. Tested with Iris 1.8.12 + Sodium 0.6.13 and Complementary Reimagined (`docs/paint-iris-complementary.png`).
- **Vanilla Backport** (+ Platform): brings back the 26.1 content that needs newer vanilla blocks and sounds. A built-in
  data pack (`resourcepacks/vanillabackport_compat`, auto-enabled) adds pale moss, leaf litter and firefly bushes to the
  graveyard, pale oak dead trees with pale moss and leaf litter, and gravestones on pale moss. Tags pick up pale moss
  carpet and the pale garden. The tangle kelp's creaking sound and the plant pot's harness sound return. Tested with
  Vanilla Backport 1.1.7.10 + Platform 1.3.3.

## Build / test
```
./gradlew build                 # jar in build/libs
./gradlew runClient             # dev client
./gradlew runServer             # dev server (run/ is the dev run dir)
./gradlew runClient -PwithIris -PwithVanillaBackport   # dev runs with the optional mods
```
`PORTING.md` explains how the port is structured (compat layer for the 26.1 APIs, mapping table).

## What was verified
- Full compile (Kotlin + Java), mod loading, registries, datapacks (recipes, loot, advancements, worldgen, tags) on a dedicated server.
- All 69 entity types summoned and ticked with no errors; plants vs zombies with AI enabled fought for two minutes with no errors.
- Dev client: a world with every block, every mob and every item in item frames renders without render errors (`docs/smoke-test-client.png`).

Not exercised yet: zombie raids from start to finish, the mailbox/time machine GUIs in use, seed packet planting by a player,
multiplayer.

## Differences from the 26.1 original (1.21.1 has no equivalent)
- No spears, so the spear arm poses and goals are gone; gnomes use stone tools instead of copper ones.
- Helmets/cone/ducky tube/bucket draw their item model on the head, as upstream does. The empty bucket goes in the helmet slot (drag or shift-click) and mobs wear it, like 26.1.
- Seed packet cooldowns are tracked per plant by the mod (1.21.1 cooldowns are per item) and shown with a hotbar overlay.
- Mailbox dye recipes no longer keep the mailbox's name/contents on the result.
- Raid music shows "Now playing" in the action bar (no now-playing toast in 1.21.1); no custom scroll-bar cursor.
- Graveyard without Vanilla Backport: no pale moss / leaf litter / firefly bushes, and dead trees use stripped birch instead of pale oak. Extra sky/fog/cloud colours are dropped either way. Undead not burning in daylight there is kept (small mixin).
- A few 26.1-only sounds are substituted with close vanilla ones; a handful of block-model rotations are rounded to 22.5° steps.
- Zombie armor follows the standard humanoid parts only.
- Copper-age content (copper tools/nuggets/golem, spears) isn't in Vanilla Backport, so those substitutions stay.

Fixed upstream bugs: paint colours were saved with an int-keyed map that NBT can't store, so paint vanished when a
chunk reloaded (the effect flags are now rebuilt from active effects on load). The flag loot table checked the attacker
in a block context, so zombies breaking a flag never dropped the Brainz flag. `plantable` relied on 26.1-only block
tags (pale moss is now listed directly; 1.21.1's `#dirt` covers the rest).

Upstream asset gaps that also exist on 26.1 (not port bugs): missing `angry_ambient` sound files for the newspaper zombie and
some missing subtitle translations.
