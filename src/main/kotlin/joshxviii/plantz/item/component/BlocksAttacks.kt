package joshxviii.plantz.item.component

import joshxviii.plantz.PazComponents
import joshxviii.plantz.entity.projectile.PazProjectile
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.tags.DamageTypeTags
import net.minecraft.tags.TagKey
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.AbstractArrow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent
import kotlin.math.acos

/**
 * 26.1's `minecraft:blocks_attacks` component on 1.21.1, whose shields are items with the SHIELD_BLOCK ability that block
 * all of a hit from anywhere in front. [onShieldBlock] applies these numbers on NeoForge's shield event instead, for the
 * item in use (26.1 `applyItemBlocking`) and the worn body blocker ([WornBodyBlockerComponent]).
 */
data class BlocksAttacks(
    val blockDelaySeconds: Float,
    val disableCooldownScale: Float,
    val damageReductions: List<DamageReduction>,
    val itemDamage: ItemDamageFunction = ItemDamageFunction.DEFAULT,
    val bypassedBy: TagKey<DamageType>? = DamageTypeTags.BYPASSES_SHIELD,
    val blockSound: SoundEvent? = null,
    val disableSound: SoundEvent? = null,
) {
    data class DamageReduction(val horizontalBlockingAngle: Float, val base: Float, val factor: Float) {
        fun resolve(dealtDamage: Float, angle: Double): Float {
            if (angle > Math.toRadians(horizontalBlockingAngle.toDouble())) return 0f
            return Mth.clamp(base + factor * dealtDamage, 0f, dealtDamage)
        }
    }

    data class ItemDamageFunction(val threshold: Float, val base: Float, val factor: Float) {
        fun apply(dealtDamage: Float): Int = if (dealtDamage < threshold) 0 else Mth.floor(base + factor * dealtDamage)

        companion object {
            val DEFAULT = ItemDamageFunction(1f, 0f, 1f)
        }
    }

    fun itemDamage(): ItemDamageFunction = itemDamage

    fun resolveBlockedDamage(dealtDamage: Float, angle: Double): Float =
        Mth.clamp(damageReductions.sumOf { it.resolve(dealtDamage, angle).toDouble() }.toFloat(), 0f, dealtDamage)

    fun canBlockFrom(angle: Double): Boolean =
        damageReductions.any { (it.base > 0f || it.factor > 0f) && angle <= Math.toRadians(it.horizontalBlockingAngle.toDouble()) }

    fun hurtBlockingItem(level: Level, item: ItemStack, user: LivingEntity, slot: EquipmentSlot, damage: Float) {
        BlockingItemBreakChance.handle(level, item, user, damage, slot)
    }

    fun onBlocked(level: ServerLevel, user: LivingEntity) {
        blockSound?.let { level.playSound(null, user.x, user.y, user.z, it, user.soundSource, 1f, 0.8f + level.random.nextFloat() * 0.4f) }
    }

    companion object {
        /** 26.1 `LivingEntity.getItemBlockingWith`'s item in use: blocking (5 ticks raised) with this component. */
        @JvmStatic
        fun heldBlocker(user: LivingEntity): ItemStack? = user.useItem.takeIf { user.isBlocking && it.has(PazComponents.BLOCKS_ATTACKS) }

        /** The chest item that blocks without being raised. */
        @JvmStatic
        fun wornBlocker(user: LivingEntity): ItemStack? = user.getItemBySlot(EquipmentSlot.CHEST)
            .takeIf { it.has(PazComponents.WORN_BODY_BLOCKER) && it.has(PazComponents.BLOCKS_ATTACKS) }

        /** The horizontal angle between where [user] looks and [pos] (26.1 `applyItemBlocking`); a hit from nowhere counts as from behind. */
        @JvmStatic
        fun angleTo(user: LivingEntity, pos: Vec3?): Double {
            if (pos == null) return Math.PI
            val look = user.calculateViewVector(0f, user.yHeadRot)
            val toSource = pos.subtract(user.position()).let { Vec3(it.x, 0.0, it.z).normalize() }
            return acos(toSource.dot(look).coerceIn(-1.0, 1.0))
        }

        private fun ignoresBlocking(source: DamageSource): Boolean {
            val direct = source.directEntity
            return (direct is AbstractArrow && direct.pierceLevel > 0) || (direct is PazProjectile && direct.getPierceLevel() > 0)
        }

        @JvmStatic
        fun onShieldBlock(event: LivingShieldBlockEvent) {
            val user = event.entity
            val level = user.level() as? ServerLevel ?: return
            val held = heldBlocker(user)
            val worn = wornBlocker(user)
            if (held == null && worn == null) return
            if (held == null && event.blocked) return // a plain 1.21.1 shield already took all of it

            val source = event.damageSource
            val damage = event.originalBlockedDamage
            val angle = angleTo(user, source.sourcePosition)
            var remaining = damage
            if (!ignoresBlocking(source)) {
                val blockers = listOfNotNull(
                    held?.let { it to LivingEntity.getSlotForHand(user.usedItemHand) },
                    worn?.takeIf { it !== held }?.let { it to EquipmentSlot.CHEST },
                )
                for ((stack, slot) in blockers) {
                    val blocksAttacks = stack.get(PazComponents.BLOCKS_ATTACKS) ?: continue
                    if (blocksAttacks.bypassedBy != null && source.`is`(blocksAttacks.bypassedBy)) continue
                    val blocked = blocksAttacks.resolveBlockedDamage(remaining, angle)
                    if (blocked <= 0f) continue
                    blocksAttacks.hurtBlockingItem(level, stack, user, slot, blocked)
                    blocksAttacks.onBlocked(level, user)
                    remaining -= blocked
                    if (remaining <= 0f) break
                }
            }
            event.blocked = remaining < damage
            if (event.blocked) event.blockedDamage = damage - remaining
            event.setShieldDamage(0f) // BlockingItemBreakChance did the item's damage
        }
    }
}
