package joshxviii.plantz.entity.zombie

import joshxviii.plantz.PazSounds
import joshxviii.plantz.ai.pathfinding.PazFlyingPathNavigation
import net.minecraft.core.BlockPos
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.phys.Vec3
import java.util.*

class PirateCaptainGhost(type: EntityType<out PirateCaptainGhost>, level: Level) : PazZombie(type, level) {

    companion object {
        val IS_CHARGING_ID: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(PirateCaptainGhost::class.java, EntityDataSerializers.BOOLEAN)
    }

    init {
        xpReward = 30
    }

    override fun getAmbientSound(): SoundEvent {
        return PazSounds.PIRATE_CAPTAIN_GHOST_AMBIENT
    }
    override fun getHurtSound(source: DamageSource): SoundEvent {
        return PazSounds.PIRATE_CAPTAIN_GHOST_HURT
    }
    override fun getDeathSound(): SoundEvent {
        return SoundEvents.EMPTY
    }
    override fun getStepSound(): SoundEvent {
        return SoundEvents.EMPTY
    }

    override fun registerGoals() {
        super.registerGoals()
    }

    var isCharging: Boolean
        get() = this.entityData.get(IS_CHARGING_ID)
        set(value) = this.entityData.set(IS_CHARGING_ID, value)

    override fun defineSynchedData(entityData: SynchedEntityData.Builder) {
        super.defineSynchedData(entityData)
        entityData.define(IS_CHARGING_ID, false)
    }

    override fun handleAttributes(difficultyModifier: Float) {}

    override fun doHurtTarget(target: Entity): Boolean {
        val result = super.doHurtTarget(target)
        return result
    }

    override fun canEquipDuckyInWater(): Boolean = false
    override fun canEquipBalloons(): Boolean = false
    override fun canPickUpLoot(): Boolean = false
    override fun isLeftHanded(): Boolean = false
    override fun hasLineOfSight(target: Entity): Boolean = true

    override fun tick() {
        noPhysics = true
        super.tick()
        noPhysics = false
        applyEffectsFromBlocks()
    }

    // 26.1 isAffectedByBlocks() = !isRemoved keeps block effects on while noPhysics; 1.21.1 skips them in move(), so
    // this mirrors 26.1 Entity.applyEffectsFromBlocks (run server side, like 26.1 LivingEntity.aiStep)
    private fun applyEffectsFromBlocks() {
        if (isRemoved || level().isClientSide) return
        if (onGround()) {
            @Suppress("DEPRECATION") // same position vanilla stepOn uses
            val effectPos = onPosLegacy
            val effectState = level().getBlockState(effectPos)
            effectState.block.stepOn(level(), effectPos, effectState, this)
        }

        val wasOnFire = isOnFire
        val wasFreezing = ticksFrozen > 0
        val previousRemainingFireTicks = remainingFireTicks
        tryCheckInsideBlocks()
        val isInRain = level().isRainingAt(blockPosition()) || level().isRainingAt(BlockPos.containing(x, boundingBox.maxY, z))
        if (isInRain) clearFire()

        if (wasOnFire && !isOnFire || wasFreezing && ticksFrozen <= 0) playEntityOnFireExtinguishedSound()

        val wasIgnitedThisTick = remainingFireTicks > previousRemainingFireTicks
        if (!isOnFire && !wasIgnitedThisTick) remainingFireTicks = -fireImmuneTicks
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnReason: MobSpawnType,
        groupData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, spawnReason, groupData)

        setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_SWORD.defaultInstance)
        setDropChance(EquipmentSlot.MAINHAND, 0.0f)

        isFlying = true
        (navigation as? PazFlyingPathNavigation)?.passThroughBlocks = true

        return data
    }
}