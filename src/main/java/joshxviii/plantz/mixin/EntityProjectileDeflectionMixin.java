package joshxviii.plantz.mixin;

import joshxviii.plantz.PazComponents;
import joshxviii.plantz.item.component.BlocksAttacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityProjectileDeflectionMixin {
    @Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
    private void plantz$deflectProjectilesWhileBlocking(
        Projectile projectile,
        CallbackInfoReturnable<ProjectileDeflection> cir
    ) {
        if (!((Object) this instanceof LivingEntity entity)) return;

        ItemStack wornBlocker = BlocksAttacks.wornBlocker(entity);
        ItemStack blockingItem = BlocksAttacks.heldBlocker(entity);
        if (blockingItem == null) blockingItem = wornBlocker;
        if (blockingItem == null) return;

        BlocksAttacks blocksAttacks = blockingItem.get(PazComponents.BLOCKS_ATTACKS);
        if (blocksAttacks == null) return;

        double angle = BlocksAttacks.angleTo(entity, projectile.position());
        boolean canBlockFromDirection = blocksAttacks.canBlockFrom(angle);
        if (!canBlockFromDirection && wornBlocker != null && wornBlocker != blockingItem) {
            BlocksAttacks wornAttacks = wornBlocker.get(PazComponents.BLOCKS_ATTACKS);
            if (wornAttacks != null) {
                blockingItem = wornBlocker;
                blocksAttacks = wornAttacks;
                canBlockFromDirection = wornAttacks.canBlockFrom(angle);
            }
        }
        if (canBlockFromDirection) {
            if (entity.level() instanceof ServerLevel serverLevel) {
                blocksAttacks.onBlocked(serverLevel, entity);
                EquipmentSlot slot = blockingItem == wornBlocker ? EquipmentSlot.CHEST : LivingEntity.getSlotForHand(entity.getUsedItemHand());
                blocksAttacks.hurtBlockingItem(serverLevel, blockingItem, entity, slot, 1.0F);
            }
            cir.setReturnValue(ProjectileDeflection.REVERSE);
        }
    }
}
