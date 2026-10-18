package joshxviii.plantz.mixin.client;

import joshxviii.plantz.gui.InvertHighlightEditBox;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 26.1 EditBox.setInvertHighlightedTextColor(false): the selection is an additive blue box instead of inverting the text.
 */
@Mixin(EditBox.class)
public class EditBoxMixin implements InvertHighlightEditBox {
    /** 26.1 RenderPipelines.GUI_TEXT_HIGHLIGHT (additive blend). */
    @Unique
    private static final RenderType PLANTZ$TEXT_HIGHLIGHT_ADDITIVE = RenderType.create(
        "plantz_gui_text_highlight_additive", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_GUI_SHADER)
            .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
            .createCompositeState(false)
    );

    @Unique
    private boolean plantz$invertHighlightedTextColor = true;

    @Override
    public void plantz$setInvertHighlightedTextColor(boolean invert) {
        this.plantz$invertHighlightedTextColor = invert;
    }

    @Redirect(method = "renderHighlight", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"))
    private void plantz$fillHighlight(GuiGraphics graphics, RenderType renderType, int minX, int minY, int maxX, int maxY, int color) {
        graphics.fill(plantz$invertHighlightedTextColor ? renderType : PLANTZ$TEXT_HIGHLIGHT_ADDITIVE, minX, minY, maxX, maxY, color);
    }
}
