package joshxviii.plantz.renderer.entity

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import joshxviii.plantz.entity.projectile.LaserBullet
import joshxviii.plantz.entity.projectile.PaintBall
import joshxviii.plantz.renderer.getProjectileTextureLocation
import joshxviii.plantz.compat.client.model.EntityModel
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector
import joshxviii.plantz.compat.client.renderer.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import joshxviii.plantz.compat.client.renderer.layers.RenderLayerParent
import joshxviii.plantz.compat.client.renderer.layers.EyesLayer
import joshxviii.plantz.compat.client.renderer.layers.RenderLayer
import joshxviii.plantz.compat.client.state.EntityRenderState
import net.minecraft.client.renderer.RenderType
import joshxviii.plantz.compat.client.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.registries.BuiltInRegistries
import joshxviii.plantz.compat.ARGB
import net.minecraft.world.entity.projectile.Projectile

class ProjectileRenderer<M: EntityModel<ProjectileRenderState>>(
    val projectileModel: M,
    context: EntityRendererProvider.Context,
) : EntityRenderer<Projectile, ProjectileRenderState>(
    context
), RenderLayerParent<ProjectileRenderState, M> {
    private val renderLayers: MutableList<RenderLayer<ProjectileRenderState, M>> = mutableListOf()

    override fun getModel(): M {
        return projectileModel
    }

    init {
        renderLayers.add(EmissiveProjectileLayer(this))
    }

    override fun submit(
        state: ProjectileRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        poseStack.pushPose()
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0f))
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot))
        poseStack.translate(0.0, -1.5, 0.0)
        val tint = state.color?: -1
        val texture = state.getProjectileTextureLocation()
        if (texture!=null) collector.submitModel(
            this.projectileModel,
            state,
            poseStack,
            this.projectileModel.renderType(texture),
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            tint,
            null,
            state.outlineColor,
            null
        )
        model.setupAnim(state)
        for (layer in renderLayers) {
            layer.submit(poseStack, collector, state.lightCoords, state, state.yRot, state.xRot)
        }
        poseStack.popPose()

        super.submit(state, poseStack, collector, camera)
    }

    override fun createRenderState(): ProjectileRenderState {
        return ProjectileRenderState()
    }

    override fun extractRenderState(entity: Projectile, state: ProjectileRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        if (entity is PaintBall) state.color = entity.dyeColor.fireworkColor
        if (entity is LaserBullet) state.color = entity.laserColor
        state.xRot = entity.getXRot(partialTick)
        state.yRot = entity.getYRot(partialTick)
        state.texturePath = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).path
    }
}

class EmissiveProjectileLayer<M : EntityModel<ProjectileRenderState>>(
    renderer: RenderLayerParent<ProjectileRenderState, M>,
) : EyesLayer<ProjectileRenderState, M>(renderer) {

    override fun submit(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        lightCoords: Int,
        state: ProjectileRenderState,
        yRot: Float,
        xRot: Float
    ) {
        val textureLocation = state.getProjectileTextureLocation(emissive = true) ?: return
        val renderType = RenderType.eyes(textureLocation)
        val tint = state.color?.let {ARGB.opaque(it) }?: -1
        submitNodeCollector.order(1).submitModel(
            this.parentModel,
            state,
            poseStack,
            renderType,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            tint,
            null,
            state.outlineColor,
            null
        );
    }

    override fun renderType(): RenderType = RenderType.lines()
}

class ProjectileRenderState : EntityRenderState() {
    companion object {
        const val TEXTURE_PATH = "textures/entity/projectile"
    }
    var xRot: Float = 0f
    var yRot: Float = 0f
    var texturePath: String = "default"
    var color: Int? = null
}