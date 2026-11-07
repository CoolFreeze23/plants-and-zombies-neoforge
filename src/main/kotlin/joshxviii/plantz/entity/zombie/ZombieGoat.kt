package joshxviii.plantz.entity.zombie
import net.minecraft.nbt.CompoundTag
import joshxviii.plantz.PazTags
import joshxviii.plantz.raid.setIgnoreFlag
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import joshxviii.plantz.compat.ValueInput
import joshxviii.plantz.compat.ValueOutput
import java.util.*

class ZombieGoat(type: EntityType<out ZombieGoat>, level: Level) : PazZombie(type, level) {

    companion object {
    }

    init {
        xpReward = 0
    }

    override fun getAmbientSound(): SoundEvent {
        return SoundEvents.GOAT_AMBIENT
    }
    override fun getHurtSound(source: DamageSource): SoundEvent {
        return SoundEvents.GOAT_HURT
    }
    override fun getDeathSound(): SoundEvent {
        return SoundEvents.GOAT_DEATH
    }
    override fun getStepSound(): SoundEvent {
        return SoundEvents.GOAT_STEP
    }

    override fun defineSynchedData(entityData: SynchedEntityData.Builder) {
        super.defineSynchedData(entityData)
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        val output = ValueOutput.of(tag, registryAccess())
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        val input = ValueInput.of(tag, registryAccess())
    }

    override fun registerGoals() {
        super.registerGoals()
    }

    override fun updateControlFlags() {
        val noController = this.controllingPassenger !is Mob || this.controllingPassenger?.type?.`is`(PazTags.EntityTypes.ZOMBIE_RAIDERS) == true
        val notInBoat = this.vehicle !is Boat
        this.goalSelector.setControlFlag(Goal.Flag.MOVE, noController)
        this.goalSelector.setControlFlag(Goal.Flag.JUMP, noController && notInBoat)
        this.goalSelector.setControlFlag(Goal.Flag.LOOK, noController)
        this.goalSelector.setControlFlag(Goal.Flag.TARGET, noController)
    }

    override fun handleAttributes(difficultyModifier: Float) {}

    override fun canEquipBalloons(): Boolean = false
    override fun canEquipDuckyInWater(): Boolean = false
    override fun canPickUpLoot(): Boolean = false
    override fun canBreakDoors(): Boolean = false
    override fun canHoldItem(itemStack: ItemStack): Boolean = false

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnReason: MobSpawnType,
        groupData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, spawnReason, groupData)
        this.setIgnoreFlag(true)
        return data
    }
}