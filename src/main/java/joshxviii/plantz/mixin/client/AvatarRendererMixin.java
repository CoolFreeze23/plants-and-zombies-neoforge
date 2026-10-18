package joshxviii.plantz.mixin.client;


import joshxviii.plantz.PazItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Josh
 * Port: 26.1 AvatarRenderer.getArmPose(Avatar, ItemStack, InteractionHand) is 1.21.1 PlayerRenderer.getArmPose(AbstractClientPlayer, InteractionHand).
 */
@Mixin(PlayerRenderer.class)
public class AvatarRendererMixin {
    @Inject(at = @At("HEAD"), method = "getArmPose(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;", cancellable = true)
    private static void p(AbstractClientPlayer avatar, InteractionHand hand, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        ItemStack itemInHand = avatar.getItemInHand(hand);
        if (!avatar.swinging && itemInHand.is(PazItems.DYE_BLASTER) && avatar.isUsingItem()) cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
    }
}
