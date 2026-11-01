package joshxviii.plantz.entity.zombie

import joshxviii.plantz.*
import joshxviii.plantz.PazDataSerializers.DATA_ZOMBIE_STATE
import joshxviii.plantz.ai.ZombieState
import joshxviii.plantz.ai.control.PazFlyingMoveControl
import joshxviii.plantz.ai.control.PazMoveControl
import joshxviii.plantz.ai.goal.FlyingPathfindingGoal
import joshxviii.plantz.compat.ai.SpearUseGoal
import joshxviii.plantz.ai.pathfinding.PazFlyingPathNavigation
import joshxviii.plantz.ai.pathfinding.PazGroundPathNavigation
import joshxviii.plantz.entity.Balloon
import joshxviii.plantz.integration.PazSpearsCompat
import joshxviii.plantz.entity.ZombieRaider
import joshxviii.plantz.entity.setCountHealth
import joshxviii.plantz.item.BalloonItem
import joshxviii.plantz.raid.BossType
import joshxviii.plantz.raid.ZombieRaid.Companion.RAID_DISTANCE
import joshxviii.plantz.raid.getZombieRaids
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.Difficulty
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeInstance
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.FlyingMoveControl
import net.minecraft.world.entity.ai.control.LookControl
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.animal.Turtle
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.monster.ZombifiedPiglin
import net.minecraft.world.entity.npc.AbstractVillager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.material.Fluids
import joshxviii.plantz.compat.AnimationState
import joshxviii.plantz.compat.ValueInput
import joshxviii.plantz.compat.ValueOutput
import net.minecraft.world.phys.Vec3
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.neoforged.neoforge.common.NeoForgeMod
import java.util.*
import kotlin.collections.getValue
import kotlin.jvm.optionals.getOrElse
import kotlin.math.max

abstract class PazZombie(type: EntityType<out PazZombie>, level: Level) : Zombie(type, level) {

    val emergeAnimation : AnimationState = AnimationState()
    val retreatAnimation : AnimationState = AnimationState()
    val floatAnimation : AnimationState = AnimationState()
    var moveDir = Vec3.ZERO
    var moveDirO = Vec3.ZERO

    companion object {
        fun checkPazZombieSpawnRules(
            type: EntityType<out Mob>,
            level: ServerLevelAccessor,
            spawnReason: MobSpawnType,
            pos: BlockPos,
            random: RandomSource
        ): Boolean {
            if (level.difficulty == Difficulty.PEACEFUL) return false

            val biome = level.getBiome(pos)
            val isRaining = level.level.isRaining
            val inWater = level.getFluidState(pos).`is`(FluidTags.WATER)

            // light / day requirements
            val canSpawn = (inWater && isRaining) || MobSpawnType.ignoresLightRequirements(spawnReason) || biome.`is`(PazTags.Biomes.DAY_SPAWNS) || isDarkEnoughToSpawn(level, pos, random)
            if (!canSpawn) return false

            // water spawning
            if (inWater) {
                val rainBonus = if (isRaining) 2.75f else 1.25f
                val spawnChance = if (biome.`is`(PazTags.Biomes.WATER_SPAWNS)) 0.085f else 0.015f
                return MobSpawnType.isSpawner(spawnReason) ||
                        (random.nextFloat() < (spawnChance * rainBonus) && pos.y > seaLevel(level) - 3)
            }

            // land spawning
            return checkMobSpawnRules(type, level, spawnReason, pos, random)
        }

        fun spawnZombieGroup(leader: Zombie, minAmount: Int = 3) {
            val level = leader.level() as? ServerLevel?: return
            var amount = minAmount
            val multi = level.getCurrentDifficultyAt(leader.blockPosition()).specialMultiplier
            if(multi > 0.0) repeat(3) {
                if(leader.random.nextFloat() < 0.3 * multi) amount++
            }

            for (i in 0..amount) {
                val spawnPos = findSpawnPosition(level, leader.blockPosition()) ?: return
                val type = rollZombieType(level.random) ?: return

                val zombie = type.create(
                    level,
                    null,
                    spawnPos,
                    MobSpawnType.REINFORCEMENT,
                    true,
                    false
                ) ?: return
                zombie.setPersistenceRequired()

                level.addFreshEntity(zombie)
            }
        }

        fun findSpawnPosition(level: ServerLevel, pos: BlockPos): BlockPos? {
            val random = level.random
            for (i in 0..7) {
                val x = pos.x + Mth.randomBetweenInclusive(random, -3, 3)
                val z = pos.z + Mth.randomBetweenInclusive(random, -3, 3)
                val pos = BlockPos(x, pos.y, z)

                var spawnY = pos.y
                while (spawnY > level.minBuildHeight && level.isEmptyBlock(pos.atY(spawnY - 1))) spawnY--
                while (spawnY < level.maxBuildHeight - 1 && !level.isEmptyBlock(pos.atY(spawnY))) spawnY++

                val finalPos = BlockPos(x, spawnY, z)

                if (level.isEmptyBlock(finalPos) && level.isEmptyBlock(finalPos.above())) return finalPos
            }
            return null
        }

        fun rollZombieType(random: RandomSource): EntityType<out Zombie>? {
            var roll = random.nextInt(SPAWN_TABLE_WEIGHTS.values.sum())
            for ((type, weight) in SPAWN_TABLE_WEIGHTS) {
                if (roll < weight) return type
                roll -= weight
            }
            return null
        }

        /** 1.21.1 LevelReader.seaLevel is always 63; 26.1 reads the dimension's generator. */
        fun seaLevel(level: ServerLevelAccessor): Int = level.level.chunkSource.generator.seaLevel

        fun isBornLeader(zombie: Zombie): Boolean {
            return Objects.requireNonNull<AttributeInstance>(zombie.getAttribute(Attributes.MAX_HEALTH)).hasModifier(ResourceLocation.withDefaultNamespace(LEADER_MODIFIER_ID))
        }

        // for group spawning and gravestone spawning
        val SPAWN_TABLE_WEIGHTS = mapOf(
            PazEntities.BROWN_COAT          to 40,
            PazEntities.NEWSPAPER_ZOMBIE    to 8,
            PazEntities.DIGGER_ZOMBIE       to 2,
            PazEntities.DISCO_ZOMBIE        to 2,
            PazEntities.ALL_STAR            to 1,
        )

        const val ZOMBIE_SPEED = 0.23
        const val MAX_EQUIPPABLE_BALLOONS = 4
        const val LEADER_MODIFIER_ID: String = "leader_zombie_bonus"
        private val REINFORCEMENT_CALLER_CHARGE_ID = ResourceLocation.withDefaultNamespace("reinforcement_caller_charge")
        private val ZOMBIE_REINFORCEMENT_CALLEE_CHARGE = AttributeModifier(
            ResourceLocation.withDefaultNamespace("reinforcement_callee_charge"), -0.05f.toDouble(), AttributeModifier.Operation.ADD_VALUE
        )

        data class PazZombieAttributes(
            val maxHealth: Double = 20.0,
            val attackDamage: Double = 1.5,
            val attackKnockback: Double = 0.4,
            val attackRange: Double = 1.5,
            val movementSpeed: Double = ZOMBIE_SPEED,
            val flyingSpeed: Double = ZOMBIE_SPEED,
            val jumpStrength: Double = 0.42,
            val followRange: Double = 50.0,
            val armor: Double = 0.0,
            val armorToughness: Double = 0.0,
            val spawnReinforcementsChance: Double = 0.0,
            val explosionKnockbackResistance: Double = 0.0,
            val knockbackResistance: Double = 0.2,
            val stepHeight: Double = 0.6,
            val scale: Double = 1.0,
            val waterMovementEfficiency: Double = 0.1
        ) {
            fun apply(builder: AttributeSupplier.Builder): AttributeSupplier.Builder {
                return builder
                    .add(Attributes.MAX_HEALTH, maxHealth)
                    .add(Attributes.FOLLOW_RANGE, followRange)
                    .add(Attributes.ATTACK_DAMAGE, attackDamage)
                    .add(Attributes.ATTACK_KNOCKBACK, attackKnockback)
                    .add(Attributes.MOVEMENT_SPEED, movementSpeed)
                    .add(Attributes.JUMP_STRENGTH, jumpStrength)
                    .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, spawnReinforcementsChance)
                    .add(Attributes.ARMOR, armor)
                    .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, explosionKnockbackResistance)
                    .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance)
                    .add(Attributes.STEP_HEIGHT, stepHeight)
                    .add(Attributes.ENTITY_INTERACTION_RANGE, attackRange)
                    .add(Attributes.SCALE, scale)
                    .add(Attributes.FLYING_SPEED, flyingSpeed)
                    .add(Attributes.WATER_MOVEMENT_EFFICIENCY, waterMovementEfficiency)
            }
        }

        val ZOMBIE_STATE: EntityDataAccessor<ZombieState> = SynchedEntityData.defineId<ZombieState>(PazZombie::class.java, DATA_ZOMBIE_STATE)
        val FLYING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId<Boolean>(PazZombie::class.java, EntityDataSerializers.BOOLEAN)
    }

    override fun getAmbientSound(): SoundEvent {
        return PazSounds.BROWNCOAT_AMBIENT
    }
    override fun getHurtSound(source: DamageSource): SoundEvent {
        return PazSounds.BROWNCOAT_HURT
    }
    override fun getDeathSound(): SoundEvent {
        return PazSounds.BROWNCOAT_DEATH
    }
    override fun getStepSound(): SoundEvent {
        return SoundEvents.ZOMBIE_STEP
    }
    override fun getVoicePitch(): Float {
        val bossified = this.getAttribute(Attributes.SCALE)!!.hasModifier(BossType.BOSSIFY)
        val pitch = super.getVoicePitch()
        return if (bossified) pitch * 0.65f else pitch
    }

    override fun onEquipItem(slot: EquipmentSlot, oldStack: ItemStack, stack: ItemStack) {
        if (stack.`is`(PazItems.DUCKY_TUBE) && slot == EquipmentSlot.LEGS) {
            this.getNavigation().setCanFloat(true)
            this.getNavigation().recomputePath()
        };
        else if (oldStack.`is`(PazItems.DUCKY_TUBE) && slot == EquipmentSlot.LEGS) {
            this.getNavigation().setCanFloat(false)
            this.getNavigation().recomputePath()
        };

        super.onEquipItem(slot, oldStack, stack)
    }

    override fun pickUpItem(entity: ItemEntity) {
        super.pickUpItem(entity)
    }

    var state: ZombieState
        get() = this.entityData.get(ZOMBIE_STATE)
        set(value) = this.entityData.set(ZOMBIE_STATE, value)

    var isFlying: Boolean
        get() = this.entityData.get(FLYING)
        set(value) {
            this.entityData.set(FLYING, value)
            updateNavigation()
        }

    protected val groundNavigation: PathNavigation = navigation
    protected val climbingNavigation: WallClimberNavigation by lazy {
        WallClimberNavigation(this, level())
    }
    protected val flyingNavigation: FlyingPathNavigation by lazy {
        val nav = PazFlyingPathNavigation(this, level())
        nav.setCanOpenDoors(false)
        nav.setCanFloat(true)
        nav
    }

    protected val groundMoveControl: MoveControl by lazy { PazMoveControl(this) }
    protected val flyingMoveControl: FlyingMoveControl by lazy { PazFlyingMoveControl(this, 20, true) }
    protected val noMoveControl = object : MoveControl(this) { override fun getSpeedModifier(): Double = 0.0 }
    protected val noLookControl = object : LookControl(this) {}

    fun updateNavigation() {
        if (isFlying) {
            if (this.navigation !== flyingNavigation) {
                this.navigation.stop()
                this.navigation = flyingNavigation
            }
            this.moveControl = flyingMoveControl
            this.isNoGravity = true
        } else {
            if (this.navigation !== if (canClimbWalls()) climbingNavigation else groundNavigation) {
                this.navigation.stop()
                this.navigation = if (canClimbWalls()) climbingNavigation else groundNavigation
            }
            this.moveControl = if (isImmobile) noMoveControl else groundMoveControl
            if (this !is ZombieRobot && this !is PirateCaptainGhost) {
                this.isNoGravity = false
            }
        }
    }

    override fun onSyncedDataUpdated(accessor: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(accessor)
        if (accessor == FLYING) updateNavigation()
    }

    override fun createNavigation(level: Level): PathNavigation {
        return PazGroundPathNavigation(this, level)
    }

    override fun getMoveControl(): MoveControl {
        if (isImmobile) return noMoveControl
        if (isFlying) return flyingMoveControl
        return super.getMoveControl()
    }

    override fun getLookControl(): LookControl {
        if (isImmobile) return noLookControl
        return super.getLookControl()
    }

    override fun registerGoals() {
        super.registerGoals()
        PazSpearsCompat.replaceModSpearGoal(this, this.goalSelector)
        this.goalSelector.addGoal(7, FlyingPathfindingGoal(this, hoverGroundHeight = 1.5))
    }

    fun addBehaviourGoalsNoMelee() {
        this.goalSelector.addGoal(2, SpearUseGoal<Zombie>(this, 1.0, 1.0, 10.0f, 2.0f)) // spears need a backport mod
        this.goalSelector.addGoal(6, MoveThroughVillageGoal(this, 1.0, true, 4) { this.canBreakDoors() })
        this.goalSelector.addGoal(7, WaterAvoidingRandomStrollGoal(this, 1.0))
        this.targetSelector.addGoal(1, HurtByTargetGoal(this).setAlertOthers(ZombifiedPiglin::class.java))
        this.targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
        this.targetSelector.addGoal(3, NearestAttackableTargetGoal(this, AbstractVillager::class.java, false))
        this.targetSelector.addGoal(3, NearestAttackableTargetGoal(this, IronGolem::class.java, true))
        this.targetSelector.addGoal(5, NearestAttackableTargetGoal(this, Turtle::class.java, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR))
    }

    var waterTime = -1
    var retreatTime = -1
    var emergeTime = -1
    override fun tick() {
        super.tick()
        moveDirO = moveDir
        val delta = deltaMovement

        val serverLevel = level() as? ServerLevel

        if (canEquipBalloons()) {
            isFlying = getHeldBalloons().isNotEmpty()
        }

        if (isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
            waterTime++
            if (waterTime>=250 && canEquipDuckyInWater()) {
                if(!getItemBySlot(EquipmentSlot.LEGS).isEmpty) {
                    val currentLegs = getItemBySlot(EquipmentSlot.LEGS)
                    val dropChance = getEquipmentDropChance(EquipmentSlot.LEGS)
                    if (!currentLegs.isEmpty && max(this.random.nextFloat() - 0.1f, 0.0f).toDouble() < dropChance) {
                        if (serverLevel!=null) spawnAtLocation(currentLegs)
                    }
                }
                setItemSlot(EquipmentSlot.LEGS, PazItems.DUCKY_TUBE.defaultInstance)
                setDropChance(EquipmentSlot.LEGS, 0.0f)
            }
        } else waterTime = -1

        if (state != ZombieState.RETREATING) retreatTime = -1
        when (state) {
            ZombieState.IDLE -> {
                emergeAnimation.stop()
                retreatAnimation.stop()
                floatAnimation.stop()
                if (isFlying) state = ZombieState.FLYING
            }
            ZombieState.FLYING -> {
                floatAnimation.startIfStopped(tickCount)
                if (!isFlying) state = ZombieState.IDLE
            }
            ZombieState.EMERGING -> {
                emergeAnimation.startIfStopped(tickCount)
                retreatAnimation.stop()
                floatAnimation.stop()
                if (emergeTime++ > emergingTime()) {
                    state = ZombieState.IDLE
                    emergeTime = -1
                }

                if (emergeTime < 15) {
                    if(emergeTime%8==0) playSound(SoundEvents.ROOTED_DIRT_HIT, 1.0f, 0.2f)
                    serverLevel?.sendParticles(
                        BlockParticleOption(ParticleTypes.BLOCK, serverLevel.getBlockState(blockPosition().below())),
                        x, y + 0.05, z, 8, 0.25, 0.0, 0.25, 0.4
                    )
                }
            }
            ZombieState.RETREATING -> {
                retreatAnimation.startIfStopped(tickCount)
                emergeAnimation.stop()
                floatAnimation.stop()
                if (retreatTime++ > 28) {
                    serverLevel?.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 10, 0.5, 0.5, 0.5, 0.02)
                    discard()
                }

                if(retreatTime%8==0) playSound(SoundEvents.ROOTED_DIRT_HIT, 1.0f, 0.2f)
                serverLevel?.sendParticles(
                    BlockParticleOption(ParticleTypes.BLOCK, serverLevel.getBlockState(blockPosition().below())),
                    x, y + 0.05, z, 4, 0.25, 0.0, 0.25, 0.4
                )
            }
        }
        moveDir = moveDir.add(delta)
    }

    fun spawnBalloons(count: Int = 2, color: DyeColor = DyeColor.RED) {
        spawnBalloons(count, listOf(color))
    }

    fun spawnBalloons(count: Int, color: List<DyeColor>) {
        val level = level() as? ServerLevel ?: return
        for (i in 0 until count) {
            val balloon = PazEntities.BALLOON.create(level) ?: return
            balloon.dyeColor = color.random()
            val randomX = (random.nextDouble() - 0.5) * 2 + x
            val randomZ = (random.nextDouble() - 0.5) * 2 + z
            balloon.moveTo(randomX, eyeY + 1.0, randomZ)
            level.addFreshEntity(balloon)
            balloon.setLeashedTo(this, true)
        }
    }

    /**
     * 26.1 `convertTo(type, ConversionParams.single(this, keepEquipment, preserveCanPickUpLoot), afterConversion)`.
     * 1.21.1 convertTo copies much less state and adds the new mob before the caller can configure it.
     */
    fun <T : PazZombie> convertToSingle(type: EntityType<T>, keepEquipment: Boolean, preserveCanPickUpLoot: Boolean, afterConversion: (T) -> Unit): T? {
        if (isRemoved) return null
        val converted = type.create(level()) ?: return null
        copyStateTo(converted, keepEquipment, preserveCanPickUpLoot)
        afterConversion(converted)
        (level() as? ServerLevel)?.addFreshEntity(converted)
        discard()
        return converted
    }

    // 26.1 ConversionType.SINGLE.convert (a PazZombie parameter, so protected members are reachable on it)
    private fun copyStateTo(converted: PazZombie, keepEquipment: Boolean, preserveCanPickUpLoot: Boolean) {
        val rootPassenger = firstPassenger
        converted.copyPosition(this)
        converted.deltaMovement = deltaMovement
        if (rootPassenger != null) {
            rootPassenger.stopRiding()
            for (passenger in converted.passengers) {
                passenger.stopRiding()
                passenger.remove(RemovalReason.DISCARDED)
            }
            rootPassenger.startRiding(converted, true)
        }
        vehicle?.let {
            stopRiding()
            converted.startRiding(it, false)
        }
        if (keepEquipment) for (slot in EquipmentSlot.entries) {
            val itemStack = getItemBySlot(slot)
            if (!itemStack.isEmpty) {
                converted.setItemSlot(slot, itemStack.copyAndClear())
                converted.setDropChance(slot, getEquipmentDropChance(slot))
            }
        }
        converted.fallDistance = fallDistance
        converted.setSharedFlag(FLAG_FALL_FLYING, isFallFlying)
        converted.lastHurtByPlayerTime = lastHurtByPlayerTime
        converted.hurtTime = hurtTime
        converted.yBodyRot = yBodyRot
        converted.setOnGround(onGround())
        sleepingPos.ifPresent(converted::setSleepingPos)
        leashHolder?.let { converted.setLeashedTo(it, true) }

        converted.absorptionAmount = absorptionAmount
        for (effect in activeEffects) converted.addEffect(MobEffectInstance(effect))
        if (isBaby) converted.isBaby = true
        if (preserveCanPickUpLoot) converted.setCanPickUpLoot(canPickUpLoot())
        converted.isLeftHanded = isLeftHanded
        converted.isNoAi = isNoAi
        if (isPersistenceRequired) converted.setPersistenceRequired()
        converted.isCustomNameVisible = isCustomNameVisible
        converted.setSharedFlagOnFire(isOnFire)
        converted.isInvulnerable = isInvulnerable
        converted.isNoGravity = isNoGravity
        converted.portalCooldown = portalCooldown
        converted.isSilent = isSilent
        tags.forEach(converted::addTag)
        converted.customName = customName
        team?.let {
            level().scoreboard.addPlayerToTeam(converted.stringUUID, it)
            level().scoreboard.removePlayerFromTeam(stringUUID, it)
        }
        if (canBreakDoors()) converted.setCanBreakDoors(true)
    }

    override fun isImmobile(): Boolean = state==ZombieState.EMERGING || state==ZombieState.RETREATING || super.isImmobile()
    override fun isWithinMeleeAttackRange(target: LivingEntity): Boolean = !isImmobile && super.isWithinMeleeAttackRange(target)

    override fun travel(travelVector: Vec3) {
        if (isFlying && this.isEffectiveAi) {
            val flyingSpeed = attributes.getValue(Attributes.FLYING_SPEED).toFloat() * this.speed * 0.25f
            if (this.isInWater) {
                this.moveRelative(flyingSpeed * 0.8f, travelVector)
                this.move(MoverType.SELF, this.deltaMovement)
                this.deltaMovement = this.deltaMovement.scale(0.8)
            } else if (this.isInLava) {
                this.moveRelative(flyingSpeed * 0.5f, travelVector)
                this.move(MoverType.SELF, this.deltaMovement)
                this.deltaMovement = this.deltaMovement.scale(0.5)
            } else {
                val input = if (travelVector.lengthSqr() > 1.0E-5) travelVector.normalize() else travelVector
                this.moveRelative(flyingSpeed, input)
                this.move(MoverType.SELF, this.deltaMovement)
                this.deltaMovement = this.deltaMovement.scale(0.91)
            }
            this.calculateEntityAnimation(true)
            return
        }
        super.travel(travelVector)
    }

    override fun defineSynchedData(entityData: SynchedEntityData.Builder) {
        super.defineSynchedData(entityData)
        entityData.define(ZOMBIE_STATE, ZombieState.IDLE)
        entityData.define(FLYING, false)
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        val output = ValueOutput.of(tag, registryAccess())
        output.putInt("plantz:State", state.ordinal)
        output.putBoolean("plantz:IsFlying", entityData.get(FLYING))
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        val input = ValueInput.of(tag, registryAccess())
        isFlying = input.getBooleanOr("plantz:IsFlying", false)
        state = ZombieState.entries[input.getInt("plantz:State").getOrElse { 0 }]
    }

    override fun hurt(source: DamageSource, damage: Float): Boolean {
        if (ignoreZombieDamage() && (source.`is`(PazTags.DamageTypes.IGNORED_BY_ZOMBIES) || source.isZombieFireworkExplosion()) && !(source.directEntity.isHypnotized())) return false
        muteVanillaReinforcements = true
        val wasHurt = try { super.hurt(source, damage) } finally { muteVanillaReinforcements = false }
        if (wasHurt) summonReinforcements(source)
        return wasHurt
    }

    // 1.21.1 Zombie.hurt always calls a plain Zombie as reinforcement; 26.1 calls one of the zombie's own type,
    // so the vanilla roll is muted here and redone in summonReinforcements.
    private var muteVanillaReinforcements = false

    override fun getAttributeValue(attribute: Holder<Attribute>): Double =
        if (muteVanillaReinforcements && attribute == Attributes.SPAWN_REINFORCEMENTS_CHANCE) 0.0 else super.getAttributeValue(attribute)

    private fun summonReinforcements(source: DamageSource) {
        val level = level() as? ServerLevel ?: return
        val target = target ?: source.entity as? LivingEntity
        if (target == null
            || level.difficulty != Difficulty.HARD
            || random.nextFloat() >= getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE)
            || !level.gameRules.getBoolean(GameRules.RULE_DOMOBSPAWNING)) return

        val x = Mth.floor(this.x)
        val y = Mth.floor(this.y)
        val z = Mth.floor(this.z)
        val type = this.type
        val reinforcement = type.create(level) as? Zombie ?: return

        for (i in 0..<50) {
            val xt = x + Mth.nextInt(random, 7, 40) * Mth.nextInt(random, -1, 1)
            val yt = y + Mth.nextInt(random, 7, 40) * Mth.nextInt(random, -1, 1)
            val zt = z + Mth.nextInt(random, 7, 40) * Mth.nextInt(random, -1, 1)
            val spawnPos = BlockPos(xt, yt, zt)
            if (SpawnPlacements.isSpawnPositionOk(type, level, spawnPos)
                && SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.REINFORCEMENT, spawnPos, level.random)) {
                reinforcement.setPos(xt.toDouble(), yt.toDouble(), zt.toDouble())
                if (!level.hasNearbyAlivePlayer(xt.toDouble(), yt.toDouble(), zt.toDouble(), 7.0)
                    && level.isUnobstructed(reinforcement)
                    && level.noCollision(reinforcement)
                    && ((reinforcement as? PazZombie)?.canSpawnInLiquids() == true || !level.containsAnyLiquid(reinforcement.boundingBox))) {
                    reinforcement.target = target
                    reinforcement.finalizeSpawn(level, level.getCurrentDifficultyAt(reinforcement.blockPosition()), MobSpawnType.REINFORCEMENT, null)
                    level.addFreshEntityWithPassengers(reinforcement)
                    val attribute = getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE)!!
                    val existingAmount = attribute.getModifier(REINFORCEMENT_CALLER_CHARGE_ID)?.amount() ?: 0.0
                    attribute.removeModifier(REINFORCEMENT_CALLER_CHARGE_ID)
                    attribute.addPermanentModifier(AttributeModifier(REINFORCEMENT_CALLER_CHARGE_ID, existingAmount - 0.05, AttributeModifier.Operation.ADD_VALUE))
                    reinforcement.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE)!!.addPermanentModifier(ZOMBIE_REINFORCEMENT_CALLEE_CHARGE)
                    break
                }
            }
        }
    }

    override fun actuallyHurt(source: DamageSource, damage: Float) {
        if (!ignoreZombieDamage()) return super.actuallyHurt(source, damage)
        if ((source.`is`(PazTags.DamageTypes.IGNORED_BY_ZOMBIES) || source.isZombieFireworkExplosion()) && !source.directEntity.isHypnotized()) {
            return
        }
        super.actuallyHurt(source, damage)
    }

    protected open fun ignoreZombieDamage(): Boolean = true

    override fun wantsToPickUp(itemStack: ItemStack): Boolean {
        if (itemStack.`is`(PazBlocks.PLANTZ_FLAG.asItem())) return false
        return super.wantsToPickUp(itemStack)
    }

    /** 26.1 Mob.getPreferredWeaponType: 1.21.1 has no such hook, so it is applied in [canReplaceCurrentItem]. */
    open fun getPreferredWeaponType(): TagKey<Item>? = null

    override fun canReplaceCurrentItem(candidate: ItemStack, existing: ItemStack): Boolean {
        val preferredWeaponType = getPreferredWeaponType()
        if (preferredWeaponType != null && !existing.isEmpty) {
            if (existing.`is`(preferredWeaponType) && !candidate.`is`(preferredWeaponType)) return false
            if (!existing.`is`(preferredWeaponType) && candidate.`is`(preferredWeaponType)) return true
        }
        return super.canReplaceCurrentItem(candidate, existing)
    }

    open fun emergingTime(): Int = 40
    open fun canEquipDuckyInWater() = true
    open fun canEquipBalloons() = true
    open fun canClimbWalls(): Boolean = false

    override fun maxUpStep(): Float = if (isInWater) 0.5f else super.maxUpStep()
    override fun isSunSensitive(): Boolean = false
    override fun convertsInWater(): Boolean = false
    /** 26.1 Mob.canSpawnInLiquids (not in 1.21.1): used for spawn obstruction and reinforcements. */
    open fun canSpawnInLiquids(): Boolean = canEquipDuckyInWater()
    override fun checkSpawnObstruction(level: LevelReader): Boolean {
        return if (canSpawnInLiquids()) level.isUnobstructed(this)
        else super.checkSpawnObstruction(level)
    }
    override fun isBaby(): Boolean = false
    override fun onClimbable(): Boolean { return if (canClimbWalls() && !level().isClientSide) this.horizontalCollision else super.onClimbable() }
    override fun populateDefaultEquipmentSlots(random: RandomSource, difficulty: DifficultyInstance) {}
    fun isBabyZombie() = super.isBaby()
    fun randomEquip(random: RandomSource, difficulty: DifficultyInstance) {
        super.populateDefaultEquipmentSlots(random, difficulty)
    }
    fun isRaider() = (this as? ZombieRaider)?.`plantz$getIsFromRaid`()?: false

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnReason: MobSpawnType,
        groupData: SpawnGroupData?
    ): SpawnGroupData? {
        val newData = PazZombieGroupData(
            isRaider = (groupData as? PazZombieGroupData)?.isRaider?: false,
            isCountedTowardRaidHealth = (groupData as? PazZombieGroupData)?.isCountedTowardRaidHealth?: false,
            isBaby = isBaby && (groupData as? ZombieGroupData)?.isBaby?: false
        )
        val level = level() as ServerLevel
        if (newData.isRaider) {
            val raid = level.getZombieRaids().getNearbyRaid(blockPosition(), RAID_DISTANCE)
            this.setCountHealth(newData.isCountedTowardRaidHealth)
            raid?.joinRaid(level, this)
        }

        val data = super.finalizeSpawn(level, difficulty, spawnReason, newData)
        // 26.1 Zombie.handleAttributes heals a new leader to its raised max health; 1.21.1 does not
        if (spawnReason != MobSpawnType.CONVERSION && isBornLeader(this)) health = maxHealth

        if (spawnReason == MobSpawnType.REINFORCEMENT || spawnReason == MobSpawnType.EVENT) state = ZombieState.EMERGING

        if (canEquipDuckyInWater() && level.getBlockState(blockPosition()).fluidState.type == Fluids.WATER) {
            setItemSlot(EquipmentSlot.LEGS, PazItems.DUCKY_TUBE.defaultInstance)
            if (spawnReason != MobSpawnType.NATURAL) setDropChance(EquipmentSlot.LEGS, 0.0f)
            else setDropChance(EquipmentSlot.LEGS, 0.15f)
        }

        return data
    }
}

class PazZombieGroupData(
    val isRaider: Boolean = false,
    val isCountedTowardRaidHealth: Boolean = true,
    isBaby: Boolean = false
) : Zombie.ZombieGroupData(
    isBaby,
    false
)