package joshxviii.plantz.renderer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.compat.ARGB
import joshxviii.plantz.mixin.LivingEntityAccessor
import joshxviii.plantz.pazResource
import joshxviii.plantz.renderer.entity.ProjectileRenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import kotlin.math.pow

/*
 * Upstream's mixin added DuckyTubeRenderLayer, DyeVatRenderLayer and SpecialEffectsLayer to every living renderer.
 * The mod's own renderers run on the compat pipeline and get those classes; these are the same layers for 1.21.1's
 * vanilla renderers (players, vanilla mobs), added by LivingEntityRendererVanillaMixin.
 * 1.21.1 baby humanoids are the adult mesh scaled by AgeableListModel, so the attachments follow that scaling.
 */

/** [SpecialEffectsLayer] for vanilla renderers: butter on the head, paint overlay. */
class VanillaSpecialEffectsLayer<T : LivingEntity, M : EntityModel<T>>(parent: RenderLayerParent<T, M>) : RenderLayer<T, M>(parent) {
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
        renderButter(poseStack, buffer, packedLight, entity)
        renderPaint(poseStack, buffer, packedLight, entity)
    }

    private fun renderButter(poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, entity: T) {
        if (!(entity as LivingEntityAccessor).`plantz$getButterId`()) return
        val humanoidModel = parentModel as? HumanoidModel<*> ?: return

        val butterState = ProjectileRenderState().apply { entityType = PazEntities.BUTTER }
        val texture = butterState.getProjectileTextureLocation() ?: return

        poseStack.pushPose()
        if (humanoidModel.young) {
            poseStack.scale(0.75f, 0.75f, 0.75f)
            poseStack.translate(0.0f, 1.0f, 0.0f)
        }
        humanoidModel.head.translateAndRotate(poseStack)
        poseStack.mulPose(Axis.YP.rotationDegrees(90f))
        poseStack.mulPose(Axis.XP.rotationDegrees(25f))
        poseStack.mulPose(Axis.ZP.rotationDegrees(90f))
        poseStack.translate(-0.6, -1.4, 0.2)

        val butterModel = SpecialEffectsLayer.BUTTER_MODEL
        butterModel.setupAnim(butterState)
        butterModel.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutout(texture)), packedLight, OverlayTexture.NO_OVERLAY)
        poseStack.popPose()
    }

    private fun renderPaint(poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, entity: T) {
        val color = paintColor((entity as LivingEntityAccessor).`plantz$getPaintedColors`()) ?: return
        val renderType = SpecialEffectsLayer.paintOverlay(getTextureLocation(entity))
        parentModel.renderToBuffer(poseStack, buffer.getBuffer(renderType), packedLight, OverlayTexture.NO_OVERLAY, color)
    }
}

/** [DuckyTubeRenderLayer] for vanilla renderers. */
class VanillaDuckyTubeRenderLayer<T : LivingEntity, M : EntityModel<T>>(parent: RenderLayerParent<T, M>) :
    VanillaBodyItemAttachmentRenderLayer<T, M>(parent, PazItems.DUCKY_TUBE, { it.getItemBySlot(EquipmentSlot.LEGS) })

/** [DyeVatRenderLayer] for vanilla renderers. */
class VanillaDyeVatRenderLayer<T : LivingEntity, M : EntityModel<T>>(parent: RenderLayerParent<T, M>) :
    VanillaBodyItemAttachmentRenderLayer<T, M>(parent, PazItems.DYE_BLASTER, { it.mainHandItem })

/** [BodyItemAttachmentRenderLayer] for vanilla renderers: the item model drawn on a humanoid body. */
abstract class VanillaBodyItemAttachmentRenderLayer<T : LivingEntity, M : EntityModel<T>>(
    parent: RenderLayerParent<T, M>,
    private val expectedItem: Item,
    private val stackSelector: (LivingEntity) -> ItemStack,
) : RenderLayer<T, M>(parent) {

    override fun render(
        poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, entity: T,
        limbSwing: Float, limbSwingAmount: Float, partialTick: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float
    ) {
        val humanoidModel = parentModel as? HumanoidModel<*> ?: return
        val itemStack = stackSelector(entity)
        if (!itemStack.`is`(expectedItem)) return

        poseStack.pushPose()
        if (humanoidModel.young) {
            poseStack.scale(0.5f, 0.5f, 0.5f)
            poseStack.translate(0.0f, 1.5f, 0.0f)
        }
        humanoidModel.body.translateAndRotate(poseStack)
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f))

        val minecraft = Minecraft.getInstance()
        minecraft.itemRenderer.renderStatic(
            itemStack, ItemDisplayContext.HEAD, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, minecraft.level, 0
        )
        poseStack.popPose()
    }
}
