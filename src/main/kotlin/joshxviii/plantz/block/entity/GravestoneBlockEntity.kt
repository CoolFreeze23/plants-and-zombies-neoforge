package joshxviii.plantz.block.entity

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazServerParticles
import joshxviii.plantz.PazSounds
import joshxviii.plantz.PazTags
import joshxviii.plantz.entity.zombie.PazZombie.Companion.findSpawnPosition
import joshxviii.plantz.entity.zombie.PazZombie.Companion.rollZombieType
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import joshxviii.plantz.compat.ValueInput
import joshxviii.plantz.compat.ValueOutput
import net.minecraft.world.phys.AABB
import java.util.UUID
import kotlin.jvm.optionals.getOrDefault

class GravestoneBlockEntity(
    worldPosition: BlockPos,
    blockState: BlockState,
) : BlockEntity(
    PazBlocks.GRAVESTONE_BLOCK_ENTITY, worldPosition, blockState
) {
    companion object {
        private const val SPAWN_DELAY_MIN = 400
        private const val SPAWN_DELAY_MAX = 600
        private const val MAX_ZOMBIES = 4

        fun tick(level: Level, pos: BlockPos, state: BlockState, blockEntity: GravestoneBlockEntity) {
            if (level.isClientSide || level !is ServerLevel) return
            blockEntity.updateTrackedZombies(level)
            if (blockEntity.ticksSinceLastSpawn++ >= blockEntity.spawnDelay) blockEntity.trySpawnZombie(level, pos)
        }
    }

    private var spawnDelay = SPAWN_DELAY_MIN
    private var ticksSinceLastSpawn = 0
    private val trackedZombieIds = mutableSetOf<UUID>()

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        val output = ValueOutput.of(tag, registries)
        output.store("tracked_zombies", UUIDUtil.CODEC_SET, trackedZombieIds)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        val input = ValueInput.of(tag, registries)
        trackedZombieIds.clear()
        trackedZombieIds.addAll(input.read("tracked_zombies", UUIDUtil.CODEC_SET).getOrDefault(emptySet()))
    }

    private fun updateTrackedZombies(level: ServerLevel) {
        if (trackedZombieIds.removeIf { uuid -> level.getEntity(uuid)?.let { !it.isAlive } == true }) {
            setChanged()
        }
    }

    private fun isDarkEnough(level: ServerLevel): Boolean {
        return !(level.isDay && level.getBrightness(LightLayer.SKY, blockPos) >= 7)
    }

    private fun canSpawn(level: ServerLevel, pos: BlockPos): Boolean {
        if ( !isDarkEnough(level) && !level.getBiome(pos).`is`(PazTags.Biomes.GRAVESTONE_IGNORE_BRIGHTNESS) ) return false

        val nearbyPlayer = level.getNearestPlayer(
            pos.center.x,
            pos.center.y,
            pos.center.z,
            PazConfig.GRAVESTONE_ACTIVATION_RANGE,
            true
        )
        if (nearbyPlayer == null) return false

        val aabb = AABB.ofSize(pos.center, 32.0, 16.0, 32.0)
        val nearbyZombies = level.getEntitiesOfClass(Zombie::class.java, aabb) { it.isAlive }

        return trackedZombieIds.size < MAX_ZOMBIES && nearbyZombies.size < MAX_ZOMBIES
    }

    private fun trySpawnZombie(level: ServerLevel, pos: BlockPos) {
        if (!canSpawn(level, pos)) return

        val type = rollZombieType(level.random) ?: return
        val spawnPos = findSpawnPosition(level, pos) ?: return

        val zombie = type.create(
            level,
            null,
            spawnPos,
            MobSpawnType.REINFORCEMENT,
            true,
            false
        ) ?: return
        zombie.setPersistenceRequired()

        if (!level.addFreshEntity(zombie)) return
        trackedZombieIds.add(zombie.uuid)
        setChanged()

        spawnDelay = Mth.randomBetweenInclusive(level.random, SPAWN_DELAY_MIN, SPAWN_DELAY_MAX)
        ticksSinceLastSpawn = 0
        level.sendParticles(
            PazServerParticles.ZOMBIE_OMEN,
            pos.center.x,
            pos.center.y,
            pos.center.z,
            4, 0.25, 0.125, 0.25, 0.0
        )
        level.sendParticles(
            PazServerParticles.ZOMBIE_OMEN,
            spawnPos.center.x,
            spawnPos.center.y,
            spawnPos.center.z,
            3, 0.15, 0.0, 0.15, 0.0
        )
        level.playSound(null, pos, PazSounds.APPLY_ZOMBIE_OMEN, SoundSource.BLOCKS, 0.75f, 2.0f)
    }
}