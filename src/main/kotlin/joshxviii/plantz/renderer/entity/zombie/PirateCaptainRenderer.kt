package joshxviii.plantz.renderer.entity.zombie

import com.mojang.blaze3d.vertex.PoseStack
import joshxviii.plantz.entity.zombie.PazZombie
import joshxviii.plantz.entity.zombie.PirateCaptain
import joshxviii.plantz.entity.zombie.PirateCaptainGhost
import joshxviii.plantz.model.zombies.PazZombieModel
import joshxviii.plantz.model.zombies.PirateCaptainModel
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.RenderType
import joshxviii.plantz.compat.client.CameraRenderState
import joshxviii.plantz.compat.AnimationState

class PirateCaptainRenderer(
    context: EntityRendererProvider.Context,
    private val model: PazZombieModel<PazZombieRenderState> = PirateCaptainModel(context.bakeLayer(PirateCaptainModel.LAYER_LOCATION)),
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
        return PirateCaptainRenderState()
    }

    override fun getRenderType(
        state: PazZombieRenderState,
        isBodyVisible: Boolean,
        forceTransparent: Boolean,
        appearGlowing: Boolean
    ): RenderType? {
        val pirateState = state as PirateCaptainRenderState
        return super.getRenderType(state, isBodyVisible, pirateState.isGhost, appearGlowing)
    }

    override fun extractRenderState(entity: PazZombie, state: PazZombieRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        (state as PirateCaptainRenderState)
        when (entity) {
            is PirateCaptain -> {

            }
            is PirateCaptainGhost -> {
                state.isGhost = true
            }
        }
    }

}

class PirateCaptainRenderState: PazZombieRenderState() {
    var isGhost = false
    val idleAnimationState: AnimationState = AnimationState()
    val walkAnimationState: AnimationState = AnimationState()
}
