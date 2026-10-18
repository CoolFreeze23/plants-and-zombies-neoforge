package joshxviii.plantz.item

import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/** 26.1 `Player.sendOverlayMessage(component)` (action bar message). */
internal fun Player.sendOverlayMessage(message: Component) = displayClientMessage(message, true)

/** 26.1 `ItemStack.hurtAndBreak(amount, owner, hand)`. */
internal fun ItemStack.hurtAndBreak(amount: Int, owner: LivingEntity, hand: InteractionHand) =
    hurtAndBreak(amount, owner, LivingEntity.getSlotForHand(hand))

/** 26.1 `ItemStack.nextDamageWillBreak()`. */
internal fun ItemStack.nextDamageWillBreak(): Boolean = isDamageableItem && damageValue >= maxDamage - 1

/** 26.1 `ItemStack.causeUseVibration(causer, event)` (default USE_EFFECTS has interact vibrations on). */
internal fun ItemStack.causeUseVibration(causer: net.minecraft.world.entity.Entity, event: net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent>) =
    causer.gameEvent(event)
