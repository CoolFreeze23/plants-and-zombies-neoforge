package joshxviii.plantz.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.client.BucketHelmet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla head layer (players, vanilla mobs): a worn bucket uses the mod's bucket helmet model (see {@link BucketHelmet}). */
@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {
    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void plantz$renderBucketHelmet(ItemInHandRenderer renderer, LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean leftHand,
                                           PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (!BucketHelmet.render(stack, poseStack, buffer, light)) renderer.renderItem(entity, stack, context, leftHand, poseStack, buffer, light);
    }
}
