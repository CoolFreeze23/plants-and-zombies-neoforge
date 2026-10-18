package joshxviii.plantz.item

import net.minecraft.core.Holder
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

/**
 * 1.21.1 stand-in for a block item carrying a 26.1 `EQUIPPABLE` component (cone on the head, flags in the off hand).
 * [swappable] mirrors 26.1 `Equippable.swappable`: when true, using the item in the air equips it.
 */
class EquipableBlockItem(
    block: Block,
    properties: Properties,
    private val slot: EquipmentSlot,
    private val equipSound: Holder<SoundEvent> = SoundEvents.ARMOR_EQUIP_GENERIC,
    private val swappable: Boolean = true,
) : BlockItem(block, properties), Equipable {
    override fun getEquipmentSlot(): EquipmentSlot = slot
    override fun getEquipSound(): Holder<SoundEvent> = equipSound

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        return if (swappable) swapWithEquipmentSlot(this, level, player, hand) else super.use(level, player, hand)
    }
}
