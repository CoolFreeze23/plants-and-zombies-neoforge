package joshxviii.plantz.gametest

import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazDamageTypes
import joshxviii.plantz.PazEffects
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.GardenHeroRewards
import joshxviii.plantz.ZombieRaider
import joshxviii.plantz.ai.ZombieState
import joshxviii.plantz.entity.projectile.Missile
import joshxviii.plantz.entity.projectile.PaintBall
import joshxviii.plantz.entity.projectile.PowderSnowChunk
import joshxviii.plantz.entity.zombie.AllStar
import joshxviii.plantz.entity.zombie.BackupDancer
import joshxviii.plantz.entity.zombie.Imp
import joshxviii.plantz.entity.zombie.ImpVariant
import joshxviii.plantz.entity.zombie.NewspaperZombie
import joshxviii.plantz.entity.zombie.PazZombie
import joshxviii.plantz.entity.zombie.PirateCaptainGhost
import joshxviii.plantz.entity.zombie.ZombieRobot
import joshxviii.plantz.gametest.zombies.discardEntities
import joshxviii.plantz.gametest.zombies.entitiesNearby
import joshxviii.plantz.gametest.zombies.goal
import joshxviii.plantz.gametest.zombies.makeModMockServerPlayer
import joshxviii.plantz.gametest.zombies.removeMockPlayer
import joshxviii.plantz.gametest.zombies.succeedWhenThenClean
import joshxviii.plantz.gametest.zombies.targeting
import joshxviii.plantz.gametest.zombies.victim
import joshxviii.plantz.gametest.zombies.wasHurt
import joshxviii.plantz.item.BlueprintItem
import joshxviii.plantz.raid.ZombieRaid
import joshxviii.plantz.raid.getZombieRaids
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.Difficulty
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.PressurePlateBlock
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

/** Zombies, blueprint machines and zombie raids. Same `platform` template as [CoreGameTests]. */
@GameTestHolder("plantz")
@PrefixGameTestTemplate(false)
object ZombieGameTests {
    private const val PLATFORM = CoreGameTests.PLATFORM
    private const val RAID_BATCH = "zombie_raid"

    // ---- zombies vs plants

    /** A browncoat targets and eats a plant. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 400)
    fun browncoatEatsPlant(helper: GameTestHelper) {
        helper.spawn(PazEntities.BROWN_COAT, 3, 1, 7)
        val sunflower = helper.spawn(PazEntities.SUNFLOWER, 10, 1, 7)
        helper.succeedWhenThenClean { helper.assertTrue(sunflower.wasHurt(), "sunflower was not attacked") }
    }

    // ---- special zombies

    /** Losing the newspaper enrages the newspaper zombie: speed and damage bonus, breaks doors. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun newspaperZombieRagesWithoutPaper(helper: GameTestHelper) {
        val zombie = helper.spawnWithNoFreeWill(PazEntities.NEWSPAPER_ZOMBIE, 7, 1, 7)
        zombie.setItemSlot(EquipmentSlot.MAINHAND, PazItems.NEWSPAPER.defaultInstance)
        val speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED)!!
        helper.startSequence()
            .thenExecuteAfter(5) {
                helper.assertFalse(zombie.isAngry(), "calm zombie reads as angry")
                helper.assertFalse(speed.hasModifier(NewspaperZombie.ANGRY_BONUS_ID), "calm zombie has the anger bonus")
                zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
            }
            .thenWaitUntil {
                helper.assertTrue(zombie.isAngry(), "zombie is not angry")
                helper.assertTrue(speed.hasModifier(NewspaperZombie.ANGRY_BONUS_ID), "no speed bonus")
                helper.assertTrue(zombie.getAttribute(Attributes.ATTACK_DAMAGE)!!.hasModifier(NewspaperZombie.ANGRY_BONUS_ID), "no damage bonus")
                helper.assertTrue(zombie.canBreakDoors(), "angry zombie cannot break doors")
            }
            .thenExecute { helper.discardEntities() }
            .thenSucceed()
    }

    /** The digger zombie mines its way towards a walled-in target. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 600)
    fun diggerZombieTunnelsToTarget(helper: GameTestHelper) {
        val box = BlockPos.betweenClosed(BlockPos(6, 1, 10), BlockPos(8, 3, 12)).map { it.immutable() }
            .filterNot { it.x == 7 && it.z == 11 && it.y < 3 }
        box.forEach { helper.setBlock(it, Blocks.STONE) }
        val villager = helper.victim(7, 1, 11, unhurtable = true)
        val digger = helper.spawn(PazEntities.DIGGER_ZOMBIE, 7, 1, 3)
        digger.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_PICKAXE.defaultInstance)
        digger.targeting(villager)
        val solid = box + BlockPos.betweenClosed(BlockPos(0, 0, 0), BlockPos(14, 0, 14)).map { it.immutable() }
        helper.succeedWhenThenClean {
            helper.assertTrue(solid.any { helper.getBlockState(it).isAir },
                "digger broke no block (at ${helper.relativeVec(digger.position())}, aggressive ${digger.isAggressive}, target ${digger.target}, path ${digger.navigation.path?.let { "${it.nodeCount} nodes to ${it.target}" }})")
        }
    }

    /** The engineer builds a blueprint machine it owns when it has a target. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 300)
    fun engineerBuildsOwnedBot(helper: GameTestHelper) {
        val villager = helper.victim(12, 1, 7, unhurtable = true)
        val engineer = helper.spawn(PazEntities.ENGINEER_ZOMBIE, 3, 1, 7).targeting(villager)
        helper.succeedWhenThenClean {
            val bots = helper.entitiesNearby(ZombieRobot::class.java)
            helper.assertTrue(bots.isNotEmpty(), "no bot built")
            helper.assertTrue(bots.all { it.owner === engineer }, "bot is not owned by its engineer")
        }
    }

    /**
     * The disco zombie's summon goal raises emerging backup dancers around it. The goal is driven directly: in a test
     * the zombie reaches its motionless target and stops being aggressive (as vanilla melee AI does) before it is done.
     */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun discoZombieSummonsBackupDancers(helper: GameTestHelper) {
        val villager = helper.victim(12, 1, 7, unhurtable = true)
        val disco = helper.spawn(PazEntities.DISCO_ZOMBIE, 5, 1, 7).targeting(villager)
        disco.isNoAi = true
        disco.isAggressive = true
        val summon = disco.goal("SummonBackupGoal")
        helper.assertTrue(summon.canUse(), "summon goal not usable while fighting")
        for (i in 0..<200) {
            summon.tick()
            if (helper.entitiesNearby(BackupDancer::class.java).isNotEmpty()) break
        }
        helper.succeedWhenThenClean {
            val dancers = helper.entitiesNearby(BackupDancer::class.java)
            helper.assertTrue(dancers.size >= 3, "expected 3+ backup dancers, got ${dancers.size}")
            helper.assertTrue(dancers.all { it.state == ZombieState.EMERGING }, "backup dancers do not emerge")
            helper.assertTrue(disco.summoningTime > 0, "no summon animation")
        }
    }

    /** The all-star charges at a distant target with its knockback boost. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun allStarCharges(helper: GameTestHelper) {
        val villager = helper.victim(14, 1, 14, unhurtable = true)
        val allStar = helper.spawn(PazEntities.ALL_STAR, 0, 1, 0).targeting(villager)
        helper.succeedWhenThenClean {
            helper.assertTrue(allStar.chargingTime >= 0, "all-star is not charging")
            helper.assertTrue(allStar.getAttribute(Attributes.KNOCKBACK_RESISTANCE)!!.hasModifier(AllStar.CHARGE_BOOST_ID), "no charge boost")
        }
    }

    /** A wounded gargantuar throws its imp at a distant target (goal driven, see the disco zombie test). */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun gargantuarThrowsImp(helper: GameTestHelper) {
        val villager = helper.victim(13, 1, 7, unhurtable = true)
        val gargantuar = helper.spawn(PazEntities.GARGANTUAR, 3, 1, 7).targeting(villager)
        gargantuar.isNoAi = true
        gargantuar.isAggressive = true
        val throwImp = gargantuar.goal("ThrowImpGoal")
        helper.assertFalse(throwImp.canUse(), "unhurt gargantuar wants to throw its imp")
        gargantuar.health = gargantuar.maxHealth * 0.5f
        helper.assertTrue(throwImp.canUse(), "wounded gargantuar does not want to throw its imp")
        for (i in 0..<200) {
            throwImp.tick()
            if (!gargantuar.hasImp) break
        }
        helper.succeedWhenThenClean {
            helper.assertFalse(gargantuar.hasImp, "gargantuar still holds its imp")
            helper.assertTrue(gargantuar.throwTime > 0, "no throw animation")
            helper.assertTrue(helper.entitiesNearby(Imp::class.java, 12.0).isNotEmpty(), "no imp thrown")
        }
    }

    /** The gargantuar's smash (an explosion in front of it) hurts its target. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun gargantuarSmashHurtsTarget(helper: GameTestHelper) {
        val villager = helper.victim(7, 1, 10)
        val gargantuar = helper.spawn(PazEntities.GARGANTUAR, 7, 1, 6).targeting(villager)
        gargantuar.smashCooldown = 0
        helper.succeedWhenThenClean {
            helper.assertTrue(gargantuar.smashCooldown > 0, "gargantuar did not smash")
            helper.assertTrue(villager.wasHurt(), "smash did not hurt the target")
        }
    }

    /** A pirate imp's barrel explodes next to its target. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun pirateImpBarrelExplodes(helper: GameTestHelper) {
        val villager = helper.victim(7, 1, 10)
        val imp = helper.spawn(PazEntities.IMP, 7, 1, 5).targeting(villager)
        imp.variant = ImpVariant.PIRATE
        imp.hasBarrel = true
        // keep it on its victim: mobs from neighbouring test areas can otherwise steal its target
        helper.onEachTick { if (imp.hasBarrel) imp.target = villager }
        helper.succeedWhenThenClean {
            helper.assertFalse(imp.hasBarrel, "barrel did not explode")
            helper.assertTrue(villager.wasHurt(), "explosion did not hurt the target")
        }
    }

    /** The robo zombie fires a missile at a distant target. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun roboZombieFiresMissile(helper: GameTestHelper) {
        val villager = helper.victim(7, 1, 13, unhurtable = true)
        val robo = helper.spawn(PazEntities.ROBO_ZOMBIE, 7, 1, 2).targeting(villager)
        robo.reassessNavigation()
        var missileSeen = false
        helper.succeedWhenThenClean {
            missileSeen = missileSeen || helper.entitiesNearby(Missile::class.java).isNotEmpty()
            helper.assertTrue(missileSeen, "no missile fired")
            helper.assertTrue(robo.missileCooldown > 0, "missile cooldown not started")
        }
    }

    /** The robo zombie bashes an adjacent target. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun roboZombieBashes(helper: GameTestHelper) {
        val villager = helper.victim(7, 1, 9)
        val robo = helper.spawn(PazEntities.ROBO_ZOMBIE, 7, 1, 7).targeting(villager)
        robo.reassessNavigation()
        robo.missileCooldown = Int.MAX_VALUE
        var bashSeen = false
        helper.succeedWhenThenClean {
            bashSeen = bashSeen || robo.bashTime > 0
            helper.assertTrue(bashSeen, "robo zombie did not bash")
            helper.assertTrue(villager.wasHurt(), "bash did not hurt the target")
        }
    }

    /** Super Brainz fires its laser beam once the cooldown is over. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun superBrainzLaser(helper: GameTestHelper) {
        val villager = helper.victim(7, 1, 12)
        val superBrainz = helper.spawn(PazEntities.SUPER_BRAINZ, 7, 1, 4).targeting(villager)
        superBrainz.beamCooldown = 0
        var beamSeen = false
        helper.succeedWhenThenClean {
            beamSeen = beamSeen || superBrainz.beamAttackTime > 0
            helper.assertTrue(beamSeen, "no laser beam")
            helper.assertTrue(villager.wasHurt(), "laser did not hurt the target")
        }
    }

    /**
     * The yeti throws powder snow at a target it cannot reach (it is fenced in, the target stands on a pillar).
     * Each 36-tick attempt throws with a 25% chance, so the timeout allows ~33 attempts.
     */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 1200)
    fun yetiThrowsPowderSnow(helper: GameTestHelper) {
        for (x in 5..9) for (z in 0..4) if (x == 5 || x == 9 || z == 0 || z == 4) helper.setBlock(x, 1, z, Blocks.OAK_FENCE)
        for (y in 1..3) helper.setBlock(7, y, 13, Blocks.STONE)
        val villager = helper.victim(7, 4, 13, unhurtable = true)
        helper.spawn(PazEntities.ZOMBIE_YETI, 7, 1, 2).targeting(villager)
        var chunkSeen = false
        helper.succeedWhenThenClean {
            chunkSeen = chunkSeen || helper.entitiesNearby(PowderSnowChunk::class.java, 8.0).isNotEmpty()
            helper.assertTrue(chunkSeen, "no powder snow thrown")
        }
    }

    /** The soldier zombie shoots paint balls at its target. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun soldierZombieShootsPaint(helper: GameTestHelper) {
        val villager = helper.victim(7, 1, 13, unhurtable = true)
        val soldier = helper.spawn(PazEntities.SOLDIER_ZOMBIE, 7, 1, 2).targeting(villager)
        soldier.setItemSlot(EquipmentSlot.MAINHAND, PazItems.DYE_BLASTER.defaultInstance)
        var paintSeen = false
        helper.succeedWhenThenClean {
            paintSeen = paintSeen || helper.entitiesNearby(PaintBall::class.java).isNotEmpty()
            helper.assertTrue(paintSeen, "no paint ball fired")
        }
    }

    /** The grave digger digs up gravestones near its target. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 300)
    fun graveDiggerDigsGraves(helper: GameTestHelper) {
        val villager = helper.victim(7, 1, 12, unhurtable = true)
        val digger = helper.spawn(PazEntities.GRAVE_DIGGER, 7, 1, 4).targeting(villager)
        digger.setItemSlot(EquipmentSlot.MAINHAND, Items.STONE_SHOVEL.defaultInstance)
        digger.maxGraves = 5
        helper.succeedWhenThenClean { helper.assertTrue(digger.dugGraves > 0, "no grave dug") }
    }

    /** A killed pirate captain comes back as its ghost, keeping its name, effects and persistence (26.1 conversion). */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun pirateCaptainBecomesGhost(helper: GameTestHelper) {
        val captain = helper.spawn(PazEntities.PIRATE_CAPTAIN, 7, 1, 7)
        captain.isNoAi = true
        captain.setItemSlot(EquipmentSlot.MAINHAND, Items.CROSSBOW.defaultInstance)
        captain.customName = Component.literal("Captain")
        captain.addEffect(MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION))
        captain.addTag("plantz_test")
        val position = captain.position()
        captain.kill()
        helper.succeedWhenThenClean {
            helper.assertTrue(captain.isRemoved, "captain was not removed")
            val ghosts = helper.entitiesNearby(PirateCaptainGhost::class.java, 32.0)
            helper.assertTrue(ghosts.size == 1, "expected one ghost, got ${ghosts.size}")
            val ghost = ghosts.single()
            helper.assertTrue(ghost.customName?.string == "Captain", "ghost lost the name")
            helper.assertTrue(ghost.hasEffect(MobEffects.GLOWING), "ghost lost the captain's effects")
            helper.assertTrue(ghost.tags.contains("plantz_test"), "ghost lost the captain's tags")
            helper.assertTrue(ghost.isNoAi, "ghost lost no-AI")
            helper.assertTrue(ghost.isPersistenceRequired, "ghost lost persistence")
            helper.assertTrue(ghost.mainHandItem.`is`(Items.IRON_SWORD), "ghost should hold an iron sword")
            helper.assertTrue(ghost.position().subtract(position).horizontalDistance() < 0.01, "ghost spawned elsewhere")
        }
    }

    /** The ghost moves without physics but still triggers blocks it is inside (26.1 isAffectedByBlocks). */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun pirateGhostTriggersBlocks(helper: GameTestHelper) {
        val plate = BlockPos(7, 1, 7)
        helper.setBlock(plate, Blocks.STONE_PRESSURE_PLATE)
        helper.spawnWithNoFreeWill(PazEntities.PIRATE_CAPTAIN_GHOST, 7, 1, 7)
        helper.succeedWhenThenClean { helper.assertBlockProperty(plate, PressurePlateBlock.POWERED, true) }
    }

    // ---- 1.21.1 vanilla gaps restored

    /** A zombie leader is healed to its raised max health on spawn, as 26.1 Zombie.handleAttributes does. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun zombieLeaderSpawnsAtFullHealth(helper: GameTestHelper) {
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 7, 1, 7)
        zombie.getAttribute(Attributes.MAX_HEALTH)!!.addPermanentModifier(
            AttributeModifier(ResourceLocation.withDefaultNamespace(PazZombie.LEADER_MODIFIER_ID), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        )
        helper.assertTrue(zombie.health < zombie.maxHealth, "leader already at full health")
        zombie.finalizeSpawn(helper.level, helper.level.getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.EVENT, null)
        helper.assertTrue(zombie.maxHealth > 20f && zombie.health == zombie.maxHealth, "leader not healed: ${zombie.health}/${zombie.maxHealth}")
        helper.discardEntities()
        helper.succeed()
    }

    /** On hard, a zombie calls reinforcements of its own type (26.1), never plain vanilla zombies (1.21.1). */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun reinforcementsAreSameType(helper: GameTestHelper) {
        val level = helper.level
        val villager = helper.victim(7, 1, 12, unhurtable = true)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.SOLDIER_ZOMBIE, 7, 1, 7).targeting(villager)
        zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE)!!.baseValue = 1.0
        // reinforcements are the new zombies that share the caller's target (other tests' zombies may be nearby)
        val searchArea = zombie.boundingBox.inflate(48.0)
        fun reinforcements() = level.getEntitiesOfClass(Zombie::class.java, searchArea) { it !== zombie && it.target === villager }

        val difficulty = level.difficulty
        val mobSpawning = level.gameRules.getRule(GameRules.RULE_DOMOBSPAWNING)
        val wasSpawning = mobSpawning.get()
        try {
            level.server.setDifficulty(Difficulty.HARD, true)
            mobSpawning.set(true, level.server)
            for (attempt in 0..<10) {
                zombie.invulnerableTime = 0
                zombie.health = zombie.maxHealth
                zombie.hurt(level.damageSources().generic(), 1f)
                if (reinforcements().isNotEmpty()) break
            }
        } finally {
            mobSpawning.set(wasSpawning, level.server)
            level.server.setDifficulty(difficulty, true)
        }

        val called = reinforcements()
        called.forEach { it.discard() }
        helper.assertTrue(called.isNotEmpty(), "no reinforcement called")
        helper.assertTrue(called.all { it.type == PazEntities.SOLDIER_ZOMBIE }, "reinforcement of another type: ${called.map { it.type }}")
        helper.assertTrue(called.all { it.target === villager }, "reinforcement does not share the target")
        helper.assertTrue(called.all { (it as PazZombie).state == ZombieState.EMERGING }, "reinforcement does not emerge")
        helper.discardEntities()
        helper.succeed()
    }

    // ---- blueprint machines

    /** The zombie turret shoots laser bullets at plants. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun zombieTurretShootsPlant(helper: GameTestHelper) {
        helper.spawn(PazEntities.ZOMBIE_TURRET, 7, 1, 3)
        val sunflower = helper.spawn(PazEntities.SUNFLOWER, 7, 1, 10)
        helper.succeedWhenThenClean { helper.assertTrue(sunflower.wasHurt(), "turret did not hit the plant") }
    }

    /** The electro turret zaps an adjacent plant and electrifies it. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun electroTurretElectrifiesPlant(helper: GameTestHelper) {
        helper.spawn(PazEntities.ELECTRO_TURRET, 7, 1, 7)
        val sunflower = helper.spawn(PazEntities.SUNFLOWER, 7, 1, 8)
        helper.succeedWhenThenClean { helper.assertTrue(sunflower.hasEffect(PazEffects.ELECTRIFIED), "plant not electrified") }
    }

    /** The zombie drone flies and attacks plants. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 300)
    fun zombieDroneFliesAndAttacks(helper: GameTestHelper) {
        val drone = helper.spawn(PazEntities.ZOMBIE_DRONE, 7, 2, 3)
        val sunflower = helper.spawn(PazEntities.SUNFLOWER, 7, 1, 10)
        helper.succeedWhenThenClean {
            helper.assertTrue(drone.isFlying && drone.isNoGravity, "drone is not flying")
            helper.assertTrue(sunflower.wasHurt(), "drone did not attack the plant")
        }
    }

    /** Owner chains resolve to the root owner, survive a save/load, and decide whether zombie damage is ignored. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun blueprintMachineOwnership(helper: GameTestHelper) {
        val level = helper.level
        val engineer = helper.spawnWithNoFreeWill(PazEntities.ENGINEER_ZOMBIE, 2, 1, 2)
        val turret = helper.spawnWithNoFreeWill(PazEntities.ZOMBIE_TURRET, 5, 1, 5)
        val drone = helper.spawnWithNoFreeWill(PazEntities.ZOMBIE_DRONE, 9, 1, 9)
        turret.owner = engineer
        drone.owner = turret
        helper.assertTrue(drone.owner === turret && drone.rootOwner === engineer, "owner chain does not reach the engineer")

        val saved = CompoundTag().also { drone.saveWithoutId(it) }
        val loaded = PazEntities.ZOMBIE_DRONE.create(level)!!
        loaded.load(saved)
        helper.assertTrue(loaded.ownerUUID == turret.uuid, "owner lost on save/load")

        val zombieEnergy = level.damageSources().source(PazDamageTypes.ZOMBIE_ENERGY, engineer)
        helper.assertFalse(turret.hurt(zombieEnergy, 1f), "zombie-owned turret took zombie damage")

        val player = helper.makeModMockServerPlayer(Vec3(1.0, 1.0, 13.0))
        try {
            val playerTurret = helper.spawnWithNoFreeWill(PazEntities.ZOMBIE_TURRET, 11, 1, 3)
            playerTurret.owner = player
            helper.assertTrue(playerTurret.rootOwner === player, "player owner not resolved")
            helper.assertTrue(playerTurret.hurt(zombieEnergy, 1f), "player-owned turret ignored zombie damage")
        } finally {
            helper.removeMockPlayer(player)
        }
        helper.discardEntities()
        helper.succeed()
    }

    /** The lawn mower breaks on any hit, picks as its blueprint and keeps its equipment through a save/load. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun lawnMowerBasics(helper: GameTestHelper) {
        val mower = helper.spawn(PazEntities.LAWN_MOWER, 7, 1, 7)
        helper.assertTrue(ItemStack.matches(mower.pickResult, BlueprintItem.stackFor(PazEntities.LAWN_MOWER)), "pick result is not the blueprint")

        mower.setItemSlot(EquipmentSlot.HEAD, Items.CARVED_PUMPKIN.defaultInstance)
        val loaded = PazEntities.LAWN_MOWER.create(helper.level)!!
        loaded.load(CompoundTag().also { mower.saveWithoutId(it) })
        helper.assertTrue(loaded.getItemBySlot(EquipmentSlot.HEAD).`is`(Items.CARVED_PUMPKIN), "equipment lost on save/load")

        helper.assertTrue(mower.hurt(helper.level.damageSources().generic(), 0.5f), "mower ignored the hit")
        helper.assertTrue(mower.removalReason == Entity.RemovalReason.KILLED, "mower not broken by the hit")
        helper.succeed()
    }

    // ---- zombie raid

    /** Credits add two waves; omen levels add two each. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun raidWaveCount(helper: GameTestHelper) {
        helper.assertTrue(ZombieRaid.getWaveSpawnCount(Difficulty.NORMAL, 1, false) == 7, "normal raid should have 7 waves")
        helper.assertTrue(ZombieRaid.getWaveSpawnCount(Difficulty.NORMAL, 1, true) == 9, "credits should add two waves")
        helper.assertTrue(ZombieRaid.getWaveSpawnCount(Difficulty.HARD, 3, true) == 14, "omen 3 should add six waves")
        helper.assertTrue(ZombieRaid.getWaveSpawnCount(Difficulty.HARD, 99, true) == ZombieRaid.MAXIMUM_WAVE_COUNT, "wave count is not capped")
        helper.succeed()
    }

    /**
     * A full raid: an omen-bearing player starts it at a flag, every wave spawns and is wiped out, the raid is won,
     * the player is rewarded, victory fireworks fly and the raid ends.
     */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = RAID_BATCH, timeoutTicks = 2400)
    fun zombieRaidIsWon(helper: GameTestHelper) {
        val level = helper.level
        val flagPos = helper.absolutePos(BlockPos(7, 1, 7))
        helper.setBlock(BlockPos(7, 1, 7), PazBlocks.PLANTZ_FLAG)
        val player = helper.makeModMockServerPlayer(Vec3(3.5, 1.0, 3.5))
        player.addEffect(MobEffectInstance(PazEffects.ZOMBIE_OMEN, 2000, 0))

        val raid = level.getZombieRaids().createOrExtendZombieRaid(player, flagPos)
        helper.assertTrue(raid != null, "raid did not start")
        raid!!
        helper.assertTrue(raid.zombieRaidOmenLevel == 1, "omen not absorbed")
        helper.assertTrue(raid.startedBy == player.uuid && !raid.starterHasSeenCredits, "wrong raid starter state")
        helper.assertTrue(raid.zombieRaidEvent.players.contains(player), "player not in the raid")

        val wavesSeen = mutableSetOf<Int>()
        var fireworkSeen = false
        fun raiders() = level.getEntities(EntityTypeTest.forClass(Zombie::class.java)) { (it as ZombieRaider).`plantz$getIsFromRaid`() }

        helper.succeedWhen {
            if (raid.status != ZombieRaid.ZombieRaidStatus.VICTORY && raid.status != ZombieRaid.ZombieRaidStatus.STOPPED) {
                val raiders = raiders()
                if (raiders.isNotEmpty()) wavesSeen += raid.wavesSpawned
                raiders.forEach { it.discard() }
            }
            if (raid.status == ZombieRaid.ZombieRaidStatus.VICTORY) {
                fireworkSeen = fireworkSeen || level.getEntitiesOfClass(FireworkRocketEntity::class.java, AABB(flagPos).inflate(8.0)).isNotEmpty()
            }
            helper.assertTrue(raid.isStopped(), "raid still running (status ${raid.status}, wave ${raid.wavesSpawned}/${raid.numWaves})")

            helper.assertTrue(wavesSeen == (1..raid.numWaves).toSet(), "waves seen $wavesSeen of ${raid.numWaves}")
            helper.assertTrue(raid.waveTypes.size == raid.numWaves, "wave types not recorded")
            helper.assertTrue(fireworkSeen, "no victory fireworks")
            val hero = player.getEffect(PazEffects.GARDEN_HERO)
            helper.assertTrue(hero != null && hero.amplifier == raid.numWaves - 1, "no hero of the garden reward")
            helper.assertTrue((player as GardenHeroRewards).`plantz$getWaveList`() == raid.waveTypes, "reward wave list not stored")
            helper.assertTrue(level.getZombieRaids().getNearbyRaid(flagPos, 16) == null, "finished raid still registered")
            helper.assertBlockPresent(PazBlocks.PLANTZ_FLAG, BlockPos(7, 1, 7))

            helper.removeMockPlayer(player)
            helper.discardEntities()
        }
    }
}
