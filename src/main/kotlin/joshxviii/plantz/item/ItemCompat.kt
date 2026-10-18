package joshxviii.plantz.item

import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent

/** 26.1 `Player.sendOverlayMessage(component)` (action bar message). */
internal fun Player.sendOverlayMessage(message: Component) = displayClientMessage(message, true)

/** 26.1 `ItemStack.hurtAndBreak(amount, owner, hand)`. */
internal fun ItemStack.hurtAndBreak(amount: Int, owner: LivingEntity, hand: InteractionHand) =
    hurtAndBreak(amount, owner, LivingEntity.getSlotForHand(hand))

/** 26.1 `ItemStack.nextDamageWillBreak()`. */
internal fun ItemStack.nextDamageWillBreak(): Boolean = isDamageableItem && damageValue >= maxDamage - 1

/** 26.1 `ItemStack.causeUseVibration(causer, event)` (default USE_EFFECTS has interact vibrations on). */
internal fun ItemStack.causeUseVibration(causer: Entity, event: Holder<GameEvent>) = causer.gameEvent(event)

/**
 * 26.1 `Equippable.swapWithEquipmentSlot` (right-click to equip). Unlike 1.21.1's `Equipable`, only one item of a
 * stack is equipped; the rest stays in the hand and the previously worn item goes to the inventory.
 */
internal fun swapWithEquipmentSlot(level: Level, player: Player, hand: InteractionHand, slot: EquipmentSlot): InteractionResultHolder<ItemStack> {
    val inHand = player.getItemInHand(hand)
    if (!player.canUseSlot(slot)) return InteractionResultHolder.pass(inHand)
    val inSlot = player.getItemBySlot(slot)
    if ((EnchantmentHelper.has(inSlot, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) && !player.isCreative)
        || ItemStack.isSameItemSameComponents(inHand, inSlot)) return InteractionResultHolder.fail(inHand)

    if (!level.isClientSide) player.awardStat(Stats.ITEM_USED.get(inHand.item))
    if (inHand.count <= 1) {
        val toHand = if (inSlot.isEmpty) inHand else inSlot.copyAndClear()
        val toSlot = if (player.isCreative) inHand.copy() else inHand.copyAndClear()
        player.setItemSlot(slot, toSlot)
        return InteractionResultHolder.sidedSuccess(toHand, level.isClientSide)
    }
    val toInventory = inSlot.copyAndClear()
    player.setItemSlot(slot, inHand.consumeAndReturn(1, player))
    if (!player.inventory.add(toInventory)) player.drop(toInventory, false)
    return InteractionResultHolder.sidedSuccess(inHand, level.isClientSide)
}
