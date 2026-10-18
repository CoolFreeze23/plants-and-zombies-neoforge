package joshxviii.plantz.mixin.client.model;

import joshxviii.plantz.compat.client.model.CuboidRotation;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bakes element rotations read by {@link BlockElementDeserializerMixin} with their 26.1 transform. */
@Mixin(FaceBakery.class)
public class FaceBakeryMixin {
    @Inject(method = "applyElementRotation", at = @At("HEAD"), cancellable = true)
    private void plantz$applyCuboidRotation(Vector3f vec, @Nullable BlockElementRotation partRotation, CallbackInfo ci) {
        if (partRotation == null) return;
        Matrix4fc transform = ((CuboidRotation.Holder) (Object) partRotation).plantz$transform();
        if (transform == null) return;
        CuboidRotation.apply(vec, partRotation.origin(), transform);
        ci.cancel();
    }
}
