package joshxviii.plantz.renderer

import com.mojang.blaze3d.vertex.PoseStack
import joshxviii.plantz.entity.PlantPotMinecart
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MinecartRenderer
import net.minecraft.world.level.block.state.BlockState

class PlantPotMinecartRenderer(
    context: EntityRendererProvider.Context,
    model: ModelLayerLocation,
): MinecartRenderer<PlantPotMinecart>(
    context,
    model
) {
    override fun renderMinecartContents(
        entity: PlantPotMinecart,
        partialTicks: Float,
        state: BlockState,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        poseStack.translate(-0.5 * 1/3, 0.15, -0.5 * 1/3)
        poseStack.scale(4/3f, 4/3f, 4/3f)
        super.renderMinecartContents(entity, partialTicks, state, poseStack, buffer, packedLight)
    }
}
