@file:JvmName("ModelCompatUtils")

package joshxviii.plantz.renderer

import joshxviii.plantz.compat.client.state.ArmedEntityRenderState
import net.minecraft.client.model.AnimationUtils
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3f

/*
 * 26.1 model helpers that do not exist on 1.21.1, called from the Java models.
 */

/** 26.1 `ModelPart.rotateBy(Quaternionf)`: post-multiplies the part's current rotation by [rotation]. */
fun rotateBy(part: ModelPart, rotation: Quaternionf) {
    val oldRotation = Matrix3f().rotationZYX(part.zRot, part.yRot, part.xRot)
    val newRotation = oldRotation.rotate(rotation)
    val newAngles = newRotation.getEulerAnglesZYX(Vector3f())
    part.setRotation(newAngles.x, newAngles.y, newAngles.z)
}

/** 26.1 `AnimationUtils.animateZombieArms(leftArm, rightArm, aggressive, state)`. */
fun animateZombieArms(leftArm: ModelPart, rightArm: ModelPart, aggressive: Boolean, state: ArmedEntityRenderState) {
    if (!state.isBaby || state.mainHandItemStack.isEmpty) {
        val attackTime = state.attackTime
        val armDrop = -Math.PI.toFloat() / (if (aggressive) 1.5f else 2.25f)
        val attackYRotModifier = Mth.sin(attackTime * Math.PI.toFloat())
        val attackXRotModifier = Mth.sin((1.0f - (1.0f - attackTime) * (1.0f - attackTime)) * Math.PI.toFloat())
        rightArm.zRot = 0.0f
        rightArm.yRot = -(0.1f - attackYRotModifier * 0.6f)
        rightArm.xRot = armDrop
        rightArm.xRot += attackYRotModifier * 1.2f - attackXRotModifier * 0.4f
        leftArm.zRot = 0.0f
        leftArm.yRot = 0.1f - attackYRotModifier * 0.6f
        leftArm.xRot = armDrop
        leftArm.xRot += attackYRotModifier * 1.2f - attackXRotModifier * 0.4f
        AnimationUtils.bobArms(rightArm, leftArm, state.ageInTicks)
    }
}

/** 26.1 `AnimationUtils.animateCrossbowCharge(rightArm, leftArm, maxChargeDuration, ticksUsingItem, rightArmHolding)`. */
fun animateCrossbowCharge(
    rightArm: ModelPart, leftArm: ModelPart, maxCrossbowChargeDuration: Float, ticksUsingItem: Float, holdingInRightArm: Boolean
) {
    val holdingArm = if (holdingInRightArm) rightArm else leftArm
    val pullingArm = if (holdingInRightArm) leftArm else rightArm
    holdingArm.yRot = if (holdingInRightArm) -0.8f else 0.8f
    holdingArm.xRot = -0.97079635f
    pullingArm.xRot = holdingArm.xRot
    val useTicks = Mth.clamp(ticksUsingItem, 0.0f, maxCrossbowChargeDuration)
    val lerpAlpha = useTicks / maxCrossbowChargeDuration
    pullingArm.yRot = Mth.lerp(lerpAlpha, 0.4f, 0.85f) * (if (holdingInRightArm) 1 else -1).toFloat()
    pullingArm.xRot = Mth.lerp(lerpAlpha, pullingArm.xRot, (-Math.PI / 2).toFloat())
}

/** 26.1 `Vec3.rotation()`: (pitch, yaw) in degrees. */
fun rotationOf(vec: Vec3): Vec2 {
    val yaw = Math.atan2(-vec.x, vec.z).toFloat() * (180.0f / Math.PI.toFloat())
    val pitch = Math.asin(-vec.y / Math.sqrt(vec.x * vec.x + vec.y * vec.y + vec.z * vec.z)).toFloat() * (180.0f / Math.PI.toFloat())
    return Vec2(pitch, yaw)
}
