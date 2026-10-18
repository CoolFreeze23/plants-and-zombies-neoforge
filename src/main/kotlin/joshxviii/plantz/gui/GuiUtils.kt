package joshxviii.plantz.gui

import com.mojang.blaze3d.systems.RenderSystem
import joshxviii.plantz.compat.ARGB
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence

/*
 * 1.21.1 stand-ins for the 26.1 GuiGraphicsExtractor calls the GUI used.
 * (`joshxviii.plantz.renderer.outlineText` is the render area's copy for GuiGraphicsExtractor; these are the GuiGraphics ones.)
 */

/** 26.1 `blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, texW, texH[, color])`: alpha-blended, ARGB-tinted. */
fun GuiGraphics.pazBlit(texture: ResourceLocation, x: Int, y: Int, u: Float, v: Float, width: Int, height: Int, textureWidth: Int, textureHeight: Int, color: Int = -1) {
    RenderSystem.enableBlend()
    RenderSystem.defaultBlendFunc()
    if (color != -1) setColor(ARGB.red(color) / 255f, ARGB.green(color) / 255f, ARGB.blue(color) / 255f, ARGB.alpha(color) / 255f)
    blit(texture, x, y, u, v, width, height, textureWidth, textureHeight)
    if (color != -1) setColor(1f, 1f, 1f, 1f)
    RenderSystem.disableBlend()
}

fun GuiGraphics.outlineText(font: Font, text: String, x: Int = 0, y: Int = 0, color: Int = 0xFFFFFF, outlineColor: Int = ARGB.multiply(color, 0x333333)) {
    val c = Component.literal(text)
    outlineText(font, c, x, y, color, outlineColor)
}

fun GuiGraphics.outlineText(font: Font, text: Component, x: Int = 0, y: Int = 0, color: Int = 0xFFFFFF, outlineColor: Int = ARGB.multiply(color, 0x333333)) {
    outlineText(font, text.visualOrderText, x, y, color, outlineColor)
}

fun GuiGraphics.outlineText(font: Font, text: FormattedCharSequence, x: Int = 0, y: Int = 0, color: Int = 0xFFFFFF, outlineColor: Int = ARGB.multiply(color, 0x333333)) {
    drawString(font, text, x+1, y, ARGB.opaque(outlineColor), false)
    drawString(font, text, x-1, y, ARGB.opaque(outlineColor), false)
    drawString(font, text, x, y+1, ARGB.opaque(outlineColor), false)
    drawString(font, text, x, y-1, ARGB.opaque(outlineColor), false)
    drawString(font, text, x, y, ARGB.opaque(color), false)
}
