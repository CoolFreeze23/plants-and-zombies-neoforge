# How the port is structured

Upstream: [joshxviii/plants-and-zombies](https://github.com/joshxviii/plants-and-zombies) @ `88ddc959` (1.5.2; Fabric, MC 26.1.2, Kotlin + Java).
This port: NeoForge 21.1 / MC 1.21.1, Java 21, Kotlin 2.3 via Kotlin for Forge 5.11, TerraBlender 4.1.

The goal is to keep upstream's code recognisable so upstream changes can be carried over. Most files are upstream's with
the minimum changes 1.21.1 forces; the newer vanilla APIs upstream relies on are re-implemented in a small compat layer.

## Layout
| Path | What |
|---|---|
| `src/main/kotlin/joshxviii/plantz/**`, `src/main/java/joshxviii/plantz/{model,animation}/**` | Upstream code (main and client source sets merged into one; client classes are only touched on the client) |
| `src/main/java/joshxviii/plantz/compat/**` | 26.1 APIs re-implemented on 1.21.1 (below) |
| `src/main/java/joshxviii/plantz/mixin/**` | Upstream mixins retargeted to 1.21.1, plus port mixins (bucket helmet, wall-nut collision, graveyard sun burn/ambience) |
| `src/main/kotlin/joshxviii/plantz/integration/**` | Optional Iris, Vanilla Backport, Copper Age Backport and Backported Spears support (registry lookups with 1.21.1 fallbacks, so nothing links against the backports) |
| `src/main/kotlin/joshxviii/plantz/client/GraveyardAmbience.kt` | The graveyard's 26.1 environment attributes (fog, clouds, sunrise, light) |
| `src/main/resources/resourcepacks/{vanillabackport,copperagebackport}_compat` | Built-in data packs, each enabled only when its backport is installed (they override different files) |
| `src/gametest/**` | Dev-only GameTests (not packaged), 142 tests covering plants, zombies, raids, blocks/items, protection and data |

## Entry points
- `PazMain` (`@Mod`): wires NeoForge events. All `Paz*.initialize()` objects still register eagerly with
  `Registry.register(...)`; they run inside the ENTITY_TYPE `RegisterEvent`, when every registry is unfrozen and
  NeoForge's own attributes are already bound. Don't convert them to DeferredRegister.
- `PazClient`: client events (renderers, layers, particles, screens, GUI layers, shaders, item models, tooltips).
- `PazHooks` / `PazClientHooks`: event wiring for hooks that live in other files.
- `PazNetwork`: payload registration (handlers run on the main thread, like Fabric's).
- Biome spawns are `plantz:add_spawns` biome modifiers (`data/plantz/neoforge/biome_modifier`), which keep upstream's
  spawn categories (plants are AMBIENT).

## Compat layer
Mirrors 26.1 names and semantics, so upstream code ports with import changes:
- `AnimationState`, `ARGB`, `LightCoordsUtil`: 26.1 copies.
- `ValueInput` / `ValueOutput`: 26.1's save API over a `CompoundTag`. Save hooks take the 1.21.1 `CompoundTag` and wrap it:
  `val output = ValueOutput.of(tag, registryAccess())`, then run upstream's body.
- `EntityReference`: UUID + cached lookup; synced as `OPTIONAL_UUID`.
- `menu.ExtendedMenuType` / `ExtendedMenuProvider`: Fabric's extended menus on NeoForge's `IContainerFactory`.
- `ai.SpearUseGoal`: 26.1's spear charge goal; `KineticWeapon` reads the `minecraft:kinetic_weapon` /
  `minecraft:swing_animation` components a spear backport registers (reflectively; inert without one); `Ease`: 26.1 easings.
- `client.model.CuboidRotation` + `mixin.client.model.*`: 26.1 model element rotations (any angle, x/y/z) on 1.21.1's
  block model loader. `client.model.SpearAnimations`: 26.1 third-person spear hold/charge/stab animations.
- `client/NowPlayingToast.kt` + `ToastComponentMixin`: 26.1's music toast, drawn with the toasts; `networking/UiSoundPayload`
  + `SoundOptionsScreenMixin`: 26.1's UI sound category as a port volume option.
- `PazCompatSounds`: `plantz:compat.<26.1 id>` stand-ins for 26.1 sounds (a backport's own event wins), defined in
  sounds.json as the 1.21.1 substitute; `client/ModernAssets.kt`: a built-in client pack serving 26.1's baby armor
  textures and those sounds from a local 26.1+ game install (vanilla launcher / Prism), never shipped in the jar.
  The baby armor uses `CompatModelLayers.ZOMBIE_BABY_ARMOR_26_1` (26.1 UVs) when its texture is there.
- `client.OnShelfDisplay` + `META-INF/enumextensions.json` + `CopperShelfRendererMixin` (optional, `require = 0`): 26.1's
  `on_shelf` display context, used by Copper Age Backport's shelves.
- Item blocking: 26.1's `BLOCKS_ATTACKS` (newspaper) is a 1.21.1 shield (`ItemAbilities.SHIELD_BLOCK`) whose 26.1 arc,
  reduction, block sound and break chance come from `NewspaperItem.onShieldBlock` (NeoForge `LivingShieldBlockEvent`);
  26.1's `applyItemBlocking` head protection (`plantz:protects_head`) is `LivingEntityProtectsHeadMixin`, on the damage
  left after shield blocking in `LivingEntity.hurt`.
- Item models: 26.1 item definitions became 1.21.1 models with `overrides` on the predicates in
  `data/PazItemModelProperties` (26.1 `has_component` / `select` / `range_dispatch`); 26.1 `tints` are per-item color
  handlers there (dyed dye blaster, closed letter). `PlantIconItemModel` and `DoorShieldItemModel` are baked-model
  wrappers for upstream's custom item model types (`plantz:plant_icon`, `plantz:door_shield_model`).
- `gui.LetterEditBox` + `MultiLineEditBoxMixin`: 26.1's `MultiLineEditBox.builder()` options (no frame, black text, line limit).
- 26.1 per-item enchantability (`enchantable(n)`) is a `getEnchantmentValue` override; 26.1's carve loot table is rolled by
  `PumpkinBlockCarveMixin` (`plantz:injection/carve/pumpkin`, a `block_use` table).
- `client.*`: the 26.1 render-state pipeline running immediate-mode on 1.21.1:
  - `state.*` render states (26.1 fields, plus `compatEntity`/`compatPartialTick` for the frame being drawn, and
    Fabric-style `setData/getData` keyed by `RenderStateDataKey`).
  - `model.*`: state-driven `Model`, `EntityModel`, `HumanoidModel`, `ZombieModel`, `BabyZombieModel` (26.1 baby mesh),
    `ArmorModelSet`, `CompatModelLayers`; `animation.KeyframeAnimation.bake(definition, root)`.
  - `renderer.*`: `EntityRenderer`, `LivingEntityRenderer`, `MobRenderer`, `AgeableMobRenderer`, `HumanoidMobRenderer`,
    `AbstractZombieRenderer(ctx, model, babyModel, armorSet, babyArmorSet)`, the `SubmitNodeCollector` (draws straight
    into the frame's buffers), `blockentity.BlockEntityRenderer`.
  - `renderer.layers.*`: `RenderLayer`, `EyesLayer`, and 26.1 versions of `HumanoidArmorLayer`, `CustomHeadLayer`,
    `ItemInHandLayer`, `WingsLayer`, `EquipmentLayerRenderer`.
  Vanilla 1.21.1 renderers (players, vanilla mobs) get the mod's tints and layers through `LivingEntityRendererVanillaMixin`.
  26.1's `ArmPose.SPEAR` (1.21.1's enum has none) is `ArmedEntityRenderState.rightArmSpear/leftArmSpear`, set when the
  arm holds an item in `#minecraft:spears`.

## Common 26.1 → 1.21.1 changes in the upstream code
- `Identifier` → `ResourceLocation`; `EntitySpawnReason` → `MobSpawnType`; `advancements.criterion` → `advancements.critereon`.
- `hurtServer(level, …)` → `hurt(…)`; `doHurtTarget(level, t)` → `doHurtTarget(t)`; `customServerAiStep(level)` → `customServerAiStep()`.
- Items: `use` returns `InteractionResultHolder`; 26.1 components without a 1.21.1 equivalent are reimplemented
  (`Equipable` items, `plantz:use_cooldown` for per-plant seed packet cooldowns, `plantz:crafting_transmute` recipes).
- Blocks: `useItemOn` returns `ItemInteractionResult`; `updateShape`/`neighborChanged` use the 1.21.1 signatures;
  contents drop from `onRemove` (26.1 `preRemoveSideEffects`).
- GUI: `GuiGraphicsExtractor` → `GuiGraphics`; particles use `TextureSheetParticle` with custom render types for beams/arcs.

## Tests
`./gradlew runGameTestServer` runs every GameTest (template `plantz:platform`: grass floor at relative y=1, spawn mobs at y=2).
Tests that need a player use a mock player whose connection allows the mod's payload channels (see the helpers in
`src/gametest/.../{plants,zombies,blocks}`). `./gradlew runServer`, `runClient`, `runClientSmoke` (quick-plays
`run/saves/smoke`) and `runClientMp` (joins the dev server) are there for manual checks.
