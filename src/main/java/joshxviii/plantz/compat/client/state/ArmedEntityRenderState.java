package joshxviii.plantz.compat.client.state;

import net.minecraft.client.model.HumanoidModel;
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

    public static void extractArmedEntityRenderState(LivingEntity entity, ArmedEntityRenderState state, float partialTicks) {
        state.mainArm = entity.getMainArm();
        state.rightHandItemStack = entity.getItemHeldByArm(HumanoidArm.RIGHT);
        state.leftHandItemStack = entity.getItemHeldByArm(HumanoidArm.LEFT);
        state.attackTime = entity.getAttackAnim(partialTicks);
    }
}
