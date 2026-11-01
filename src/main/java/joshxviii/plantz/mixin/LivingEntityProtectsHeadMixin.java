package joshxviii.plantz.mixin;

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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityProtectsHeadMixin {
    // 26.1 applyItemBlocking (RETURN) -> 1.21.1 hurt, the damage left after shield blocking (read right before the freezing check)
    @ModifyVariable(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;IS_FREEZING:Lnet/minecraft/tags/TagKey;"), argsOnly = true)
    private float plantz$applyPassiveHeadProtection(float damage, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
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
            if (blocked <= 0.0F) continue;

            component.hurtBlockingItem(entity.level(), item, entity, slot, blocked);
            component.onBlocked(level, entity);
            remainingDamage -= blocked;
            if (remainingDamage <= 0.0F) break;
        }

        return remainingDamage;
    }
}
