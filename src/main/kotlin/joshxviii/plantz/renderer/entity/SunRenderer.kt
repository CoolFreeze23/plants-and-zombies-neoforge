package joshxviii.plantz.renderer.entity

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import joshxviii.plantz.entity.Sun
import joshxviii.plantz.pazResource
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector
import joshxviii.plantz.compat.client.renderer.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import joshxviii.plantz.compat.client.state.EntityRenderState
import net.minecraft.client.renderer.RenderType
import joshxviii.plantz.compat.client.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import joshxviii.plantz.compat.LightCoordsUtil
import net.minecraft.util.Mth

class SunRenderer(context: EntityRendererProvider.Context) :
    EntityRenderer<Sun, SunRenderState>(context) {
    init {
        this.shadowRadius = 0.15f
        this.shadowStrength = 0.75f
    }

    override fun getBlockLightLevel(entity: Sun, blockPos: BlockPos): Int {
        return 15
    }

    override fun submit(
        state: SunRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val blinkedOut = state.isExpiring && state.ageInTicks.toInt() % 2 == 0
        if (!blinkedOut) {
            val scale = Sun.scaleForValue(state.value)
            poseStack.pushPose()
            poseStack.translate(0.0f, state.centerY, 0.0f)
            poseStack.mulPose(camera.orientation)
            poseStack.scale(scale, scale, scale)
            //poseStack.mulPose(Axis.YP.rotation(state.ageInTicks * SPIN_SPEED + state.phase))
            submitPlane(state, poseStack, submitNodeCollector)
            //poseStack.mulPose(Axis.YP.rotation(Mth.HALF_PI))
            //submitPlane(state, poseStack, submitNodeCollector)
            poseStack.popPose()
        }

        super.submit(state, poseStack, submitNodeCollector, camera)
    }

    private fun submitPlane(state: SunRenderState, poseStack: PoseStack, collector: SubmitNodeCollector) {
        val light = LightCoordsUtil.FULL_BRIGHT
        collector.submitCustomGeometry(poseStack, EMISSIVE_SUN) { pose: PoseStack.Pose, buffer: VertexConsumer ->
            vertex(buffer, pose, -HALF, -HALF, 0f, 1f, light)
            vertex(buffer, pose, HALF, -HALF, 1f, 1f, light)
            vertex(buffer, pose, HALF, HALF, 1f, 0f, light)
            vertex(buffer, pose, -HALF, HALF, 0f, 0f, light)

            vertex(buffer, pose, -HALF, -HALF, 0f, 1f, light)
            vertex(buffer, pose, -HALF, HALF, 0f, 0f, light)
            vertex(buffer, pose, HALF, HALF, 1f, 0f, light)
            vertex(buffer, pose, HALF, -HALF, 1f, 1f, light)
        }
    }

    override fun createRenderState(): SunRenderState {
        return SunRenderState()
    }

    override fun extractRenderState(entity: Sun, state: SunRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        state.value = entity.value
        state.isExpiring = entity.isExpiring
        state.centerY = entity.bbHeight / 2f
        state.phase = entity.id * 0.7f
    }

    companion object {
        private val SUN_LOCATION = pazResource("textures/entity/sun.png")
        // 26.1 RenderPipelines.BEACON_BEAM_TRANSLUCENT render setup: translucent and full bright
        val EMISSIVE_SUN: RenderType = RenderType.entityTranslucentEmissive(SUN_LOCATION)

        private const val HALF = 0.5f
        private const val SPIN_SPEED = 0.06f

        private fun vertex(buffer: VertexConsumer, pose: PoseStack.Pose, x: Float, y: Float, u: Float, v: Float, lightCoords: Int) {
            buffer.addVertex(pose, x, y, 0.0f)
                .setColor(255, 255, 255, 200)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0f, 1.0f, 0.0f)
        }
    }
}

class SunRenderState : EntityRenderState() {
    var value: Int = 0
    var isExpiring: Boolean = false
    var phase: Float = 0f
    var centerY: Float = 0.15f
}