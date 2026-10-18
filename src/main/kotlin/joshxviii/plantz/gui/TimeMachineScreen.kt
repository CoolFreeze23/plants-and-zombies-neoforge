package joshxviii.plantz.gui

import joshxviii.plantz.inventory.TimeMachineMenu
import joshxviii.plantz.pazResource
import joshxviii.plantz.renderer.outlineText
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import joshxviii.plantz.compat.ARGB
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import org.lwjgl.glfw.GLFW

class TimeMachineScreen(
    val menu: TimeMachineMenu,
    val inventory: Inventory,
    title: Component,
) : AbstractContainerScreen<TimeMachineMenu>(menu, inventory, title) {

    init {
        imageWidth = 176
        imageHeight = 180
        inventoryLabelY = imageHeight - 94
    }

    companion object {
        val BACKGROUND: ResourceLocation = pazResource("textures/gui/time_machine/background.png")
        val SUN_BATTERY_SLOT: ResourceLocation = pazResource("textures/gui/time_machine/sun_battery_slot.png")
    }

    override fun init() {
        super.init()
        val xo = (width - imageWidth) / 2
        val yo = (height - imageHeight) / 2
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, a: Float) {
        super.render(graphics, mouseX, mouseY, a)
        renderTooltip(graphics, mouseX, mouseY)
    }

    override fun renderBg(graphics: GuiGraphics, a: Float, xm: Int, ym: Int) {
        val xo = leftPos
        val yo = topPos

        val batterySlot: Slot = this.menu.batterySlot

        graphics.pazBlit(BACKGROUND, xo, yo, 0f, 0f, imageWidth, imageHeight, 256, 256)
        if (!batterySlot.hasItem()) {
            graphics.pazBlit(SUN_BATTERY_SLOT, xo + batterySlot.x, yo + batterySlot.y, 0f, 0f, 16, 16, 16, 16)
        }

        val wipText = Component.translatable("container.plantz.time_machine.wip").withStyle(ChatFormatting.BOLD)
        val textWidth = font.width(wipText)
        val textColor = 0xCC4444
        graphics.outlineText(font, wipText, xo + (imageWidth / 2) - (textWidth / 2), yo + 64, color = textColor, outlineColor = ARGB.multiply(textColor, 0x333333))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val xo = leftPos + 152
        val yo = topPos + 28

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dx: Double, dy: Double): Boolean {
        return super.mouseDragged(mouseX, mouseY, button, dx, dy)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        return super.mouseScrolled(x, y, scrollX, scrollY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().player!!.closeContainer()
            return true
        } else return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun resize(minecraft: Minecraft, width: Int, height: Int) {
        super.resize(minecraft, width, height)
    }

}
