package joshxviii.plantz.compat.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.client.state.HumanoidRenderState;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;
import java.util.function.Function;

/**
 * Backport of the 26.1 state-driven humanoid model (hat is a child of head). Like 26.1, babies use their own
 * meshes (see {@link BabyZombieModel}) instead of 1.21.1's ageable scaling.
 */
public class HumanoidModel<T extends HumanoidRenderState> extends EntityModel<T> implements ArmedModel<T>, HeadedModel {
    public final ModelPart head;
    public final ModelPart hat;
    public final ModelPart body;
    public final ModelPart rightArm;
    public final ModelPart leftArm;
    public final ModelPart rightLeg;
    public final ModelPart leftLeg;

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
    public void setupAnim(T state) {
        super.setupAnim(state);
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
            float rightArmSwimAmount = !state.rightArmSpear && (attackArm != HumanoidArm.RIGHT || !(state.attackTime > 0.0F)) ? swimAmount : 0.0F;
            float leftArmSwimAmount = !state.leftArmSpear && (attackArm != HumanoidArm.LEFT || !(state.attackTime > 0.0F)) ? swimAmount : 0.0F;
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
        if (state.rightArmSpear) {
            this.poseSpearArm(this.rightArm, true, state);
            return;
        }
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
        if (state.leftArmSpear) {
            this.poseSpearArm(this.leftArm, false, state);
            return;
        }
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

    /** 26.1 ArmPose.SPEAR: SpearAnimations.thirdPersonHandUse without the kinetic-charge sway (no KineticWeapon component on 1.21.1). */
    private void poseSpearArm(ModelPart arm, boolean right, T state) {
        int invert = right ? 1 : -1;
        arm.yRot = -0.1F * (float) invert + this.head.yRot;
        arm.xRot = (float) (-Math.PI / 2) + this.head.xRot + 0.8F;
        if (state.isFallFlying || state.swimAmount > 0.0F) {
            arm.xRot -= 0.9599311F;
        }
        arm.yRot = (float) (Math.PI / 180.0) * Mth.clamp((180.0F / (float) Math.PI) * arm.yRot, -60.0F, 60.0F);
        arm.xRot = (float) (Math.PI / 180.0) * Mth.clamp((180.0F / (float) Math.PI) * arm.xRot, -120.0F, 30.0F);
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

    @Override
    public void translateToHand(T state, HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.getArm(arm).translateAndRotate(poseStack);
    }

    public ModelPart getArm(HumanoidArm arm) {
        return arm == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }

    /** 26.1 {@code HumanoidModel.createMesh}: the hat is a child of the head. */
    public static MeshDefinition createMesh(CubeDeformation g, float yOffset) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, g), PartPose.offset(0.0F, yOffset, 0.0F));
        head.addOrReplaceChild("hat",
            CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, g.extend(0.5F)), PartPose.ZERO);
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, g), PartPose.offset(0.0F, yOffset, 0.0F));
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, g), PartPose.offset(-5.0F, 2.0F + yOffset, 0.0F));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, g), PartPose.offset(5.0F, 2.0F + yOffset, 0.0F));
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g), PartPose.offset(-1.9F, 12.0F + yOffset, 0.0F));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g), PartPose.offset(1.9F, 12.0F + yOffset, 0.0F));
        return mesh;
    }

    /**
     * 26.1 {@code createArmorMeshSet}: one humanoid mesh per slot holding only that slot's cubes (inner deformation for the
     * leggings), so an armor model is a full humanoid model that animates like the wearer's model.
     */
    public static ArmorModelSet<MeshDefinition> createArmorMeshSet(float innerDeformation, float outerDeformation) {
        return new ArmorModelSet<>(
            createArmorMesh(outerDeformation, Set.of("head", "hat")),
            createArmorMesh(outerDeformation, Set.of("body", "left_arm", "right_arm")),
            createArmorMesh(innerDeformation, Set.of("left_leg", "right_leg", "body")),
            createArmorMesh(outerDeformation, Set.of("left_leg", "right_leg"))
        );
    }

    /** 26.1 {@code createBaseArmorMesh} keeping only the cubes of {@code parts}. */
    private static MeshDefinition createArmorMesh(float deformation, Set<String> parts) {
        CubeDeformation g = new CubeDeformation(deformation);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
            cubes(parts, "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, g)), PartPose.ZERO);
        head.addOrReplaceChild("hat",
            cubes(parts, "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, g.extend(0.5F))), PartPose.ZERO);
        root.addOrReplaceChild("body",
            cubes(parts, "body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, g)), PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
            cubes(parts, "right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, g)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
            cubes(parts, "left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, g)), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
            cubes(parts, "right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g.extend(-0.1F))), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
            cubes(parts, "left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g.extend(-0.1F))), PartPose.offset(1.9F, 12.0F, 0.0F));
        return mesh;
    }

    /**
     * 26.1 {@code createBabyArmorMeshSet} geometry, which fits {@link BabyZombieModel}. 26.1 maps it onto baby armor
     * textures that 1.21.1 does not have, so each box samples the matching region of the adult armor textures instead
     * (see {@link #stretchedBox}). Deformations are per axis like 26.1's baby armor deformations; no trims, like 26.1.
     */
    public static ArmorModelSet<MeshDefinition> createBabyArmorMeshSet(float[] innerDeformation, float[] outerDeformation) {
        return new ArmorModelSet<>(
            createBabyArmorMesh(outerDeformation, Set.of("head")),
            createBabyArmorMesh(outerDeformation, Set.of("body", "left_arm", "right_arm")),
            createBabyArmorMesh(innerDeformation, Set.of("left_leg", "right_leg", "waist")),
            createBabyArmorMesh(outerDeformation, Set.of("left_foot", "right_foot"))
        );
    }

    private static MeshDefinition createBabyArmorMesh(float[] g, Set<String> parts) {
        float[] legG = {g[0] - 0.1F, g[1] - 0.1F, g[2] - 0.1F};
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
            stretchedBox(parts, "head", 0, 0, false, -4.5F, -7.0F, -4.5F, 9.0F, 8.0F, 8.0F, 8, 8, 8, g), PartPose.offset(0.0F, 15.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body",
            stretchedBox(parts, "body", 16, 16, false, -3.0F, -3.0F, -1.5F, 6.0F, 5.0F, 3.0F, 8, 12, 4, g), PartPose.offset(0.0F, 18.0F, 0.0F));
        // leggings belt: the top rows of the adult body region
        root.addOrReplaceChild("waist",
            stretchedBox(parts, "waist", 16, 16, false, -3.0F, -1.2F, -1.49F, 5.9F, 2.0F, 2.9F, 8, 4, 4, legG), PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("right_arm",
            stretchedBox(parts, "right_arm", 40, 16, false, -1.0F, 0.0F, -1.53F, 2.0F, 5.0F, 3.0F, 4, 12, 4, g), PartPose.offset(-3.5F, 15.5F, 0.0F));
        root.addOrReplaceChild("left_arm",
            stretchedBox(parts, "left_arm", 40, 16, true, -1.0F, 0.0F, -1.53F, 2.0F, 5.0F, 3.0F, 4, 12, 4, g), PartPose.offset(3.5F, 15.5F, 0.0F));
        // leggings: the upper rows of the adult leg region; boots: its bottom rows
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
            stretchedBox(parts, "right_leg", 0, 16, false, -1.0F, -0.2F, -2.0F, 3.0F, 4.0F, 3.0F, 4, 6, 4, legG), PartPose.offset(-1.5F, 20.0F, 0.5F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
            stretchedBox(parts, "left_leg", 0, 16, true, -2.0F, -0.2F, -2.0F, 3.0F, 4.0F, 3.0F, 4, 6, 4, legG), PartPose.offset(1.5F, 20.0F, 0.5F));
        rightLeg.addOrReplaceChild("right_foot",
            stretchedBox(parts, "right_foot", 0, 24, false, -1.0F, 2.9F, -2.0F, 3.0F, 1.0F, 3.0F, 4, 4, 4, g), PartPose.ZERO);
        leftLeg.addOrReplaceChild("left_foot",
            stretchedBox(parts, "left_foot", 0, 24, true, -2.0F, 2.9F, -2.0F, 3.0F, 1.0F, 3.0F, 4, 4, 4, g), PartPose.ZERO);
        return mesh;
    }

    /** {@code cubes} when {@code part} is one of {@code parts}, else no cubes (26.1 retained only the slot's parts). */
    private static CubeListBuilder cubes(Set<String> parts, String part, CubeListBuilder cubes) {
        return parts.contains(part) ? cubes : CubeListBuilder.create();
    }

    /**
     * A box spanning {@code (x, y, z)} to {@code (x, y, z) + (w, h, d)}, grown by {@code g}, whose faces sample the texture
     * layout of a {@code uvW x uvH x uvD} box at {@code (u, v)}. 1.21.1 cubes take their UV from the box size and grow the
     * geometry by the deformation, so the size difference goes into the deformation.
     */
    private static CubeListBuilder stretchedBox(Set<String> parts, String part, int u, int v, boolean mirror,
                                                float x, float y, float z, float w, float h, float d, int uvW, int uvH, int uvD, float[] g) {
        float dx = (w - uvW) / 2.0F, dy = (h - uvH) / 2.0F, dz = (d - uvD) / 2.0F;
        return cubes(parts, part, CubeListBuilder.create().texOffs(u, v).mirror(mirror)
            .addBox(x + dx, y + dy, z + dz, uvW, uvH, uvD, new CubeDeformation(g[0] + dx, g[1] + dy, g[2] + dz)));
    }
}
