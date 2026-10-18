package joshxviii.plantz.compat.client.state;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LivingEntityRenderState extends EntityRenderState {
    public float bodyRot;
    public float yRot;
    public float xRot;
    public float deathTime;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float scale = 1.0F;
    public float ageScale = 1.0F;
    public boolean isUpsideDown;
    public boolean isFullyFrozen;
    public boolean isBaby;
    public boolean isInWater;
    public boolean isAutoSpinAttack;
    public boolean hasRedOverlay;
    public boolean isInvisibleToPlayer;
    @Nullable public Direction bedOrientation;
    public Pose pose = Pose.STANDING;
    public ItemStack headItem = ItemStack.EMPTY;
    public float wornHeadAnimationPos;

    public boolean hasPose(Pose pose) {
        return this.pose == pose;
    }
}
