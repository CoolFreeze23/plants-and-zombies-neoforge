package joshxviii.plantz.compat.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.client.CameraRenderState;
import joshxviii.plantz.compat.client.model.EntityModel;
import joshxviii.plantz.compat.client.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;

public abstract class AgeableMobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends MobRenderer<T, S, M> {
    private final M adultModel;
    private final M babyModel;

    public AgeableMobRenderer(EntityRendererProvider.Context context, M adultModel, M babyModel, float shadow) {
        super(context, adultModel, shadow);
        this.adultModel = adultModel;
        this.babyModel = babyModel;
    }

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.model = state.isBaby ? this.babyModel : this.adultModel;
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
