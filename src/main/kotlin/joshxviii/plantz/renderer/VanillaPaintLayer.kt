package joshxviii.plantz.renderer

import com.mojang.blaze3d.vertex.PoseStack
import joshxviii.plantz.PazRenderPipelines
import joshxviii.plantz.compat.ARGB
import joshxviii.plantz.mixin.LivingEntityAccessor
import joshxviii.plantz.pazResource
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.LivingEntity
import kotlin.math.pow

/**
 * Paint overlay for vanilla 1.21.1 renderers (players, vanilla mobs). Upstream's paint layer applies to every living
 * renderer; on 1.21.1 the mod's own renderers get [SpecialEffectsLayer] and vanilla ones get this equivalent.
 */
class VanillaPaintLayer<T : LivingEntity, M : EntityModel<T>>(parent: RenderLayerParent<T, M>) : RenderLayer<T, M>(parent) {
    companion object {
        private val NOISE_TEXTURE = pazResource("textures/entity/paint_overlay.png")

        /** Same mix as SpecialEffectsLayer: average of the paint colours, alpha from the summed amplifiers. */
        fun paintColor(colors: Map<Int, Int>): Int? {
            var colorMix = -1
            var amplifier = 0
            colors.forEach { (color, amp) ->
                amplifier += amp
                colorMix = if (colorMix == -1) ARGB.opaque(color) else ARGB.average(colorMix, ARGB.opaque(color))
            }
            if (amplifier == 0 || colorMix == -1) return null
            val strength = (amplifier.coerceIn(0, 20) / 20f).pow(.5f).coerceIn(0f, 1f)
            val alpha = (strength * 0xFF).toInt().coerceIn(50, 0xFF)
            return (alpha shl 24) or (colorMix and 0x00FFFFFF)
        }
    }

    override fun render(
        poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, entity: T,
        limbSwing: Float, limbSwingAmount: Float, partialTick: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float
    ) {
        if (entity.isInvisible) return
        val color = paintColor((entity as LivingEntityAccessor).`plantz$getPaintedColors`()) ?: return
        val renderType = PazRenderPipelines.paintOverlay(getTextureLocation(entity), NOISE_TEXTURE)
        parentModel.renderToBuffer(poseStack, buffer.getBuffer(renderType), packedLight, OverlayTexture.NO_OVERLAY, color)
    }
}
