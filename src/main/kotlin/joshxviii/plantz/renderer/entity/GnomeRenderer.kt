package joshxviii.plantz.renderer.entity

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import joshxviii.plantz.GnomeArmorSet
import joshxviii.plantz.PazModels.ARMOR_LAYER_LOCATION
import joshxviii.plantz.block.GardenGnomePose
import joshxviii.plantz.entity.gnome.Gnome
import joshxviii.plantz.entity.gnome.GnomeVariant
import joshxviii.plantz.model.GnomeArmorModel
import joshxviii.plantz.model.GnomeModel
import net.minecraft.client.model.HumanoidModel.ArmPose
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import joshxviii.plantz.compat.client.renderer.MobRenderer
import joshxviii.plantz.compat.client.renderer.layers.RenderLayerParent
import joshxviii.plantz.compat.client.renderer.layers.RenderLayer
import joshxviii.plantz.compat.client.state.ArmedEntityRenderState
import joshxviii.plantz.compat.client.CameraRenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.model.SkullModelBase
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import joshxviii.plantz.compat.AnimationState
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.AbstractSkullBlock
import net.minecraft.world.level.block.SkullBlock
import net.neoforged.neoforge.client.ClientHooks
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions

class GnomeRenderer(
    context: EntityRendererProvider.Context,
    defaultModel: GnomeModel<GnomeRenderState> = GnomeModel(context.bakeLayer(GnomeModel.LAYER_LOCATION)),
    armorModels: GnomeArmorSet<GnomeArmorModel<GnomeRenderState>> = GnomeArmorSet.bake(ARMOR_LAYER_LOCATION, context.modelSet) { GnomeArmorModel(it) }
) : MobRenderer<Gnome, GnomeRenderState, GnomeModel<GnomeRenderState>>(
    context,
    defaultModel,
    0.2f
) {
    companion object {
        val CUSTOM_HEAD_TRANSFORMS: GnomeCustomHeadLayer.Transforms = GnomeCustomHeadLayer.Transforms(-0.1171875F, -0.07421875F, 1.0F)
    }

    init {
        addLayer(GnomeArmorLayer(this, armorModels))
        addLayer(GnomeCustomHeadLayer(this, context, CUSTOM_HEAD_TRANSFORMS))
        addLayer(GnomeItemInHandLayer(this, context.itemInHandRenderer))
    }

    override fun submit(
        state: GnomeRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        super.submit(state, poseStack, submitNodeCollector, camera)
    }

    override fun getTextureLocation(state: GnomeRenderState): ResourceLocation = state.variant.getTexture()

    override fun createRenderState(): GnomeRenderState {
        return GnomeRenderState()
    }

    fun getArmPose(mob: Gnome, arm: HumanoidArm): ArmPose {
        val itemHeldByArm: ItemStack = if (mob.mainArm == arm) mob.mainHandItem else mob.offhandItem
        // 26.1 spears / stab swing animations (ArmPose.SPEAR) do not exist on 1.21.1
        return if (itemHeldByArm.`is`(Items.BOW) && mob.isAggressive) ArmPose.BOW_AND_ARROW else ArmPose.EMPTY
    }

    override fun extractRenderState(entity: Gnome, state: GnomeRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        state.mainArm = entity.mainArm
        state.rightHandItemStack = if (entity.mainArm == HumanoidArm.RIGHT) entity.mainHandItem else entity.offhandItem
        state.leftHandItemStack = if (entity.mainArm == HumanoidArm.LEFT) entity.mainHandItem else entity.offhandItem
        state.attackTime = entity.getAttackAnim(partialTicks)
        state.variant = entity.variant
        state.isPassenger = entity.isPassenger
        state.isUsingItem = entity.isUsingItem
        state.headEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.HEAD)
        state.chestEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.CHEST)
        state.legsEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.LEGS)
        state.feetEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.FEET)
        state.leftArmPose = getArmPose(entity, HumanoidArm.LEFT)
        state.rightArmPose = getArmPose(entity, HumanoidArm.RIGHT)
    }

    private fun getEquipmentIfRenderable(entity: LivingEntity, slot: EquipmentSlot): ItemStack {
        val itemStack = entity.getItemBySlot(slot)
        return if (GnomeArmorLayer.shouldRender(itemStack, slot)) itemStack.copy() else ItemStack.EMPTY
    }
}

/**
 * 26.1 drew gnome armor through EquipmentLayerRenderer (equipment assets). On 1.21.1 the gnome armor
 * models are drawn directly with the ArmorItem's material layer textures (`textures/models/armor/..._layer_N.png`),
 * dye tint, trims and glint, like vanilla HumanoidArmorLayer.
 */
class GnomeArmorLayer<S: GnomeModel<GnomeRenderState>>(
    renderer: RenderLayerParent<GnomeRenderState, S>,
    private val armorModels: GnomeArmorSet<GnomeArmorModel<GnomeRenderState>>,
) : RenderLayer<GnomeRenderState, S>(renderer) {

    override fun submit(
        poseStack: PoseStack, submitNodeCollector: SubmitNodeCollector, lightCoords: Int, state: GnomeRenderState, yRot: Float, xRot: Float
    ) {
        this.renderArmorPiece(poseStack, submitNodeCollector, state.chestEquipment, EquipmentSlot.CHEST, lightCoords, state)
        this.renderArmorPiece(poseStack, submitNodeCollector, state.legsEquipment, EquipmentSlot.LEGS, lightCoords, state)
        this.renderArmorPiece(poseStack, submitNodeCollector, state.feetEquipment, EquipmentSlot.FEET, lightCoords, state)
        this.renderArmorPiece(poseStack, submitNodeCollector, state.headEquipment, EquipmentSlot.HEAD, lightCoords, state)
    }

    private fun renderArmorPiece(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        itemStack: ItemStack,
        slot: EquipmentSlot,
        lightCoords: Int,
        state: GnomeRenderState,
    ) {
        if (!shouldRender(itemStack, slot)) return
        val armorItem = itemStack.item as ArmorItem
        val entity = state.compatEntity as? LivingEntity ?: return
        val inner = this.usesInnerModel(slot)
        val armorModel = armorModels[slot]
        val material = armorItem.material

        poseStack.pushPose()
        val extensions = IClientItemExtensions.of(itemStack)
        val fallbackColor = extensions.getDefaultDyeColor(itemStack)
        material.value().layers().forEachIndexed { layerIdx, layer ->
            val color = extensions.getArmorLayerTintColor(itemStack, entity, layer, layerIdx, fallbackColor)
            if (color != 0) {
                val texture = ClientHooks.getArmorTexture(entity, itemStack, layer, inner, slot)
                submitNodeCollector.submitModel(
                    armorModel, state, poseStack, RenderType.armorCutoutNoCull(texture),
                    lightCoords, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor, null
                )
            }
        }

        itemStack.get(DataComponents.TRIM)?.let { trim ->
            val atlas = Minecraft.getInstance().modelManager.getAtlas(Sheets.ARMOR_TRIMS_SHEET)
            val sprite = atlas.getSprite(if (inner) trim.innerTexture(material) else trim.outerTexture(material))
            submitNodeCollector.submitModel(
                armorModel, state, poseStack, Sheets.armorTrimsSheet(trim.pattern().value().decal()),
                lightCoords, OverlayTexture.NO_OVERLAY, -1, sprite, state.outlineColor, null
            )
        }

        if (itemStack.hasFoil()) {
            submitNodeCollector.submitModel(
                armorModel, state, poseStack, RenderType.armorEntityGlint(),
                lightCoords, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null
            )
        }
        poseStack.popPose()
    }

    private fun usesInnerModel(slot: EquipmentSlot?): Boolean {
        return slot == EquipmentSlot.LEGS
    }

    companion object {
        fun shouldRender(stack: ItemStack, slot: EquipmentSlot): Boolean {
            val armorItem = stack.item as? ArmorItem ?: return false
            return armorItem.equipmentSlot == slot
        }
    }
}

/** 26.1 ItemInHandLayer for the gnome's (non-humanoid) model. */
class GnomeItemInHandLayer<M : GnomeModel<GnomeRenderState>>(
    renderer: RenderLayerParent<GnomeRenderState, M>,
    private val itemInHandRenderer: ItemInHandRenderer,
) : RenderLayer<GnomeRenderState, M>(renderer) {
    override fun submit(
        poseStack: PoseStack, submitNodeCollector: SubmitNodeCollector, lightCoords: Int, state: GnomeRenderState, yRot: Float, xRot: Float
    ) {
        submitArmWithItem(state, state.rightHandItemStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, submitNodeCollector, lightCoords)
        submitArmWithItem(state, state.leftHandItemStack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, submitNodeCollector, lightCoords)
    }

    private fun submitArmWithItem(
        state: GnomeRenderState, item: ItemStack, context: ItemDisplayContext, arm: HumanoidArm,
        poseStack: PoseStack, submitNodeCollector: SubmitNodeCollector, lightCoords: Int
    ) {
        if (item.isEmpty) return
        val entity = state.compatEntity as? LivingEntity ?: return
        poseStack.pushPose()
        parentModel.translateToHand(state, arm, poseStack)
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f))
        val left = arm == HumanoidArm.LEFT
        poseStack.translate((if (left) -1 else 1).toFloat() / 16.0f, 0.125f, -0.625f)
        itemInHandRenderer.renderItem(entity, item, context, left, poseStack, submitNodeCollector.bufferSource(), lightCoords)
        poseStack.popPose()
    }
}

/** 26.1 CustomHeadLayer (with Transforms) for the gnome's (non-humanoid) model. */
class GnomeCustomHeadLayer<M : GnomeModel<GnomeRenderState>>(
    renderer: RenderLayerParent<GnomeRenderState, M>,
    context: EntityRendererProvider.Context,
    private val transforms: Transforms,
) : RenderLayer<GnomeRenderState, M>(renderer) {
    @JvmRecord
    data class Transforms(val yOffset: Float, val skullYOffset: Float, val horizontalScale: Float)

    private val skullModels: Map<SkullBlock.Type, SkullModelBase> = SkullBlockRenderer.createSkullRenderers(context.modelSet)

    override fun submit(
        poseStack: PoseStack, submitNodeCollector: SubmitNodeCollector, lightCoords: Int, state: GnomeRenderState, yRot: Float, xRot: Float
    ) {
        val headItem = state.headItem
        if (headItem.isEmpty) return
        val item = headItem.item
        val skullBlock = (item as? BlockItem)?.block as? AbstractSkullBlock
        if (skullBlock == null && item is ArmorItem && item.equipmentSlot == EquipmentSlot.HEAD) return

        poseStack.pushPose()
        poseStack.scale(transforms.horizontalScale, 1.0f, transforms.horizontalScale)
        val parentModel = this.parentModel
        parentModel.root().translateAndRotate(poseStack)
        parentModel.translateToHead(poseStack)
        if (skullBlock != null) {
            poseStack.translate(0.0f, transforms.skullYOffset, 0.0f)
            poseStack.scale(1.1875f, -1.1875f, -1.1875f)
            poseStack.translate(-0.5, 0.0, -0.5)
            val type = skullBlock.type
            val skullModel = skullModels[type]
            if (skullModel != null) {
                val renderType = SkullBlockRenderer.getRenderType(type, headItem.get(DataComponents.PROFILE))
                SkullBlockRenderer.renderSkull(
                    null, 180.0f, state.wornHeadAnimationPos, poseStack, submitNodeCollector.bufferSource(), lightCoords, skullModel, renderType
                )
            }
        } else {
            poseStack.translate(0.0f, -0.25f + transforms.yOffset, 0.0f)
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f))
            poseStack.scale(0.625f, -0.625f, -0.625f)
            val minecraft = Minecraft.getInstance()
            minecraft.itemRenderer.renderStatic(
                headItem, ItemDisplayContext.HEAD, lightCoords, OverlayTexture.NO_OVERLAY,
                poseStack, submitNodeCollector.bufferSource(), minecraft.level, 0
            )
        }
        poseStack.popPose()
    }
}

class GnomeRenderState(
    val gnomePose: GardenGnomePose = GardenGnomePose.NONE
) : ArmedEntityRenderState() {
    var isPassenger: Boolean = false
    var isUsingItem: Boolean = false
    var variant: GnomeVariant = GnomeVariant.BLUE
    var headEquipment: ItemStack = ItemStack.EMPTY
    var chestEquipment: ItemStack = ItemStack.EMPTY
    var legsEquipment: ItemStack = ItemStack.EMPTY
    var feetEquipment: ItemStack = ItemStack.EMPTY
    val idleAnimationState: AnimationState = AnimationState()
    val actionAnimationState: AnimationState = AnimationState()
}
