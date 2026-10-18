package joshxviii.plantz.mixin.client.ambience;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import joshxviii.plantz.client.GraveyardAmbience;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Graveyard sky/ambient light colours in the lightmap (see {@link GraveyardAmbience#adjustLightmap}). */
@Mixin(LightTexture.class)
public abstract class LightTextureGraveyardMixin {
    @WrapOperation(method = "updateLightTexture", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;adjustLightmapColors(Lnet/minecraft/client/multiplayer/ClientLevel;FFFFIILorg/joml/Vector3f;)V"))
    private void plantz$graveyardLightmap(DimensionSpecialEffects effects, ClientLevel level, float partialTicks, float skyDarken,
                                          float blockLightRedFlicker, float skyLight, int pixelX, int pixelY, Vector3f colors, Operation<Void> original) {
        GraveyardAmbience.adjustLightmap(skyDarken, skyLight, colors);
        original.call(effects, level, partialTicks, skyDarken, blockLightRedFlicker, skyLight, pixelX, pixelY, colors);
    }
}
