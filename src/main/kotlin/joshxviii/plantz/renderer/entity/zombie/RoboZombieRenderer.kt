package joshxviii.plantz.renderer.entity.zombie

import com.mojang.blaze3d.vertex.PoseStack
import joshxviii.plantz.entity.zombie.PazZombie
import joshxviii.plantz.entity.zombie.RoboZombie
import joshxviii.plantz.model.zombies.PazZombieModel
import joshxviii.plantz.model.zombies.RoboZombieModel
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import joshxviii.plantz.compat.client.CameraRenderState
import joshxviii.plantz.compat.AnimationState

class RoboZombieRenderer(
    context: EntityRendererProvider.Context,
    private val model: PazZombieModel<PazZombieRenderState> = RoboZombieModel(context.bakeLayer(RoboZombieModel.LAYER_LOCATION)),
): PazZombieRenderer(context, model, model) {

    override fun submit(
        state: PazZombieRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        super.submit(state, poseStack, collector, camera)
    }

    override fun createRenderState(): PazZombieRenderState {
        return RoboZombieRenderState()
    }

    override fun extractRenderState(entity: PazZombie, state: PazZombieRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        (state as RoboZombieRenderState)
        (entity as RoboZombie)
        state.isTankTransformation = entity.isTransformed
        state.idleAnimationState.copyFrom(entity.idleAnimation)
        state.shootAnimationState.copyFrom(entity.shootAnimation)
        state.bashAnimationState.copyFrom(entity.bashAnimation)
    }

}

class RoboZombieRenderState: PazZombieRenderState() {
    var isTankTransformation = false
    val idleAnimationState: AnimationState = AnimationState()
    val shootAnimationState: AnimationState = AnimationState()
    val bashAnimationState: AnimationState = AnimationState()
}
