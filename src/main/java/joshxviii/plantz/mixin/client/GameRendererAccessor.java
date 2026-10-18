package joshxviii.plantz.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** ZombieHudMarkers: 1.21.1 has no Camera.fov, so the FOV comes from GameRenderer.getFov. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("getFov")
    double plantz$getFov(Camera camera, float partialTicks, boolean useFovSetting);
}
