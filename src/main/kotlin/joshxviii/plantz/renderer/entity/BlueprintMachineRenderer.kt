package joshxviii.plantz.renderer.entity

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import joshxviii.plantz.ai.ZombieState
import joshxviii.plantz.entity.zombie.ZombieRobot
import joshxviii.plantz.model.blueprint_machines.ZombieTurretModel
import joshxviii.plantz.renderer.getEmissiveTextureLocation
import joshxviii.plantz.renderer.getTextureLocation
import joshxviii.plantz.compat.client.model.EntityModel
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector
import joshxviii.plantz.compat.client.renderer.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import joshxviii.plantz.compat.client.renderer.LivingEntityRenderer
import joshxviii.plantz.compat.client.renderer.layers.RenderLayerParent
import joshxviii.plantz.compat.client.renderer.layers.EyesLayer
import joshxviii.plantz.compat.client.state.EntityRenderState
import joshxviii.plantz.compat.client.state.LivingEntityRenderState
import net.minecraft.client.renderer.RenderType
import joshxviii.plantz.compat.client.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import joshxviii.plantz.compat.AnimationState
import net.minecraft.world.entity.LivingEntity

class BlueprintMachineRenderer(
    context: EntityRendererProvider.Context,
    val machineModel: EntityModel<BlueprintMachineRenderState> = ZombieTurretModel(context.bakeLayer(ZombieTurretModel.LAYER_LOCATION))
) : LivingEntityRenderer<LivingEntity, BlueprintMachineRenderState, EntityModel<BlueprintMachineRenderState>>(
    context,
    machineModel,
    0.5f
) {
    init {
        addLayer(EmissiveBlueprintMachineLayer(this))
    }

    override fun submit(
        state: BlueprintMachineRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        if (state.ageInTicks>1) super.submit(state, poseStack, collector, camera)
    }

    override fun getFlipDegrees(): Float = 0f
    override fun shouldShowName(entity: LivingEntity, distanceToCameraSq: Double): Boolean = false

    override fun extractRenderState(entity: LivingEntity, state: BlueprintMachineRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        if (entity is ZombieRobot) {
            state.initAnimationState.copyFrom(entity.initAnimation)
            state.idleAnimationState.copyFrom(entity.idleAnimation)
            state.actionAnimationState.copyFrom(entity.actionAnimation)
        }
    }

    override fun createRenderState(): BlueprintMachineRenderState {
        return BlueprintMachineRenderState()
    }

    override fun getTextureLocation(state: BlueprintMachineRenderState): ResourceLocation {
        val texture = state.getTextureLocation(BlueprintMachineRenderState.TEXTURE_PATH)
        return texture
    }

}

class EmissiveBlueprintMachineLayer<M : EntityModel<BlueprintMachineRenderState>>(
    renderer: RenderLayerParent<BlueprintMachineRenderState, M>,
) : EyesLayer<BlueprintMachineRenderState, M>(renderer) {
    override fun submit(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        lightCoords: Int,
        state: BlueprintMachineRenderState,
        yRot: Float,
        xRot: Float
    ) {
        val textureLocation = state.getEmissiveTextureLocation(BlueprintMachineRenderState.TEXTURE_PATH) ?: return
        val renderType = RenderType.eyes(textureLocation)
        submitNodeCollector.order(1).submitModel(this.parentModel, state, poseStack, renderType, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
    }

    override fun renderType(): RenderType = RenderType.lines()
}

class BlueprintMachineRenderState : LivingEntityRenderState() {
    companion object {
        const val TEXTURE_PATH = "textures/entity/blueprint_machine"
    }
    val initAnimationState: AnimationState = AnimationState()
    val idleAnimationState: AnimationState = AnimationState()
    val actionAnimationState: AnimationState = AnimationState()
}