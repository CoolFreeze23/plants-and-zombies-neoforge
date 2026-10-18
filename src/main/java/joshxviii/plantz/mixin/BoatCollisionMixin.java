package joshxviii.plantz.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import joshxviii.plantz.entity.plant.WallNut;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 26.1 {@code Entity.canBeCollidedWith(vehicle)} for boats and minecarts (see {@link EntityCollisionMixin}). */
@Mixin(Boat.class)
public abstract class BoatCollisionMixin {
    @WrapOperation(method = "canVehicleCollide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;canBeCollidedWith()Z"))
    private static boolean plantz$canBeCollidedWithVehicle(Entity entity, Operation<Boolean> original, Entity vehicle, Entity collidingEntity) {
        return entity instanceof WallNut wallNut ? wallNut.canBeCollidedWith(vehicle) : original.call(entity);
    }
}
