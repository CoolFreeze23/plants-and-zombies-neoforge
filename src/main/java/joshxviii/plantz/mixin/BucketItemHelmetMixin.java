package joshxviii.plantz.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 26.1 gives the empty bucket an EQUIPPABLE (head) component. On 1.21.1 this overrides NeoForge's stack-aware
 * {@code IItemExtension.getEquipmentSlot}, so an empty bucket goes into the helmet slot (drag or shift-click) and mobs
 * treat it as headgear. Filled buckets are unaffected. Armor and projectile blocking come from the default components.
 */
@Mixin(BucketItem.class)
public abstract class BucketItemHelmetMixin {
    @Nullable
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return stack.is(Items.BUCKET) ? EquipmentSlot.HEAD : null;
    }
}
