package joshxviii.plantz.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

open class PazButton(
    var buttonX: Int,
    var buttonY: Int,
    val buttonWidth: Int,
    val buttonHeight: Int,
    val clickAction: OnPress,
    val texture: ResourceLocation,
    val hoverTexture: ResourceLocation = texture,
    val disabledTexture: ResourceLocation = texture,
    val enabledRequirement: ((button: PazButton) -> Boolean) = { true },
    val clickRequirement: ((button: PazButton) -> Boolean) = enabledRequirement,
    val text: Component = Component.empty(),
    val color: Int = -1
) : Button(buttonX, buttonY, buttonWidth, buttonHeight, text, clickAction, DEFAULT_NARRATION) {

    override fun renderWidget(
        graphics: GuiGraphics,
        mx: Int,
        my: Int,
        a: Float
    ) {
        val pressed = isPressed()
        val font = Minecraft.getInstance().font

        graphics.pazBlit(
            if (pressed) disabledTexture else if (isButtonHovered(mx, my)) hoverTexture else texture,
            buttonX, buttonY, 0.0f, 0.0f, buttonWidth, buttonHeight, buttonWidth, buttonHeight, color
        )

        val line = font.split(text, buttonWidth-8).firstOrNull()
        if (line!=null) graphics.drawString(font, line, buttonX + if(pressed) 3 else 2, buttonY+3, -1, false)
    }

    fun isPressed(): Boolean {
        return !enabledRequirement.invoke(this)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (clickRequirement.invoke(this)) super.mouseClicked(mouseX, mouseY, button)
        return false
    }

    fun isButtonHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in (buttonX..(buttonX + (buttonWidth-1)))
                && mouseY in (buttonY..(buttonY + (buttonHeight-1)))
    }

}