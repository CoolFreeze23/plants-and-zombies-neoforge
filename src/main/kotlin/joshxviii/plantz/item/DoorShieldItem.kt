package joshxviii.plantz.item

import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazItems
import joshxviii.plantz.item.component.DoorShieldComponent
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.common.ItemAbilities
import net.neoforged.neoforge.common.ItemAbility

class DoorShieldItem(properties: Properties) : Item(properties), Equipable {

    override fun getName(itemStack: ItemStack): Component {
        val doorData = itemStack.get(PazComponents.DOOR_SHIELD) ?: return super.getName(itemStack)
        return Component.translatable("item.plantz.door_shield.type", doorData.doorType.name)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        player.startUsingItem(hand)
        return InteractionResultHolder.consume(player.getItemInHand(hand))
    }

    override fun getUseAnimation(itemStack: ItemStack): UseAnim = UseAnim.TOOT_HORN

    // 26.1 Item.getUseDuration: 72000 with BLOCKS_ATTACKS
    override fun getUseDuration(itemStack: ItemStack, user: LivingEntity): Int = 72000

    // 26.1 equippableUnswappable(OFFHAND)
    override fun getEquipmentSlot(): EquipmentSlot = EquipmentSlot.OFFHAND

    // 26.1 BLOCKS_ATTACKS: 1.21.1 shields are items with the SHIELD_BLOCK ability
    override fun canPerformAction(stack: ItemStack, itemAbility: ItemAbility): Boolean =
        itemAbility == ItemAbilities.SHIELD_BLOCK || super.canPerformAction(stack, itemAbility)

    // 26.1 BREAK_SOUND
    override fun getBreakingSound(): SoundEvent = SoundEvents.SHIELD_BREAK

    companion object {
        fun stackFor(doorType: Block): ItemStack {
            val stack = ItemStack(PazItems.DOOR_SHIELD)
            stack.set(PazComponents.DOOR_SHIELD, DoorShieldComponent(doorType))
            return stack
        }
    }
}
