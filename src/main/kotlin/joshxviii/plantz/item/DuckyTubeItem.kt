package joshxviii.plantz.item

import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

class DuckyTubeItem(properties: Properties) : Item(properties), Equipable {
    override fun getEquipmentSlot(): EquipmentSlot = EquipmentSlot.LEGS

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> =
        swapWithEquipmentSlot(level, player, hand, EquipmentSlot.LEGS)

    override fun isValidRepairItem(stack: ItemStack, repairCandidate: ItemStack): Boolean = repairCandidate.`is`(Items.PHANTOM_MEMBRANE)
}
