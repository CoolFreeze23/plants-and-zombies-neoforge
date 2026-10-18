package joshxviii.plantz.gui

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f

class ItemIconToolTipComponent(private val stack: ItemStack, val message : Component = Component.empty()) : ClientTooltipComponent {

    override fun getHeight(): Int = 16

    override fun getWidth(font: Font): Int = 20

    override fun renderImage(font: Font, x: Int, y: Int, graphics: GuiGraphics) {
        graphics.renderItem(stack, x + 2, y + 1)
        graphics.renderItemDecorations(font, stack, x + 2, y + 1)    }

    override fun renderText(font: Font, x: Int, y: Int, matrix: Matrix4f, bufferSource: MultiBufferSource.BufferSource) {
        font.drawInBatch(message, x.toFloat(), y.toFloat(), -1, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880)
    }
}
