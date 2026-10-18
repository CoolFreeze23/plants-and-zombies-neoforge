package joshxviii.plantz.gametest

import joshxviii.plantz.GardenHeroRewards
import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazCriteria
import joshxviii.plantz.PazDamageTypes
import joshxviii.plantz.PazEffects
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.ai.PlantState
import joshxviii.plantz.api.SeedMutationManager
import joshxviii.plantz.api.TimePredicate
import joshxviii.plantz.api.WeatherPredicate
import joshxviii.plantz.entity.Balloon
import joshxviii.plantz.entity.Sun
import joshxviii.plantz.entity.gnome.GnomeVariant
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.projectile.Butter
import joshxviii.plantz.entity.projectile.Melon
import joshxviii.plantz.entity.projectile.Needle
import joshxviii.plantz.entity.projectile.Pea
import joshxviii.plantz.gametest.plants.*
import joshxviii.plantz.item.SeedPacketItem
import joshxviii.plantz.raid.getZombieRaids
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.StringTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.NeoForgeMod
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import java.util.Optional

/**
 * Plants, projectiles, suns, balloons, plant pot minecart, gnome, mob effects and advancement triggers.
 * See plants/PlantTestHelpers.kt for the batches (time of day) and layout rules.
 */
@GameTestHolder("plantz")
@PrefixGameTestTemplate(false)
object PlantGameTests {
    private const val PLATFORM = CoreGameTests.PLATFORM

    @JvmStatic @BeforeBatch(batch = NIGHT_BATCH)
    fun beforeNight(level: ServerLevel) = setTime(level, NIGHT_TIME)

    @JvmStatic @BeforeBatch(batch = LONG_RANGE_BATCH)
    fun beforeLongRange(level: ServerLevel) = setTime(level, NIGHT_TIME)

    @JvmStatic @BeforeBatch(batch = DAY_BATCH)
    fun beforeDay(level: ServerLevel) = setTime(level, NOON_TIME)

    @JvmStatic @BeforeBatch(batch = GNOME_BATCH)
    fun beforeGnome(level: ServerLevel) = setTime(level, NIGHT_TIME)

    // ---- sun producers ----

    @JvmStatic
    @GameTest(template = PLATFORM, batch = DAY_BATCH, timeoutTicks = 200)
    fun sunflowerMakesSun(helper: GameTestHelper) {
        val sunflower = helper.spawn(PazEntities.SUNFLOWER, 7, 2, 7)
        sunflower.cooldown = 0
        helper.succeedWhen {
            helper.assertFalse(sunflower.isAsleep, "sunflower asleep at noon")
            helper.assertTrue(helper.getEntities(PazEntities.SUN).isNotEmpty(), "no sun: ${describe(sunflower)} cooldown=${sunflower.cooldown}")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun sunShroomMakesSunAtNight(helper: GameTestHelper) {
        val shroom = helper.spawn(PazEntities.SUN_SHROOM, 7, 2, 7)
        shroom.cooldown = 0
        helper.succeedWhen {
            val sun = helper.getEntities(PazEntities.SUN).firstOrNull()
            helper.assertTrue(sun != null, "sun-shroom made no sun: ${describe(shroom)} asleep=${shroom.isAsleep} cooldown=${shroom.cooldown}")
            helper.assertValueEqual(sun!!.value, 2, "sun-shroom sun value")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun sunIsPickedUpByPlayer(helper: GameTestHelper) {
        val player = helper.mockPlayer(7.5, 2.0, 7.5)
        val sun = Sun(helper.level, helper.absoluteVec(Vec3(7.5, 3.5, 9.5)), Vec3.ZERO, 1)
        helper.level.addFreshEntity(sun)
        helper.succeedWhenThen({ helper.removeMockPlayer(player) }) {
            helper.assertTrue(sun.isRemoved, "sun was not collected")
            helper.assertTrue(player.inventory.countItem(PazItems.SUN) > 0, "player got no sun item")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun sunHealsDamagedPlant(helper: GameTestHelper) {
        val plant = helper.spawn(PazEntities.WALL_NUT, 7, 2, 7)
        plant.health = 20f
        Sun.award(helper.level, helper.absoluteVec(Vec3(7.5, 3.5, 8.5)), 3)
        helper.succeedWhen {
            helper.assertTrue(plant.health > 20f, "sun did not heal the plant: ${describe(plant)} suns=${helper.getEntities(PazEntities.SUN).map { helper.relativePosition(it) }}")
            helper.assertEntityNotPresent(PazEntities.SUN)
        }
    }

    // ---- peashooters ----

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun icePeaChillsZombie(helper: GameTestHelper) {
        helper.spawn(PazEntities.ICE_PEA_SHOOTER, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        helper.succeedWhen {
            helper.assertHurt(zombie, "zombie not hit")
            helper.assertHasEffect(zombie, PazEffects.CHILLED, "zombie not chilled")
            helper.assertTrue(zombie.effectFlags.`plantz$getChillId`(), "chill flag not synced")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun firePeaIgnitesZombie(helper: GameTestHelper) {
        helper.spawn(PazEntities.FIRE_PEA_SHOOTER, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        helper.succeedWhen {
            helper.assertHurt(zombie, "zombie not hit")
            helper.assertTrue(zombie.remainingFireTicks > 0, "zombie not burning")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun electricPeaElectrifiesZombie(helper: GameTestHelper) {
        helper.spawn(PazEntities.ELECTRIC_PEA_SHOOTER, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        zombie.toughen()
        helper.succeedWhen {
            helper.assertHurt(zombie, "zombie not hit")
            helper.assertHasEffect(zombie, PazEffects.ELECTRIFIED, "zombie not electrified")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun repeaterFiresTwoPeasPerVolley(helper: GameTestHelper) {
        val repeater = helper.spawn(PazEntities.REPEATER, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        zombie.toughen()
        val peas = mutableSetOf<Int>()
        var firstPeaTick = -1L
        helper.onEachTick {
            helper.getEntities(PazEntities.PEA).filter { it.owner == repeater }.forEach {
                if (peas.add(it.id) && firstPeaTick < 0) firstPeaTick = helper.tick
            }
        }
        helper.succeedWhen {
            helper.assertTrue(peas.size >= 2, "repeater fired ${peas.size} peas: ${describe(repeater)} zombie ${describe(zombie)}")
            // the second pea of a volley follows within the 4 tick action cooldown (single shooters wait ~20 ticks)
            helper.assertTrue(helper.tick - firstPeaTick <= 10, "second pea came too late")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun lightningTurnsPeashooterElectric(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        val peashooter = helper.spawn(PazEntities.PEA_SHOOTER, 7, 2, 7)
        peashooter.tame(player)
        peashooter.hurt(peashooter.damageSources().lightningBolt(), 1f)
        helper.succeedWhenThen({ helper.removeMockPlayer(player) }) {
            helper.assertTrue(peashooter.isRemoved, "peashooter not converted")
            val electric = helper.getEntities(PazEntities.ELECTRIC_PEA_SHOOTER).firstOrNull()
            helper.assertTrue(electric != null, "no electric peashooter")
            helper.assertTrue(electric!!.isTame && electric.owner == player, "conversion lost the owner")
        }
    }

    // ---- other shooters ----

    @JvmStatic
    @GameTest(template = PLATFORM, batch = LONG_RANGE_BATCH, timeoutTicks = 300, skyAccess = true)
    fun cactusNeedlesHitZombie(helper: GameTestHelper) {
        helper.spawn(PazEntities.CACTUS, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        helper.succeedWhen { helper.assertHurt(zombie, "cactus needles missed") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = LONG_RANGE_BATCH, timeoutTicks = 300, skyAccess = true)
    fun cabbagePultLobsCabbage(helper: GameTestHelper) {
        helper.spawn(PazEntities.CABBAGE_PULT, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        var sawCabbage = false
        helper.onEachTick { if (helper.getEntities(PazEntities.CABBAGE).isNotEmpty()) sawCabbage = true }
        helper.succeedWhen {
            helper.assertTrue(sawCabbage, "no cabbage thrown, zombie ${describe(zombie)}")
            helper.assertHurt(zombie, "cabbage missed")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = LONG_RANGE_BATCH, timeoutTicks = 300, skyAccess = true)
    fun kernelPultLobsKernels(helper: GameTestHelper) {
        helper.spawn(PazEntities.KERNEL_PULT, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        zombie.toughen()
        helper.succeedWhen { helper.assertHurt(zombie, "kernels missed") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = LONG_RANGE_BATCH, timeoutTicks = 300, skyAccess = true)
    fun melonPultLobsMelon(helper: GameTestHelper) {
        helper.spawn(PazEntities.MELON_PULT, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        helper.succeedWhen { helper.assertHurt(zombie, "melon missed") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = LONG_RANGE_BATCH, timeoutTicks = 300, skyAccess = true)
    fun winterMelonChillsZombie(helper: GameTestHelper) {
        helper.spawn(PazEntities.WINTER_MELON, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        zombie.toughen()
        helper.succeedWhen {
            helper.assertHurt(zombie, "winter melon missed")
            helper.assertHasEffect(zombie, PazEffects.CHILLED, "winter melon did not chill")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun lightningReedZapsZombie(helper: GameTestHelper) {
        helper.spawn(PazEntities.LIGHTNING_REED, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 8, 2, 7)
        zombie.toughen()
        helper.succeedWhen {
            helper.assertHurt(zombie, "lightning reed did not hit")
            helper.assertHasEffect(zombie, PazEffects.ELECTRIFIED, "lightning reed did not electrify")
        }
    }

    // ---- mushrooms ----

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun puffShroomSporesPoison(helper: GameTestHelper) {
        helper.spawn(PazEntities.PUFF_SHROOM, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(8.0, 2.0, 7.5))
        zombie.toughen()
        helper.succeedWhen { helper.assertHasEffect(zombie, PazEffects.TOXIC, "spore did not poison") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun scaredyShroomHidesFromCloseZombie(helper: GameTestHelper) {
        val shroom = helper.spawn(PazEntities.SCAREDY_SHROOM, 7, 2, 7)
        helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        helper.succeedWhen { helper.assertTrue(shroom.isHiding, "scaredy-shroom did not hide: ${describe(shroom)} asleep=${shroom.isAsleep}") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun scaredyShroomShootsFarZombie(helper: GameTestHelper) {
        val shroom = helper.spawn(PazEntities.SCAREDY_SHROOM, 4, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 10, 2, 7)
        zombie.toughen()
        helper.failIfEver { helper.assertFalse(shroom.isHiding, "hid from a far zombie") }
        helper.succeedWhen { helper.assertHurt(zombie, "scaredy-shroom did not shoot") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun fumeShroomPiercesZombies(helper: GameTestHelper) {
        helper.spawn(PazEntities.FUME_SHROOM, 4, 2, 7)
        val first = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 7, 2, 7)
        val second = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 10, 2, 7)
        first.toughen(); second.toughen()
        helper.succeedWhen {
            helper.assertTrue(first.isHurt && second.isHurt, "fume did not reach both zombies")
            helper.assertHasEffect(second, PazEffects.TOXIC, "fume did not poison")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun seaShroomShootsFromWater(helper: GameTestHelper) {
        helper.setBlock(5, 1, 7, Blocks.WATER)// a pool in the floor: the water does not flow
        helper.spawn(PazEntities.SEA_SHROOM, 5, 1, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(8.0, 2.0, 7.5))
        zombie.toughen()
        helper.succeedWhen { helper.assertHurt(zombie, "sea-shroom did not shoot") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun underwaterPlantsDontDrown(helper: GameTestHelper) {
        for (x in 4..10) for (y in 2..4) helper.setBlock(x, y, 7, Blocks.WATER)
        val peashooter = helper.spawn(PazEntities.PEA_SHOOTER, 5, 2, 7)
        val seaShroom = helper.spawn(PazEntities.SEA_SHROOM, 9, 2, 7)
        helper.assertFalse(seaShroom.canDrownInFluidType(NeoForgeMod.WATER_TYPE.value()), "sea-shroom can drown")
        helper.succeedWhen {
            helper.assertTrue(peashooter.airSupply < peashooter.maxAirSupply, "peashooter keeps its air under water")
            helper.assertValueEqual(seaShroom.airSupply, seaShroom.maxAirSupply, "sea-shroom air")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun iceShroomFreezesZombies(helper: GameTestHelper) {
        val shroom = helper.spawn(PazEntities.ICE_SHROOM, 7, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        zombie.toughen()
        helper.succeedWhen {
            helper.assertHasEffect(zombie, PazEffects.FROZEN, "zombie not frozen")
            helper.assertTrue(shroom.isAlive, "ice-shroom should stay after freezing")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun doomShroomExplodesAndBreaksBlocks(helper: GameTestHelper) {
        helper.setBlock(7, 2, 9, Blocks.DIRT)
        val doom = helper.spawn(PazEntities.DOOM_SHROOM, 7, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        zombie.toughen()
        helper.succeedWhen {
            helper.assertTrue(doom.isRemoved, "doom-shroom did not explode: ${describe(doom)} asleep=${doom.isAsleep} zombie ${describe(zombie)}")
            helper.assertHurt(zombie, "explosion missed the zombie")
            helper.assertBlockNotPresent(Blocks.DIRT, 7, 2, 9)
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun hypnoShroomHypnotizesAttacker(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        val shroom = helper.spawn(PazEntities.HYPNOSHROOM, 7, 2, 7)
        shroom.tame(player)
        val disco = helper.spawnWithNoFreeWill(PazEntities.DISCO_ZOMBIE, 9, 2, 7)
        shroom.hurt(shroom.damageSources().mobAttack(disco), 1f)
        helper.succeedWhenThen({ helper.removeMockPlayer(player) }) {
            helper.assertHasEffect(disco, PazEffects.HYPNOTIZE, "attacker not hypnotized")
            helper.assertTrue(disco.effectFlags.`plantz$getHypnoId`(), "hypno flag not synced")
            helper.assertTrue(player.hasPazAdvancement("disco_hypno"), "disco_hypno advancement not granted")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun hypnoShroomLeavesCloudOnDeath(helper: GameTestHelper) {
        val shroom = helper.spawn(PazEntities.HYPNOSHROOM, 7, 2, 7)
        shroom.kill()
        helper.succeedWhen {
            val cloud = helper.getEntities(EntityType.AREA_EFFECT_CLOUD).firstOrNull()
            helper.assertTrue(cloud != null, "no hypnosis cloud")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun mushroomWakesAtNight(helper: GameTestHelper) {
        val shroom = helper.spawn(PazEntities.PUFF_SHROOM, 7, 2, 7)
        shroom.isAsleep = true
        helper.succeedWhen { helper.assertFalse(shroom.isAsleep, "mushroom still asleep at night") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = DAY_BATCH, timeoutTicks = 200)
    fun mushroomSleepsByDay(helper: GameTestHelper) {
        val shroom = helper.spawn(PazEntities.PUFF_SHROOM, 7, 2, 7)
        helper.succeedWhen {
            helper.assertTrue(shroom.isAsleep, "mushroom awake at noon")
            helper.assertValueEqual(shroom.state, PlantState.SLEEP, "plant state")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun sunflowerSleepsAtNight(helper: GameTestHelper) {
        val sunflower = helper.spawn(PazEntities.SUNFLOWER, 7, 2, 7)
        helper.succeedWhen { helper.assertTrue(sunflower.isAsleep, "sunflower awake at night") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = DAY_BATCH, timeoutTicks = 300)
    fun coffeeBeanWakesSleepingMushroom(helper: GameTestHelper) {
        val shroom = helper.spawn(PazEntities.PUFF_SHROOM, 7, 2, 7)
        val bean = helper.spawn(PazEntities.COFFEE_BEAN, 8, 2, 7)
        var sawAsleep = false
        helper.onEachTick { if (shroom.isAsleep && !bean.isRemoved) sawAsleep = true }
        helper.succeedWhen {
            helper.assertTrue(sawAsleep, "mushroom never slept")
            helper.assertTrue(bean.isRemoved, "coffee bean was not used up: ${describe(bean)} asleep=${bean.isAsleep} shroom ${describe(shroom)} asleep=${shroom.isAsleep}")
            helper.assertTrue(shroom.coffeeBuff > 0, "mushroom got no coffee buff")
            helper.assertFalse(shroom.isAsleep, "mushroom still asleep")
        }
    }

    // ---- melee plants ----

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun chomperEatsZombie(helper: GameTestHelper) {
        val chomper = helper.spawn(PazEntities.CHOMPER, 6, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 8, 2, 7)
        helper.succeedWhen {
            helper.assertTrue(zombie.isRemoved, "zombie not eaten: ${describe(zombie)} chomper ${describe(chomper)}")
            helper.assertTrue(chomper.isChewing, "chomper not chewing")
            helper.assertTrue(chomper.cooldown > 100, "chew time not applied")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 600)
    fun bonkChoyPunchesAndUppercuts(helper: GameTestHelper) {
        helper.spawn(PazEntities.BONK_CHOY, 6, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 8, 2, 7)
        zombie.toughen(2000.0)
        val startY = zombie.y
        var launched = false
        helper.onEachTick { if (zombie.y > startY + 0.5) launched = true }
        helper.succeedWhen {
            helper.assertHurt(zombie, "bonk choy did not punch")
            helper.assertTrue(launched, "no uppercut launched the zombie")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun tangleKelpDragsZombie(helper: GameTestHelper) {
        helper.setBlock(6, 1, 7, Blocks.WATER)// a pool in the floor: the water does not flow
        helper.spawn(PazEntities.TANGLE_KELP, 6, 1, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 8, 2, 7)
        zombie.toughen()
        val startX = zombie.x
        var pulled = false
        helper.onEachTick { if (zombie.x < startX - 0.3) pulled = true }
        helper.succeedWhen {
            helper.assertHasEffect(zombie, PazEffects.TANGLED, "zombie not tangled")
            helper.assertTrue(pulled, "zombie not pulled towards the kelp")
        }
    }

    // ---- explosives ----

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun cherryBombExplodes(helper: GameTestHelper) {
        val bomb = helper.spawn(PazEntities.CHERRY_BOMB, 7, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        zombie.toughen()
        helper.succeedWhen {
            helper.assertTrue(bomb.isRemoved, "cherry bomb did not explode: ${describe(bomb)} swell=${bomb.swell} cooldown=${bomb.cooldown} zombie ${describe(zombie)}")
            helper.assertHurt(zombie, "explosion missed")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun potatoMineExplodesOnContact(helper: GameTestHelper) {
        val mine = helper.spawn(PazEntities.POTATO_MINE, 7, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 7, 2, 7)
        zombie.toughen()
        helper.succeedWhen {
            helper.assertTrue(mine.isRemoved, "potato mine did not explode")
            helper.assertHurt(zombie, "explosion missed")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun explodeONutExplodesWhenRolledIntoZombie(helper: GameTestHelper) {
        val nut = helper.spawn(PazEntities.EXPLODE_O_NUT, 5, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        zombie.toughen()
        nut.roll(Vec3(1.0, 0.0, 0.0))
        helper.succeedWhen {
            helper.assertTrue(nut.isRemoved, "explode-o-nut did not explode: ${describe(nut)} rolling=${nut.isRolling} at ${helper.relativePosition(nut)} zombie ${describe(zombie)}")
            helper.assertHurt(zombie, "explosion missed")
        }
    }

    // ---- wall-nut ----

    /** 26.1 canBeCollidedWith(other): a zombie walking into a wall-nut is blocked and bites it. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun wallNutBlocksAndGetsBitten(helper: GameTestHelper) {
        val nut = helper.spawn(PazEntities.WALL_NUT, 8, 2, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 5, 2, 7)
        val nutX = nut.x
        helper.onEachTick { zombie.deltaMovement = Vec3(0.15, zombie.deltaMovement.y, 0.0) }
        helper.failIfEver { helper.assertTrue(zombie.x < nutX - 0.5, "zombie walked through the wall-nut") }
        helper.succeedWhen {
            helper.assertTrue(nut.health < nut.maxHealth, "wall-nut was not bitten")
            helper.assertTrue(zombie.x > 6.0 + helper.absolutePos(BlockPos.ZERO).x, "zombie never reached the wall-nut: at ${helper.relativePosition(zombie)} ${describe(zombie)}")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun sunsPassThroughWallNut(helper: GameTestHelper) {
        val nut = helper.spawn(PazEntities.WALL_NUT, 7, 2, 7)
        val sun = Sun(helper.level, nut.position(), Vec3.ZERO, 1)
        helper.assertFalse(nut.canBeCollidedWith(sun), "wall-nut collides with suns")
        helper.assertTrue(nut.canBeCollidedWith(helper.spawnWithNoFreeWill(EntityType.PIG, 4, 2, 7)), "wall-nut does not block mobs")
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun wallNutBowling(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        val nut = helper.spawn(PazEntities.WALL_NUT, 5, 2, 7)
        nut.tame(player)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        zombie.toughen()
        nut.roll(Vec3(1.0, 0.0, 0.0))
        helper.succeedWhenThen({ helper.removeMockPlayer(player) }) {
            helper.assertHurt(zombie, "rolling wall-nut did not hit the zombie")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 20)
    fun wallNutDeflectsArrows(helper: GameTestHelper) {
        val nut = helper.spawn(PazEntities.WALL_NUT, 7, 2, 7)
        val arrow = EntityType.ARROW.create(helper.level)!!
        helper.assertFalse(nut.hurt(nut.damageSources().source(PazDamageTypes.PLANT, arrow, null), 5f), "arrow hurt the wall-nut")
        helper.succeed()
    }

    // ---- grave buster ----

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun graveBusterEatsGravestone(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        helper.setBlock(7, 2, 7, PazBlocks.GRAVESTONE)
        val buster = helper.spawn(PazEntities.GRAVE_BUSTER, 7, 3, 7)
        buster.tame(player)
        helper.succeedWhenThen({ helper.removeMockPlayer(player) }) {
            helper.assertBlockNotPresent(PazBlocks.GRAVESTONE, 7, 2, 7)
            helper.assertTrue(buster.isRemoved, "grave buster stayed")
            helper.assertTrue(player.hasPazAdvancement("grave_buster_bust"), "grave_buster_bust advancement not granted")
        }
    }

    // ---- caring for plants ----

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun sunTamesWildPlant(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        val plant = helper.spawn(PazEntities.PEA_SHOOTER, 7, 2, 7)
        var tries = 0
        while (!plant.isTame && tries++ < 100) {
            player.holding(ItemStack(PazItems.SUN))
            plant.mobInteract(player, InteractionHand.MAIN_HAND)
        }
        helper.assertTrue(plant.isTame && plant.owner == player, "plant not tamed after $tries suns")
        helper.removeMockPlayer(player)
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun sunItemHealsTamePlant(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        val plant = helper.spawn(PazEntities.PEA_SHOOTER, 7, 2, 7)
        plant.tame(player)
        plant.health = 5f
        player.holding(ItemStack(PazItems.SUN, 2))
        plant.mobInteract(player, InteractionHand.MAIN_HAND)
        helper.assertTrue(plant.health > 5f, "sun did not heal")
        helper.assertValueEqual(player.mainHandItem.count, 1, "suns left")
        helper.removeMockPlayer(player)
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun plantGrowsSeedsFromWaterAndSun(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        helper.setBlock(7, 1, 7, Blocks.FARMLAND)
        val plant = helper.spawn(PazEntities.CABBAGE_PULT, 7, 2, 7)
        plant.tame(player)
        helper.runAfterDelay(5) {
            player.holding(ItemStack(Items.WATER_BUCKET))
            plant.mobInteract(player, InteractionHand.MAIN_HAND)
            helper.assertTrue(plant.receivedWater > 0, "plant not watered")
            helper.assertTrue(player.mainHandItem.`is`(Items.BUCKET), "bucket not emptied")
            helper.assertTrue(plant.seedGrowCooldown > 0, "watering did not start the grow timer")
            plant.seedGrowCooldown = 0
            var suns = 0
            while (helper.getEntities(EntityType.ITEM).none { it.item.`is`(PazItems.SEED_PACKET) } && suns++ < 64) {
                player.holding(ItemStack(PazItems.SUN))
                plant.mobInteract(player, InteractionHand.MAIN_HAND)
            }
            val seeds = helper.getEntities(EntityType.ITEM).firstOrNull { it.item.`is`(PazItems.SEED_PACKET) }
            helper.assertTrue(seeds != null, "no seed packet after $suns suns")
            helper.assertTrue(SeedPacketItem.typeFromStack(seeds!!.item) == PazEntities.CABBAGE_PULT, "seed packet plant")
            helper.assertValueEqual(plant.receivedSun, 0, "received sun after harvest")
            helper.assertTrue(player.hasPazAdvancement("grow_plant_seeds"), "grow_plant_seeds advancement not granted")
            helper.removeMockPlayer(player)
            helper.succeed()
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun shovelRelocatesPlant(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        val plant = helper.spawn(PazEntities.PEA_SHOOTER, 7, 2, 7)
        plant.tame(player)
        player.holding(ItemStack(Items.IRON_SHOVEL))
        plant.mobInteract(player, InteractionHand.MAIN_HAND)
        helper.assertTrue(plant.isRemoved, "plant not dug up")
        helper.assertTrue(player.hasPazAdvancement("relocation"), "relocation advancement not granted")
        helper.removeMockPlayer(player)
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun plantWornOnPlayerHead(helper: GameTestHelper) {
        val player = helper.mockPlayer(7.5, 2.0, 9.5)
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack(PazItems.PLANT_POT_HELMET))
        val plant = helper.spawn(PazEntities.PEA_SHOOTER, 7, 2, 7)
        plant.tame(player)
        player.holding(ItemStack.EMPTY)
        player.isShiftKeyDown = true
        plant.mobInteract(player, InteractionHand.MAIN_HAND)
        helper.assertTrue(plant.isAttached() && plant.attachedEntity == player, "plant not attached")
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(plant.y > player.y + 1.0, "plant not on the player's head")
                helper.assertTrue(plant.distanceTo(player) < 2.5, "plant does not follow the player")
            }
            .thenExecute { player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY) }
            .thenWaitUntil {
                helper.assertTrue(plant.isRemoved, "plant stayed after the pot came off")
                helper.assertTrue(helper.getEntities(EntityType.ITEM).any { it.item.`is`(PazItems.SEED_PACKET) }, "plant not dropped as a seed packet")
            }
            .thenExecute { helper.removeMockPlayer(player) }
            .thenSucceed()
    }

    // ---- seed mutations ----

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 20)
    fun seedMutationsAreDataDriven(helper: GameTestHelper) {
        val level = helper.level
        for (type in listOf(PazEntities.PEA_SHOOTER, PazEntities.WALL_NUT, PazEntities.EXPLODE_O_NUT, PazEntities.MELON_PULT)) {
            helper.assertTrue(SeedMutationManager.get(level, type) != null, "no seed mutation data for $type")
        }
        helper.assertTrue(SeedMutationManager.get(level, PazEntities.SUNFLOWER) == null, "unexpected sunflower mutation data")

        val nut = helper.spawn(PazEntities.EXPLODE_O_NUT, 7, 2, 7)
        val results = (1..300).map { SeedMutationManager.resolve(nut) }.toSet()
        helper.assertTrue(results == setOf(PazEntities.WALL_NUT, PazEntities.EXPLODE_O_NUT), "explode-o-nut mutations: $results")

        val thunder = SeedMutationManager.get(level, PazEntities.ELECTRIC_PEA_SHOOTER)!!.mutations.first()
        helper.assertValueEqual(thunder.weather, Optional.of(WeatherPredicate.THUNDER), "electric peashooter rule")
        helper.assertFalse(thunder.matches(level, nut.blockPosition(), level.random), "thunder rule matched in clear weather")

        val night = TimePredicate.CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf("night")).getOrThrow()
        val range = TimePredicate.CODEC.parse(NbtOps.INSTANCE, CompoundTag().apply { putInt("min", 23000); putInt("max", 1000) }).getOrThrow()
        helper.assertValueEqual(night, TimePredicate.Named(false), "night predicate")
        helper.assertValueEqual(range, TimePredicate.Range(23000, 1000), "range predicate")
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 20)
    fun seedMutationTimeOfDay(helper: GameTestHelper) {
        helper.assertTrue(TimePredicate.Named(false).matches(helper.level), "night predicate false at night")
        helper.assertFalse(TimePredicate.Named(true).matches(helper.level), "day predicate true at night")
        helper.assertTrue(TimePredicate.Range(17000, 19500).matches(helper.level), "range predicate around midnight")
        helper.assertTrue(TimePredicate.Range(16000, 2000).matches(helper.level), "wrapping range predicate")
        helper.succeed()
    }

    // ---- projectiles ----

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun needlePiercesSeveralZombies(helper: GameTestHelper) {
        val first = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 6, 2, 7)
        val second = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        first.toughen(); second.toughen()
        val needle = Needle(helper.level)
        val start = helper.absoluteVec(Vec3(4.0, 3.0, 7.5))
        needle.setPos(start.x, start.y, start.z)
        needle.shoot(1.0, 0.0, 0.0, 1.5f, 0f)
        helper.level.addFreshEntity(needle)
        helper.succeedWhen { helper.assertTrue(first.isHurt && second.isHurt, "needle did not pierce") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun butterStunsZombie(helper: GameTestHelper) {
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 8, 2, 7)
        zombie.toughen()
        val butter = Butter(helper.level)
        val start = helper.absoluteVec(Vec3(8.5, 6.0, 7.5))
        butter.setPos(start.x, start.y, start.z)
        butter.shoot(0.0, -1.0, 0.0, 0.5f, 0f)
        helper.level.addFreshEntity(butter)
        helper.succeedWhen {
            helper.assertHasEffect(zombie, PazEffects.BUTTERED, "zombie not buttered")
            helper.assertTrue(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 0.0, "buttered zombie can move")
            helper.assertTrue(zombie.effectFlags.`plantz$getButterId`(), "butter flag not synced")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun melonSplashesNearbyZombies(helper: GameTestHelper) {
        val hit = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 7, 2, 7)
        val nearby = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 8, 2, 7)
        hit.toughen(); nearby.toughen()
        val melon = Melon(helper.level)
        val start = helper.absoluteVec(Vec3(7.5, 5.0, 7.5))
        melon.setPos(start.x, start.y, start.z)
        melon.shoot(0.0, -1.0, 0.0, 0.5f, 0f)
        helper.level.addFreshEntity(melon)
        helper.succeedWhen { helper.assertTrue(hit.isHurt && nearby.isHurt, "melon splash missed a zombie") }
    }

    /** Pea.isOnFire swaps a burning pea for a fire pea (like upstream, the fire pea starts at the shooter's eye). */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 40)
    fun burningPeaBecomesFirePea(helper: GameTestHelper) {
        val shooter = helper.spawnWithNoFreeWill(PazEntities.PEA_SHOOTER, 7, 2, 7)
        val pea = Pea(helper.level, shooter)
        helper.level.addFreshEntity(pea)
        pea.igniteForSeconds(5f)
        helper.assertTrue(pea.isOnFire, "pea not burning")
        helper.assertTrue(pea.isRemoved, "burning pea stayed")
        val firePea = helper.getEntities(PazEntities.PEA_FIRE).firstOrNull()
        helper.assertTrue(firePea != null && firePea.owner == shooter, "no fire pea from the shooter")
        helper.succeed()
    }

    // ---- balloons ----

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun balloonLiftsZombieAndLetsGoWhenItDies(helper: GameTestHelper) {
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 7, 2, 7)
        val balloon = Balloon(PazEntities.BALLOON, helper.level)
        val pos = helper.absoluteVec(Vec3(7.5, 4.5, 7.5))
        balloon.setPos(pos.x, pos.y, pos.z)
        balloon.dyeColor = DyeColor.RED
        helper.level.addFreshEntity(balloon)
        balloon.setLeashedTo(zombie, true)
        val startY = zombie.y
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(zombie.y > startY + 0.5, "balloon did not lift the zombie") }
            .thenExecute { zombie.kill() }
            .thenWaitUntil {
                helper.assertFalse(balloon.isLeashed, "balloon still leashed to a dead zombie")
                helper.assertTrue(balloon.isAlive, "balloon gone")
            }
            .thenExecute {
                // 26.1 balloons never drop a lead (Balloon.dropLeash -> removeLeash)
                helper.assertTrue(helper.getEntities(EntityType.ITEM).none { it.item.`is`(Items.LEAD) }, "balloon dropped a lead")
                balloon.hurt(balloon.damageSources().generic(), 1f)
                helper.assertTrue(balloon.isRemoved, "balloon did not pop")
            }
            .thenSucceed()
    }

    // ---- plant pot minecart ----

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 300)
    fun plantPotMinecartCarriesPlant(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        val cart = helper.spawn(PazEntities.PLANT_POT_MINECART, 7, 2, 7)
        player.holding(SeedPacketItem.stackFor(PazEntities.PEA_SHOOTER))
        cart.interact(player, InteractionHand.MAIN_HAND)
        val plant = cart.passengers.firstOrNull() as? Plant
        helper.assertTrue(plant != null, "no plant in the minecart")
        helper.assertTrue(plant!!.isTame && plant.owner == player, "minecart plant not tamed")
        helper.assertTrue(player.hasPazAdvancement("plant_pot_minecart"), "plant_pot_minecart advancement not granted")
        helper.removeMockPlayer(player)
        // longer than a plant survives on bad ground: the minecart counts as valid ground
        helper.runAfterDelay(220) {
            helper.assertTrue(plant.isAlive && plant.health == plant.maxHealth, "minecart plant dried out")
            helper.assertTrue(plant.vehicle == cart, "plant left the minecart")
            helper.succeed()
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100)
    fun plantPotMinecartDoesNotPickUpMobs(helper: GameTestHelper) {
        val cart = helper.spawn(PazEntities.PLANT_POT_MINECART, 5, 2, 7)
        val pig = helper.spawnWithNoFreeWill(EntityType.PIG, 7, 2, 7)
        helper.assertFalse(cart.canBeRidden(), "plant pot minecart is rideable")
        helper.onEachTick { if (helper.tick < 30) cart.deltaMovement = Vec3(0.3, 0.0, 0.0) }
        helper.runAfterDelay(60) {
            helper.assertTrue(pig.vehicle == null, "plant pot minecart picked up a mob")
            helper.succeed()
        }
    }

    // ---- gnome ----

    @JvmStatic
    @GameTest(template = PLATFORM, batch = GNOME_BATCH, timeoutTicks = 300)
    fun gnomeAttacksPlants(helper: GameTestHelper) {
        val gnome = helper.spawn(PazEntities.GNOME, 6, 2, 7)
        gnome.setItemSlot(EquipmentSlot.MAINHAND, ItemStack(Items.STONE_SWORD))
        val plant = helper.spawn(PazEntities.SUNFLOWER, 8, 2, 7)
        plant.toughen()
        helper.succeedWhen { helper.assertHurt(plant, "gnome did not attack the plant") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = GNOME_BATCH, timeoutTicks = 300)
    fun gnomeRidesChicken(helper: GameTestHelper) {
        val gnome = helper.spawn(PazEntities.GNOME, 6, 2, 7)
        val chicken = helper.spawnWithNoFreeWill(EntityType.CHICKEN, 8, 2, 7)
        helper.succeedWhen { helper.assertTrue(gnome.vehicle == chicken, "gnome did not mount the chicken: ${describe(gnome)} at ${helper.relativePosition(gnome)}") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = GNOME_BATCH, timeoutTicks = 20)
    fun gnomeVariantIsSaved(helper: GameTestHelper) {
        val gnome = helper.spawn(PazEntities.GNOME, 7, 2, 7)
        gnome.variant = GnomeVariant.YELLOW
        val tag = CompoundTag()
        gnome.saveWithoutId(tag)
        val copy = PazEntities.GNOME.create(helper.level)!!
        copy.load(tag)
        helper.assertValueEqual(copy.variant, GnomeVariant.YELLOW, "loaded gnome variant")
        helper.succeed()
    }

    // ---- mob effects ----

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun toxicEffectHurts(helper: GameTestHelper) {
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 7, 2, 7)
        zombie.addEffect(MobEffectInstance(PazEffects.TOXIC, 60, 0))
        helper.succeedWhen { helper.assertHurt(zombie, "toxic did no damage") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 20)
    fun frozenAndChilledSlow(helper: GameTestHelper) {
        val frozen = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 5, 2, 7)
        val chilled = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        val speed = chilled.getAttributeValue(Attributes.MOVEMENT_SPEED)
        frozen.addEffect(MobEffectInstance(PazEffects.FROZEN, 100))
        chilled.addEffect(MobEffectInstance(PazEffects.CHILLED, 100))
        helper.assertTrue(frozen.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 0.0, "frozen zombie can move")
        helper.assertFalse(frozen.canAttack(chilled), "frozen zombie can attack")
        helper.assertTrue(frozen.effectFlags.`plantz$getFrozenId`(), "frozen flag not synced")
        helper.assertTrue(chilled.getAttributeValue(Attributes.MOVEMENT_SPEED) < speed, "chilled zombie not slowed")
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 20)
    fun tangledAndButteredStopMovement(helper: GameTestHelper) {
        val tangled = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 5, 2, 7)
        val buttered = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        tangled.addEffect(MobEffectInstance(PazEffects.TANGLED, 100))
        buttered.addEffect(MobEffectInstance(PazEffects.BUTTERED, 100))
        for (zombie in listOf(tangled, buttered)) {
            helper.assertTrue(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 0.0, "zombie can still move")
            helper.assertTrue(zombie.getAttributeValue(Attributes.ATTACK_DAMAGE) <= 0.0, "zombie can still deal damage")
        }
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 40)
    fun electrifiedChainsToNearbyZombie(helper: GameTestHelper) {
        val hit = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 6, 2, 7)
        val nearby = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 8, 2, 7)
        hit.toughen(); nearby.toughen()
        hit.addEffect(MobEffectInstance(PazEffects.ELECTRIFIED, 200, 1))
        hit.hurt(hit.damageSources().source(PazDamageTypes.PLANT_ELECTRIC), 1f)
        helper.assertHurt(nearby, "zap did not chain")
        helper.assertHasEffect(nearby, PazEffects.ELECTRIFIED, "chained zombie not electrified")
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun electrifiedZapsInWater(helper: GameTestHelper) {
        helper.setBlock(7, 2, 7, Blocks.WATER)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 7, 2, 7)
        zombie.toughen()
        zombie.addEffect(MobEffectInstance(PazEffects.ELECTRIFIED, 100))
        helper.succeedWhen { helper.assertHurt(zombie, "no zap in water") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 20)
    fun electricPeashooterIsImmuneToElectricity(helper: GameTestHelper) {
        val plant = helper.spawn(PazEntities.ELECTRIC_PEA_SHOOTER, 7, 2, 7)
        helper.assertFalse(plant.hurt(plant.damageSources().source(PazDamageTypes.ZAP), 3f), "electric peashooter got zapped")
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 200)
    fun hypnotizedZombieFightsZombies(helper: GameTestHelper) {
        val hypnotized = helper.spawn(PazEntities.BROWN_COAT, 6, 2, 7)
        val victim = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 8, 2, 7)
        victim.toughen()
        hypnotized.addEffect(MobEffectInstance(PazEffects.HYPNOTIZE, 400))
        helper.assertTrue(PazEffects.HYPNOTIZED_GOAL_ATTACHMENT[hypnotized] != null, "hypnotized goal not added on start")
        helper.succeedWhen { helper.assertHurt(victim, "hypnotized zombie did not attack") }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun hypnosisGoalIsRemovedWithEffect(helper: GameTestHelper) {
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 7, 2, 7)
        zombie.addEffect(MobEffectInstance(PazEffects.HYPNOTIZE, 20))
        val goal = PazEffects.HYPNOTIZED_GOAL_ATTACHMENT[zombie]
        helper.assertTrue(goal != null, "no hypnotized goal")
        helper.succeedWhen {
            helper.assertFalse(zombie.hasEffect(PazEffects.HYPNOTIZE), "effect still active")
            helper.assertTrue(PazEffects.HYPNOTIZED_GOAL_ATTACHMENT[zombie] == null, "goal still attached")
            helper.assertTrue(zombie.targetSelector.availableGoals.none { it.goal === goal }, "goal still in the target selector")
            helper.assertFalse(zombie.effectFlags.`plantz$getHypnoId`(), "hypno flag still set")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 100)
    fun paintWashesOffInWater(helper: GameTestHelper) {
        for (y in 2..4) helper.setBlock(7, y, 7, Blocks.WATER)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 7, 2, 7)
        val red = PazEffects.PAINTED[DyeColor.RED]!!
        zombie.addEffect(MobEffectInstance(red, 400))
        helper.assertTrue(zombie.effectFlags.`plantz$getPaintedColors`().containsKey(DyeColor.RED.fireworkColor), "paint color not synced")
        helper.succeedWhen {
            helper.assertFalse(zombie.hasEffect(red), "paint stayed under water")
            helper.assertTrue(zombie.effectFlags.`plantz$getPaintedColors`().isEmpty(), "paint color flag stayed")
        }
    }

    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 20)
    fun effectFlagsSurviveSaveAndLoad(helper: GameTestHelper) {
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 7, 2, 7)
        zombie.addEffect(MobEffectInstance(PazEffects.CHILLED, 400))
        zombie.addEffect(MobEffectInstance(PazEffects.PAINTED[DyeColor.BLUE]!!, 400))
        val tag = CompoundTag()
        zombie.saveWithoutId(tag)
        val copy = PazEntities.BROWN_COAT.create(helper.level)!!
        copy.load(tag)
        val accessor = copy.effectFlags
        helper.assertTrue(copy.hasEffect(PazEffects.CHILLED) && accessor.`plantz$getChillId`(), "chill lost on reload")
        helper.assertTrue(accessor.`plantz$getPaintedColors`().containsKey(DyeColor.BLUE.fireworkColor), "paint lost on reload")
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 40)
    fun gardenHeroTracksRaidWaves(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        val hero = player as GardenHeroRewards
        player.addEffect(MobEffectInstance(PazEffects.GARDEN_HERO, 20, 2))
        helper.onEachTick { player.doTick() }
        helper.assertValueEqual(hero.`plantz$getWaveList`().size, 3, "waves added with the effect")
        helper.succeedWhenThen({ helper.removeMockPlayer(player) }) {
            helper.assertFalse(player.hasEffect(PazEffects.GARDEN_HERO), "effect still active")
            helper.assertTrue(hero.`plantz$getWaveList`().isEmpty(), "waves not cleared when the effect ended")
        }
    }

    /** The omen fires on its last tick; without a plantz flag in range no raid starts (raids themselves: zombies area). */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = NIGHT_BATCH, timeoutTicks = 40)
    fun zombieOmenIsConsumedOnLastTick(helper: GameTestHelper) {
        val player = helper.mockPlayer(7.5, 2.0, 7.5)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, Vec3(9.0, 2.0, 7.5))
        player.addEffect(MobEffectInstance(PazEffects.ZOMBIE_OMEN, 3))
        zombie.addEffect(MobEffectInstance(PazEffects.ZOMBIE_OMEN, 3))
        helper.onEachTick { player.doTick() }
        helper.succeedWhenThen({ helper.removeMockPlayer(player) }) {
            helper.assertFalse(player.hasEffect(PazEffects.ZOMBIE_OMEN), "omen not consumed")
            helper.assertFalse(zombie.hasEffect(PazEffects.ZOMBIE_OMEN), "omen stayed on a mob")
            helper.assertTrue(helper.level.getZombieRaids().getNearbyRaid(player.blockPosition(), 64) == null, "raid started without a flag")
        }
    }

    // ---- advancement triggers without a gameplay path covered above ----

    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 20)
    fun customAdvancementTriggers(helper: GameTestHelper) {
        val player = helper.mockPlayer()
        PazCriteria.BOWLING_TRIGGER.trigger(player, 9)
        helper.assertFalse(player.hasPazAdvancement("wallnut_bowling"), "bowling granted below 10 hits")
        PazCriteria.BOWLING_TRIGGER.trigger(player, 10)
        helper.assertTrue(player.hasPazAdvancement("wallnut_bowling"), "wallnut_bowling")
        PazCriteria.SEND_MAIL.trigger(player, true)
        helper.assertTrue(player.hasPazAdvancement("send_mail"), "send_mail")
        PazCriteria.RECEIVE_HERO_MAIL.trigger(player, true)
        helper.assertTrue(player.hasPazAdvancement("receive_hero_mail"), "receive_hero_mail")
        PazCriteria.WIN_ZOMBIE_RAID.trigger(player, true)
        helper.assertTrue(player.hasPazAdvancement("win_zombie_raid"), "win_zombie_raid")
        PazCriteria.START_HARDEST_RAID.trigger(player, true)
        helper.assertTrue(player.hasPazAdvancement("start_hardest_raid"), "start_hardest_raid")
        PazCriteria.RELOCATION.trigger(player, false)
        helper.assertFalse(player.hasPazAdvancement("relocation"), "relocation granted for a failed dig")
        for (wave in 1..10) PazCriteria.RAID_WAVE_TRIGGER.trigger(player, wave)
        helper.assertTrue(player.hasPazAdvancement("taco_time"), "taco_time")
        helper.removeMockPlayer(player)
        helper.succeed()
    }
}
