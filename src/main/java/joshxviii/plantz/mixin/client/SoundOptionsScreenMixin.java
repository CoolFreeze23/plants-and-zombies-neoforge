package joshxviii.plantz.mixin.client;

import java.util.Arrays;
import joshxviii.plantz.PazConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds 26.1's "UI" volume slider next to the sound categories. 1.21.1 has no UI sound category (SoundSource can't be
 * extended), so the slider is the port's client option that scales the raid announcement sounds (UiSoundPayload).
 */
@Mixin(SoundOptionsScreen.class)
public class SoundOptionsScreenMixin {
    @Inject(method = "getAllSoundOptionsExceptMaster", at = @At("RETURN"), cancellable = true)
    private void plantz$addUiVolume(CallbackInfoReturnable<OptionInstance<?>[]> cir) {
        OptionInstance<?>[] options = cir.getReturnValue();
        OptionInstance<?>[] withUi = Arrays.copyOf(options, options.length + 1);
        withUi[options.length] = new OptionInstance<>(
            "soundCategory.ui",
            OptionInstance.noTooltip(),
            (caption, value) -> value == 0.0 ? Options.genericValueLabel(caption, CommonComponents.OPTION_OFF)
                : Component.translatable("options.percent_value", caption, (int) (value * 100.0)),
            OptionInstance.UnitDouble.INSTANCE,
            PazConfig.INSTANCE.getClient().getUiVolume(),
            value -> {
                PazConfig.INSTANCE.getClient().setUiVolume(value);
                PazConfig.INSTANCE.saveClient();
            }
        );
        cir.setReturnValue(withUi);
    }
}
