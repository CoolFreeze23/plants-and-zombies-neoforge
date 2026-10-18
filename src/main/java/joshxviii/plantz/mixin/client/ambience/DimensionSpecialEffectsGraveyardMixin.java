package joshxviii.plantz.mixin.client.ambience;

import joshxviii.plantz.client.GraveyardAmbience;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Graveyard sunrise/sunset colour. */
@Mixin(DimensionSpecialEffects.class)
public abstract class DimensionSpecialEffectsGraveyardMixin {
    @Inject(method = "getSunriseColor", at = @At("RETURN"), cancellable = true)
    private void plantz$graveyardSunrise(float timeOfDay, float partialTicks, CallbackInfoReturnable<float[]> cir) {
        cir.setReturnValue(GraveyardAmbience.sunriseColor(cir.getReturnValue()));
    }
}
