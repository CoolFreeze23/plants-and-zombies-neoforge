package joshxviii.plantz.compat.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import joshxviii.plantz.compat.client.CameraRenderState;
import joshxviii.plantz.compat.client.model.Model;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Immediate-mode backport of the 1.21.9+ {@code SubmitNodeCollector}: every submit is drawn straight
 * into the frame's {@link MultiBufferSource}, so 26.1 renderer code can run on 1.21.1 unchanged.
 */
public class SubmitNodeCollector {
    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer buffer);
    }

    private final MultiBufferSource bufferSource;

    public SubmitNodeCollector(MultiBufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    public MultiBufferSource bufferSource() {
        return bufferSource;
    }

    /** Draw ordering does not exist in immediate mode; buffers are already sorted by render type. */
    public SubmitNodeCollector order(int order) {
        return this;
    }

    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords,
                                int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        model.setupAnim(state);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);
        if (sprite != null) buffer = sprite.wrap(buffer);
        model.renderToBuffer(poseStack, buffer, lightCoords, overlayCoords, tintedColor);
    }

    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords,
                                int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, -1, null, outlineColor, crumblingOverlay);
    }

    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, ResourceLocation texture, int lightCoords, int overlayCoords,
                                int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        submitModel(model, state, poseStack, model.renderType(texture), lightCoords, overlayCoords, -1, null, outlineColor, crumblingOverlay);
    }

    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite) {
        submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite, false, false, -1, null, 0);
    }

    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite,
                                int tintedColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite, false, false, tintedColor, crumblingOverlay, 0);
    }

    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite,
                                boolean sheeted, boolean hasFoil) {
        submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite, sheeted, hasFoil, -1, null, 0);
    }

    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite,
                                boolean sheeted, boolean hasFoil, int tintedColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int outlineColor) {
        VertexConsumer buffer = bufferSource.getBuffer(renderType);
        if (sprite != null) buffer = sprite.wrap(buffer);
        modelPart.render(poseStack, buffer, lightCoords, overlayCoords, tintedColor);
    }

    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
        customGeometryRenderer.render(poseStack.last(), bufferSource.getBuffer(renderType));
    }

    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough,
                              int lightCoords, double distanceToCameraSq, CameraRenderState camera) {
        if (nameTagAttachment == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        poseStack.pushPose();
        poseStack.translate(nameTagAttachment.x, nameTagAttachment.y + 0.5, nameTagAttachment.z);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);
        Matrix4f pose = poseStack.last().pose();
        int background = (int) (minecraft.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        float x = (float) (-font.width(name) / 2);
        font.drawInBatch(name, x, (float) offset, 0x20FFFFFF, false, pose, bufferSource,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, background, lightCoords);
        if (seeThrough) {
            font.drawInBatch(name, x, (float) offset, -1, false, pose, bufferSource, Font.DisplayMode.NORMAL, 0, lightCoords);
        }
        poseStack.popPose();
    }

    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode,
                           int lightCoords, int color, int backgroundColor, int outlineColor) {
        Font font = Minecraft.getInstance().font;
        if (outlineColor != 0) {
            font.drawInBatch8xOutline(string, x, y, color, outlineColor, poseStack.last().pose(), bufferSource, lightCoords);
        } else {
            font.drawInBatch(string, x, y, color, dropShadow, poseStack.last().pose(), bufferSource, displayMode, backgroundColor, lightCoords);
        }
    }
}
