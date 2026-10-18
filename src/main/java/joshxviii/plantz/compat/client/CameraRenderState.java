package joshxviii.plantz.compat.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Backport of the 1.21.9+ camera render state, refreshed from the main camera on demand. */
public class CameraRenderState {
    private static final CameraRenderState CURRENT = new CameraRenderState();

    public BlockPos blockPos = BlockPos.ZERO;
    public Vec3 pos = Vec3.ZERO;
    public float xRot;
    public float yRot;
    public boolean initialized;
    public Quaternionf orientation = new Quaternionf();

    public static CameraRenderState current() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        CURRENT.blockPos = camera.getBlockPosition();
        CURRENT.pos = camera.getPosition();
        CURRENT.xRot = camera.getXRot();
        CURRENT.yRot = camera.getYRot();
        CURRENT.initialized = camera.isInitialized();
        CURRENT.orientation = camera.rotation();
        return CURRENT;
    }
}
