package joshxviii.plantz.entity.zombie

import joshxviii.plantz.PazSounds
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor

class BackupDancer(type: EntityType<out BackupDancer>, level: Level) : PazZombie(type, level) {

    override fun getAmbientSound(): SoundEvent {
        return PazSounds.BACKUP_DANCER_AMBIENT
    }
    override fun getHurtSound(source: DamageSource): SoundEvent {
        return PazSounds.BACKUP_DANCER_HURT
    }
    override fun getDeathSound(): SoundEvent {
        return PazSounds.BACKUP_DANCER_DEATH
    }
    override fun getStepSound(): SoundEvent {
        return SoundEvents.ZOMBIE_STEP
    }

    override fun handleAttributes(difficultyModifier: Float) {}

    override fun doHurtTarget(target: Entity): Boolean {
        val result = super.doHurtTarget(target)
        return result
    }

    override fun randomizeReinforcementsChance() {}

    override fun tick() {
        super.tick()
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnReason: MobSpawnType,
        groupData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, spawnReason, groupData)
        setCanBreakDoors(false)

        return data
    }
}