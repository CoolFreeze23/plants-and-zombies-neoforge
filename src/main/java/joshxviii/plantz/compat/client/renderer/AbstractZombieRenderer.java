package joshxviii.plantz.compat.client.renderer;

import joshxviii.plantz.compat.client.model.ZombieModel;
import joshxviii.plantz.compat.client.renderer.layers.VanillaHumanoidLayers;
import joshxviii.plantz.compat.client.state.ZombieRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Backport of the 1.21.2+ zombie renderer. Armor is drawn by 1.21.1's HumanoidArmorLayer through
 * {@link VanillaHumanoidLayers}, using the given inner/outer armor model layers.
 */
public abstract class AbstractZombieRenderer<T extends Zombie, S extends ZombieRenderState, M extends ZombieModel<S>> extends HumanoidMobRenderer<T, S, M> {
    private static final ResourceLocation ZOMBIE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    protected AbstractZombieRenderer(EntityRendererProvider.Context context, M model, M babyModel) {
        this(context, model, babyModel, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR);
    }

    protected AbstractZombieRenderer(EntityRendererProvider.Context context, M model, M babyModel, ModelLayerLocation innerArmor, ModelLayerLocation outerArmor) {
        super(context, model, babyModel, 0.5F);
        this.addLayer(new VanillaHumanoidLayers<>(this, context, ModelLayers.ZOMBIE).withArmor(innerArmor, outerArmor, context));
    }

    @Override
    public ResourceLocation getTextureLocation(S state) {
        return ZOMBIE_LOCATION;
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isAggressive = entity.isAggressive();
        state.isConverting = entity.isUnderWaterConverting();
    }

    @Override
    protected boolean isShaking(S state) {
        return super.isShaking(state) || state.isConverting;
    }
}
