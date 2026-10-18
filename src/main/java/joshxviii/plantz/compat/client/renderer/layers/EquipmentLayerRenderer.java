package joshxviii.plantz.compat.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.client.model.Model;
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector;
import joshxviii.plantz.compat.client.state.EntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;

/**
 * 26.1 {@code EquipmentLayerRenderer.renderLayers} for armor, on 1.21.1 armor materials: each material layer texture
 * (with NeoForge's texture and tint hooks), then the trim and the glint, like 1.21.1's {@code HumanoidArmorLayer}.
 */
public final class EquipmentLayerRenderer {
    /** The 26.1 {@code EquipmentClientInfo.LayerType}s used for armor. */
    public enum LayerType {
        HUMANOID,
        HUMANOID_LEGGINGS,
        HUMANOID_BABY
    }

    private EquipmentLayerRenderer() {
    }

    public static <S extends EntityRenderState> void renderLayers(
        LayerType layerType, Model<? super S> model, S state, ItemStack itemStack, PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector, int lightCoords, int outlineColor
    ) {
        if (!(itemStack.getItem() instanceof ArmorItem armorItem) || !(state.compatEntity instanceof LivingEntity entity)) return;
        EquipmentSlot slot = armorItem.getEquipmentSlot();
        boolean innerTexture = slot == EquipmentSlot.LEGS;
        Holder<ArmorMaterial> material = armorItem.getMaterial();

        IClientItemExtensions extensions = IClientItemExtensions.of(itemStack);
        int fallbackColor = extensions.getDefaultDyeColor(itemStack);
        List<ArmorMaterial.Layer> layers = material.value().layers();
        for (int layerIdx = 0; layerIdx < layers.size(); layerIdx++) {
            ArmorMaterial.Layer layer = layers.get(layerIdx);
            int color = extensions.getArmorLayerTintColor(itemStack, entity, layer, layerIdx, fallbackColor);
            if (color != 0) {
                ResourceLocation texture = ClientHooks.getArmorTexture(entity, itemStack, layer, innerTexture, slot);
                submitNodeCollector.submitModel(model, state, poseStack, RenderType.armorCutoutNoCull(texture), lightCoords,
                    OverlayTexture.NO_OVERLAY, color, null, outlineColor, null);
            }
        }

        ArmorTrim trim = itemStack.get(DataComponents.TRIM);
        if (trim != null && layerType != LayerType.HUMANOID_BABY) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET)
                .getSprite(innerTexture ? trim.innerTexture(material) : trim.outerTexture(material));
            submitNodeCollector.submitModel(model, state, poseStack, Sheets.armorTrimsSheet(trim.pattern().value().decal()), lightCoords,
                OverlayTexture.NO_OVERLAY, -1, sprite, outlineColor, null);
        }

        if (itemStack.hasFoil()) {
            submitNodeCollector.submitModel(model, state, poseStack, RenderType.armorEntityGlint(), lightCoords,
                OverlayTexture.NO_OVERLAY, -1, null, outlineColor, null);
        }
    }
}
