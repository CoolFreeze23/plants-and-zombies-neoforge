package joshxviii.plantz.gui

import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import joshxviii.plantz.gui.ZombieHudMarkers.ICON_SIZE
import joshxviii.plantz.compat.client.model.HumanoidModel
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import joshxviii.plantz.compat.LightCoordsUtil
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.item.ItemStack
import org.joml.Vector3f

/**
 * Zombie head icon on the HUD. 26.1 queued this as a GuiElementRenderState drawn with the HEAD_MARKER_BAKE
 * pipeline (entity snippet, cutout, no cull, no cardinal lighting); 1.21.1 draws it immediately with
 * entityCutoutNoCull, lit head-on so the visible face is full bright.
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

        graphics.flush()
        RenderSystem.setShaderLights(LIGHT_TOWARDS_VIEWER, LIGHT_TOWARDS_VIEWER)
        model.head.render(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(texture)), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
        graphics.flush()
        Lighting.setupFor3DItems()
        // (26.1 also resolved the worn head item here but never drew it; worn items are not rendered)

        pose.popPose()
    }

    companion object {
        private val LIGHT_TOWARDS_VIEWER = Vector3f(0f, 0f, 1f)
    }
}
