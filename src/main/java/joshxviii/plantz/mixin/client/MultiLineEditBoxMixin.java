package joshxviii.plantz.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import joshxviii.plantz.gui.LetterEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 26.1 MultiLineEditBox.builder() textColor / cursorColor / textShadow, for {@link LetterEditBox}. */
@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxMixin {
    @WrapOperation(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"))
    private int plantz$letterText(GuiGraphics graphics, Font font, String text, int x, int y, int color, Operation<Integer> original) {
        if ((Object) this instanceof LetterEditBox box) return graphics.drawString(font, text, x, y, box.getTextColor(), false);
        return original.call(graphics, font, text, x, y, color);
    }

    @ModifyArg(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), index = 4)
    private int plantz$letterCursor(int color) {
        return (Object) this instanceof LetterEditBox box ? box.getTextColor() : color;
    }
}
