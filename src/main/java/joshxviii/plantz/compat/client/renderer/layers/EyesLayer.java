package joshxviii.plantz.compat.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.client.model.EntityModel;
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector;
import joshxviii.plantz.compat.client.state.EntityRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

public abstract class EyesLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    public EyesLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        submitNodeCollector.order(1).submitModel(this.getParentModel(), state, poseStack, this.renderType(), lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
    }

    public abstract RenderType renderType();
}
