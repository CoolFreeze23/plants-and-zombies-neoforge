# Plants & Zombies: 26.1.2 Fabric → 1.21.1 NeoForge port guide

Upstream: https://github.com/joshxviii/plants-and-zombies @ e92d5937 (Fabric, MC 26.1.2, Kotlin + Java).
Target: NeoForge 21.1.262, MC 1.21.1, Java 21, Kotlin 2.3.0 via Kotlin for Forge 5.11.0, TerraBlender 4.1.0.8.

Reference sources (read-only, grep these instead of guessing):
- 1.21.1 + NeoForge decompiled (parchment names): `C:\Homework\Sessions\Japan\pvz-ref\neo-1.21.1\` (net/minecraft/..., net/neoforged/...)
- 26.1.2 decompiled: `C:\Homework\Sessions\Japan\pvz-ref\mc-26.1.2\src\` (see what the upstream code called)
- Untouched upstream sources: `C:\Homework\Sessions\Japan\pvz-src\src\`

## Build
```
export GRADLE_USER_HOME="$PWD/.gradle-home"   # inside your checkout; or point at the shared one (see your brief)
./gradlew compileKotlin --console=plain > build.log 2>&1   # ~3 min
grep '^e: ' build.log | grep '/your/area/'                 # Kotlin errors for your files
```
Kotlin compiles before Java, so Java errors (models, mixins, compat) only show once Kotlin is clean.
Kotlin errors in files you do not own are expected, so ignore them.

## Ground rules
- Keep upstream behaviour. This is a port, not a rewrite. Change only what 1.21.1 forces.
- Keep the public API of upstream classes (class names, method names, params) unless a 1.21.1 type forces a change; other areas call it.
- Never edit files outside your assigned area. Need something from another area or the compat layer? Note it in your final report
  (or, if it blocks you, add a NEW file under `compat/` with a distinctive name and say so).
- Registration stays eager (`@JvmField val X = Registry.register(...)` in `Paz*` objects). It runs inside NeoForge's
  RegisterEvent window, so all `PazEntities.X`, `PazItems.X`, ... are real objects. Do not convert to DeferredRegister.
- Mod id `plantz`; `pazResource(path)` (Utils.kt) builds a ResourceLocation.

## Mechanical renames already applied everywhere
- `Identifier` → `ResourceLocation` (same factory methods: fromNamespaceAndPath, withDefaultNamespace, parse)
- `EntitySpawnReason` → `MobSpawnType` (`SPAWN_ITEM_USE` → `SPAWN_EGG`; `LOAD`/`DIMENSION_TRAVEL` do not exist, so pick a sane one)
- `net.minecraft.advancements.criterion` → `net.minecraft.advancements.critereon`
- 26.1 sub-packages relocated to 1.21.1 packages (e.g. `monster.zombie.Zombie` → `monster.Zombie`)
- `RenderTypes.x(...)` → `RenderType.x(...)`

## Compat layer (`src/main/java/joshxviii/plantz/compat/`): 26.1 APIs re-implemented on 1.21.1
Use these instead of rewriting code to 1.21.1 idioms. They mirror 26.1 names and semantics.
- `compat.AnimationState`: 26.1 AnimationState (startTick-based, `copyFrom`, `getTimeInMillis`). Use everywhere instead of vanilla's.
- `compat.ARGB`, `compat.LightCoordsUtil`: verbatim 26.1 copies.
- `compat.ValueInput` / `compat.ValueOutput`: wrappers over CompoundTag. Port save code like:
  ```kotlin
  override fun addAdditionalSaveData(tag: CompoundTag) { super.addAdditionalSaveData(tag); val output = ValueOutput.of(tag, registryAccess()); /* upstream body */ }
  override fun readAdditionalSaveData(tag: CompoundTag) { super.readAdditionalSaveData(tag); val input = ValueInput.of(tag, registryAccess()); /* upstream body */ }
  // block entities: saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) / loadAdditional(tag, registries)
  ```
- `compat.EntityReference<T : Entity>`: 26.1 EntityReference (UUID + lookup). For synced data use `EntityDataSerializers.OPTIONAL_UUID`.
- Client (`compat.client.*`): a full 26.1-style render-state pipeline running immediate-mode on 1.21.1:
  - states: `compat.client.state.{EntityRenderState, LivingEntityRenderState, ArmedEntityRenderState, HumanoidRenderState, ZombieRenderState, BlockEntityRenderState}`
    (26.1 field names; plus `compatEntity` (the entity being drawn this frame) and `compatPartialTick`; Fabric-style `setData/getData/getDataOrDefault` with `compat.client.RenderStateDataKey`)
  - models: `compat.client.model.{Model<S>, EntityModel<S>, HumanoidModel<S>, ZombieModel<S>}` (`setupAnim(state)` resets pose first, like 26.1).
    HumanoidModel babies use 1.21.1-style `young` scaling, so there is no separate baby mesh (vanilla `ModelLayers.ZOMBIE_BABY` etc. do not exist in 1.21.1).
  - `compat.client.animation.KeyframeAnimation`: `KeyframeAnimation.bake(definition, root)` (26.1 `definition.bake(root)`), `apply/applyWalk/applyStatic`.
  - renderers: `compat.client.renderer.{EntityRenderer<T,S>, LivingEntityRenderer<T,S,M>, MobRenderer, AgeableMobRenderer, HumanoidMobRenderer, AbstractZombieRenderer}`
    with 26.1 methods `createRenderState / extractRenderState / submit / scale(state, pose) / getTextureLocation(state) / getShadowRadius(state) / getWhiteOverlayProgress(state) / setupRotations / isShaking`.
    `AbstractZombieRenderer(ctx, model, babyModel[, innerArmorLayer, outerArmorLayer])` draws armor via vanilla HumanoidArmorLayer.
  - layers: `compat.client.renderer.layers.{RenderLayer, RenderLayerParent, EyesLayer, VanillaHumanoidLayers}` (`VanillaHumanoidLayers` runs vanilla armor/held-item/head/elytra layers for a compat HumanoidModel)
  - `compat.client.renderer.SubmitNodeCollector`: `submitModel`, `submitModelPart`, `submitCustomGeometry`, `submitNameTag`, `submitText`, `order(n)`; draws immediately into `bufferSource()`
  - `compat.client.CameraRenderState.current()`, `compat.client.renderer.blockentity.BlockEntityRenderer<T,S>` (26.1 interface; registered with vanilla `BlockEntityRenderers.register(type) { renderer }`)
  - Not shimmed (port by hand): GUI (`GuiGraphicsExtractor` → `GuiGraphics`), particles (1.21.1 `TextureSheetParticle`/`ParticleRenderType`), item models / ItemStackRenderState (use `ItemRenderer.renderStatic`), RenderPipelines (→ `RenderType.create`/`ShaderInstance`).

## Common 26.1 → 1.21.1 API mapping
- `hurtServer(level, source, amount)` → `hurt(source, amount)` (get the level via `level() as ServerLevel` when needed)
- `doHurtTarget(level, target)` → `doHurtTarget(target)`; `customServerAiStep(level)` → `customServerAiStep()`
- `dropCustomDeathLoot(level, source, recent)` → `dropCustomDeathLoot(source, recent)`; `die(source)` same
- `finalizeSpawn(level, difficulty, reason, data)` same shape with `MobSpawnType`
- `isClientSide()` → `isClientSide` (field); `level().random` ok; `snapTo(...)` → `moveTo(...)`; `teleportTo` overloads differ
- Items: `use(level, player, hand): InteractionResult` → `InteractionResultHolder<ItemStack>`; `InteractionResult.SUCCESS_SERVER` → `sidedSuccess(...)`/`SUCCESS`;
  `Item.Properties().setId(...)` → delete; equippable/consumable components → ArmorItem / FoodProperties.
- Blocks: `useItemOn(...)` returns `ItemInteractionResult`; `useWithoutItem` returns `InteractionResult`; `updateShape(state, level, tickAccess, pos, dir, nPos, nState, random)` →
  `updateShape(state, dir, nState, level, pos, nPos)`; `ScheduledTickAccess` → `LevelAccessor`; `BlockBehaviour.Properties.setId` → delete; `Orientation` → removed (neighborChanged takes fromPos/isMoving).
- Effects: `applyEffectTick(level, entity, amp)` → `applyEffectTick(entity, amp)`.
- `WeightedList` → `SimpleWeightedRandomList`; `Util` → `net.minecraft.Util`; `Profiler.get()` → `level.profiler`.
- Saved data: `SavedDataType` → `SavedData.Factory` + `DimensionDataStorage.computeIfAbsent(factory, name)`.
- Networking: Fabric `ServerPlayNetworking.send(player, payload)` → `PacketDistributor.sendToPlayer(player, payload)`; client send → `PacketDistributor.sendToServer(payload)`.
- Menus: `compat.menu.ExtendedMenuType` / `compat.menu.ExtendedMenuProvider` (Fabric names). `player.openMenu(blockEntity)` works as-is;
  `getScreenOpeningData(player: ServerPlayer?)` receives null (NeoForge does not pass the player), so make the parameter nullable.

## Area ownership (one agent per area, each in its own worktree/branch)
| area | files |
|---|---|
| core (lead, master) | top-level `kotlin/joshxviii/plantz/*.kt` except the ones listed below, `networking/*`, `inventory/*`, `compat/*`, `META-INF/*`, `plantz.mixins.json` |
| zombies | `entity/zombie/**`, `entity/blueprint_machines/**`, `raid/**`, `ai/ZombieState.kt`, `java/PazOwnableZombie.java` |
| plants | `entity/plant/**`, `entity/projectile/**`, `ai/**` (not ZombieState), `api/**`, `entity/Sun.kt`, `entity/Balloon.kt`, `entity/PlantPotMinecart.kt`, `entity/gnome/**`, `effect/**`, `advancement/**`, `java/LeashableEntity.java`, `java/joshxviii/plantz/mixin/*.java` (server mixins) |
| blocks | `block/**`, `item/**` |
| render | `renderer/**`, `java/joshxviii/plantz/model/**`, `java/joshxviii/plantz/animation/**`, `PazModels.kt`, `PazRenderPipelines.kt`, `GnomeArmorSet.kt`, `PlantIconItemModel.kt`, `assets/plantz/shaders/**` |
| gui | `gui/**`, `particles/**`, `PazParticles.kt`, `PazScreens.kt`, `RaidMusicManager.kt`, `java/joshxviii/plantz/mixin/client/**`, `plantz.client.mixins.json` |
| data | `src/main/resources/**` except shaders, mixin jsons, META-INF; may add `kotlin/joshxviii/plantz/data/**` for things that moved from data to code (villager trades) |

## Client wiring contract (core's `PazClient` calls these; owners implement them)
- `PazModels.registerLayers(event: EntityRenderersEvent.RegisterLayerDefinitions)` and `PazModels.registerRenderers(event: EntityRenderersEvent.RegisterRenderers)` (render)
- `PazRenderPipelines.registerShaders(event: RegisterShadersEvent)` (render; may be empty), plus any `RenderType`s it exposes
- `PazParticles.registerProviders(event: RegisterParticleProvidersEvent)` (gui)
- `PazScreens.registerScreens(event: RegisterMenuScreensEvent)` and `PazScreens.registerGuiLayers(event: RegisterGuiLayersEvent)` (gui)
- Item model properties / custom item models: render registers them from `PazModels` (`registerItemProperties()` called in FMLClientSetupEvent.enqueueWork, `registerAdditionalModels(event: ModelEvent.RegisterAdditional)`, `modifyBakingResult(event: ModelEvent.ModifyBakingResult)`) as needed
