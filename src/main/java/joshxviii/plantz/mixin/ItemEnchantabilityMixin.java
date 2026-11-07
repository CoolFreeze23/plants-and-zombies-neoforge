package joshxviii.plantz.mixin;

import joshxviii.plantz.PazItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.1's Enchantable component on items whose classes aren't the mod's own (the worn bucket, the cone). */
@Mixin(Item.class)
public abstract class ItemEnchantabilityMixin {
    @Inject(method = "getEnchantmentValue()I", at = @At("HEAD"), cancellable = true)
    private void plantz$enchantmentValue(CallbackInfoReturnable<Integer> cir) {
        int value = PazItems.enchantabilityOf((Item) (Object) this);
        if (value > 0) cir.setReturnValue(value);
    }

    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void plantz$enchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (PazItems.enchantabilityOf((Item) (Object) this) > 0) cir.setReturnValue(stack.getCount() == 1);
    }
}
