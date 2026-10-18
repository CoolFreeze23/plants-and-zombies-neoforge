package joshxviii.plantz.compat.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import joshxviii.plantz.compat.client.state.HumanoidRenderState;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

/**
 * Backport of the 1.21.2+ state-driven humanoid model (hat is a child of head, as in 26.1).
 * Babies use 1.21.1's ageable scaling (big head, half-size body) instead of a separate baby mesh.
 */
public class HumanoidModel<T extends HumanoidRenderState> extends EntityModel<T> {
    public final ModelPart head;
    public final ModelPart hat;
    public final ModelPart body;
    public final ModelPart rightArm;
    public final ModelPart leftArm;
    public final ModelPart rightLeg;
    public final ModelPart leftLeg;
    public boolean young;

    public HumanoidModel(ModelPart root) {
        this(root, RenderType::entityCutout);
    }

    public HumanoidModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        super(root, renderType);
        this.head = root.getChild("head");
        this.hat = this.head.hasChild("hat") ? this.head.getChild("hat") : root.getChild("hat");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        if (!this.young || !this.root.visible) {
            super.renderToBuffer(poseStack, buffer, lightCoords, overlayCoords, color);
            return;
        }
        // 1.21.1 AgeableListModel layout: head scaled 0.75, body halved and moved down
        poseStack.pushPose();
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.translate(0.0F, 1.0F, 0.0F);
        this.root.translateAndRotate(poseStack);
        this.head.render(poseStack, buffer, lightCoords, overlayCoords, color);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0F, 1.5F, 0.0F);
        this.root.translateAndRotate(poseStack);
        for (ModelPart child : this.root.children.values()) {
            if (child != this.head) child.render(poseStack, buffer, lightCoords, overlayCoords, color);
        }
        poseStack.popPose();
    }

    @Override
    public void setupAnim(T state) {
        super.setupAnim(state);
        this.young = state.isBaby;
        net.minecraft.client.model.HumanoidModel.ArmPose leftArmPose = state.leftArmPose;
        net.minecraft.client.model.HumanoidModel.ArmPose rightArmPose = state.rightArmPose;
        float swimAmount = state.swimAmount;
        boolean fallFlying = state.isFallFlying;
        this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
        this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
        if (fallFlying) {
            this.head.xRot = (float) (-Math.PI / 4);
        } else if (swimAmount > 0.0F) {
            this.head.xRot = Mth.rotLerp(swimAmount, this.head.xRot, (float) (-Math.PI / 4));
        }

        float animationPos = state.walkAnimationPos;
        float animationSpeed = state.walkAnimationSpeed;
        this.rightArm.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 2.0F * animationSpeed * 0.5F / state.speedValue;
        this.leftArm.xRot = Mth.cos(animationPos * 0.6662F) * 2.0F * animationSpeed * 0.5F / state.speedValue;
        this.rightLeg.xRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed / state.speedValue;
        this.leftLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 1.4F * animationSpeed / state.speedValue;
        this.rightLeg.yRot = 0.005F;
        this.leftLeg.yRot = -0.005F;
        this.rightLeg.zRot = 0.005F;
        this.leftLeg.zRot = -0.005F;
        if (state.isPassenger) {
            this.rightArm.xRot += (float) (-Math.PI / 5);
            this.leftArm.xRot += (float) (-Math.PI / 5);
            this.rightLeg.xRot = -1.4137167F;
            this.rightLeg.yRot = (float) (Math.PI / 10);
            this.rightLeg.zRot = 0.07853982F;
            this.leftLeg.xRot = -1.4137167F;
            this.leftLeg.yRot = (float) (-Math.PI / 10);
            this.leftLeg.zRot = -0.07853982F;
        }

        boolean rightHanded = state.mainArm == HumanoidArm.RIGHT;
        if (state.isUsingItem) {
            boolean mainHandUsed = state.useItemHand == InteractionHand.MAIN_HAND;
            if (mainHandUsed == rightHanded) {
                this.poseRightArm(state);
            } else {
                this.poseLeftArm(state);
            }
        } else {
            boolean twoHandedOffhand = rightHanded ? leftArmPose.isTwoHanded() : rightArmPose.isTwoHanded();
            if (rightHanded != twoHandedOffhand) {
                this.poseLeftArm(state);
                this.poseRightArm(state);
            } else {
                this.poseRightArm(state);
                this.poseLeftArm(state);
            }
        }

        this.setupAttackAnimation(state);
        if (state.isCrouching) {
            this.body.xRot = 0.5F;
            this.rightArm.xRot += 0.4F;
            this.leftArm.xRot += 0.4F;
            this.rightLeg.z += 4.0F;
            this.leftLeg.z += 4.0F;
            this.head.y += 4.2F;
            this.body.y += 3.2F;
            this.leftArm.y += 3.2F;
            this.rightArm.y += 3.2F;
        }

        if (rightArmPose != net.minecraft.client.model.HumanoidModel.ArmPose.SPYGLASS) {
            AnimationUtils.bobModelPart(this.rightArm, state.ageInTicks, 1.0F);
        }
        if (leftArmPose != net.minecraft.client.model.HumanoidModel.ArmPose.SPYGLASS) {
            AnimationUtils.bobModelPart(this.leftArm, state.ageInTicks, -1.0F);
        }

        if (swimAmount > 0.0F) {
            float swimPos = animationPos % 26.0F;
            HumanoidArm attackArm = state.attackArm;
            float rightArmSwimAmount = attackArm == HumanoidArm.RIGHT && state.attackTime > 0.0F ? 0.0F : swimAmount;
            float leftArmSwimAmount = attackArm == HumanoidArm.LEFT && state.attackTime > 0.0F ? 0.0F : swimAmount;
            if (!state.isUsingItem) {
                if (swimPos < 14.0F) {
                    this.leftArm.xRot = Mth.rotLerp(leftArmSwimAmount, this.leftArm.xRot, 0.0F);
                    this.rightArm.xRot = Mth.lerp(rightArmSwimAmount, this.rightArm.xRot, 0.0F);
                    this.leftArm.yRot = Mth.rotLerp(leftArmSwimAmount, this.leftArm.yRot, (float) Math.PI);
                    this.rightArm.yRot = Mth.lerp(rightArmSwimAmount, this.rightArm.yRot, (float) Math.PI);
                    this.leftArm.zRot = Mth.rotLerp(leftArmSwimAmount, this.leftArm.zRot, (float) Math.PI + 1.8707964F * this.quadraticArmUpdate(swimPos) / this.quadraticArmUpdate(14.0F));
                    this.rightArm.zRot = Mth.lerp(rightArmSwimAmount, this.rightArm.zRot, (float) Math.PI - 1.8707964F * this.quadraticArmUpdate(swimPos) / this.quadraticArmUpdate(14.0F));
                } else if (swimPos < 22.0F) {
                    float internalSwimPos = (swimPos - 14.0F) / 8.0F;
                    this.leftArm.xRot = Mth.rotLerp(leftArmSwimAmount, this.leftArm.xRot, (float) (Math.PI / 2) * internalSwimPos);
                    this.rightArm.xRot = Mth.lerp(rightArmSwimAmount, this.rightArm.xRot, (float) (Math.PI / 2) * internalSwimPos);
                    this.leftArm.yRot = Mth.rotLerp(leftArmSwimAmount, this.leftArm.yRot, (float) Math.PI);
                    this.rightArm.yRot = Mth.lerp(rightArmSwimAmount, this.rightArm.yRot, (float) Math.PI);
                    this.leftArm.zRot = Mth.rotLerp(leftArmSwimAmount, this.leftArm.zRot, 5.012389F - 1.8707964F * internalSwimPos);
                    this.rightArm.zRot = Mth.lerp(rightArmSwimAmount, this.rightArm.zRot, 1.2707963F + 1.8707964F * internalSwimPos);
                } else {
                    float internalSwimPos = (swimPos - 22.0F) / 4.0F;
                    this.leftArm.xRot = Mth.rotLerp(leftArmSwimAmount, this.leftArm.xRot, (float) (Math.PI / 2) - (float) (Math.PI / 2) * internalSwimPos);
                    this.rightArm.xRot = Mth.lerp(rightArmSwimAmount, this.rightArm.xRot, (float) (Math.PI / 2) - (float) (Math.PI / 2) * internalSwimPos);
                    this.leftArm.yRot = Mth.rotLerp(leftArmSwimAmount, this.leftArm.yRot, (float) Math.PI);
                    this.rightArm.yRot = Mth.lerp(rightArmSwimAmount, this.rightArm.yRot, (float) Math.PI);
                    this.leftArm.zRot = Mth.rotLerp(leftArmSwimAmount, this.leftArm.zRot, (float) Math.PI);
                    this.rightArm.zRot = Mth.lerp(rightArmSwimAmount, this.rightArm.zRot, (float) Math.PI);
                }
            }
            this.leftLeg.xRot = Mth.lerp(swimAmount, this.leftLeg.xRot, 0.3F * Mth.cos(animationPos * 0.33333334F + (float) Math.PI));
            this.rightLeg.xRot = Mth.lerp(swimAmount, this.rightLeg.xRot, 0.3F * Mth.cos(animationPos * 0.33333334F));
        }
    }

    private void poseRightArm(T state) {
        switch (state.rightArmPose) {
            case EMPTY -> this.rightArm.yRot = 0.0F;
            case ITEM -> {
                this.rightArm.xRot = this.rightArm.xRot * 0.5F - (float) (Math.PI / 10);
                this.rightArm.yRot = 0.0F;
            }
            case BLOCK -> this.poseBlockingArm(this.rightArm, true);
            case BOW_AND_ARROW -> {
                this.rightArm.yRot = -0.1F + this.head.yRot;
                this.leftArm.yRot = 0.1F + this.head.yRot + 0.4F;
                this.rightArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
                this.leftArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
            }
            case THROW_SPEAR -> {
                this.rightArm.xRot = this.rightArm.xRot * 0.5F - (float) Math.PI;
                this.rightArm.yRot = 0.0F;
            }
            case CROSSBOW_CHARGE -> {
                if (state.compatEntity instanceof LivingEntity living) AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, living, true);
            }
            case CROSSBOW_HOLD -> AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, true);
            case SPYGLASS -> {
                this.rightArm.xRot = Mth.clamp(this.head.xRot - 1.9198622F - (state.isCrouching ? (float) (Math.PI / 12) : 0.0F), -2.4F, 3.3F);
                this.rightArm.yRot = this.head.yRot - (float) (Math.PI / 12);
            }
            case TOOT_HORN -> {
                this.rightArm.xRot = Mth.clamp(this.head.xRot, -1.2F, 1.2F) - 1.4835298F;
                this.rightArm.yRot = this.head.yRot - (float) (Math.PI / 6);
            }
            case BRUSH -> {
                this.rightArm.xRot = this.rightArm.xRot * 0.5F - (float) (Math.PI / 5);
                this.rightArm.yRot = 0.0F;
            }
            default -> {}
        }
    }

    private void poseLeftArm(T state) {
        switch (state.leftArmPose) {
            case EMPTY -> this.leftArm.yRot = 0.0F;
            case ITEM -> {
                this.leftArm.xRot = this.leftArm.xRot * 0.5F - (float) (Math.PI / 10);
                this.leftArm.yRot = 0.0F;
            }
            case BLOCK -> this.poseBlockingArm(this.leftArm, false);
            case BOW_AND_ARROW -> {
                this.rightArm.yRot = -0.1F + this.head.yRot - 0.4F;
                this.leftArm.yRot = 0.1F + this.head.yRot;
                this.rightArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
                this.leftArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
            }
            case THROW_SPEAR -> {
                this.leftArm.xRot = this.leftArm.xRot * 0.5F - (float) Math.PI;
                this.leftArm.yRot = 0.0F;
            }
            case CROSSBOW_CHARGE -> {
                if (state.compatEntity instanceof LivingEntity living) AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, living, false);
            }
            case CROSSBOW_HOLD -> AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, false);
            case SPYGLASS -> {
                this.leftArm.xRot = Mth.clamp(this.head.xRot - 1.9198622F - (state.isCrouching ? (float) (Math.PI / 12) : 0.0F), -2.4F, 3.3F);
                this.leftArm.yRot = this.head.yRot + (float) (Math.PI / 12);
            }
            case TOOT_HORN -> {
                this.leftArm.xRot = Mth.clamp(this.head.xRot, -1.2F, 1.2F) - 1.4835298F;
                this.leftArm.yRot = this.head.yRot + (float) (Math.PI / 6);
            }
            case BRUSH -> {
                this.leftArm.xRot = this.leftArm.xRot * 0.5F - (float) (Math.PI / 5);
                this.leftArm.yRot = 0.0F;
            }
            default -> {}
        }
    }

    private void poseBlockingArm(ModelPart arm, boolean right) {
        arm.xRot = arm.xRot * 0.5F - 0.9424779F + Mth.clamp(this.head.xRot, (float) (-Math.PI * 4.0 / 9.0), 0.43633232F);
        arm.yRot = (right ? -30.0F : 30.0F) * (float) (Math.PI / 180.0) + Mth.clamp(this.head.yRot, (float) (-Math.PI / 6), (float) (Math.PI / 6));
    }

    protected void setupAttackAnimation(T state) {
        float attackTime = state.attackTime;
        if (!(attackTime <= 0.0F)) {
            this.body.yRot = Mth.sin(Mth.sqrt(attackTime) * (float) (Math.PI * 2)) * 0.2F;
            if (state.attackArm == HumanoidArm.LEFT) {
                this.body.yRot *= -1.0F;
            }
            float ageScale = state.ageScale;
            this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F * ageScale;
            this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F * ageScale;
            this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F * ageScale;
            this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F * ageScale;
            this.rightArm.yRot = this.rightArm.yRot + this.body.yRot;
            this.leftArm.yRot = this.leftArm.yRot + this.body.yRot;
            this.leftArm.xRot = this.leftArm.xRot + this.body.yRot;
            float swing = 1.0F - attackTime;
            swing *= swing;
            swing *= swing;
            swing = 1.0F - swing;
            float aa = Mth.sin(swing * (float) Math.PI);
            float bb = Mth.sin(attackTime * (float) Math.PI) * -(this.head.xRot - 0.7F) * 0.75F;
            ModelPart attackArm = this.getArm(state.attackArm);
            attackArm.xRot -= aa * 1.2F + bb;
            attackArm.yRot = attackArm.yRot + this.body.yRot * 2.0F;
            attackArm.zRot = attackArm.zRot + Mth.sin(attackTime * (float) Math.PI) * -0.4F;
        }
    }

    private float quadraticArmUpdate(float x) {
        return -65.0F * x + x * x;
    }

    public void translateToHand(HumanoidRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.getArm(arm).translateAndRotate(poseStack);
    }

    public ModelPart getArm(HumanoidArm arm) {
        return arm == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
    }

    public ModelPart getHead() {
        return this.head;
    }
}
