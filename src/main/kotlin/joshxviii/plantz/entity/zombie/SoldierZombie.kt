package joshxviii.plantz.entity.zombie

import net.minecraft.nbt.CompoundTag
import joshxviii.plantz.PazDataSerializers.DATA_DYE_COLOR
import joshxviii.plantz.PazItems
import joshxviii.plantz.PazSounds
import joshxviii.plantz.ai.goal.NavigateToTargetGoal
import joshxviii.plantz.ai.goal.ProjectileAttackGoal
import joshxviii.plantz.entity.projectile.PaintBall
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.entity.*
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import joshxviii.plantz.compat.ValueInput
import joshxviii.plantz.compat.ValueOutput

class SoldierZombie(type: EntityType<out SoldierZombie>, level: Level) : PazZombie(type, level) {

    companion object {
        val potentialDyes = listOf(
            DyeColor.GREEN,
            DyeColor.PURPLE,
            DyeColor.ORANGE,
            DyeColor.BLUE,
        )

        val DYE_COLOR: EntityDataAccessor<DyeColor> = SynchedEntityData.defineId<DyeColor>(SoldierZombie::class.java, DATA_DYE_COLOR)
    }

    init {
        xpReward = 10
    }

    var dyeColor: DyeColor
        get() = this.entityData.get(DYE_COLOR)
        set(value) = this.entityData.set(DYE_COLOR, value)

    override fun defineSynchedData(entityData: SynchedEntityData.Builder) {
        super.defineSynchedData(entityData)
        entityData.define(DYE_COLOR, DyeColor.WHITE)
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        val output = ValueOutput.of(tag, registryAccess())
        output.store("dyeColor", DyeColor.CODEC, dyeColor)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        val input = ValueInput.of(tag, registryAccess())
        input.read("dyeColor", DyeColor.CODEC).ifPresent { dyeColor -> this.dyeColor = dyeColor }
    }

    override fun registerGoals() {
        super.registerGoals()
        this.goalSelector.addGoal(3, NavigateToTargetGoal(this, keepAwayDistance = 7.0, alwaysFaceTarget = true))
        this.goalSelector.addGoal(4, ProjectileAttackGoal(
            usingEntity = this,
            projectileFactory =  { PaintBall(level(), this, color = dyeColor, damage = 1f) },
            velocity = 1.1,
            actionDelay = 40,
            soundEvent = PazSounds.DYE_BLASTER,
            actionEndEffect = {

            }))
    }

    override fun addBehaviourGoals() {
        addBehaviourGoalsNoMelee()
    }

    override fun doHurtTarget(target: Entity): Boolean {
        val result = super.doHurtTarget(target)
        return result
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnReason: MobSpawnType,
        groupData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, spawnReason, groupData)

        setItemSlot(EquipmentSlot.MAINHAND, PazItems.DYE_BLASTER.defaultInstance)
        dyeColor = potentialDyes.random()
        setDropChance(EquipmentSlot.MAINHAND, 0.0f)
        if (spawnReason != MobSpawnType.CONVERSION) {
            setCanBreakDoors(true)
        }

        return data
    }
}