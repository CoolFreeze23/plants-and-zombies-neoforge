package joshxviii.plantz.compat.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.client.state.EntityRenderState;
import net.minecraft.world.entity.HumanoidArm;

/** 26.1 state-driven {@code ArmedModel}. */
public interface ArmedModel<T extends EntityRenderState> {
    void translateToHand(T state, HumanoidArm arm, PoseStack poseStack);
}
