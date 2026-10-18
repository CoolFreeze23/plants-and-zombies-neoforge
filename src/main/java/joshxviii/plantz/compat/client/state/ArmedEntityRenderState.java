package joshxviii.plantz.compat.client.state;

import joshxviii.plantz.compat.KineticWeapon;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ArmedEntityRenderState extends LivingEntityRenderState {
    /** 26.1 ItemTags.SPEARS; on 1.21.1 spears come from a backport mod (e.g. Backported Spears). */
    public static final TagKey<Item> SPEARS = TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("spears"));

    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public HumanoidArm attackArm = HumanoidArm.RIGHT;
    public HumanoidModel.ArmPose rightArmPose = HumanoidModel.ArmPose.EMPTY;
    public ItemStack rightHandItemStack = ItemStack.EMPTY;
    public HumanoidModel.ArmPose leftArmPose = HumanoidModel.ArmPose.EMPTY;
    public ItemStack leftHandItemStack = ItemStack.EMPTY;
    public float attackTime;
    /** 26.1 ArmPose.SPEAR, which 1.21.1's ArmPose doesn't have: the arm holds an item in {@link #SPEARS}. */
    public boolean rightArmSpear;
    public boolean leftArmSpear;
    /** 26.1 {@code swingAnimationType == SwingAnimationType.STAB} (the attack arm holds a stabbing item). */
    public boolean stabSwing;
    /** 26.1 LivingEntityRenderState field (spear hit feedback). */
    public float ticksSinceKineticHitFeedback = Float.MAX_VALUE;

    public ItemStack getMainHandItemStack() {
        return this.mainArm == HumanoidArm.RIGHT ? this.rightHandItemStack : this.leftHandItemStack;
    }

    /** 26.1 {@code ticksUsingItem(arm)}: 0 unless the render state knows about item use (HumanoidRenderState). */
    public float ticksUsingItem(HumanoidArm arm) {
        return 0.0F;
    }

    public static ItemStack itemHeldByArm(LivingEntity entity, HumanoidArm arm) {
        return entity.getItemInHand(entity.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public static void extractArmedEntityRenderState(LivingEntity entity, ArmedEntityRenderState state, float partialTicks) {
        state.mainArm = entity.getMainArm();
        state.rightHandItemStack = itemHeldByArm(entity, HumanoidArm.RIGHT);
        state.leftHandItemStack = itemHeldByArm(entity, HumanoidArm.LEFT);
        state.attackTime = entity.getAttackAnim(partialTicks);
        HumanoidArm attackArm = entity.swingingArm != InteractionHand.OFF_HAND ? state.mainArm : state.mainArm.getOpposite();
        state.stabSwing = KineticWeapon.isStab(itemHeldByArm(entity, attackArm));
        state.ticksSinceKineticHitFeedback = KineticWeapon.ticksSinceLastKineticHitFeedback(entity, partialTicks);
        state.rightArmSpear = state.rightHandItemStack.is(SPEARS);
        state.leftArmSpear = state.leftHandItemStack.is(SPEARS);
    }
}
