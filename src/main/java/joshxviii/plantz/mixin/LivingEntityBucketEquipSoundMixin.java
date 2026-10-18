package joshxviii.plantz.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Equip sound for the bucket helmet (26.1: the bucket's equippable component uses ARMOR_EQUIP_IRON). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBucketEquipSoundMixin extends Entity {
    protected LivingEntityBucketEquipSoundMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "onEquipItem", at = @At("TAIL"))
    private void plantz$bucketHelmetEquipSound(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, CallbackInfo ci) {
        if (slot != EquipmentSlot.HEAD || !newItem.is(Items.BUCKET) || ItemStack.isSameItemSameComponents(oldItem, newItem)) return;
        if (this.firstTick || this.level().isClientSide() || this.isSpectator() || this.isSilent()) return;
        this.level().playSeededSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_EQUIP_IRON, this.getSoundSource(), 1.0F, 1.0F, this.random.nextLong());
    }
}
