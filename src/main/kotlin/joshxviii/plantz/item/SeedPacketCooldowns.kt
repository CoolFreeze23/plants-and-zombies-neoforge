package joshxviii.plantz.item

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.WeakHashMap

/**
 * Per-plant seed packet cooldowns. 26.1 used the USE_COOLDOWN component with a cooldown group per entity type
 * (`player.cooldowns.addCooldown(group, ticks)`); 1.21.1 ItemCooldowns is keyed by Item only, so all seed packets
 * would share one cooldown. This keeps one cooldown per plant type per player instead (ticks of `player.tickCount`).
 *
 * Server side is authoritative. For the client (hotbar overlay / prediction) the lead can set [syncListener] to send a
 * payload, and the client handler calls [addCooldown] on the local player with the same group and ticks.
 */
object SeedPacketCooldowns {
    private class Cooldown(val start: Int, val end: Int)

    private val cooldowns = WeakHashMap<Player, MutableMap<ResourceLocation, Cooldown>>()

    /** Called on the server whenever a cooldown is set (ticks > 0) or cleared (ticks == 0). */
    @JvmStatic
    var syncListener: ((player: ServerPlayer, group: ResourceLocation, ticks: Int) -> Unit)? = null

    @JvmStatic
    fun groupFor(type: EntityType<*>): ResourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(type)

    @JvmStatic
    fun groupFor(stack: ItemStack): ResourceLocation? = stack.entityDataType()?.let { groupFor(it) }

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
    fun isOnCooldown(player: Player, stack: ItemStack): Boolean {
        val group = groupFor(stack) ?: return false
        return isOnCooldown(player, group)
    }

    @JvmStatic
    fun getCooldownPercent(player: Player, stack: ItemStack, partialTick: Float): Float {
        val group = groupFor(stack) ?: return 0f
        val cooldown = active(player, group) ?: return 0f
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
