package joshxviii.plantz.renderer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import joshxviii.plantz.block.GardenGnomeBlock
import joshxviii.plantz.block.GardenGnomeColor
import joshxviii.plantz.block.GardenGnomePose
import joshxviii.plantz.block.entity.GardenGnomeBlockEntity
import joshxviii.plantz.model.GnomeModel
import joshxviii.plantz.renderer.entity.GnomeRenderState
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector
import joshxviii.plantz.compat.client.renderer.blockentity.BlockEntityRenderer
import joshxviii.plantz.compat.client.state.BlockEntityRenderState
import joshxviii.plantz.compat.client.renderer.ModelFeatureRenderer
import joshxviii.plantz.compat.client.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

class GardenGnomeBlockRenderer(
    val gnomeModel: GnomeModel<GnomeRenderState>,
) : BlockEntityRenderer<GardenGnomeBlockEntity, GardenGnomeBlockRenderState> {
    override fun createRenderState(): GardenGnomeBlockRenderState {
        return GardenGnomeBlockRenderState()
    }

    fun getTextureLocation(state: GardenGnomeBlockRenderState): ResourceLocation {
        val texture = state.getTextureLocation(GardenGnomeBlockRenderState.TEXTURE_PATH + "/" + state.color.serializedName)
        return texture
    }

    override fun extractRenderState(
        blockEntity: GardenGnomeBlockEntity,
        state: GardenGnomeBlockRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        val blockState = blockEntity.blockState
        val dir = blockState.getValue(GardenGnomeBlock.FACING)
        val pose = blockState.getValue(GardenGnomeBlock.POSE)
        state.pose = pose
        state.color = blockState.getValue(GardenGnomeBlock.COLOR)
        state.rotation = dir.toYRot()
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
    }

    override fun submit(
        state: GardenGnomeBlockRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        poseStack.pushPose()
        poseStack.translate(0.5, 1.5, 0.5)
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f))
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotation))

        collector.submitModel(
            gnomeModel,
            GnomeRenderState(state.pose),
            poseStack,
            getTextureLocation(state),
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            0,
            null
        )
        poseStack.popPose()
    }
}

class GardenGnomeBlockRenderState : BlockEntityRenderState() {
    companion object {
        const val TEXTURE_PATH = "textures/block/garden_gnome"
    }
    var pose: GardenGnomePose = GardenGnomePose.NONE
    var rotation: Float = 0f
    var color: GardenGnomeColor = GardenGnomeColor.BLUE
}