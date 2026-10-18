package joshxviii.plantz.compat.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import joshxviii.plantz.compat.client.model.EntityModel;
import joshxviii.plantz.compat.client.model.HeadedModel;
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector;
import joshxviii.plantz.compat.client.state.LivingEntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;

import java.util.Map;

/** 26.1 {@code CustomHeadLayer}: skulls and non-armor head items (cones, buckets, blocks) drawn on the model's head. */
public class CustomHeadLayer<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> extends RenderLayer<S, M> {
    private final Transforms transforms;
    private final Map<SkullBlock.Type, SkullModelBase> skullModels;

    public CustomHeadLayer(RenderLayerParent<S, M> renderer, EntityModelSet modelSet) {
        this(renderer, modelSet, Transforms.DEFAULT);
    }

    public CustomHeadLayer(RenderLayerParent<S, M> renderer, EntityModelSet modelSet, Transforms transforms) {
        super(renderer);
        this.transforms = transforms;
        this.skullModels = SkullBlockRenderer.createSkullRenderers(modelSet);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        ItemStack headItem = state.headItem;
        if (headItem.isEmpty() || !(state.compatEntity instanceof LivingEntity entity)) return;
        AbstractSkullBlock skull = headItem.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock skullBlock ? skullBlock : null;
        if (skull == null && HumanoidArmorLayer.shouldRender(headItem, EquipmentSlot.HEAD)) return;

        poseStack.pushPose();
        poseStack.scale(this.transforms.horizontalScale(), 1.0F, this.transforms.horizontalScale());
        M parentModel = this.getParentModel();
        parentModel.root().translateAndRotate(poseStack);
        parentModel.translateToHead(poseStack);
        if (skull != null) {
            poseStack.translate(0.0F, this.transforms.skullYOffset(), 0.0F);
            poseStack.scale(1.1875F, -1.1875F, -1.1875F);
            poseStack.translate(-0.5F, 0.0F, -0.5F);
            SkullBlock.Type type = skull.getType();
            SkullModelBase skullModel = this.skullModels.get(type);
            if (skullModel != null) {
                SkullBlockRenderer.renderSkull(null, 180.0F, state.wornHeadAnimationPos, poseStack, submitNodeCollector.bufferSource(), lightCoords,
                    skullModel, SkullBlockRenderer.getRenderType(type, headItem.get(DataComponents.PROFILE)));
            }
        } else {
            translateToHead(poseStack, this.transforms);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
                .renderItem(entity, headItem, ItemDisplayContext.HEAD, false, poseStack, submitNodeCollector.bufferSource(), lightCoords);
        }
        poseStack.popPose();
    }

    public static void translateToHead(PoseStack poseStack, Transforms transforms) {
        poseStack.translate(0.0F, -0.25F + transforms.yOffset(), 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.625F, -0.625F, -0.625F);
    }

    public record Transforms(float yOffset, float skullYOffset, float horizontalScale) {
        public static final Transforms DEFAULT = new Transforms(0.0F, 0.0F, 1.0F);
    }
}
