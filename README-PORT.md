# Plants & Zombies: NeoForge 1.21.1 port

Unofficial backport of [joshxviii/plants-and-zombies](https://github.com/joshxviii/plants-and-zombies) (Fabric, MC 26.1.2,
upstream commit `e92d5937`, mod version 1.5) to **NeoForge 21.1 / Minecraft 1.21.1**. Licensed MPL-2.0 like upstream.

## Install
Put `build/libs/plants-and-zombies-neoforge-1.5-neoforge.1.jar` in `mods/` together with:
- Kotlin for Forge 5.11.0 or newer (NeoForge build)
- TerraBlender for NeoForge 1.21.1 (4.1.0.x)

## Build / test
```
./gradlew build                 # jar in build/libs
./gradlew runClient             # dev client
./gradlew runServer             # dev server (run/ is the dev run dir)
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
- Players can't right-click equip the bucket as a helmet (mobs still wear it). Helmets/cone/ducky tube draw their item model on the head, as upstream does.
- Seed packet cooldowns are tracked per plant by the mod (1.21.1 cooldowns are per item) and shown with a hotbar overlay.
- Mailbox dye recipes no longer keep the mailbox's name/contents on the result.
- Raid music shows "Now playing" in the action bar (no now-playing toast in 1.21.1); no custom scroll-bar cursor.
- Graveyard: no pale moss / leaf litter / firefly bushes; dead trees use stripped birch instead of pale oak; extra sky/fog/cloud colours are dropped. Undead not burning in daylight there is kept (small mixin).
- A few 26.1-only sounds are substituted with close vanilla ones; a handful of block-model rotations are rounded to 22.5° steps.
- Iris shader-pipeline hooks are dropped. Zombie armor follows the standard humanoid parts only.

Upstream asset gaps that also exist on 26.1 (not port bugs): missing `angry_ambient` sound files for the newspaper zombie and
some missing subtitle translations.
