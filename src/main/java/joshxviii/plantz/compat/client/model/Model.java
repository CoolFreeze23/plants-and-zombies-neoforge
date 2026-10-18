package joshxviii.plantz.compat.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.function.Function;

/**
 * Backport of the 1.21.2+ state-driven {@code Model}: a root part, a render type factory and a
 * {@link #setupAnim(Object)} that poses the parts from a render state.
 */
public abstract class Model<S> {
    protected final ModelPart root;
    protected final Function<ResourceLocation, RenderType> renderType;
    private final List<ModelPart> allParts;

    public Model(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        this.root = root;
        this.renderType = renderType;
        this.allParts = root.getAllParts().toList();
    }

    public final Function<ResourceLocation, RenderType> renderType() {
        return this.renderType;
    }

    public final RenderType renderType(ResourceLocation texture) {
        return this.renderType().apply(texture);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        this.root().render(poseStack, buffer, lightCoords, overlayCoords, color);
    }

    public final void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords) {
        this.renderToBuffer(poseStack, buffer, lightCoords, overlayCoords, -1);
    }

    public final ModelPart root() {
        return this.root;
    }

    public final List<ModelPart> allParts() {
        return this.allParts;
    }

    public void setupAnim(S state) {
        this.resetPose();
    }

    public final void resetPose() {
        for (ModelPart part : this.allParts) {
            part.resetPose();
        }
    }

    public static class Simple extends Model<Unit> {
        public Simple(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
            super(root, renderType);
        }

        @Override
        public void setupAnim(Unit state) {
        }
    }
}
