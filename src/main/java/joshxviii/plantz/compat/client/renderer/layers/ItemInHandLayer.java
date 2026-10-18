package joshxviii.plantz.compat.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import joshxviii.plantz.compat.client.model.ArmedModel;
import joshxviii.plantz.compat.client.model.EntityModel;
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector;
import joshxviii.plantz.compat.client.state.ArmedEntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 26.1 {@code ItemInHandLayer} (without the spear animations, which 1.21.1 does not have). */
public class ItemInHandLayer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel<S>> extends RenderLayer<S, M> {
    public ItemInHandLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        this.submitArmWithItem(state, state.rightHandItemStack, HumanoidArm.RIGHT, poseStack, submitNodeCollector, lightCoords);
        this.submitArmWithItem(state, state.leftHandItemStack, HumanoidArm.LEFT, poseStack, submitNodeCollector, lightCoords);
    }

    protected void submitArmWithItem(S state, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        if (itemStack.isEmpty() || !(state.compatEntity instanceof LivingEntity entity)) return;
        poseStack.pushPose();
        this.getParentModel().translateToHand(state, arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        boolean isLeftHand = arm == HumanoidArm.LEFT;
        float offsetX = this.useBabyOffset(state) ? 0.0F : 1.0F;
        float offsetY = this.useBabyOffset(state) ? 1.0F : 2.0F;
        float offsetZ = this.useBabyOffset(state) ? -4.5F : -10.0F;
        poseStack.translate((float) (isLeftHand ? -1 : 1) * offsetX / 16.0F, offsetY / 16.0F, offsetZ / 16.0F);
        ItemDisplayContext context = isLeftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
            .renderItem(entity, itemStack, context, isLeftHand, poseStack, submitNodeCollector.bufferSource(), lightCoords);
        poseStack.popPose();
    }

    private boolean useBabyOffset(S state) {
        return state.isBaby && state.entityType != EntityType.ARMOR_STAND;
    }
}
