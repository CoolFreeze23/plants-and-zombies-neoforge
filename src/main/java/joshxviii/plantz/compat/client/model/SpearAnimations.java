package joshxviii.plantz.compat.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import joshxviii.plantz.compat.Ease;
import joshxviii.plantz.compat.KineticWeapon;
import joshxviii.plantz.compat.client.state.ArmedEntityRenderState;
import joshxviii.plantz.compat.client.state.HumanoidRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * 26.1 {@code SpearAnimations} (third person): the spear hold pose with its charge sway, the spear lowering while it
 * charges, and the stab swing. The kinetic weapon data comes from a spear backport ({@link KineticWeapon}).
 */
public final class SpearAnimations {
    private SpearAnimations() {
    }

    private static float progress(float time, float start, float end) {
        return Mth.clamp(Mth.inverseLerp(time, start, end), 0.0F, 1.0F);
    }

    public static <T extends HumanoidRenderState> void thirdPersonHandUse(ModelPart arm, ModelPart head, boolean holdingInRightArm, ItemStack item, T state) {
        int invert = holdingInRightArm ? 1 : -1;
        arm.yRot = -0.1F * (float) invert + head.yRot;
        arm.xRot = (float) (-Math.PI / 2) + head.xRot + 0.8F;
        if (state.isFallFlying || state.swimAmount > 0.0F) {
            arm.xRot -= 0.9599311F;
        }

        arm.yRot = (float) (Math.PI / 180.0) * Mth.clamp((180.0F / (float) Math.PI) * arm.yRot, -60.0F, 60.0F);
        arm.xRot = (float) (Math.PI / 180.0) * Mth.clamp((180.0F / (float) Math.PI) * arm.xRot, -120.0F, 30.0F);
        if (!(state.ticksUsingItem <= 0.0F)
            && (!state.isUsingItem || state.useItemHand == (holdingInRightArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND))) {
            KineticWeapon kineticWeapon = KineticWeapon.of(item);
            if (kineticWeapon != null) {
                UseParams params = UseParams.fromKineticWeapon(kineticWeapon, state.ticksUsingItem);
                arm.yRot = arm.yRot + (float) (-invert) * params.swayScaleFast() * (float) (Math.PI / 180.0) * params.swayIntensity() * 1.0F;
                arm.zRot = arm.zRot + (float) (-invert) * params.swayScaleSlow() * (float) (Math.PI / 180.0) * params.swayIntensity() * 0.5F;
                arm.xRot = arm.xRot
                    + (float) (Math.PI / 180.0)
                    * (
                    -40.0F * params.raiseProgressStart()
                        + 30.0F * params.raiseProgressMiddle()
                        + -20.0F * params.raiseProgressEnd()
                        + 20.0F * params.lowerProgress()
                        + 10.0F * params.raiseBackProgress()
                        + 0.6F * params.swayScaleSlow() * params.swayIntensity()
                );
            }
        }
    }

    public static <S extends ArmedEntityRenderState> void thirdPersonUseItem(S state, PoseStack poseStack, float timeHeld, HumanoidArm arm, ItemStack actualItem) {
        KineticWeapon kineticWeapon = KineticWeapon.of(actualItem);
        if (kineticWeapon != null && timeHeld != 0.0F) {
            float attack = Ease.inQuad(progress(state.attackTime, 0.05F, 0.2F));
            float retract = Ease.inOutExpo(progress(state.attackTime, 0.4F, 1.0F));
            UseParams params = UseParams.fromKineticWeapon(kineticWeapon, timeHeld);
            int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
            float raiseProgressModified = 1.0F - Ease.outBack(1.0F - params.raiseProgress());
            float hitFeedback = hitFeedbackAmount(state.ticksSinceKineticHitFeedback);
            poseStack.translate(
                0.0, (double) (-hitFeedback) * 0.4, (double) (-kineticWeapon.forwardMovement() * (raiseProgressModified - params.raiseBackProgress()) + hitFeedback)
            );
            rotateAround(poseStack, Axis.XN.rotationDegrees(70.0F * (params.raiseProgress() - params.raiseBackProgress()) - 40.0F * (attack - retract)), 0.0F, -0.03125F, 0.125F);
            rotateAround(poseStack, Axis.YP.rotationDegrees((float) (invert * 90) * (params.raiseProgress() - params.swayProgress() + 3.0F * retract + attack)), 0.0F, 0.0F, 0.125F);
        }
    }

    public static <T extends HumanoidRenderState> void thirdPersonAttackHand(HumanoidModel<T> model, T state) {
        float attackTime = state.attackTime;
        HumanoidArm arm = state.attackArm;
        model.rightArm.yRot = model.rightArm.yRot - model.body.yRot;
        model.leftArm.yRot = model.leftArm.yRot - model.body.yRot;
        model.leftArm.xRot = model.leftArm.xRot - model.body.yRot;
        float prepare = Ease.inOutSine(progress(attackTime, 0.0F, 0.05F));
        float attack = Ease.inQuad(progress(attackTime, 0.05F, 0.2F));
        float retract = Ease.inOutExpo(progress(attackTime, 0.4F, 1.0F));
        model.getArm(arm).xRot += (90.0F * prepare - 120.0F * attack + 30.0F * retract) * (float) (Math.PI / 180.0);
    }

    public static <S extends ArmedEntityRenderState> void thirdPersonAttackItem(S state, PoseStack poseStack) {
        if (!(state.attackTime <= 0.0F)) {
            KineticWeapon kineticWeapon = KineticWeapon.of(state.getMainHandItemStack());
            float jetForward = kineticWeapon != null ? kineticWeapon.forwardMovement() : 0.0F;
            float attackTime = state.attackTime;
            float attack = Ease.inQuad(progress(attackTime, 0.05F, 0.2F));
            float retract = Ease.inOutExpo(progress(attackTime, 0.4F, 1.0F));
            rotateAround(poseStack, Axis.XN.rotationDegrees(70.0F * (attack - retract)), 0.0F, -0.125F, 0.125F);
            poseStack.translate(0.0F, jetForward * (attack - retract), 0.0F);
        }
    }

    private static float hitFeedbackAmount(float ticksSinceFeedbackStart) {
        return 0.4F * (Ease.outQuart(progress(ticksSinceFeedbackStart, 1.0F, 3.0F)) - Ease.inOutSine(progress(ticksSinceFeedbackStart, 3.0F, 10.0F)));
    }

    /** 26.1 {@code PoseStack.rotateAround}. */
    private static void rotateAround(PoseStack poseStack, org.joml.Quaternionf rotation, float x, float y, float z) {
        poseStack.translate(x, y, z);
        poseStack.mulPose(rotation);
        poseStack.translate(-x, -y, -z);
    }

    record UseParams(
        float raiseProgress,
        float raiseProgressStart,
        float raiseProgressMiddle,
        float raiseProgressEnd,
        float swayProgress,
        float lowerProgress,
        float raiseBackProgress,
        float swayIntensity,
        float swayScaleSlow,
        float swayScaleFast
    ) {
        static UseParams fromKineticWeapon(KineticWeapon kineticWeapon, float time) {
            int finishRaisingTick = kineticWeapon.delayTicks();
            int finishSwayingTick = kineticWeapon.dismountTicks() + finishRaisingTick;
            int startSwayingTick = finishSwayingTick - 20;
            int finishLoweringTick = kineticWeapon.knockbackTicks() + finishRaisingTick;
            int startLoweringTick = finishLoweringTick - 40;
            int finishRaisingBackTick = kineticWeapon.damageTicks() + finishRaisingTick;
            float raiseProgress = progress(time, 0.0F, (float) finishRaisingTick);
            float raiseProgressStart = progress(raiseProgress, 0.0F, 0.5F);
            float raiseProgressMiddle = progress(raiseProgress, 0.5F, 0.8F);
            float raiseProgressEnd = progress(raiseProgress, 0.8F, 1.0F);
            float swayProgress = progress(time, (float) startSwayingTick, (float) finishSwayingTick);
            float lowerProgress = Ease.outCubic(Ease.inOutElastic(progress(time - 20.0F, (float) startLoweringTick, (float) finishLoweringTick)));
            float raiseBackProgress = progress(time, (float) (finishRaisingBackTick - 5), (float) finishRaisingBackTick);
            float swayIntensity = 2.0F * Ease.outCirc(swayProgress) - 2.0F * Ease.inCirc(raiseBackProgress);
            float swayScaleSlow = Mth.sin(time * 19.0F * (float) (Math.PI / 180.0)) * swayIntensity;
            float swayScaleFast = Mth.sin(time * 30.0F * (float) (Math.PI / 180.0)) * swayIntensity;
            return new UseParams(raiseProgress, raiseProgressStart, raiseProgressMiddle, raiseProgressEnd, swayProgress, lowerProgress,
                raiseBackProgress, swayIntensity, swayScaleSlow, swayScaleFast);
        }
    }
}
