package joshxviii.plantz.entity.plant

import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

/*
 * Small 26.1 API shims used by the plants area (plants, projectiles, ai, effects).
 * Kept in this package (and imported explicitly elsewhere) so they never clash with
 * helpers the core area may add to joshxviii.plantz.
 */

/** 26.1 `OwnableEntity.getRootOwner()`: walks the owner chain, null on a cycle. */
val OwnableEntity.rootOwner: LivingEntity?
    get() {
        val seen = HashSet<Any?>()
        var owner = this.owner
        seen.add(this)
        while (owner is OwnableEntity) {
            val ownersOwner = owner.owner
            if (seen.contains(ownersOwner)) return null
            seen.add(owner)
            owner = ownersOwner
        }
        return owner
    }

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

/** 26.1 `Direction.getUnitVec3()`. */
val Direction.unitVec3: Vec3
    get() = Vec3.atLowerCornerOf(this.normal)

/** 26.1 `Vec3.horizontal()`. */
fun Vec3.horizontal(): Vec3 = Vec3(x, 0.0, z)
