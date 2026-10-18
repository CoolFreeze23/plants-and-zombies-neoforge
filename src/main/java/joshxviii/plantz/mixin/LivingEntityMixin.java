package joshxviii.plantz.mixin;

import com.mojang.serialization.Codec;
import joshxviii.plantz.*;
import joshxviii.plantz.effect.PaintedMobEffect;
import joshxviii.plantz.effect.PazMobEffectHooks;
import joshxviii.plantz.entity.plant.Plant;
import joshxviii.plantz.raid.WaveType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathType;
import joshxviii.plantz.compat.ValueInput;
import joshxviii.plantz.compat.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

import static joshxviii.plantz.PazDataSerializers.DATA_PAINT_COLORS;
import static joshxviii.plantz.PazItems.DUCKY_TUBE_DAMAGE_INTERVAL;

@Mixin(LivingEntity.class)
abstract public class LivingEntityMixin implements PlantHeadAttachment, GardenHeroRewards {

    @Unique
    private static final EntityDataAccessor<Boolean> DATA_HYPNO_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Boolean> DATA_CHILL_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Boolean> DATA_FROZEN_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Boolean> DATA_BUTTER_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    /** NBT compound keys must be strings, so the colour keys are stored as decimal strings. */
    @Unique
    private static final Codec<Map<Integer, Integer>> PAINTED_COLORS_CODEC = Codec.unboundedMap(Codec.STRING.xmap(Integer::parseInt, String::valueOf), Codec.INT);
    @Unique
    private static final EntityDataAccessor<Map<Integer, Integer>> DATA_PAINTED_COLORS = SynchedEntityData.defineId(LivingEntity.class, DATA_PAINT_COLORS);

    @Shadow
    public abstract boolean hasEffect(Holder<MobEffect> effect);

    @Shadow
    public int swingTime;

    @Shadow
    public abstract @Nullable MobEffectInstance getEffect(Holder<MobEffect> effect);

    @Unique
    private CompoundTag plantData = new CompoundTag();

    @Unique
    private List<WaveType> completedRaidWaveList = List.of();

    @Unique
    private @Nullable Plant plantEntity = null;

    @Override
    public @Nullable Plant plantz$getPlant() {
        return plantEntity;
    }

    @Override
    public void plantz$setPlant(@Nullable Plant value) {
        plantEntity = value;
    }

    @Override
    public @NotNull CompoundTag plantz$getPlantData() {
        return plantData;
    }

    @Override
    public void plantz$setPlantData(@NotNull CompoundTag value) {
        plantData = value;
    }

    @Override
    public boolean plantz$hasPlantOnHead() {
        return plantEntity != null &&  plantEntity.isAlive() && !plantEntity.isRemoved();
    }

    @Override
    public @NotNull List<WaveType> plantz$getWaveList() {
        return completedRaidWaveList;
    }

    @Override
    public void plantz$setWaveList(@NotNull List<WaveType> value) {
        completedRaidWaveList = value;
    }



    @Unique
    public boolean plantz$getHypnoId() {
        return ((Entity) (Object) this).getEntityData().get(DATA_HYPNO_ID);
    }
    @Unique
    public boolean plantz$getChillId() {
        return ((Entity) (Object) this).getEntityData().get(DATA_CHILL_ID);
    }
    @Unique
    public boolean plantz$getFrozenId() {
        return ((Entity) (Object) this).getEntityData().get(DATA_FROZEN_ID);
    }
    @Unique
    public boolean plantz$getButterId() {
        return ((Entity) (Object) this).getEntityData().get(DATA_BUTTER_ID);
    }
    @Unique
    public Map<Integer, Integer> plantz$getPaintedColors() {
        return ((Entity) (Object) this).getEntityData().get(DATA_PAINTED_COLORS);
    }

    @Unique
    private boolean prevFloatTag = false;
    @Unique
    private float prevWaterMalus = 0f;

    @Inject(method = "onEquipItem", at = @At("TAIL"))
    private void plantz$checkFloatTag(EquipmentSlot slot, ItemStack oldStack, ItemStack stack, CallbackInfo ci) {
        if ((LivingEntity) (Object) this instanceof PathfinderMob mob) {
            if (stack.is(PazItems.DUCKY_TUBE) && slot == EquipmentSlot.LEGS) {
                prevFloatTag = mob.getNavigation().canFloat();
                prevWaterMalus = mob.getPathfindingMalus(PathType.WATER);
                mob.getNavigation().setCanFloat(true);
            }
            else if (oldStack.is(PazItems.DUCKY_TUBE) && slot == EquipmentSlot.LEGS) {
                mob.getNavigation().setCanFloat(prevFloatTag);
                mob.setPathfindingMalus(PathType.WATER, 0.0F);
            }
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void plantz$applyDuckyTubeBuoyancy(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        //Duck tube floating
        var item = entity.getItemBySlot(EquipmentSlot.LEGS);
        if (!item.is(PazItems.DUCKY_TUBE) && !entity.getType().is(PazTags.EntityTypes.PLANTABLE_ON_WATER)) return;
        if (entity instanceof Player player && player.getAbilities().flying) return;
        var fluidType = entity.level().getBlockState(BlockPos.containing(entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0))).getFluidState().getType();
        if (fluidType == Fluids.EMPTY ) return;

        //base
        double upwardForce = fluidType == Fluids.LAVA ? 0.1 : 0.015;
        // submerged
        if (entity.isEyeInFluid(FluidTags.WATER)) upwardForce = 0.135;
        if (entity.isEyeInFluid(FluidTags.LAVA)) upwardForce = 0.15;
        // sneaking
        if (entity.isShiftKeyDown()) upwardForce *= fluidType == Fluids.LAVA? 0.0 : 0.2;

        entity.addDeltaMovement(new Vec3(0.0, upwardForce, 0.0));

        entity.fallDistance = 0.0F;

        if (!entity.level().isClientSide() && entity.tickCount % DUCKY_TUBE_DAMAGE_INTERVAL==0 && entity.getRandom().nextFloat() > 0.5f)
            item.hurtAndBreak(1, entity, EquipmentSlot.LEGS);
    }

    @Inject(method = "defineSynchedData", at = @At(value = "TAIL"))
    public void defineData(SynchedEntityData.Builder entityData, CallbackInfo ci) {
        entityData.define(DATA_HYPNO_ID, false);
        entityData.define(DATA_CHILL_ID, false);
        entityData.define(DATA_FROZEN_ID, false);
        entityData.define(DATA_BUTTER_ID, false);
        entityData.define(DATA_PAINTED_COLORS, new HashMap<>());
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveCustomFlags(CompoundTag tag, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        ValueOutput output = ValueOutput.of(tag, self.registryAccess());
        output.putBoolean("plantz:IsHypnotized", self.getEntityData().get(DATA_HYPNO_ID));
        output.putBoolean("plantz:IsChilled", self.getEntityData().get(DATA_CHILL_ID));
        output.putBoolean("plantz:IsFrozen", self.getEntityData().get(DATA_FROZEN_ID));
        output.putBoolean("plantz:IsButtered", self.getEntityData().get(DATA_BUTTER_ID));
        output.store("plantz:PaintedColor", PAINTED_COLORS_CODEC, self.getEntityData().get(DATA_PAINTED_COLORS));
        if (!this.plantz$getPlantData().isEmpty()) {
            output.store("plantz:AttachedPlant", CompoundTag.CODEC, this.plantz$getPlantData());
        }
        if (self instanceof ServerPlayer) output.store("plantz:CompletedWaveList", Codec.list(WaveType.Companion.getCODEC()), this.plantz$getWaveList());

    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadCustomFlags(CompoundTag tag, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        ValueInput input = ValueInput.of(tag, self.registryAccess());
        self.getEntityData().set(DATA_HYPNO_ID, input.getBooleanOr("plantz:IsHypnotized", false));
        self.getEntityData().set(DATA_CHILL_ID, input.getBooleanOr("plantz:IsChilled", false));
        self.getEntityData().set(DATA_FROZEN_ID, input.getBooleanOr("plantz:IsFrozen", false));
        self.getEntityData().set(DATA_BUTTER_ID, input.getBooleanOr("plantz:IsButtered", false));
        self.getEntityData().set(DATA_PAINTED_COLORS, input.read("plantz:PaintedColor", PAINTED_COLORS_CODEC).orElseGet(HashMap::new));
        plantz$setPlantData(input.read("plantz:AttachedPlant", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        if (self instanceof ServerPlayer) plantz$setWaveList(input.read("plantz:CompletedWaveList", Codec.list(WaveType.Companion.getCODEC())).orElseGet(List::of));
        // Fix (upstream too): the flags above mirror active effects, which LivingEntity has just loaded, so derive them
        // from the effects. The old int-keyed map codec could never be written to NBT, so paint was lost on reload.
        if (!self.level().isClientSide()) updateEffects();

        if (self instanceof PathfinderMob mob) {
            prevFloatTag = mob.getNavigation().canFloat();
            prevWaterMalus = mob.getPathfindingMalus(PathType.WATER);
            if (mob.getItemBySlot(EquipmentSlot.LEGS).is(PazItems.DUCKY_TUBE)) {
                mob.getNavigation().setCanFloat(true);
                mob.setPathfindingMalus(PathType.WATER, 0.0F);
            }
        }
    }

    @Inject(method = "onEffectAdded", at = @At(value = "TAIL"))
    public void onHypnoAdded(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        updateEffects();
        // 26.1 Fabric API MobEffect.onEffectAdded(instance, entity)
        if (effect.getEffect().value() instanceof PazMobEffectHooks hooks) hooks.onEffectAdded(effect, (LivingEntity) (Object) this);
    }

    // 26.1 Fabric API MobEffect.onEffectStarted(instance, entity): runs where vanilla calls MobEffectInstance.onEffectStarted
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;onEffectStarted(Lnet/minecraft/world/entity/LivingEntity;)V", shift = At.Shift.AFTER))
    public void plantz$onEffectStarted(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (effect.getEffect().value() instanceof PazMobEffectHooks hooks) hooks.onEffectStarted(effect, (LivingEntity) (Object) this);
    }

    // 26.1 onEffectsRemoved(Collection) -> 1.21.1 onEffectRemoved(MobEffectInstance), called per effect
    // (removeAllEffects calls it before the effect leaves the map, so the removed effect is excluded explicitly)
    @Inject(method = "onEffectRemoved", at = @At(value = "TAIL"))
    public void onHypnoRemoved(MobEffectInstance effect, CallbackInfo ci) {
        updateEffects(effect);
        // 26.1 Fabric API MobEffect.onEffectRemoved(instance, entity)
        if (effect.getEffect().value() instanceof PazMobEffectHooks hooks) hooks.onEffectRemoved(effect, (LivingEntity) (Object) this);
    }
    @Unique
    public void updateEffects() {
        updateEffects(null);
    }
    @Unique
    private boolean plantz$hasEffectExcept(Holder<MobEffect> effect, @Nullable MobEffectInstance removed) {
        return this.hasEffect(effect) && (removed == null || !removed.is(effect));
    }
    @Unique
    public void updateEffects(@Nullable MobEffectInstance removed) {
        var self = (LivingEntity) (Object) this;

        self.getEntityData().set(DATA_HYPNO_ID, plantz$hasEffectExcept(PazEffects.HYPNOTIZE, removed));
        self.getEntityData().set(DATA_CHILL_ID, plantz$hasEffectExcept(PazEffects.CHILLED, removed));
        self.getEntityData().set(DATA_FROZEN_ID, plantz$hasEffectExcept(PazEffects.FROZEN, removed));
        self.getEntityData().set(DATA_BUTTER_ID, plantz$hasEffectExcept(PazEffects.BUTTERED, removed));
        Map<Integer, Integer> colors = PaintedMobEffect.getPaintColors(self);
        if (removed != null && removed.getEffect().value() instanceof PaintedMobEffect painted && self.hasEffect(removed.getEffect())) {
            colors = new HashMap<>(colors);
            colors.remove(painted.getPaintColor());
        }
        self.getEntityData().set(DATA_PAINTED_COLORS, colors);
    }

    @Inject(method = "canBeAffected", at = @At(value = "RETURN"), cancellable = true)
    public void immuneToHypnosisOrFreeze(MobEffectInstance newEffect, CallbackInfoReturnable<Boolean> cir) {
        var entity = (LivingEntity) (Object) this;
        if (newEffect.is(PazEffects.HYPNOTIZE)) {
            cir.setReturnValue(!entity.getType().is(PazTags.EntityTypes.CANNOT_HYPNOTIZE));
        }
        if (newEffect.is(PazEffects.CHILLED) || newEffect.is(PazEffects.FROZEN)) {
            cir.setReturnValue(entity.canFreeze());
        }
        updateEffects();
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At(value = "RETURN"), cancellable = true)
    public void noAttackWhenFrozen(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasEffect(PazEffects.FROZEN)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    public void updateEffect(CallbackInfo ci) {
        updateEffects();
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At(value = "RETURN"), cancellable = true)
    public void stopTargetingFriendlies(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasEffect(PazEffects.HYPNOTIZE) && (target instanceof Plant || target instanceof Player || target.hasEffect(PazEffects.HYPNOTIZE))) {
            cir.setReturnValue(false);
        }
    }

    // 26.1 hurtServer -> 1.21.1 hurt (server side only, like hurtServer)
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void ownerIgnorePlantAttacks(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        var self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        var sourceEntity = source.getEntity();
        var directEntity = source.getDirectEntity();
        if (source.is(DamageTypes.LIGHTNING_BOLT)) self.addEffect(new MobEffectInstance(PazEffects.ELECTRIFIED, 300, 1));
        if (source.is(PazTags.DamageTypes.IS_ELECTRIC) && self.getType().is(PazTags.EntityTypes.IMMUNE_TO_ELECTRICITY)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
        if (sourceEntity instanceof Plant plant) {
            if (plant.hasSameOwner(self)) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
        else if (directEntity instanceof Plant plant) {
            if (plant.hasSameOwner(self)) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }


}
