# Plants & Zombies: NeoForge 1.21.1 port

Unofficial backport of [joshxviii/plants-and-zombies](https://github.com/joshxviii/plants-and-zombies) (Fabric, MC 26.1.2,
upstream commit `bbf9ae9a`, mod version 1.5.2) to **NeoForge 21.1 / Minecraft 1.21.1**. Licensed MPL-2.0 like upstream.
`PORTING.md` explains how the port is structured.

## Install
Put `build/libs/plants-and-zombies-neoforge-1.5.2-neoforge.2.jar` in `mods/` together with:
- Kotlin for Forge 5.11.0 or newer (NeoForge build)
- TerraBlender for NeoForge 1.21.1 (4.1.0.x)

Optional, detected automatically:
- **A 26.1 or newer Minecraft install** (vanilla launcher or Prism Launcher, on the same computer): 26.1's baby armor
  textures and the 26.1 sounds the mod uses (copper chest, copper golem statue and steps, creaking, happy ghast harness,
  HUD bubble, lead snap) are game assets 1.21.1 doesn't have, and the mod doesn't redistribute Mojang's files. When it finds
  a 26.1+ game (the version's client jar, and its asset index for the sounds), a built-in resource pack serves those
  files from it; the log says "Using 26.1 assets from Minecraft <version>". Without one the 1.21.1 substitutes play
  (with 26.1's subtitles) and baby armor maps the adult textures. Installing 26.1 from the launcher once is enough.
- **Iris** (+ Sodium): with a shader pack active, the paint overlay renders through Iris' entity program, like the
  26.1 original's Iris hook. Tested with Iris 1.8.12 + Sodium 0.6.13 and Complementary Reimagined.
- **Vanilla Backport** (+ Platform): brings back the 26.1 content that needs newer vanilla blocks and sounds. A built-in
  data pack (auto-enabled) adds pale moss, leaf litter and firefly bushes to the graveyard, pale oak dead trees with
  pale moss and leaf litter, gravestones on pale moss, and leaf-litter tint; tags pick up pale moss carpet and the pale
  garden; its creaking and happy ghast sounds are used for the tangle kelp and the plant pot. Tested with Vanilla Backport 1.1.7.10 + Platform 1.3.3.
- **Copper Age Backport**: brings back the 26.1 copper content. Gnomes spawn with copper axes and pickaxes, the
  relocation advancement shows a copper shovel (built-in data pack, auto-enabled), its copper chest and copper golem
  statue and step sounds are used for the mailboxes, garden gnomes and robo zombie, and
  Plants & Zombies items on its shelves use their 26.1 `on_shelf` transforms. Copper golems are immune to electric plants. Tested with Copper Age Backport 0.1.5 (0.1.4 crashes on startup on current NeoForge 21.1, a bug fixed in 0.1.5); works alongside Vanilla Backport.
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
- Every Plants & Zombies tag expands to the same blocks, items and entities as upstream does against 26.1's vanilla tags
  (1.21.1's `#minecraft:dirt` and carver tags hold different blocks, so the port uses 26.1's contents).
- **All 148 required GameTests pass** (repeated runs): every plant's behaviour, all special zombies and blueprint machines, ownership, a full zombie
  raid (waves, victory, rewards, boss types), mailboxes (delivery, queue, full/discard), collection box, sun battery, time machine,
  flags, gravestones, garden gnomes, pots, watering can, gardening glove (incl. Lobbing), seed packets and cooldowns, wearables
  (incl. the bucket) and their head protection, the newspaper and door shields, the coffin, letters, pumpkin armor and
  carving loot, the brainz alloy door and trapdoor, zombats, dispensers, blueprints, spawn egg variants and loadouts,
  every upstream recipe, loot tables, advancements, tags, worldgen features, seed mutations and villager trades.
- Raids saved by older versions (plain wave types, no boss health, no starter) load and save again in the new format.
- Dedicated server + client over the network: config sync, a zombie raid started with `/pz raid` with its HUD, boss
  bar, boss chance and wave timer, a saved raid surviving a restart, `/pz options`, the sun HUD, the door shield held and
  raised, the coffin worn, Baron von Bats, Zombie Goat, Zombat, Marigold, a Lobbing glove throw, the creative tab eggs.
- Dev client: every entity and block entity renderer, item icons, screens (mailbox, collection box, time machine),
  raid overlay, particles, graveyard ambience; with Iris + a shader pack; with Vanilla Backport.
- GameTests also pass with Backported Spears, Backported Spears + Copper Age Backport, and all three backports
  installed (the data tests check the backport content).

## Differences from the 26.1 original (things 1.21.1 doesn't have)
- Without Backported Spears there are no spears, and gnomes that would carry a spear get a stone sword.
- Without Copper Age Backport: gnomes use stone tools instead of copper ones and the relocation advancement icon is an
  iron shovel.
- 26.1 sounds and baby armor textures need a local 26.1+ game install (see Install); without one, close 1.21.1 sounds
  play and baby armor maps the adult armor textures onto the 26.1 baby shapes.
- 26.1's UI sound category is a "UI" slider the port adds to Music & Sounds (1.21.1's sound categories can't be
  extended); it scales the raid announcement sounds, which play on top of Master like 26.1's UI sounds.
- The raid music "Now playing" toast is drawn with 1.21.1's toast background and a note glyph (26.1's
  `toast/now_playing` and music-note sprites don't exist in 1.21.1).
- Graveyard: without Vanilla Backport it has no pale moss, leaf litter or firefly bushes and dead trees use stripped
  birch. Its sky/fog/cloud/sunrise/light colours (26.1 environment attributes) are applied client-side and blend in at
  biome borders. Undead don't burn in daylight there.
- Block models: 26.1 element rotations (any angle, x/y/z rotations) load through a port mixin, so the upstream models are
  unchanged. 26.1's `on_shelf` display context is `plantz:on_shelf` (NeoForge requires a namespaced name), falling
  back to `fixed`; 1.21.1 only has shelves through Copper Age Backport.
- The newspaper blocks like a 1.21.1 shield (raised after a quarter second, axes disable it) with 26.1's numbers: 90%
  of a hit from within 90 degrees of where its holder looks. The door shield and the worn coffin block through the same
  path (`item/component/BlocksAttacks`, 26.1's `blocks_attacks` numbers). Head protection (bucket, cone, football
  helmet) applies right after shield blocking, where 26.1 applies it, and now takes the whole hit from above.

The worn bucket uses the mod's own `plantz:item/bucket_head` model in the head layers instead of overriding the
vanilla bucket model like upstream, so resource packs that retexture the bucket (e.g. Alittle Axolotl) no longer hide it.

Upstream quirk kept: the raid's "vanish" wither sound is played with `playLocalSound` on the server level, which plays
nothing (in 26.1 too).

## Upstream bugs fixed along the way
Paint vanished on chunk reload (paint colours were saved in a form NBT can't store); zombies breaking a flag never
dropped the Brainz flag; the lime mailbox recipe made a red mailbox; picking a sun battery block shared its stored sun
with the placed block; balloons dropped a lead when their holder died; 66 missing English strings (subtitles, death
messages, biome/entity/block names) and the three death sounds with a broken subtitle key. Sound events registered without a
definition: the zombie yeti's snow chunk burst was silent (now vanilla's powder snow break, with a subtitle) and the
per-variant gnome jump events logged "missing sound" warnings (now the shared gnome jump). Hypnotized zombies stopped
fighting other zombies (a new `canAttack` check also applied to them); raids saved before wave details existed were
dropped on load; a raid with no starter couldn't load at all; the Pirate Captain Ghost egg had no name; five spawn egg
models pointed at textures that don't exist.

Upstream gap kept: the newspaper zombie's `angry_ambient` sound files don't exist upstream.
