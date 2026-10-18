package joshxviii.plantz.gui

import com.mojang.blaze3d.systems.RenderSystem
import joshxviii.plantz.compat.ARGB
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.ContainerEventHandler
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW

// 1.21.1 stand-ins for the 26.1 GuiGraphicsExtractor / EditBox calls the GUI uses.

/** 26.1 `blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, texW, texH[, color])`: alpha-blended, ARGB-tinted. */
fun GuiGraphics.pazBlit(texture: ResourceLocation, x: Int, y: Int, u: Float, v: Float, width: Int, height: Int, textureWidth: Int, textureHeight: Int, color: Int = -1) {
    RenderSystem.enableBlend()
    RenderSystem.defaultBlendFunc()
    if (color != -1) setColor(ARGB.red(color) / 255f, ARGB.green(color) / 255f, ARGB.blue(color) / 255f, ARGB.alpha(color) / 255f)
    blit(texture, x, y, u, v, width, height, textureWidth, textureHeight)
    if (color != -1) setColor(1f, 1f, 1f, 1f)
    RenderSystem.disableBlend()
}

/**
 * 26.1 hands a click only to the widget under the mouse, and a PazButton (whose mouseClicked returns false) clicks
 * without taking focus. 1.21.1 keeps offering the click to the next children, which breaks when the button's action
 * rebuilds the widget list, so screens with PazButtons dispatch to them here first.
 */
fun ContainerEventHandler.clickPazButton(mouseX: Double, mouseY: Double, button: Int): Boolean {
    val widget = getChildAt(mouseX, mouseY).orElse(null) as? PazButton ?: return false
    widget.mouseClicked(mouseX, mouseY, button)
    return true
}

/** 26.1 `com.mojang.blaze3d.platform.cursor.CursorTypes`, as GLFW standard cursors. */
enum class CursorTypes(val glfwShape: Int) {
    POINTING_HAND(GLFW.GLFW_HAND_CURSOR),
    RESIZE_NS(GLFW.GLFW_VRESIZE_CURSOR);

    internal val handle: Long by lazy { GLFW.glfwCreateStandardCursor(glfwShape) }
}

/**
 * 26.1 `GuiGraphicsExtractor.requestCursor`: a cursor requested during a frame is shown for that frame, the default arrow
 * otherwise. Screens that request one call [applyRequestedCursor] after rendering and [resetCursor] when removed.
 */
object GuiCursor {
    private var requested: CursorTypes? = null
    var current: CursorTypes? = null
        private set

    fun request(type: CursorTypes) { requested = type }

    fun applyRequestedCursor() {
        set(requested)
        requested = null
    }

    fun resetCursor() {
        requested = null
        set(null)
    }

    private fun set(type: CursorTypes?) {
        if (type == current) return
        current = type
        GLFW.glfwSetCursor(Minecraft.getInstance().window.window, type?.handle ?: 0L)
    }
}

fun GuiGraphics.requestCursor(type: CursorTypes) = GuiCursor.request(type)

/** Implemented by EditBox through `EditBoxMixin` (26.1 `EditBox.invertHighlightedTextColor`). */
interface InvertHighlightEditBox {
    fun `plantz$setInvertHighlightedTextColor`(invert: Boolean)
}

fun EditBox.setInvertHighlightedTextColor(invert: Boolean) = (this as InvertHighlightEditBox).`plantz$setInvertHighlightedTextColor`(invert)
