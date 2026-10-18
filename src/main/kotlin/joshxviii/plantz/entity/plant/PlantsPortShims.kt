package joshxviii.plantz.entity.plant

import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantedItemInUse
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3

/* 26.1 API helpers that 1.21.1 lacks, used by plants, projectiles, AI goals, effects, suns and balloons. */

/** 26.1 `Entity.getHeadLookAngle()` (= calculateViewVector(xRot, yHeadRot)). */
val Entity.headLookAngle: Vec3
    get() = calculateViewVector(xRot, yHeadRot)

/** 26.1 `Entity.getKnownSpeed()`: position change over the last tick. */
val Entity.knownSpeed: Vec3
    get() {
        val controller = controllingPassenger
        if (controller is Player && isAlive) return controller.knownSpeed
        if (this is Player) return knownMovement// ServerPlayer: last client-reported movement
        return Vec3(x - xo, y - yo, z - zo)
    }

/**
 * 26.1 `Projectile.spawnProjectile(projectile, level, itemStack)`: adds the projectile, then runs the item's
 * projectile-spawned enchantment effects (an arrow's weapon effects already run in the 1.21.1 AbstractArrow constructor).
 */
fun spawnProjectile(projectile: Projectile, level: ServerLevel, itemStack: ItemStack) {
    level.addFreshEntity(projectile)
    val itemInUse = EnchantedItemInUse(itemStack, null, projectile.owner as? LivingEntity) {}
    EnchantmentHelper.runIterationOnItem(itemStack) { enchantment, enchantmentLevel ->
        enchantment.value().onProjectileSpawned(level, enchantmentLevel, itemInUse, projectile)
    }
}

/** 26.1 `Direction.getUnitVec3()`. */
val Direction.unitVec3: Vec3
    get() = Vec3.atLowerCornerOf(this.normal)

/** 26.1 `Vec3.horizontal()`. */
fun Vec3.horizontal(): Vec3 = Vec3(x, 0.0, z)

/**
 * 26.1 `InterpolationHandler` (client-side smoothing of server position updates, 3 steps by default).
 * Wire it up through the entity's `lerpTo`/`lerpTarget*` overrides and call [interpolate] each tick.
 */
class InterpolationHandler(private val entity: Entity, private val interpolationSteps: Int = 3) {
    private var steps = 0
    private var targetPosition: Vec3 = Vec3.ZERO
    private var targetYRot = 0f
    private var targetXRot = 0f
    private var previousTickPosition: Vec3? = null
    private var previousTickRot: Vec2? = null

    fun position(): Vec3 = if (steps > 0) targetPosition else entity.position()
    fun yRot(): Float = if (steps > 0) targetYRot else entity.yRot
    fun xRot(): Float = if (steps > 0) targetXRot else entity.xRot

    fun hasActiveInterpolation(): Boolean = steps > 0

    fun interpolateTo(position: Vec3, yRot: Float, xRot: Float) {
        if (interpolationSteps == 0) {
            entity.moveTo(position.x, position.y, position.z, yRot, xRot)
            cancel()
        } else if (!hasActiveInterpolation() || yRot() != yRot || xRot() != xRot || position() != position) {
            steps = interpolationSteps
            targetPosition = position
            targetYRot = yRot
            targetXRot = xRot
            previousTickPosition = entity.position()
            previousTickRot = Vec2(entity.xRot, entity.yRot)
        }
    }

    fun interpolate() {
        if (!hasActiveInterpolation()) {
            cancel()
            return
        }
        val alpha = 1.0 / steps
        previousTickPosition?.let { previous ->
            val delta = entity.position().subtract(previous)
            val box = entity.boundingBox.move(targetPosition.add(delta).subtract(entity.position()))
            if (entity.level().noCollision(entity, box)) targetPosition = targetPosition.add(delta)
        }
        previousTickRot?.let { previous ->
            targetYRot += entity.yRot - previous.y
            targetXRot += entity.xRot - previous.x
        }
        val newPosition = Vec3(
            Mth.lerp(alpha, entity.x, targetPosition.x),
            Mth.lerp(alpha, entity.y, targetPosition.y),
            Mth.lerp(alpha, entity.z, targetPosition.z)
        )
        entity.setPos(newPosition)
        entity.yRot = (entity.yRot + alpha * Mth.wrapDegrees((targetYRot - entity.yRot).toDouble())).toFloat() % 360.0f
        entity.xRot = Mth.lerp(alpha, entity.xRot.toDouble(), targetXRot.toDouble()).toFloat() % 360.0f
        steps--
        previousTickPosition = newPosition
        previousTickRot = Vec2(entity.xRot, entity.yRot)
    }

    fun cancel() {
        steps = 0
        previousTickPosition = null
        previousTickRot = null
    }
}
