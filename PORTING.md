# How the port is structured

Upstream: [joshxviii/plants-and-zombies](https://github.com/joshxviii/plants-and-zombies) @ `e92d5937` (Fabric, MC 26.1.2, Kotlin + Java).
This port: NeoForge 21.1 / MC 1.21.1, Java 21, Kotlin 2.3 via Kotlin for Forge 5.11, TerraBlender 4.1.

The goal is to keep upstream's code recognisable so upstream changes can be carried over. Most files are upstream's with
the minimum changes 1.21.1 forces; the newer vanilla APIs upstream relies on are re-implemented in a small compat layer.

## Layout
| Path | What |
|---|---|
| `src/main/kotlin/joshxviii/plantz/**`, `src/main/java/joshxviii/plantz/{model,animation}/**` | Upstream code (main and client source sets merged into one; client classes are only touched on the client) |
| `src/main/java/joshxviii/plantz/compat/**` | 26.1 APIs re-implemented on 1.21.1 (below) |
| `src/main/java/joshxviii/plantz/mixin/**` | Upstream mixins retargeted to 1.21.1, plus port mixins (bucket helmet, wall-nut collision, graveyard sun burn/ambience) |
| `src/main/kotlin/joshxviii/plantz/integration/**` | Optional Iris, Vanilla Backport and Copper Age Backport support (registry lookups with 1.21.1 fallbacks, so nothing links against the backports) |
| `src/main/kotlin/joshxviii/plantz/client/GraveyardAmbience.kt` | The graveyard's 26.1 environment attributes (fog, clouds, sunrise, light) |
| `src/main/resources/resourcepacks/{vanillabackport,copperagebackport}_compat` | Built-in data packs, each enabled only when its backport is installed (they override different files) |
| `src/gametest/**` | Dev-only GameTests (not packaged), 129 tests covering plants, zombies, raids, blocks/items and data |

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
