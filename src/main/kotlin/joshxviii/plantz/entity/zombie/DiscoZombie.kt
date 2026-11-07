package joshxviii.plantz.entity.zombie

import net.minecraft.nbt.CompoundTag
import joshxviii.plantz.compat.AnimationState
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazSounds
import joshxviii.plantz.ai.goal.SummonMinionsGoal
import net.minecraft.core.UUIDUtil
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import joshxviii.plantz.compat.ValueInput
import joshxviii.plantz.compat.ValueOutput
import java.util.UUID
import kotlin.jvm.optionals.getOrDefault

class DiscoZombie(type: EntityType<out DiscoZombie>, level: Level) : PazZombie(type, level) {

    companion object {
        val SUMMON_TIME_ID: EntityDataAccessor<Int> =
            SynchedEntityData.defineId<Int>(DiscoZombie::class.java, EntityDataSerializers.INT)
    }

    init {
        xpReward = 12
    }

    val summonAnimation: AnimationState = AnimationState()
    var summoningTime: Int
        get() = this.entityData.get(SUMMON_TIME_ID)
        set(value) = this.entityData.set(SUMMON_TIME_ID, value)
    private val trackedBackupDancerIds = mutableSetOf<UUID>()

    override fun defineSynchedData(entityData: SynchedEntityData.Builder) {
        super.defineSynchedData(entityData)
        entityData.define(SUMMON_TIME_ID, 0)
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        val output = ValueOutput.of(tag, registryAccess())
        output.store("tracked_backup", UUIDUtil.CODEC_SET, trackedBackupDancerIds)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        val input = ValueInput.of(tag, registryAccess())
        trackedBackupDancerIds.clear()
        trackedBackupDancerIds.addAll(input.read("tracked_backup", UUIDUtil.CODEC_SET).getOrDefault(emptySet()))
    }

    override fun getMoveControl(): MoveControl = if (summoningTime > 0) noMoveControl else super.getMoveControl()

    override fun tick() {
        super.tick()
        if (summoningTime > 0) {
            summonAnimation.startIfStopped(tickCount)
            summoningTime++
        }
        if (summoningTime == 10) playSound(PazSounds.DISCO_ZOMBIE_BOOGIE, 1.0f, 1.0f)
        if (summoningTime == 20) playSound(PazSounds.DISCO_ZOMBIE_SUMMON, 1.0f, 1.0f)
        if (summoningTime > 40) {
            summonAnimation.stop()
            summoningTime = 0
        }
    }

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(
            1,
            SummonMinionsGoal(
                this,
                PazEntities.BACKUP_DANCER,
                trackedBackupDancerIds,
                isSummoning = { summoningTime > 0 },
                beginSummoning = { summoningTime = 1 },
                shouldAddToRaid = false
            )
        )
    }

    override fun getAmbientSound(): SoundEvent {
        return PazSounds.DISCO_ZOMBIE_AMBIENT
    }

    override fun getHurtSound(source: DamageSource): SoundEvent {
        return PazSounds.DISCO_ZOMBIE_HURT
    }

    override fun getDeathSound(): SoundEvent {
        return PazSounds.DISCO_ZOMBIE_DEATH
    }

    override fun getStepSound(): SoundEvent {
        return SoundEvents.ZOMBIE_STEP
    }

    override fun doHurtTarget(target: Entity): Boolean {
        val result = super.doHurtTarget(target)
        return result
    }

    override fun randomizeReinforcementsChance() {}

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnReason: MobSpawnType,
        groupData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, spawnReason, groupData)
        if (spawnReason != MobSpawnType.CONVERSION) setCanBreakDoors(true)
        return data
    }

}