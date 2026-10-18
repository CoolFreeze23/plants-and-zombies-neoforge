package joshxviii.plantz.compat.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.client.model.HumanoidModel;
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector;
import joshxviii.plantz.compat.client.state.HumanoidRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs 1.21.1's own humanoid layers (armor with trims/glint, held items, custom heads, elytra) for a
 * state-driven compat {@link HumanoidModel}. The compat model's pose is copied onto a vanilla proxy
 * humanoid model, which the vanilla layers then read like they would for any vanilla mob.
 */
public class VanillaHumanoidLayers<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private final net.minecraft.client.model.HumanoidModel<LivingEntity> proxyModel;
    private final net.minecraft.client.renderer.entity.RenderLayerParent<LivingEntity, net.minecraft.client.model.HumanoidModel<LivingEntity>> proxyParent;
    private final List<net.minecraft.client.renderer.entity.layers.RenderLayer<LivingEntity, net.minecraft.client.model.HumanoidModel<LivingEntity>>> layers = new ArrayList<>();

    public VanillaHumanoidLayers(RenderLayerParent<S, M> renderer, EntityRendererProvider.Context context, ModelLayerLocation proxyLayer) {
        super(renderer);
        this.proxyModel = new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(proxyLayer));
        this.proxyParent = new net.minecraft.client.renderer.entity.RenderLayerParent<>() {
            @Override
            public net.minecraft.client.model.HumanoidModel<LivingEntity> getModel() {
                return proxyModel;
            }

            @Override
            public ResourceLocation getTextureLocation(LivingEntity entity) {
                return MissingTextureAtlasSprite.getLocation();
            }
        };
    }

    public VanillaHumanoidLayers<S, M> withArmor(ModelLayerLocation innerLayer, ModelLayerLocation outerLayer, EntityRendererProvider.Context context) {
        layers.add(new HumanoidArmorLayer<>(proxyParent,
            new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(innerLayer)),
            new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(outerLayer)),
            context.getModelManager()));
        return this;
    }

    public VanillaHumanoidLayers<S, M> withCustomHead(EntityRendererProvider.Context context) {
        layers.add(new CustomHeadLayer<>(proxyParent, context.getModelSet(), context.getItemInHandRenderer()));
        return this;
    }

    public VanillaHumanoidLayers<S, M> withElytra(EntityRendererProvider.Context context) {
        layers.add(new ElytraLayer<>(proxyParent, context.getModelSet()));
        return this;
    }

    public VanillaHumanoidLayers<S, M> withItemInHand(EntityRendererProvider.Context context) {
        layers.add(new ItemInHandLayer<>(proxyParent, context.getItemInHandRenderer()));
        return this;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        if (layers.isEmpty() || !(state.compatEntity instanceof LivingEntity entity)) return;
        M model = this.getParentModel();
        copyPose(model, state);
        poseStack.pushPose();
        model.root().translateAndRotate(poseStack);
        for (var layer : layers) {
            layer.render(poseStack, submitNodeCollector.bufferSource(), lightCoords, entity,
                state.walkAnimationPos, state.walkAnimationSpeed, state.compatPartialTick, state.ageInTicks, yRot, xRot);
        }
        poseStack.popPose();
    }

    private void copyPose(M model, S state) {
        copyPart(model.head, proxyModel.head);
        copyPart(model.head, proxyModel.hat);
        copyPart(model.body, proxyModel.body);
        copyPart(model.rightArm, proxyModel.rightArm);
        copyPart(model.leftArm, proxyModel.leftArm);
        copyPart(model.rightLeg, proxyModel.rightLeg);
        copyPart(model.leftLeg, proxyModel.leftLeg);
        proxyModel.young = state.isBaby;
        proxyModel.crouching = state.isCrouching;
        proxyModel.riding = state.isPassenger;
        proxyModel.attackTime = state.attackTime;
        proxyModel.swimAmount = state.swimAmount;
        proxyModel.rightArmPose = state.rightArmPose;
        proxyModel.leftArmPose = state.leftArmPose;
    }

    private static void copyPart(ModelPart from, ModelPart to) {
        to.copyFrom(from);
        to.visible = from.visible;
    }
}
