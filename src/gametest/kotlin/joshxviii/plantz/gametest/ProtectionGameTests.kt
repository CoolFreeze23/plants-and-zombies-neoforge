package joshxviii.plantz.gametest

import joshxviii.plantz.gametest.zombies.discardEntities
import joshxviii.plantz.gametest.blocks.check
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.entity.zombie.PazZombie
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.projectile.Arrow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.ItemAbilities
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

/**
 * Damage protection: head protection on worn items (bucket, cone, football helmet; 26.1 applyItemBlocking) and the
 * newspaper's shield block (26.1 BLOCKS_ATTACKS). Zombies stand still (no goals) and take arrow hits from a set position.
 */
@GameTestHolder("plantz")
@PrefixGameTestTemplate(false)
object ProtectionGameTests {
    private const val PLATFORM = CoreGameTests.PLATFORM
    private const val BATCH = "plantz_protection"

    /** An arrow hit coming from [pos] (its source position is the arrow's). */
    private fun arrowFrom(helper: GameTestHelper, pos: Vec3): DamageSource {
        val arrow = Arrow(EntityType.ARROW, helper.level)
        arrow.setPos(pos)
        return helper.level.damageSources().arrow(arrow, null)
    }

    private fun standingZombie(helper: GameTestHelper, x: Int, z: Int): PazZombie {
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, x, 2, z)
        zombie.yRot = 0f
        zombie.yBodyRot = 0f
        zombie.yHeadRot = 0f
        return zombie
    }

    private fun PazZombie.taken() = maxHealth - health

    /** A worn bucket takes 90% of a hit from above eye level, before armor; a hit from below gets armor only. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun bucketProtectsHead(helper: GameTestHelper) {
        val bare = standingZombie(helper, 3, 7)
        val top = standingZombie(helper, 7, 7)
        val low = standingZombie(helper, 11, 7)
        for (zombie in listOf(top, low)) zombie.setItemSlot(EquipmentSlot.HEAD, ItemStack(Items.BUCKET))
        helper.check(top.getItemBySlot(EquipmentSlot.HEAD).has(PazComponents.PROTECTS_HEAD)) { "bucket has no head protection" }

        bare.hurt(arrowFrom(helper, Vec3(bare.x, bare.eyeY + 1.0, bare.z)), 6f)
        top.hurt(arrowFrom(helper, Vec3(top.x, top.eyeY + 1.0, top.z)), 6f)
        low.hurt(arrowFrom(helper, Vec3(low.x, low.y + 0.3, low.z)), 6f)
        helper.check(bare.taken() > 5f) { "bare zombie took ${bare.taken()}" }
        helper.check(top.taken() > 0f && top.taken() < bare.taken() * 0.15f) { "bucket from above: ${top.taken()} of ${bare.taken()}" }
        helper.check(low.taken() > bare.taken() * 0.5f) { "bucket from below: ${low.taken()} of ${bare.taken()}" }
        helper.discardEntities()
        helper.succeed()
    }

    /** The football helmet protects the head too. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun footballHelmetProtectsHead(helper: GameTestHelper) {
        val zombie = standingZombie(helper, 7, 7)
        val helmet = ItemStack(PazItems.FOOTBALL_HELMET)
        zombie.setItemSlot(EquipmentSlot.HEAD, helmet)
        zombie.hurt(arrowFrom(helper, Vec3(zombie.x, zombie.eyeY + 1.0, zombie.z)), 6f)
        helper.check(zombie.taken() < 1f) { "football helmet from above: ${zombie.taken()}" }
        helper.discardEntities()
        helper.succeed()
    }

    /**
     * A raised newspaper is a shield: from within 80 degrees of where its holder looks it takes 90% of the hit
     * (1.21.1 shields take all of it), from the side or behind nothing.
     */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH, timeoutTicks = 60)
    fun newspaperBlocksFromTheFront(helper: GameTestHelper) {
        val front = standingZombie(helper, 3, 7)
        val side = standingZombie(helper, 7, 7)
        val back = standingZombie(helper, 11, 7)
        val zombies = listOf(front, side, back)
        for (zombie in zombies) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack(PazItems.NEWSPAPER))
            zombie.startUsingItem(InteractionHand.MAIN_HAND)
        }
        helper.check(ItemStack(PazItems.NEWSPAPER).canPerformAction(ItemAbilities.SHIELD_BLOCK)) { "newspaper is not a shield" }
        helper.runAfterDelay(8) {
            for (zombie in zombies) {
                zombie.yRot = 0f
                zombie.yBodyRot = 0f
                zombie.yHeadRot = 0f
            }
            helper.check(zombies.all { it.isBlocking }) { "newspaper not raised" }
            // yaw 0 looks south (+z)
            front.hurt(arrowFrom(helper, front.position().add(0.5, 1.0, 4.0)), 10f)
            side.hurt(arrowFrom(helper, side.position().add(4.0, 1.0, 0.5)), 10f)
            back.hurt(arrowFrom(helper, back.position().add(0.0, 1.0, -4.0)), 10f)
            helper.check(back.taken() > 7f) { "hit from behind: ${back.taken()}" }
            helper.check(side.taken() > 7f) { "hit from the side: ${side.taken()}" }
            helper.check(front.taken() > 0f && front.taken() < back.taken() * 0.2f) { "hit from the front: ${front.taken()} of ${back.taken()}" }
            helper.discardEntities()
            helper.succeed()
        }
    }
}
