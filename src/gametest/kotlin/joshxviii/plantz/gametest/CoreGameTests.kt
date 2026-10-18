package joshxviii.plantz.gametest

import joshxviii.plantz.PazEntities
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

/**
 * Dev-only GameTests (source set `gametest`, not packaged). Every test uses the 15x6x15 grass platform
 * `plantz:platform` (floor at y=0, air above). Run all with `./gradlew runGameTestServer`.
 * Area test files live next to this one (one object per area, all with the same two annotations).
 */
@GameTestHolder("plantz")
@PrefixGameTestTemplate(false)
object CoreGameTests {
    const val PLATFORM = "platform"

    /** A peashooter on its own targets and damages a browncoat in range. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    fun peashooterDamagesZombie(helper: GameTestHelper) {
        val plant = helper.spawn(PazEntities.PEA_SHOOTER, 3, 1, 7)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, 11, 1, 7)
        val startHealth = zombie.health
        helper.succeedWhen {
            helper.assertTrue(plant.isAlive, "peashooter died")
            helper.assertTrue(zombie.health < startHealth || !zombie.isAlive, "zombie took no damage")
        }
    }
}
