package joshxviii.plantz.item.model

import joshxviii.plantz.pazResource
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Transformation
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.client.model.BakedModelWrapper
import net.neoforged.neoforge.client.model.QuadTransformers
import net.neoforged.neoforge.client.model.data.ModelData
import net.minecraft.client.renderer.RenderType
import org.joml.Matrix4f
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * 26.1 `plantz:plant_icon` item model (a composite layer showing the stored plant's icon) ported to
 * 1.21.1 baked models:
 * - every `models/item/plant_icon/<entity>.json` is baked as a standalone model ([registerAdditionalModels]);
 * - the seed packet and gardening glove inventory models are wrapped ([modifyBakingResult]) with
 *   [ItemOverrides] that pick, per stack, a composite of the base layers plus the stack's plant icon,
 *   drawn as NeoForge render passes. Each layer keeps its own display transform, like 26.1 composites.
 *
 * Mirrors the 26.1 item definitions `items/seed_packet.json` and `items/gardening_glove.json`:
 * - seed_packet: composite(item/seed_packet, plant_icon)
 * - gardening_glove: third person -> item/gardening_glove/thirdperson; else with entity data ->
 *   composite(grab, plant_icon, grab_overlay); else -> item/gardening_glove/open. The glove's own model
 *   (`models/item/gardening_glove.json`, NeoForge separate transforms) covers the open / third-person cases.
 */
object PlantIconItemModel {
    private val SEED_PACKET = pazResource("seed_packet")
    private val GARDENING_GLOVE = pazResource("gardening_glove")
    private val GLOVE_GRAB = pazResource("item/gardening_glove/grab")
    private val GLOVE_GRAB_OVERLAY = pazResource("item/gardening_glove/grab_overlay")
    private val GLOVE_THIRDPERSON = pazResource("item/gardening_glove/thirdperson")

    fun plantEntityTypes(): Iterable<EntityType<*>> {
        val reg = BuiltInRegistries.ENTITY_TYPE
        val resourceManager = Minecraft.getInstance().resourceManager
        return reg.mapNotNull { type ->
            val overlayId = overlayIdFor(type)
            val modelLocation = ResourceLocation.fromNamespaceAndPath(overlayId.namespace, "models/${overlayId.path}.json")
            if (resourceManager.getResource(modelLocation).isPresent) type else null
        }
    }

    fun overlayIdFor(type: EntityType<*>): ResourceLocation {
        val id = BuiltInRegistries.ENTITY_TYPE.getKey(type)
        return ResourceLocation.fromNamespaceAndPath(id.namespace, "item/plant_icon/${id.path}")
    }

    private fun standalone(id: ResourceLocation) = ModelResourceLocation.standalone(id)

    /** `ModelEvent.RegisterAdditional`: bake the plant icons and the glove layers as standalone models. */
    fun registerAdditionalModels(event: ModelEvent.RegisterAdditional) {
        for (type in plantEntityTypes()) event.register(standalone(overlayIdFor(type)))
        for (id in listOf(GLOVE_GRAB, GLOVE_GRAB_OVERLAY, GLOVE_THIRDPERSON)) event.register(standalone(id))
    }

    /** `ModelEvent.ModifyBakingResult`: wrap the seed packet and gardening glove item models. */
    fun modifyBakingResult(event: ModelEvent.ModifyBakingResult) {
        val models = event.models
        val icons = HashMap<EntityType<*>, BakedModel>()
        for (type in plantEntityTypes()) models[standalone(overlayIdFor(type))]?.let { icons[type] = it }

        val seedKey = ModelResourceLocation.inventory(SEED_PACKET)
        models[seedKey]?.let { base ->
            models[seedKey] = IconHolderModel(base, icons, layers = { icon -> listOf(base, icon) })
        }

        val gloveKey = ModelResourceLocation.inventory(GARDENING_GLOVE)
        val gloveBase = models[gloveKey]
        val grab = models[standalone(GLOVE_GRAB)]
        val grabOverlay = models[standalone(GLOVE_GRAB_OVERLAY)]
        val thirdPerson = models[standalone(GLOVE_THIRDPERSON)]
        if (gloveBase != null && grab != null && grabOverlay != null && thirdPerson != null) {
            models[gloveKey] = IconHolderModel(
                gloveBase, icons,
                layers = { icon -> listOf(grab, icon, grabOverlay) },
                thirdPerson = thirdPerson,
                iconlessLayers = listOf(grab, grabOverlay),
            )
        }
    }

    private fun plantTypeOf(stack: ItemStack): EntityType<*>? {
        val data = stack.get(DataComponents.ENTITY_DATA) ?: return null
        @Suppress("DEPRECATION")
        return EntityType.by(data.unsafe).orElse(null)
    }

    /**
     * The item's top-level model. Without a plant in the stack it renders the original model; with one,
     * a [CompositeModel] of [layers].
     */
    private class IconHolderModel(
        base: BakedModel,
        private val icons: Map<EntityType<*>, BakedModel>,
        private val layers: (icon: BakedModel) -> List<BakedModel>,
        private val thirdPerson: BakedModel? = null,
        iconlessLayers: List<BakedModel>? = null,
    ) : BakedModelWrapper<BakedModel>(base) {
        private val composites = ConcurrentHashMap<EntityType<*>, CompositeModel>()
        private val iconless: CompositeModel? = iconlessLayers?.let { CompositeModel(it, thirdPerson) }

        private val overrides = object : ItemOverrides() {
            override fun resolve(model: BakedModel, stack: ItemStack, level: ClientLevel?, entity: LivingEntity?, seed: Int): BakedModel? {
                val type = plantTypeOf(stack)
                val icon = if (type != null) icons[type] else null
                if (type == null || icon == null) {
                    // 26.1 glove: has_component(entity_data) picks the grab layers even without a known icon
                    if (stack.has(DataComponents.ENTITY_DATA) && iconless != null) return iconless
                    return originalModel
                }
                return composites.computeIfAbsent(type) { CompositeModel(layers(icon), thirdPerson) }
            }
        }

        override fun getOverrides(): ItemOverrides = overrides
    }

    private fun isThirdPerson(context: ItemDisplayContext) =
        context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND

    /**
     * Draws [layers] as render passes. The first layer's display transform is applied by the item renderer;
     * the other layers' quads are re-transformed so each ends up with its own display transform.
     */
    private class CompositeModel(
        private val layers: List<BakedModel>,
        private val thirdPerson: BakedModel?,
    ) : BakedModelWrapper<BakedModel>(layers.first()) {
        private val variants = EnumMap<ItemDisplayContext, Array<PassModel?>>(ItemDisplayContext::class.java)

        override fun applyTransform(cameraTransformType: ItemDisplayContext, poseStack: PoseStack, applyLeftHandTransform: Boolean): BakedModel {
            if (thirdPerson != null && isThirdPerson(cameraTransformType)) {
                return thirdPerson.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform)
            }
            originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform)
            return variant(cameraTransformType, applyLeftHandTransform)
        }

        @Synchronized
        private fun variant(context: ItemDisplayContext, leftHand: Boolean): PassModel {
            val slots = variants.getOrPut(context) { arrayOfNulls(2) }
            val index = if (leftHand) 1 else 0
            slots[index]?.let { return it }
            val baseMatrix = displayMatrix(originalModel, context, leftHand)
            val passes = layers.mapIndexed { i, layer ->
                if (i == 0) layer else correctedLayer(layer, baseMatrix, displayMatrix(layer, context, leftHand))
            }
            return PassModel(originalModel, passes).also { slots[index] = it }
        }

        override fun getRenderPasses(itemStack: ItemStack, fabulous: Boolean): List<BakedModel> = layers
    }

    private class PassModel(base: BakedModel, private val passes: List<BakedModel>) : BakedModelWrapper<BakedModel>(base) {
        override fun getRenderPasses(itemStack: ItemStack, fabulous: Boolean): List<BakedModel> = passes
        override fun applyTransform(cameraTransformType: ItemDisplayContext, poseStack: PoseStack, applyLeftHandTransform: Boolean): BakedModel = this
    }

    private fun displayMatrix(model: BakedModel, context: ItemDisplayContext, leftHand: Boolean): Matrix4f {
        val poseStack = PoseStack()
        model.applyTransform(context, poseStack, leftHand)
        return Matrix4f(poseStack.last().pose())
    }

    private fun correctedLayer(layer: BakedModel, baseMatrix: Matrix4f, layerMatrix: Matrix4f): BakedModel {
        if (baseMatrix.equals(layerMatrix, 1.0e-6f)) return layer
        if (abs(baseMatrix.determinant()) < 1.0e-8f) return layer
        // the renderer draws  base * T(-0.5) * q ; we want  layer * T(-0.5) * p  =>  q = T(0.5) * base^-1 * layer * T(-0.5) * p
        val correction = Matrix4f().translation(0.5f, 0.5f, 0.5f)
            .mul(Matrix4f(baseMatrix).invert())
            .mul(layerMatrix)
            .translate(-0.5f, -0.5f, -0.5f)
        return TransformedQuadsModel(layer, Transformation(correction))
    }

    private class TransformedQuadsModel(base: BakedModel, transformation: Transformation) : BakedModelWrapper<BakedModel>(base) {
        private val transformer = QuadTransformers.applying(transformation)
        private val cache = ConcurrentHashMap<Any, List<BakedQuad>>()

        private fun transformed(side: Direction?, quads: () -> List<BakedQuad>): List<BakedQuad> =
            cache.computeIfAbsent(side ?: NO_SIDE) { transformer.process(quads()) }

        override fun getQuads(state: BlockState?, side: Direction?, rand: RandomSource): List<BakedQuad> =
            if (state != null) super.getQuads(state, side, rand) else transformed(side) { originalModel.getQuads(null, side, rand, ModelData.EMPTY, null) }

        override fun getQuads(state: BlockState?, side: Direction?, rand: RandomSource, extraData: ModelData, renderType: RenderType?): List<BakedQuad> =
            if (state != null || renderType != null) super.getQuads(state, side, rand, extraData, renderType)
            else transformed(side) { originalModel.getQuads(null, side, rand, extraData, null) }

        override fun applyTransform(cameraTransformType: ItemDisplayContext, poseStack: PoseStack, applyLeftHandTransform: Boolean): BakedModel = this
    }

    private val NO_SIDE = Any()
}
