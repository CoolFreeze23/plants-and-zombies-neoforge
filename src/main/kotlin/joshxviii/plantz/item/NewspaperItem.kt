package joshxviii.plantz.item

import joshxviii.plantz.PazSounds
import joshxviii.plantz.item.component.BlockingItemBreakChance
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.ItemAbilities
import net.neoforged.neoforge.common.ItemAbility
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent
import kotlin.math.acos

class NewspaperItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        player.startUsingItem(hand)
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8f, 1.2f)
        return InteractionResultHolder.consume(player.getItemInHand(hand))
    }

    // Fabric FuelValueEvents: builder.add(NEWSPAPER, baseSmeltTime)
    override fun getBurnTime(itemStack: ItemStack, recipeType: RecipeType<*>?): Int = 200

    // 26.1 BREAK_SOUND component
    override fun getBreakingSound(): SoundEvent = SoundEvents.SHIELD_BREAK

    override fun releaseUsing(
        itemStack: ItemStack,
        level: Level,
        entity: LivingEntity,
        remainingTime: Int
    ) {
        super.releaseUsing(itemStack, level, entity, remainingTime)
    }

    override fun getUseDuration(itemStack: ItemStack, user: LivingEntity): Int {
        return 72000
    }

    override fun onUseTick(level: Level, livingEntity: LivingEntity, itemStack: ItemStack, ticksRemaining: Int) {
        super.onUseTick(level, livingEntity, itemStack, ticksRemaining)
    }

    override fun getUseAnimation(itemStack: ItemStack): UseAnim {
        return UseAnim.TOOT_HORN
    }

    // 26.1 BLOCKS_ATTACKS: 1.21.1 shields are items with the SHIELD_BLOCK ability (5 tick block delay, like blockDelaySeconds 0.25)
    override fun canPerformAction(stack: ItemStack, itemAbility: ItemAbility): Boolean =
        itemAbility == ItemAbilities.SHIELD_BLOCK || super.canPerformAction(stack, itemAbility)

    companion object {
        /** 26.1 BlocksAttacks.DamageReduction(horizontalBlockingAngle = 80, base = 0, factor = 0.9). */
        const val BLOCKING_ANGLE = 80.0
        const val BLOCKED_DAMAGE_FACTOR = 0.9f

        /**
         * The rest of 26.1's BLOCKS_ATTACKS for the newspaper on top of 1.21.1's shield blocking (which blocks all of the damage
         * from anywhere in front): the 80 degree arc, the 90% reduction, the paper block sound, and BLOCKING_ITEM_BREAK_CHANCE
         * in place of shield durability damage.
         */
        @JvmStatic
        fun onShieldBlock(event: LivingShieldBlockEvent) {
            val user = event.entity
            val stack = user.useItem
            if (!event.blocked || stack.item !is NewspaperItem) return
            val level = user.level() as? ServerLevel ?: return
            val sourcePos = event.damageSource.sourcePosition
            if (sourcePos != null) {
                val look = user.calculateViewVector(0.0f, user.yHeadRot)
                val toSource = user.position().vectorTo(sourcePos).let { Vec3(it.x, 0.0, it.z).normalize() }
                val angle = Math.toDegrees(acos(toSource.dot(look).coerceIn(-1.0, 1.0)))
                if (angle > BLOCKING_ANGLE) {
                    event.blocked = false
                    return
                }
            }
            val blocked = event.originalBlockedDamage * BLOCKED_DAMAGE_FACTOR
            event.blockedDamage = blocked
            event.setShieldDamage(0f)
            BlockingItemBreakChance.handle(level, stack, user, blocked)
            level.playSound(null, user.x, user.y, user.z, PazSounds.PROJECTILE_HIT_PAPER, user.soundSource, 1.0f, 0.8f + level.random.nextFloat() * 0.4f)
        }
    }
}