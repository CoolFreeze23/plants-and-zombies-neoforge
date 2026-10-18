package joshxviii.plantz.compat.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.client.model.ArmorModelSet;
import joshxviii.plantz.compat.client.model.HumanoidModel;
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector;
import joshxviii.plantz.compat.client.state.HumanoidRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** 26.1 {@code HumanoidArmorLayer}: per-slot armor models (any {@link HumanoidModel}), posed by the wearer's render state. */
public class HumanoidArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private final ArmorModelSet<A> modelSet;
    private final ArmorModelSet<A> babyModelSet;
    @Nullable
    private final ArmorModelSet<A> modernBabyModelSet;

    public HumanoidArmorLayer(RenderLayerParent<S, M> renderer, ArmorModelSet<A> modelSet) {
        this(renderer, modelSet, modelSet);
    }

    public HumanoidArmorLayer(RenderLayerParent<S, M> renderer, ArmorModelSet<A> modelSet, ArmorModelSet<A> babyModelSet) {
        this(renderer, modelSet, babyModelSet, null);
    }

    /** {@code modernBabyModelSet}: 26.1's baby armor UVs, used with 26.1's baby armor textures when they are available. */
    public HumanoidArmorLayer(RenderLayerParent<S, M> renderer, ArmorModelSet<A> modelSet, ArmorModelSet<A> babyModelSet, @Nullable ArmorModelSet<A> modernBabyModelSet) {
        super(renderer);
        this.modelSet = modelSet;
        this.babyModelSet = babyModelSet;
        this.modernBabyModelSet = modernBabyModelSet;
    }

    /** 26.1: the stack is equippable in {@code slot} with an equipment asset; 1.21.1: an {@link ArmorItem} for that slot. */
    public static boolean shouldRender(ItemStack itemStack, EquipmentSlot slot) {
        return itemStack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == slot;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        this.renderArmorPiece(poseStack, submitNodeCollector, state.chestEquipment, EquipmentSlot.CHEST, lightCoords, state);
        this.renderArmorPiece(poseStack, submitNodeCollector, state.legsEquipment, EquipmentSlot.LEGS, lightCoords, state);
        this.renderArmorPiece(poseStack, submitNodeCollector, state.feetEquipment, EquipmentSlot.FEET, lightCoords, state);
        this.renderArmorPiece(poseStack, submitNodeCollector, state.headEquipment, EquipmentSlot.HEAD, lightCoords, state);
    }

    private void renderArmorPiece(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot slot, int lightCoords, S state) {
        if (!shouldRender(itemStack, slot)) return;
        A model = this.getArmorModel(state, slot);
        EquipmentLayerRenderer.LayerType layerType = state.isBaby && state.entityType != EntityType.ARMOR_STAND
            ? EquipmentLayerRenderer.LayerType.HUMANOID_BABY
            : (this.usesInnerModel(slot) ? EquipmentLayerRenderer.LayerType.HUMANOID_LEGGINGS : EquipmentLayerRenderer.LayerType.HUMANOID);
        A modernBabyModel = layerType == EquipmentLayerRenderer.LayerType.HUMANOID_BABY && this.modernBabyModelSet != null ? this.modernBabyModelSet.get(slot) : null;
        EquipmentLayerRenderer.renderLayers(layerType, model, modernBabyModel, state, itemStack, poseStack, submitNodeCollector, lightCoords, state.outlineColor);
    }

    private A getArmorModel(S state, EquipmentSlot slot) {
        return (state.isBaby ? this.babyModelSet : this.modelSet).get(slot);
    }

    private boolean usesInnerModel(EquipmentSlot slot) {
        return slot == EquipmentSlot.LEGS;
    }
}
