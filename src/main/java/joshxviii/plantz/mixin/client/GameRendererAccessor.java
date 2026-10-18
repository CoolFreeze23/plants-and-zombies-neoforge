package joshxviii.plantz.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Port helper: 26.1 Camera.fov does not exist in 1.21.1; the HUD markers use GameRenderer.getFov instead. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("getFov")
    double plantz$getFov(Camera camera, float partialTicks, boolean useFovSetting);
}
