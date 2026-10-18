package joshxviii.plantz.compat.client.state;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class HumanoidRenderState extends ArmedEntityRenderState {
    public float swimAmount;
    public float speedValue = 1.0F;
    public float maxCrossbowChargeDuration;
    public float ticksUsingItem;
    public InteractionHand useItemHand = InteractionHand.MAIN_HAND;
    public boolean isCrouching;
    public boolean isFallFlying;
    public boolean isVisuallySwimming;
    public boolean isPassenger;
    public boolean isUsingItem;
    public ItemStack headEquipment = ItemStack.EMPTY;
    public ItemStack chestEquipment = ItemStack.EMPTY;
    public ItemStack legsEquipment = ItemStack.EMPTY;
    public ItemStack feetEquipment = ItemStack.EMPTY;
}
