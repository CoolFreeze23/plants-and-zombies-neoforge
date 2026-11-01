package joshxviii.plantz.gui

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.MultiLineEditBox
import net.minecraft.client.gui.components.Whence
import net.minecraft.network.chat.Component

/**
 * 26.1's `MultiLineEditBox.builder()` options the letter uses, on 1.21.1's MultiLineEditBox: no background or scroll bar
 * (setShowBackground / setShowDecorations false), one text and cursor color without shadow (MultiLineEditBoxMixin) and
 * `setLineLimit`, which 1.21.1's MultilineTextField doesn't have (edits that would go past it are undone).
 */
class LetterEditBox(
    font: Font, x: Int, y: Int, width: Int, height: Int,
    val textColor: Int,
    private val lineLimit: Int
) : MultiLineEditBox(font, x, y, width, height, Component.empty(), Component.empty()) {

    override fun renderBackground(guiGraphics: GuiGraphics) {}

    override fun renderDecorations(guiGraphics: GuiGraphics) {}

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean = limited { super.charTyped(codePoint, modifiers) }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = limited { super.keyPressed(keyCode, scanCode, modifiers) }

    private inline fun limited(edit: () -> Boolean): Boolean {
        val value = this.value
        val cursor = textField.cursor()
        val handled = edit()
        if (textField.lineCount > lineLimit) {
            setValue(value)
            textField.seekCursor(Whence.ABSOLUTE, cursor)
        }
        return handled
    }
}
