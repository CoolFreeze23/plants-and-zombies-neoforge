package joshxviii.plantz.compat.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.client.CameraRenderState;
import joshxviii.plantz.compat.client.renderer.ModelFeatureRenderer;
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector;
import joshxviii.plantz.compat.client.state.BlockEntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Backport of the 1.21.9+ two-phase block entity renderer on top of 1.21.1's immediate renderer.
 * A fresh state is extracted every frame, then submitted straight to the frame's buffers.
 */
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> extends net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T> {
    S createRenderState();

    default void extractRenderState(T blockEntity, S state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
    }

    void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera);

    @Override
    default void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        S state = createRenderState();
        CameraRenderState camera = CameraRenderState.current();
        extractRenderState(blockEntity, state, partialTick, camera.pos, null);
        state.lightCoords = packedLight;
        submit(state, poseStack, new SubmitNodeCollector(bufferSource), camera);
    }

    static Vec3 cameraPosition() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }
}
