package joshxviii.plantz.item

import joshxviii.plantz.PazComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.WeakHashMap
import kotlin.jvm.optionals.getOrNull

/**
 * 26.1 item cooldown groups for seed packets. Upstream stamped a USE_COOLDOWN component with the plant's group on the
 * stack (see [SeedPacketItem.applyCooldown]) and used `player.cooldowns.addCooldown(group, ticks)`; 1.21.1 ItemCooldowns
 * is keyed by Item only, so the groups are kept here per player (ticks of `player.tickCount`).
 *
 * Server side is authoritative; [syncListener] mirrors changes to the client, whose handler calls [addCooldown].
 */
object SeedPacketCooldowns {
    private class Cooldown(val start: Int, val end: Int)

    private val cooldowns = WeakHashMap<Player, MutableMap<ResourceLocation, Cooldown>>()

    /** Called on the server whenever a cooldown is set (ticks > 0) or cleared (ticks == 0). */
    @JvmStatic
    var syncListener: ((player: ServerPlayer, group: ResourceLocation, ticks: Int) -> Unit)? = null

    @JvmStatic
    fun groupFor(type: EntityType<*>): ResourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(type)

    /** 26.1 `ItemCooldowns.getCooldownGroup`: the stack's USE_COOLDOWN group, else the item id. */
    @JvmStatic
    fun groupFor(stack: ItemStack): ResourceLocation =
        stack.get(PazComponents.USE_COOLDOWN)?.cooldownGroup?.getOrNull() ?: BuiltInRegistries.ITEM.getKey(stack.item)

    /** 26.1 `UseCooldown.apply` after a successful instant use of [stackBeforeUse]. */
    @JvmStatic
    fun applyUseCooldown(player: Player, stackBeforeUse: ItemStack) {
        val useCooldown = stackBeforeUse.get(PazComponents.USE_COOLDOWN) ?: return
        addCooldown(player, groupFor(stackBeforeUse), useCooldown.ticks())
    }

    private fun active(player: Player, group: ResourceLocation): Cooldown? {
        val map = synchronized(cooldowns) { cooldowns[player] } ?: return null
        val cooldown = map[group] ?: return null
        if (player.tickCount >= cooldown.end || player.tickCount < cooldown.start) {
            map.remove(group)
            return null
        }
        return cooldown
    }

    @JvmStatic
    fun isOnCooldown(player: Player, group: ResourceLocation): Boolean = active(player, group) != null

    @JvmStatic
    fun isOnCooldown(player: Player, stack: ItemStack): Boolean = isOnCooldown(player, groupFor(stack))

    @JvmStatic
    fun getCooldownPercent(player: Player, stack: ItemStack, partialTick: Float): Float {
        val cooldown = active(player, groupFor(stack)) ?: return 0f
        val total = (cooldown.end - cooldown.start).toFloat()
        val remaining = cooldown.end - (player.tickCount + partialTick)
        return Mth.clamp(remaining / total, 0f, 1f)
    }

    @JvmStatic
    fun addCooldown(player: Player, group: ResourceLocation, ticks: Int) {
        if (ticks <= 0) {
            removeCooldown(player, group)
            return
        }
        synchronized(cooldowns) {
            cooldowns.getOrPut(player) { HashMap() }[group] = Cooldown(player.tickCount, player.tickCount + ticks)
        }
        if (player is ServerPlayer) syncListener?.invoke(player, group, ticks)
    }

    @JvmStatic
    fun removeCooldown(player: Player, group: ResourceLocation) {
        synchronized(cooldowns) { cooldowns[player]?.remove(group) }
        if (player is ServerPlayer) syncListener?.invoke(player, group, 0)
    }
}
