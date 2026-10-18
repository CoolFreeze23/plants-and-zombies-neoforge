package joshxviii.plantz.mixin;

import joshxviii.plantz.PlantHeadAttachment;
import joshxviii.plantz.entity.plant.Plant;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Josh
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "remove", at = @At(value = "TAIL"))
    public void removePlantAttachment(ServerPlayer player, CallbackInfo ci) {
        if (((PlantHeadAttachment) player).plantz$hasPlantOnHead()) {
            var plant = ((PlantHeadAttachment) player).plantz$getPlant();
            if (plant!=null) {
                plant.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
            }
        }
    }

    /**
     * 26.1 ServerPlayerMixin#respawnAttachedPlant (ServerPlayer.loadAndSpawnParentVehicle). On 1.21.1 the parent vehicle
     * is loaded in placeNewPlayer, so the attached plant is respawned at the same point. The plant tag is the one
     * LivingEntityMixin read from "plantz:AttachedPlant".
     */
    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;initInventoryMenu()V"))
    public void respawnAttachedPlant(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        var plantData = ((PlantHeadAttachment) player).plantz$getPlantData();
        if ( !plantData.isEmpty() ) {
            ServerLevel serverLevel = player.serverLevel();
            Entity entity = EntityType.loadEntityRecursive(
                    plantData.copy(), serverLevel, e -> !serverLevel.addWithUUID(e) ? null : e
            );
            if (entity instanceof Plant plantAttachment) {
                plantAttachment.attachToEntity(player);
            }
        }
    }
}
