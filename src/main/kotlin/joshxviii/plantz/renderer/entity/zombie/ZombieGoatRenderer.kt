package joshxviii.plantz.renderer.entity.zombie

import com.mojang.blaze3d.vertex.PoseStack
import joshxviii.plantz.entity.zombie.ZombieGoat
import joshxviii.plantz.model.zombies.ZombieGoatModel
import joshxviii.plantz.renderer.getTextureLocation
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import joshxviii.plantz.compat.client.renderer.MobRenderer
import net.minecraft.client.renderer.RenderType
import joshxviii.plantz.compat.client.CameraRenderState
import net.minecraft.resources.ResourceLocation

class ZombieGoatRenderer(
    context: EntityRendererProvider.Context,
): MobRenderer<ZombieGoat, ZombieGoatRenderState, ZombieGoatModel<ZombieGoatRenderState>>(context,
    ZombieGoatModel(context.bakeLayer(ZombieGoatModel.LAYER_LOCATION)), 0.4f) {

    override fun submit(
        state: ZombieGoatRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        super.submit(state, poseStack, submitNodeCollector, camera)
    }

    override fun extractRenderState(entity: ZombieGoat, state: ZombieGoatRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
    }

    override fun createRenderState(): ZombieGoatRenderState {
        return ZombieGoatRenderState()
    }

    override fun getRenderType(state: ZombieGoatRenderState, isBodyVisible: Boolean, forceTransparent: Boolean, appearGlowing: Boolean): RenderType? {
        return if (isBodyVisible && !forceTransparent) RenderType.entityCutout(getTextureLocation(state))
        else super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing)
    }

    override fun getTextureLocation(state: ZombieGoatRenderState): ResourceLocation {
        val suffixes = mutableListOf<String>()
        return state.getTextureLocation(PazZombieRenderState.TEXTURE_PATH, suffixes)
    }

}

class ZombieGoatRenderState: PazZombieRenderState() {
}
