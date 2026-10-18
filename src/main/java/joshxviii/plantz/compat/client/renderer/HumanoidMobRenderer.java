package joshxviii.plantz.compat.client.renderer;

import joshxviii.plantz.compat.client.model.HumanoidModel;
import joshxviii.plantz.compat.client.renderer.layers.CustomHeadLayer;
import joshxviii.plantz.compat.client.renderer.layers.ItemInHandLayer;
import joshxviii.plantz.compat.client.renderer.layers.WingsLayer;
import joshxviii.plantz.compat.client.state.ArmedEntityRenderState;
import joshxviii.plantz.compat.client.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CrossbowItem;

public abstract class HumanoidMobRenderer<T extends Mob, S extends HumanoidRenderState, M extends HumanoidModel<S>> extends AgeableMobRenderer<T, S, M> {
    public HumanoidMobRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        this(context, model, model, shadow);
    }

    public HumanoidMobRenderer(EntityRendererProvider.Context context, M model, M babyModel, float shadow) {
        this(context, model, babyModel, shadow, CustomHeadLayer.Transforms.DEFAULT);
    }

    public HumanoidMobRenderer(EntityRendererProvider.Context context, M model, M babyModel, float shadow, CustomHeadLayer.Transforms customHeadTransforms) {
        super(context, model, babyModel, shadow);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), customHeadTransforms));
        this.addLayer(new WingsLayer<>(this, context.getModelSet()));
        this.addLayer(new ItemInHandLayer<>(this));
    }

    protected net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(T mob, HumanoidArm arm) {
        return net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        extractHumanoidRenderState(entity, state, partialTicks);
        state.leftArmPose = this.getArmPose(entity, HumanoidArm.LEFT);
        state.rightArmPose = this.getArmPose(entity, HumanoidArm.RIGHT);
    }

    public static void extractHumanoidRenderState(LivingEntity entity, HumanoidRenderState state, float partialTicks) {
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, partialTicks);
        state.isCrouching = entity.isCrouching();
        state.isFallFlying = entity.isFallFlying();
        state.isVisuallySwimming = entity.isVisuallySwimming();
        state.isPassenger = entity.isPassenger();
        state.speedValue = 1.0F;
        if (state.isFallFlying) {
            state.speedValue = (float) entity.getDeltaMovement().lengthSqr();
            state.speedValue /= 0.2F;
            state.speedValue = state.speedValue * state.speedValue * state.speedValue;
        }
        if (state.speedValue < 1.0F) {
            state.speedValue = 1.0F;
        }
        state.swimAmount = entity.getSwimAmount(partialTicks);
        HumanoidArm mainArm = entity.getMainArm();
        state.attackArm = entity.swingingArm != InteractionHand.OFF_HAND ? mainArm : mainArm.getOpposite();
        state.useItemHand = entity.getUsedItemHand();
        state.maxCrossbowChargeDuration = (float) CrossbowItem.getChargeDuration(entity.getUseItem(), entity);
        state.ticksUsingItem = entity.getTicksUsingItem() + (entity.isUsingItem() ? partialTicks : 0.0F);
        state.isUsingItem = entity.isUsingItem();
        // all equipment, not only armor (26.1 kept equippables with an asset, e.g. the ducky tube, which is not an ArmorItem here)
        state.headEquipment = entity.getItemBySlot(EquipmentSlot.HEAD);
        state.chestEquipment = entity.getItemBySlot(EquipmentSlot.CHEST);
        state.legsEquipment = entity.getItemBySlot(EquipmentSlot.LEGS);
        state.feetEquipment = entity.getItemBySlot(EquipmentSlot.FEET);
    }
}
