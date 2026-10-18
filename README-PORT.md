# Plants & Zombies: NeoForge 1.21.1 port

Unofficial backport of [joshxviii/plants-and-zombies](https://github.com/joshxviii/plants-and-zombies) (Fabric, MC 26.1.2,
upstream commit `e92d5937`, mod version 1.5) to **NeoForge 21.1 / Minecraft 1.21.1**. Licensed MPL-2.0 like upstream.
`PORTING.md` explains how the port is structured.

## Install
Put `build/libs/plants-and-zombies-neoforge-1.5-neoforge.1.jar` in `mods/` together with:
- Kotlin for Forge 5.11.0 or newer (NeoForge build)
- TerraBlender for NeoForge 1.21.1 (4.1.0.x)

Optional, detected automatically:
- **Iris** (+ Sodium): with a shader pack active, the paint overlay renders through Iris' entity program, like the
  26.1 original's Iris hook. Tested with Iris 1.8.12 + Sodium 0.6.13 and Complementary Reimagined.
- **Vanilla Backport** (+ Platform): brings back the 26.1 content that needs newer vanilla blocks and sounds. A built-in
  data pack (auto-enabled) adds pale moss, leaf litter and firefly bushes to the graveyard, pale oak dead trees with
  pale moss and leaf litter, gravestones on pale moss, and leaf-litter tint; tags pick up pale moss carpet and the pale
  garden; the tangle kelp's creaking sound and the plant pot's harness sound return. Tested with Vanilla Backport 1.1.7.10 + Platform 1.3.3.
- **Copper Age Backport**: brings back the 26.1 copper content. Gnomes spawn with copper axes and pickaxes, gravestone
  treasure has copper nuggets again, the relocation advancement shows a copper shovel (built-in data pack,
  auto-enabled), and mailboxes, the collection box and garden gnome posing use the copper chest and copper golem
  statue sounds. Copper golems are immune to electric plants. Tested with Copper Age Backport 0.1.5 (0.1.4 crashes on startup on current NeoForge 21.1, a bug fixed in 0.1.5); works alongside Vanilla Backport.
- **Backported Spears**: brings back the 26.1 spears. Gnomes spawn with spears again (a copper spear with Copper Age
  Backport installed too, since Backported Spears only adds copper spears then; otherwise a stone spear), gnomes and
  plant zombies charge with spears again (the port's copy of 26.1's `SpearUseGoal`), and they hold spears in 26.1's
  spear arm pose. Tested with Backported Spears 1.8.0. Its own spear goal for vanilla zombies stops after the first
  charge (the zombie then stands still while it has a target); the port replaces it on plant zombies, but vanilla
  zombies with spears keep the mod's behaviour.

## Build / test
```
./gradlew build                 # jar in build/libs
./gradlew runGameTestServer     # all GameTests (dev only, not packaged)
./gradlew runClient             # dev client;  runServer / runClientMp for multiplayer
./gradlew runClient -PwithIris -PwithVanillaBackport -PwithCopperAgeBackport -PwithSpears   # dev runs with the optional mods
```

## What was verified
- Every upstream source and resource file was compared against the port; differences are restored or listed below.
- **All 132 required GameTests pass** (repeated runs): every plant's behaviour, all special zombies and blueprint machines, ownership, a full zombie
  raid (waves, victory, rewards), mailboxes (delivery, queue, full/discard), collection box, sun battery, time machine,
  flags, gravestones, garden gnomes, pots, watering can, gardening glove, seed packets and cooldowns, wearables
  (incl. the bucket), dispensers, blueprints, every upstream recipe, loot tables, advancements, tags, worldgen
  features, seed mutations and villager trades.
- Dedicated server + client over the network: config sync, a zombie raid with its HUD, boss bar and head markers.
- Dev client: every entity and block entity renderer, item icons, screens (mailbox, collection box, time machine),
  raid overlay, particles, graveyard ambience; with Iris + a shader pack; with Vanilla Backport.
- GameTests also pass with Backported Spears, Backported Spears + Copper Age Backport, and all three backports
  installed (the data tests check the backport content).

## Differences from the 26.1 original (things 1.21.1 doesn't have)
- Without Backported Spears there are no spears: no spear goals, and gnomes that would carry a spear get a stone sword.
  With it, the spear arm pose skips the charge sway (1.21.1 items have no 26.1 kinetic weapon data to drive it).
- Without Copper Age Backport: gnomes use stone tools instead of copper ones, copper nuggets in loot become an
  equivalent chance of a copper ingot, and the relocation advancement icon is an iron shovel.
- Sounds missing from 1.21.1 use close vanilla ones unless a backport provides them: mailbox open/close and gnome pose
  (Copper Age Backport restores them), bubble pop and lead break (no backport has them); raid sounds play on the
  Master volume slider (no UI category).
- Raid music shows "Now playing" in the action bar when it starts (1.21.1 has no now-playing toast).
- Baby zombies wear armor with the adult armor textures (1.21.1 has no baby armor textures).
- Graveyard: without Vanilla Backport it has no pale moss, leaf litter or firefly bushes and dead trees use stripped
  birch. Its sky/fog/cloud/sunrise/light colours (26.1 environment attributes) are applied client-side and blend in at
  biome borders. Undead don't burn in daylight there.
- Block models: free rotations are rounded to 22.5° steps and the shelf display is dropped.
- Planting accepts every `#minecraft:dirt` block (26.1's `#grass_blocks`/`#moss_blocks` don't exist), which adds muddy mangrove roots.

## Upstream bugs fixed along the way
Paint vanished on chunk reload (paint colours were saved in a form NBT can't store); zombies breaking a flag never
dropped the Brainz flag; the lime mailbox recipe made a red mailbox; picking a sun battery block shared its stored sun
with the placed block; balloons dropped a lead when their holder died; 66 missing English strings (subtitles, death
messages, biome/entity/block names) and the three death sounds with a broken subtitle key.

Upstream gap kept: the newspaper zombie's `angry_ambient` sound files don't exist upstream.
