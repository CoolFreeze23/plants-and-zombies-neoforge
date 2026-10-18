package joshxviii.plantz.compat.client.renderer;

import joshxviii.plantz.compat.client.model.EntityModel;
import joshxviii.plantz.compat.client.state.LivingEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public abstract class MobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends LivingEntityRenderer<T, S, M> {
    public MobRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context, model, shadow);
    }

    @Override
    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        return super.shouldShowName(entity, distanceToCameraSq)
            && (entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
    }

    @Override
    public boolean shouldRender(T entity, Frustum culler, double camX, double camY, double camZ) {
        if (super.shouldRender(entity, culler, camX, camY, camZ)) {
            return true;
        }
        Entity leashHolder = entity.getLeashHolder();
        return leashHolder != null && culler.isVisible(leashHolder.getBoundingBoxForCulling());
    }
}
