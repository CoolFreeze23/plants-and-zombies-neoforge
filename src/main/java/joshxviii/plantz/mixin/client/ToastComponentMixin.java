package joshxviii.plantz.mixin.client;

import joshxviii.plantz.client.NowPlayingToast;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws 26.1's NowPlayingToast (which 1.21.1 doesn't have) with the other toasts. */
@Mixin(ToastComponent.class)
public class ToastComponentMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void plantz$renderNowPlaying(GuiGraphics guiGraphics, CallbackInfo ci) {
        NowPlayingToast.render(guiGraphics);
    }
}
