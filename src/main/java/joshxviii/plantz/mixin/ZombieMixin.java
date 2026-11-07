package joshxviii.plantz.mixin;

import joshxviii.plantz.*;
import joshxviii.plantz.compat.ValueInput;
import joshxviii.plantz.compat.ValueOutput;
import joshxviii.plantz.entity.zombie.PazZombie;
import net.minecraft.nbt.CompoundTag;
import joshxviii.plantz.raid.ZombieRaider;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * @author Josh
 */
@Mixin(Zombie.class)
public class ZombieMixin implements ZombieRaider {

    @Unique
    static private final EntityDataAccessor<Boolean> IS_FROM_RAID  = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BOOLEAN);
    @Unique
    static private final EntityDataAccessor<Boolean> ADDS_HEALTH_TO_RAID  = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BOOLEAN);
    @Unique
    static private final EntityDataAccessor<Boolean> IS_LEADER  = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BOOLEAN);
    @Unique
    static private final EntityDataAccessor<Boolean> IS_BOSS  = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BOOLEAN);
    @Unique
    static private final EntityDataAccessor<Boolean> IGNORE_FLAG  = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BOOLEAN);

    @Override
    public boolean plantz$getIsFromRaid() {
        return ((Entity) (Object) this).getEntityData().get(IS_FROM_RAID);
    }

    @Override
    public void plantz$setIsFromRaid(boolean value) {
        ((Entity) (Object) this).getEntityData().set(IS_FROM_RAID, value);
    }

    @Override
    public boolean plantz$getAddsHealthToRaid() {
        return ((Entity) (Object) this).getEntityData().get(ADDS_HEALTH_TO_RAID);
    }

    @Override
    public void plantz$setAddsHealthToRaid(boolean value) {
        ((Entity) (Object) this).getEntityData().set(ADDS_HEALTH_TO_RAID, value);
    }

    @Override
    public void plantz$setIsLeader(boolean value) {
        ((Entity) (Object) this).getEntityData().set(IS_LEADER, value);
    }

    @Override
    public boolean plantz$getIsLeader() {
        return ((Entity) (Object) this).getEntityData().get(IS_LEADER);
    }

    @Override
    public boolean plantz$getIsBoss() {
        return ((Entity) (Object) this).getEntityData().get(IS_BOSS);
    }

    @Override
    public void plantz$setIsBoss(boolean value) {
        ((Entity) (Object) this).getEntityData().set(IS_BOSS, value);
    }

    @Override
    public boolean plantz$getIgnoreFlag() {
        return ((Entity) (Object) this).getEntityData().get(IGNORE_FLAG);
    }

    @Override
    public void plantz$setIgnoreFlag(boolean value) {
        ((Entity) (Object) this).getEntityData().set(IGNORE_FLAG, value);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    public void syncedData(SynchedEntityData.Builder entityData, CallbackInfo ci) {
        entityData.define(IS_FROM_RAID, false);
        entityData.define(ADDS_HEALTH_TO_RAID, true);
        entityData.define(IS_LEADER, false);
        entityData.define(IS_BOSS, false);
        entityData.define(IGNORE_FLAG, false);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveCustomFlags(CompoundTag tag, CallbackInfo ci) {
        ValueOutput output = ValueOutput.of(tag, ((Entity) (Object) this).registryAccess());
        output.putBoolean("plantz:IsFromRaid", this.plantz$getIsFromRaid());
        output.putBoolean("plantz:AddsHealthToRaid", this.plantz$getAddsHealthToRaid());
        output.putBoolean("plantz:IsLeader", this.plantz$getIsLeader());
        output.putBoolean("plantz:IsBoss", this.plantz$getIsBoss());
        output.putBoolean("plantz:IgnoreFlag", this.plantz$getIgnoreFlag());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadCustomFlags(CompoundTag tag, CallbackInfo ci) {
        ValueInput input = ValueInput.of(tag, ((Entity) (Object) this).registryAccess());
        this.plantz$setIsFromRaid(input.getBooleanOr("plantz:IsFromRaid", false));
        this.plantz$setAddsHealthToRaid(input.getBooleanOr("plantz:AddsHealthToRaid", true));
        this.plantz$setIsLeader(input.getBooleanOr("plantz:IsLeader", false));
        this.plantz$setIsBoss(input.getBooleanOr("plantz:IsBoss", false));
        this.plantz$setIgnoreFlag(input.getBooleanOr("plantz:IgnoreFlag", false));
    }

    // 26.1 hurtServer -> 1.21.1 hurt (Zombie.hurt calls super.hurt(source, amount) first)
    @ModifyArgs(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Monster;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private void updateDamage(Args args) {
        DamageSource source = args.get(0);
        float damage = args.get(1);
        if (source.is(PazDamageTypes.PLANT_EXPLODE)) args.set(1, damage*3f);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void zombieAoeIgnore(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        Zombie entity = (Zombie) (Object) this;
        if (source.is(PazDamageTypes.ZOMBIE_SMASH) && !entity.hasEffect(PazEffects.HYPNOTIZE)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }


    @Unique
    private static final float BONUS_LEADER_CHANCE = 0.0015F;
    @Unique
    private static final int LEADER_GROUP_SIZE = 3;

    @Inject( method = "finalizeSpawn", at = @At("RETURN"))
    public void checkForLeader(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnReason, SpawnGroupData groupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (spawnReason == MobSpawnType.REINFORCEMENT || spawnReason == MobSpawnType.EVENT) return;

        Zombie entity = (Zombie) (Object) this;
        if (!entity.getType().is(PazTags.EntityTypes.CAN_BE_LEADER)) {
            PazZombie.Companion.removeLeaderModifiers(entity);
            return;
        }

        boolean isBornLeader = PazZombie.Companion.isBornLeader(entity);
        boolean bonusPromotionChance = level.getRandom().nextFloat() < BONUS_LEADER_CHANCE;

        if (isBornLeader || bonusPromotionChance) promoteToLeader(entity, spawnReason);
    }

    @Unique
    private void promoteToLeader(Zombie entity, MobSpawnType spawnReason) {
        entity.setItemSlot(EquipmentSlot.OFFHAND, PazBlocks.BRAINZ_FLAG.asItem().getDefaultInstance());
        entity.setDropChance(EquipmentSlot.OFFHAND, 1.0F);

        if (spawnReason == MobSpawnType.NATURAL) PazZombie.Companion.spawnZombieGroup(entity, LEADER_GROUP_SIZE);
    }
}
