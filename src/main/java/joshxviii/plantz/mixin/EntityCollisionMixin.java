package joshxviii.plantz.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import joshxviii.plantz.entity.plant.WallNut;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 26.1 {@code Entity.canBeCollidedWith(other)}: the wall-nut sees which entity is colliding with it. */
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @WrapOperation(method = "canCollideWith", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;canBeCollidedWith()Z"))
    private boolean plantz$canBeCollidedWithOther(Entity entity, Operation<Boolean> original) {
        return entity instanceof WallNut wallNut ? wallNut.canBeCollidedWith((Entity) (Object) this) : original.call(entity);
    }
}
