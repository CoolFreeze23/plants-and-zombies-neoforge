package joshxviii.plantz.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import joshxviii.plantz.PazComponents;
import joshxviii.plantz.entity.projectile.PazProjectile;
import joshxviii.plantz.item.component.ProtectsHeadDataComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityProtectsHeadMixin {
    @Unique private boolean plantz$headBlockedAll;

    // 26.1 applyItemBlocking (RETURN) -> 1.21.1 hurt, the damage left after shield blocking (read right before the freezing check)
    @ModifyVariable(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;IS_FREEZING:Lnet/minecraft/tags/TagKey;"), argsOnly = true)
    private float plantz$applyPassiveHeadProtection(float damage, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        plantz$headBlockedAll = false;
        if (damage <= 0.0F || !(entity.level() instanceof ServerLevel level)) return damage;

        if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) return damage;
        if (source.getDirectEntity() instanceof PazProjectile projectile && projectile.getPierceLevel() > 0) return damage;

        float remainingDamage = damage;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = entity.getItemBySlot(slot);
            ProtectsHeadDataComponent component = item.get(PazComponents.PROTECTS_HEAD);
            if (component == null || !component.getSlot().test(slot)) continue;
            if (!component.canBlockFrom(source, entity)) continue;

            float blocked = component.resolveBlockedDamage(source, remainingDamage);
            component.hurtBlockingItem(entity.level(), item, entity, slot, blocked);
            component.onBlocked(level, entity);
            if (!component.isFromAbove(source, entity)) continue;

            if (blocked <= 0.0F) continue;
            remainingDamage -= blocked;
            if (remainingDamage <= 0.0F) break;
        }

        plantz$headBlockedAll = remainingDamage <= 0.0F;
        return remainingDamage;
    }

    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;playHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void plantz$suppressHurtSoundWhenFullyBlocked(LivingEntity entity, DamageSource source, Operation<Void> original) {
        if (!plantz$headBlockedAll) original.call(entity, source);
    }
}
