package joshxviii.plantz.mixin;

import joshxviii.plantz.entity.plant.Plant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow
    public abstract void tickNonPassenger(Entity entity);

    @Inject(method = "tickNonPassenger", at = @At(value = "HEAD"), cancellable = true)
    public void tickSyncPlant(Entity entity, CallbackInfo ci) {
        if (entity instanceof Plant) {
            var attachedEntity = ((Plant) entity).getAttachedEntity();
            if (attachedEntity!=null) {
                tickNonPassenger(attachedEntity);
            }
        }
    }
}
