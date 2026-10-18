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
            AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, state.isAggressive, state.attackTime, state.ageInTicks);
        }
    }
}
