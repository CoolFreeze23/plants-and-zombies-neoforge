package joshxviii.plantz.mixin.client.model;

import joshxviii.plantz.compat.client.model.CuboidRotation;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Lets a 1.21.1 element rotation carry a 26.1 transform (see {@link CuboidRotation}). */
@Mixin(BlockElementRotation.class)
public class BlockElementRotationMixin implements CuboidRotation.Holder {
    @Unique
    @Nullable
    private Matrix4fc plantz$transform;

    @Override
    @Nullable
    public Matrix4fc plantz$transform() {
        return this.plantz$transform;
    }

    @Override
    public void plantz$setTransform(Matrix4fc transform) {
        this.plantz$transform = transform;
    }
}
