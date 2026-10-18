package joshxviii.plantz.mixin;

import joshxviii.plantz.PlantHeadAttachment;
import joshxviii.plantz.entity.plant.Plant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Josh
 * <p>
 * Port note: the 26.1 {@code loadAndSpawnParentVehicle} hook (respawning the attached plant when the player joins)
 * moved to {@link PlayerListMixin} (1.21.1 loads vehicles in {@code PlayerList.placeNewPlayer}).
 * 26.1 {@code ServerPlayer.teleport(TeleportTransition)} is 1.21.1 {@code ServerPlayer.changeDimension(DimensionTransition)},
 * which also handles same-dimension teleports.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    //TODO probably should move to a EntityMixin instead
    @Inject(method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;", at = @At(value = "RETURN"))
    public void teleportAttachedPlant(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        ServerPlayer targetPlayer = cir.getReturnValue() instanceof ServerPlayer returned ? returned : (ServerPlayer) (Object) this;
        if ( ((PlantHeadAttachment) this).plantz$getPlant() instanceof Plant plantAttachment ) {
                plantAttachment.detachFromEntity();
                plantAttachment.changeDimension(
                    new DimensionTransition(
                            transition.newLevel(),
                            transition.pos(),
                            transition.speed(),
                            transition.yRot(),
                            transition.xRot(),
                            DimensionTransition.DO_NOTHING.then( entity -> {
                                if (entity instanceof Plant plantEntity) {
                                    plantEntity.setYRot(plantAttachment.yRotO);
                                    plantEntity.setXRot(plantAttachment.xRotO);
                                    plantEntity.attachToEntity(targetPlayer);
                                }
                            })
                    )
            );
        }
    }
}
