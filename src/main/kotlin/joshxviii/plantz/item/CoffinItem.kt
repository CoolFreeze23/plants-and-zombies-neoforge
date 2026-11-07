package joshxviii.plantz.item

import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.ItemTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

/** 26.1's coffin item: EQUIPPABLE(CHEST), repairable(PLANKS), enchantable(1), BREAK_SOUND. */
class CoffinItem(properties: Properties) : Item(properties), Equipable {
    override fun getEquipmentSlot(): EquipmentSlot = EquipmentSlot.CHEST

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> =
        swapWithEquipmentSlot(level, player, hand, EquipmentSlot.CHEST)

    override fun isValidRepairItem(stack: ItemStack, repairCandidate: ItemStack): Boolean = repairCandidate.`is`(ItemTags.PLANKS)

    override fun getEnchantmentValue(stack: ItemStack): Int = 1

    override fun getBreakingSound(): SoundEvent = SoundEvents.SHIELD_BREAK
}
