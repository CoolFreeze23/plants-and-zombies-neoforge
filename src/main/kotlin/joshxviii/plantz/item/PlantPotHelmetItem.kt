package joshxviii.plantz.item

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

/** 26.1 EQUIPPABLE(HEAD, generic equip sound) + repairable(brick). Rendered on the head by the item model. */
class PlantPotHelmetItem(properties: Properties) : Item(properties), Equipable {
    companion object {
        fun addToTooltip(consumer: MutableList<Component>, shiftKey: Component) {
            consumer.add(1, Component.translatable("item.plantz.plant_pot_helmet.description", shiftKey)
                .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC))
        }
    }

    override fun getEquipmentSlot(): EquipmentSlot = EquipmentSlot.HEAD

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> =
        swapWithEquipmentSlot(this, level, player, hand)

    override fun isValidRepairItem(stack: ItemStack, repairCandidate: ItemStack): Boolean = repairCandidate.`is`(Items.BRICK)
}