package joshxviii.plantz.entity.zombie

import net.minecraft.world.item.DyeColor
import net.minecraft.nbt.CompoundTag
import joshxviii.plantz.PazDataSerializers.BROWN_COAT_VARIANT
import joshxviii.plantz.PazTags
import joshxviii.plantz.entity.ZombieEquipment.equipZombieEquipment
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.StructureTags
import net.minecraft.util.RandomSource
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.entity.*
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import joshxviii.plantz.compat.ValueInput
import joshxviii.plantz.compat.ValueOutput
import kotlin.jvm.optionals.getOrDefault

class BrownCoat(type: EntityType<out BrownCoat>, level: Level) : PazZombie(type, level) {
    companion object {
        val DATA_VARIANT_ID: EntityDataAccessor<BrownCoatVariant> = SynchedEntityData.defineId(BrownCoat::class.java, BROWN_COAT_VARIANT)
    }

    init {
        xpReward = 3
    }

    var variant: BrownCoatVariant
        get() = this.entityData.get(DATA_VARIANT_ID)
        set(value) = this.entityData.set(DATA_VARIANT_ID, value)

    override fun defineSynchedData(entityData: SynchedEntityData.Builder) {
        super.defineSynchedData(entityData)
        entityData.define(DATA_VARIANT_ID, BrownCoatVariant.getDefault())
    }

    override fun doHurtTarget(target: Entity): Boolean {
        val result = super.doHurtTarget(target)
        return result
    }

    override fun isBaby(): Boolean = isBabyZombie()
    override fun populateDefaultEquipmentSlots(random: RandomSource, difficulty: DifficultyInstance) {
        randomEquip(random, difficulty)
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        val output = ValueOutput.of(tag, registryAccess())
        output.store("variant", BrownCoatVariant.CODEC, variant)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        val input = ValueInput.of(tag, registryAccess())
        variant = input.read<BrownCoatVariant>("variant", BrownCoatVariant.CODEC).getOrDefault(BrownCoatVariant.getDefault())
    }

    override fun canFreeze(): Boolean {
        return if (variant == BrownCoatVariant.SNOWCOAT) false
        else super.canFreeze()
    }

    fun spawnWithBalloons() {
        var balloonCount = random.nextIntBetweenInclusive(2,3)
        if (variant == BrownCoatVariant.BROWNCOAT && random.nextFloat() < 0.33) {
            variant = BrownCoatVariant.BALLOON
            balloonCount+=2
        }
        val balloonColors = when (variant) {
            BrownCoatVariant.DESERT -> listOf(DyeColor.ORANGE, DyeColor.BROWN)
            BrownCoatVariant.SNOWCOAT -> listOf(DyeColor.LIGHT_BLUE, DyeColor.WHITE)
            BrownCoatVariant.BUCCANEER -> listOf(DyeColor.BLACK, DyeColor.RED)
            else -> listOf(DyeColor.RED, DyeColor.YELLOW, DyeColor.BLUE)
        }
        spawnBalloons(balloonCount, balloonColors)
    }

    override fun onSpawnedFromItem() {
        if (variant == BrownCoatVariant.BALLOON) spawnWithBalloons()
    }

    override fun getTypeName(): Component {
        return if (variant != BrownCoatVariant.BROWNCOAT) Component.translatable("entity.plantz.browncoat.variant.${variant.name.lowercase()}")
        else super.getTypeName()
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnReason: MobSpawnType,
        groupData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, spawnReason, groupData)
        if (spawnReason == MobSpawnType.SPAWN_EGG) return data
        val random = level.random
        setCanBreakDoors(true)
        val structureManager = (level as ServerLevel).structureManager()
        val isShipwreckSpawn = structureManager.getStructureWithPieceAt(blockPosition(), StructureTags.SHIPWRECK).isValid
        variant = BrownCoatVariant.pickForBiome(
            level.getBiome(blockPosition()).`is`(PazTags.Biomes.HAS_BROWNCOAT_SNOW),
            level.getBiome(blockPosition()).`is`(PazTags.Biomes.HAS_BROWNCOAT_DESERT),
            level.getBiome(blockPosition()).`is`(PazTags.Biomes.HAS_BROWNCOAT_BEACH),
            isShipwreckSpawn,
            random
        )

        if (spawnReason != MobSpawnType.EVENT) equipZombieEquipment(random, difficulty.specialMultiplier + 0.1f)

        val spawnBalloons = (spawnReason != MobSpawnType.EVENT && spawnReason != MobSpawnType.REINFORCEMENT && y > level.seaLevel && random.nextFloat() < 0.02)
        if (spawnBalloons) spawnWithBalloons()

        return data
    }


}
