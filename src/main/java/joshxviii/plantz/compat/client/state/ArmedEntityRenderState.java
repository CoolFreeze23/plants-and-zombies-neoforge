package joshxviii.plantz.compat.client.state;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ArmedEntityRenderState extends LivingEntityRenderState {
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public HumanoidArm attackArm = HumanoidArm.RIGHT;
    public HumanoidModel.ArmPose rightArmPose = HumanoidModel.ArmPose.EMPTY;
    public ItemStack rightHandItemStack = ItemStack.EMPTY;
    public HumanoidModel.ArmPose leftArmPose = HumanoidModel.ArmPose.EMPTY;
    public ItemStack leftHandItemStack = ItemStack.EMPTY;
    public float attackTime;

    public ItemStack getMainHandItemStack() {
        return this.mainArm == HumanoidArm.RIGHT ? this.rightHandItemStack : this.leftHandItemStack;
    }

    public static ItemStack itemHeldByArm(LivingEntity entity, HumanoidArm arm) {
        return entity.getItemInHand(entity.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public static void extractArmedEntityRenderState(LivingEntity entity, ArmedEntityRenderState state, float partialTicks) {
        state.mainArm = entity.getMainArm();
        state.rightHandItemStack = itemHeldByArm(entity, HumanoidArm.RIGHT);
        state.leftHandItemStack = itemHeldByArm(entity, HumanoidArm.LEFT);
        state.attackTime = entity.getAttackAnim(partialTicks);
    }
}
