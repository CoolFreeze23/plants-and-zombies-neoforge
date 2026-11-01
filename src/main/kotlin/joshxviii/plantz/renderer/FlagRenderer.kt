package joshxviii.plantz.renderer

import net.minecraft.client.renderer.RenderType

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazNetwork
import joshxviii.plantz.block.FlagBlock
import joshxviii.plantz.block.entity.FlagBlockEntity
import joshxviii.plantz.model.FlagBlockModel
import joshxviii.plantz.pazResource
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector
import joshxviii.plantz.compat.client.renderer.blockentity.BlockEntityRenderer
import joshxviii.plantz.compat.client.state.BlockEntityRenderState
import joshxviii.plantz.compat.client.renderer.ModelFeatureRenderer
import joshxviii.plantz.compat.client.CameraRenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.Material
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.state.properties.RotationSegment
import net.minecraft.world.phys.Vec3
import kotlin.math.pow

class FlagRenderer(
    val flagModel: FlagBlockModel
) : BlockEntityRenderer<FlagBlockEntity, FlagRenderState> {
    companion object {
        val PLANTZ_FLAG_MATERIAL = Material(
            pazResource("textures/block/plantz_flag.png"),
            pazResource("base")
        )
        val BRAINZ_FLAG_MATERIAL = Material(
            pazResource("textures/block/brainz_flag.png"),
            pazResource("base")
        )
        const val MINIMUM_BEAM_DISTANCE = 32f
    }
    override fun createRenderState(): FlagRenderState {
        return FlagRenderState()
    }

    override fun extractRenderState(
        blockEntity: FlagBlockEntity,
        state: FlagRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
        val pos = blockEntity.blockPos
        val blockState = blockEntity.blockState
        val gameTime = blockEntity.getLevel()?.gameTime ?: 0L
        state.health = blockEntity.health
        state.maxHealth = blockEntity.maxHealth
        state.phase = (Math.floorMod((pos.x*7 + pos.y*9 + pos.z*13).toLong() + gameTime, 90L).toFloat() + partialTicks) / 90.0f
        state.angle = -RotationSegment.convertToDegrees(blockState.getValue(FlagBlock.ROTATION))
        state.beamAnimTime = Math.floorMod(gameTime, 40).toFloat() + partialTicks
        state.beamDistance = cameraPosition.subtract(state.blockPos.center).horizontalDistance().toFloat()
        if (blockState.`is`(PazBlocks.PLANTZ_FLAG)) state.sprite = PLANTZ_FLAG_MATERIAL
        else state.sprite = BRAINZ_FLAG_MATERIAL
    }

    override fun submit(
        state: FlagRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        if (PazConfig.SHOW_DEBUG_INFO) submitNodeCollector.submitNameTag(
            poseStack, Vec3(0.5,1.0,0.5), -20,
            Component.literal("${state.health} / ${state.maxHealth}").withColor(0xFFFFFFF),
            true, -1, 20.0, camera
        )

        if (PazNetwork.ZombieRaidClientCache.isActive() && state.maxHealth > 100) renderBeam(state, poseStack, submitNodeCollector, camera)

        val material = state.sprite?: return
        val renderType = material.renderType { RenderType.entityCutout(it) }
        poseStack.translate(0.5f, 0.0f, 0.5f)
        poseStack.mulPose(Axis.YP.rotationDegrees(state.angle+90f))
        submitNodeCollector.submitModel(
            flagModel,
            state,
            poseStack,
            renderType,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            0,
            state.breakProgress
        )

    }

    fun renderBeam(
        state: FlagRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val distanceScale = ((state.beamDistance - MINIMUM_BEAM_DISTANCE).pow(.8f) / state.beamDistance)
        // 26.1 submitBeaconBeam(animationTime) -> 1.21.1 renderBeaconBeam(partialTick + floorMod(gameTime, 40))
        if (distanceScale > 0) BeaconRenderer.renderBeaconBeam(
            poseStack,
            submitNodeCollector.bufferSource(),
            BeaconRenderer.BEAM_LOCATION,
            state.beamAnimTime,
            1.0f,
            0L,
            0,
            2048,
            0x11EE22,
            distanceScale,
            distanceScale * 1.5f
        )

    }

    override fun shouldRenderOffScreen(blockEntity: FlagBlockEntity): Boolean {
        return PazNetwork.ZombieRaidClientCache.isActive()
    }

    override fun getViewDistance(): Int {
        return Minecraft.getInstance().options.effectiveRenderDistance * 16
    }

    override fun shouldRender(blockEntity: FlagBlockEntity, cameraPosition: Vec3): Boolean {
        val flagPos = Vec3.atCenterOf(blockEntity.blockPos).multiply(1.0, 0.0, 1.0)
        val cameraPos = cameraPosition.multiply(1.0, 0.0, 1.0)

        return flagPos.closerThan(cameraPos, this.viewDistance.toDouble())
    }
}

class FlagRenderState : BlockEntityRenderState() {
    var health: Float = 0f
    var maxHealth: Float = 0f
    var phase: Float = 0f
    var sprite: Material? = null
    var angle: Float = 0f
    var beamAnimTime: Float = 0f
    var beamDistance: Float = 0f
}