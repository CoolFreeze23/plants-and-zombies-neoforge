package joshxviii.plantz.compat.client.model;

import joshxviii.plantz.compat.client.state.ZombieRenderState;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;

/** Backport of the 1.21.2+ state-driven zombie model (AbstractZombieModel + ZombieModel). */
public class ZombieModel<S extends ZombieRenderState> extends HumanoidModel<S> {
    public ZombieModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        if (!state.isBaby || state.getMainHandItemStack().isEmpty()) {
            // 26.1 skips the raised zombie arms for stabbing (spear) items and keeps their spear pose
            if (state.stabSwing) AnimationUtils.bobArms(this.rightArm, this.leftArm, state.ageInTicks);
            else AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, state.isAggressive, state.attackTime, state.ageInTicks);
        }
    }
}
