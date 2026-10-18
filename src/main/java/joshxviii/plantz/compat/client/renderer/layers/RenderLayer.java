package joshxviii.plantz.compat.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.client.model.EntityModel;
import joshxviii.plantz.compat.client.model.Model;
import joshxviii.plantz.compat.client.renderer.LivingEntityRenderer;
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector;
import joshxviii.plantz.compat.client.state.EntityRenderState;
import joshxviii.plantz.compat.client.state.LivingEntityRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public abstract class RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>> {
    private final RenderLayerParent<S, M> renderer;

    public RenderLayer(RenderLayerParent<S, M> renderer) {
        this.renderer = renderer;
    }

    protected static <S extends LivingEntityRenderState> void coloredCutoutModelCopyLayerRender(
        Model<? super S> model, ResourceLocation texture, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        int lightCoords, S state, int color, int order
    ) {
        if (!state.isInvisible) {
            renderColoredCutoutModel(model, texture, poseStack, submitNodeCollector, lightCoords, state, color, order);
        }
    }

    protected static <S extends LivingEntityRenderState> void renderColoredCutoutModel(
        Model<? super S> model, ResourceLocation texture, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        int lightCoords, S state, int color, int order
    ) {
        submitNodeCollector.order(order).submitModel(model, state, poseStack, RenderType.entityCutoutNoCull(texture), lightCoords,
            LivingEntityRenderer.getOverlayCoords(state, 0.0F), color, null, state.outlineColor, null);
    }

    public M getParentModel() {
        return this.renderer.getModel();
    }

    public abstract void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot);
}
