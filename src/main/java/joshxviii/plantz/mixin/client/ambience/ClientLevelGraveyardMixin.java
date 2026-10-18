package joshxviii.plantz.mixin.client.ambience;

import joshxviii.plantz.client.GraveyardAmbience;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Graveyard cloud colour. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelGraveyardMixin {
    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void plantz$graveyardClouds(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(GraveyardAmbience.cloudColor(cir.getReturnValue()));
    }
}
