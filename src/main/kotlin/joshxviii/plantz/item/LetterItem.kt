package joshxviii.plantz.item

import joshxviii.plantz.inventory.LetterMenu
import joshxviii.plantz.compat.menu.ExtendedMenuProvider
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class LetterItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (!level.isClientSide && player is ServerPlayer) {
            val handIndex = if (hand == InteractionHand.MAIN_HAND) 0 else 1
            player.openMenu(object : ExtendedMenuProvider<Int> {
                override fun getDisplayName(): Component = Component.translatable("gui.plantz.letter")

                override fun createMenu(
                    containerId: Int,
                    inventory: Inventory,
                    player: Player
                ): AbstractContainerMenu = LetterMenu(containerId, inventory, handIndex)

                override fun getScreenOpeningData(player: ServerPlayer?): Int = handIndex
            })
        }
        player.awardStat(Stats.ITEM_USED.get(this))
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }

}