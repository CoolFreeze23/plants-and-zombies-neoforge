package joshxviii.plantz.item

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

class NewspaperItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        player.startUsingItem(hand)
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8f, 1.2f)
        return InteractionResultHolder.consume(player.getItemInHand(hand))
    }

    // Fabric FuelValueEvents: builder.add(NEWSPAPER, baseSmeltTime)
    override fun getBurnTime(itemStack: ItemStack, recipeType: RecipeType<*>?): Int = 200

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
}