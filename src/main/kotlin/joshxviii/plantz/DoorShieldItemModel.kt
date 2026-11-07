package joshxviii.plantz

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Transformation
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.client.model.BakedModelWrapper
import net.neoforged.neoforge.client.model.QuadTransformers
import net.neoforged.neoforge.client.model.data.ModelData
import org.joml.Vector3f
import java.util.concurrent.ConcurrentHashMap

/**
 * 26.1 `plantz:door_shield_model` item model (the item is the door its DOOR_SHIELD component names, both block halves
 * stacked, shown with the display transforms of `models/item/door_shield.json`) ported to 1.21.1 baked models: the door
 * shield's inventory model is wrapped ([modifyBakingResult]) with [ItemOverrides] that pick, per stack, a model holding
 * the door's block quads.
 */
object DoorShieldItemModel {
    private val DOOR_SHIELD = pazResource("door_shield")
    private val BLOCKING = ModelResourceLocation.standalone(pazResource("item/door_shield/door_shield_blocking"))

    fun registerAdditionalModels(event: ModelEvent.RegisterAdditional) {
        event.register(BLOCKING)
    }

    fun modifyBakingResult(event: ModelEvent.ModifyBakingResult) {
        val key = ModelResourceLocation.inventory(DOOR_SHIELD)
        val base = event.models[key] ?: return
        event.models[key] = DoorShieldModel(base, event.models[BLOCKING] ?: base)
    }

    private class DoorShieldModel(base: BakedModel, private val blockingBase: BakedModel) : BakedModelWrapper<BakedModel>(base) {
        private val doors = ConcurrentHashMap<Block, BakedModel>()
        private val blockingDoors = ConcurrentHashMap<Block, BakedModel>()
        private val overrides = object : ItemOverrides() {
            override fun resolve(model: BakedModel, stack: ItemStack, level: ClientLevel?, entity: LivingEntity?, seed: Int): BakedModel {
                val door = stack.get(PazComponents.DOOR_SHIELD)?.doorType?.takeIf { it is DoorBlock } ?: return model
                // 26.1: player && !using_item -> door_shield; else door_shield_blocking
                if (entity is Player && !(entity.isUsingItem && entity.useItem == stack)) {
                    return doors.computeIfAbsent(door) { DoorModel(originalModel, it.defaultBlockState()) }
                }
                return blockingDoors.computeIfAbsent(door) { DoorModel(blockingBase, it.defaultBlockState()) }
            }
        }

        override fun getOverrides(): ItemOverrides = overrides
    }

    /** The door's two halves in the door shield's display transforms. */
    private class DoorModel(base: BakedModel, lower: BlockState) : BakedModelWrapper<BakedModel>(base) {
        private val quads: List<BakedQuad> = doorQuads(lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 0f) +
            doorQuads(lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 1f)
        private val particle = Minecraft.getInstance().blockRenderer.getBlockModel(lower).getParticleIcon(ModelData.EMPTY)

        private fun doorQuads(state: BlockState, yOffset: Float): List<BakedQuad> {
            val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)
            val random = RandomSource.create(42L)
            val all = (Direction.entries.toList<Direction?>() + null).flatMap { side -> model.getQuads(state, side, random, ModelData.EMPTY, null) }
            // 26.1 layer.setLocalTransform(translate(0, 0, -8/16))
            val transform = Transformation(Vector3f(0f, yOffset, -8 / 16f), null, null, null)
            return QuadTransformers.applying(transform).process(all)
        }

        override fun getQuads(state: BlockState?, side: Direction?, rand: RandomSource): List<BakedQuad> =
            if (state == null && side == null) quads else emptyList()

        override fun getQuads(state: BlockState?, side: Direction?, rand: RandomSource, extraData: ModelData, renderType: RenderType?): List<BakedQuad> =
            if (state == null && side == null) quads else emptyList()

        override fun getParticleIcon(data: ModelData) = particle

        override fun getOverrides(): ItemOverrides = ItemOverrides.EMPTY

        override fun getRenderTypes(itemStack: ItemStack, fabulous: Boolean): List<RenderType> = listOf(Sheets.cutoutBlockSheet())

        // the wrapper hands these two back as the wrapped model, and its door would be drawn instead
        override fun getRenderPasses(itemStack: ItemStack, fabulous: Boolean): List<BakedModel> = listOf(this)

        override fun applyTransform(context: ItemDisplayContext, poseStack: PoseStack, leftHand: Boolean): BakedModel {
            originalModel.applyTransform(context, poseStack, leftHand)
            return this
        }
    }
}
