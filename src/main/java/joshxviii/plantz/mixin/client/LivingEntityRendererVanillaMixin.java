package joshxviii.plantz.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.ARGB;
import joshxviii.plantz.mixin.LivingEntityAccessor;
import joshxviii.plantz.PazComponents;
import joshxviii.plantz.renderer.VanillaBodyBlockerRenderLayer;
import joshxviii.plantz.renderer.VanillaDuckyTubeRenderLayer;
import joshxviii.plantz.renderer.VanillaDyeVatRenderLayer;
import joshxviii.plantz.renderer.VanillaSpecialEffectsLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Upstream's LivingEntityRendererMixin for 1.21.1's own living renderer (players, vanilla mobs); the mod's renderers
 * run on the compat renderer and get it from LivingEntityRendererMixin. Adds the same layers, the hypno / chill /
 * frozen / butter tint, and the frozen pose. 26.1 froze the extracted render state; 1.21.1 has none, so the pose
 * values are swapped where render() hands them to setupRotations, the model and the layers.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererVanillaMixin<T extends LivingEntity, M extends EntityModel<T>> implements RenderLayerParent<T, M> {
    @Unique
    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Shadow
    protected abstract boolean addLayer(RenderLayer<T, M> layer);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void plantz$addDuckyTubeLayer(EntityRendererProvider.Context context, M model, float shadowRadius, CallbackInfo ci) {
        this.addLayer(new VanillaDuckyTubeRenderLayer<>(this));
        this.addLayer(new VanillaDyeVatRenderLayer<>(this));
        this.addLayer(new VanillaBodyBlockerRenderLayer<>(this));
        this.addLayer(new VanillaSpecialEffectsLayer<>(this));
    }

    @Unique private boolean plantz$freezePose;
    @Unique private boolean plantz$hasPreviousState;
    @Unique private float plantz$ageInTicks;
    @Unique private float plantz$walkAnimationPos;
    @Unique private float plantz$walkAnimationSpeed;
    @Unique private float plantz$bodyRot;
    @Unique private float plantz$yRot;
    @Unique private float plantz$xRot;

    @Inject(method = RENDER, at = @At("HEAD"))
    private void plantz$checkFrozen(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        plantz$freezePose = ((LivingEntityAccessor) entity).plantz$getFrozenId() && plantz$hasPreviousState;
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"))
    private void plantz$freezeRotations(LivingEntityRenderer<T, M> renderer, T entity, PoseStack poseStack, float ageInTicks, float bodyRot, float partialTick, float scale,
                                        Operation<Void> original) {
        if (plantz$freezePose) {
            ageInTicks = plantz$ageInTicks;
            bodyRot = plantz$bodyRot;
        } else {
            plantz$ageInTicks = ageInTicks;
            plantz$bodyRot = bodyRot;
        }
        original.call(renderer, entity, poseStack, ageInTicks, bodyRot, partialTick, scale);
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/model/EntityModel;prepareMobModel(Lnet/minecraft/world/entity/Entity;FFF)V"))
    private void plantz$freezePrepare(EntityModel<T> model, Entity entity, float walkPos, float walkSpeed, float partialTick, Operation<Void> original) {
        if (plantz$freezePose) {
            walkPos = plantz$walkAnimationPos;
            walkSpeed = plantz$walkAnimationSpeed;
        }
        original.call(model, entity, walkPos, walkSpeed, partialTick);
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    private void plantz$freezeAnim(EntityModel<T> model, Entity entity, float walkPos, float walkSpeed, float ageInTicks, float yRot, float xRot, Operation<Void> original) {
        if (plantz$freezePose) {
            walkPos = plantz$walkAnimationPos;
            walkSpeed = plantz$walkAnimationSpeed;
            ageInTicks = plantz$ageInTicks;
            yRot = plantz$yRot;
            xRot = plantz$xRot;
        } else {
            plantz$walkAnimationPos = walkPos;
            plantz$walkAnimationSpeed = walkSpeed;
            plantz$yRot = yRot;
            plantz$xRot = xRot;
            plantz$hasPreviousState = true;
        }
        // a worn coffin holds the head straight
        if (entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.CHEST).has(PazComponents.WORN_BODY_BLOCKER)) {
            yRot = 0.0F;
            xRot = living.isCrouching() ? 25.0F : 0.0F;
        }
        original.call(model, entity, walkPos, walkSpeed, ageInTicks, yRot, xRot);
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"))
    private void plantz$freezeLayer(RenderLayer<T, M> layer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, Entity entity,
                                    float walkPos, float walkSpeed, float partialTick, float ageInTicks, float yRot, float xRot, Operation<Void> original) {
        if (plantz$freezePose) {
            walkPos = plantz$walkAnimationPos;
            walkSpeed = plantz$walkAnimationSpeed;
            ageInTicks = plantz$ageInTicks;
            yRot = plantz$yRot;
            xRot = plantz$xRot;
        }
        original.call(layer, poseStack, buffer, packedLight, entity, walkPos, walkSpeed, partialTick, ageInTicks, yRot, xRot);
    }

    @Unique
    private static final int PLANTZ_HYPNO_TINT = ARGB.opaque(0xD036FF);
    @Unique
    private static final int PLANTZ_FREEZE_TINT = ARGB.opaque(0x0098DC);
    @Unique
    private static final int PLANTZ_BUTTER_TINT = ARGB.opaque(0xdbdc8c);

    @ModifyArg(method = RENDER, index = 4, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private int plantz$applyHypnoTint(int color, @Local(argsOnly = true) LivingEntity entity) {
        LivingEntityAccessor accessor = (LivingEntityAccessor) entity;
        int finalColor = color;
        if (accessor.plantz$getHypnoId()) finalColor = ARGB.multiply(finalColor, PLANTZ_HYPNO_TINT);
        if (accessor.plantz$getChillId() || accessor.plantz$getFrozenId()) finalColor = ARGB.multiply(finalColor, PLANTZ_FREEZE_TINT);
        if (accessor.plantz$getButterId()) finalColor = ARGB.multiply(finalColor, PLANTZ_BUTTER_TINT);
        return finalColor;
    }
}
