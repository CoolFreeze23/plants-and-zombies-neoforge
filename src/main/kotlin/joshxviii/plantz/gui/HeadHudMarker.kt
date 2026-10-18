package joshxviii.plantz.gui

import com.mojang.math.Axis
import joshxviii.plantz.PazRenderPipelines
import joshxviii.plantz.gui.ZombieHudMarkers.ICON_SIZE
import joshxviii.plantz.compat.client.model.HumanoidModel
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import joshxviii.plantz.compat.LightCoordsUtil
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.item.ItemStack

/**
 * 26.1 queued this as a GuiElementRenderState for the HEAD_MARKER_BAKE / HEAD_MARKER_OUTLINE pipelines; 1.21.1 draws
 * it right away with the equivalent render types.
 */
class HeadHudMarker(
    val zombie: Zombie,
    val model: HumanoidModel<*>,
    val texture: ResourceLocation,
    val itemStack: ItemStack,
    val cx: Double,
    val cy: Double,
    val scale: Float,
    val outlinePad: Float = 0f
) {
    val mc = Minecraft.getInstance()

    fun render(graphics: GuiGraphics) {
        model.resetPose()
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(cx, cy, if (outlinePad>0f) 149.0 else 150.0)
        pose.translate(0.0, (ICON_SIZE + outlinePad)*0.5, 0.0)
        pose.mulPose(Axis.YP.rotationDegrees(180.0f))
        pose.scale(scale, scale, scale)
        if (outlinePad>0f) pose.scale(outlinePad, outlinePad, outlinePad)
        model.head.setPos(0f, 0f, 0f)

        val renderType = if (outlinePad > 0f) PazRenderPipelines.HEAD_MARKER_OUTLINE.apply(texture) else PazRenderPipelines.HEAD_MARKER_BAKE.apply(texture)
        graphics.flush()
        model.head.render(pose, graphics.bufferSource().getBuffer(renderType), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
        graphics.flush()
        // (26.1 also resolved the worn head item here but never drew it; worn items are not rendered)

        pose.popPose()
    }
}
