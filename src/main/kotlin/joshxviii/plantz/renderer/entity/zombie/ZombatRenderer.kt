package joshxviii.plantz.renderer.entity.zombie

import com.mojang.blaze3d.vertex.PoseStack
import joshxviii.plantz.entity.zombie.Zombat
import joshxviii.plantz.model.zombies.ZombatModel
import joshxviii.plantz.renderer.getTextureLocation
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import joshxviii.plantz.compat.client.renderer.MobRenderer
import net.minecraft.client.renderer.RenderType
import joshxviii.plantz.compat.client.CameraRenderState
import net.minecraft.resources.ResourceLocation

class ZombatRenderer(
    context: EntityRendererProvider.Context,
): MobRenderer<Zombat, ZombatRenderState, ZombatModel<ZombatRenderState>>(context,
    ZombatModel(context.bakeLayer(ZombatModel.LAYER_LOCATION)), 0.25f) {

    override fun submit(
        state: ZombatRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        super.submit(state, poseStack, submitNodeCollector, camera)
    }

    override fun extractRenderState(entity: Zombat, state: ZombatRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        state.isBaronBat = entity.isBaronBat
    }

    override fun createRenderState(): ZombatRenderState {
        return ZombatRenderState()
    }

    override fun getRenderType(state: ZombatRenderState, isBodyVisible: Boolean, forceTransparent: Boolean, appearGlowing: Boolean): RenderType? {
        return if (isBodyVisible && !forceTransparent) RenderType.entityCutout(getTextureLocation(state))
        else super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing)
    }

    override fun getTextureLocation(state: ZombatRenderState): ResourceLocation {
        val suffixes = mutableListOf<String>()
        if (state.isBaronBat) suffixes.add("baron")
        return state.getTextureLocation(PazZombieRenderState.TEXTURE_PATH, suffixes)
    }

}

class ZombatRenderState: PazZombieRenderState() {
    var isBaronBat = false
}
