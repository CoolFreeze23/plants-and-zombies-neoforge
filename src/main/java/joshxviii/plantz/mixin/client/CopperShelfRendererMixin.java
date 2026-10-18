package joshxviii.plantz.mixin.client;

import joshxviii.plantz.compat.client.OnShelfDisplay;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Copper Age Backport's shelves draw items with the {@code fixed} transform; 26.1 shelves use {@code on_shelf}, which
 * the upstream models define. Optional: skipped without Copper Age Backport, and require = 0 keeps a changed shelf
 * renderer from failing the game (items then keep the fixed transform).
 */
@Pseudo
@Mixin(targets = "com.github.smallinger.copperagebackport.client.renderer.ShelfRenderer")
public class CopperShelfRendererMixin {
    @ModifyArg(method = "*", require = 0, index = 1, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V"))
    private ItemDisplayContext plantz$onShelf(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIXED ? OnShelfDisplay.ON_SHELF : context;
    }
}
