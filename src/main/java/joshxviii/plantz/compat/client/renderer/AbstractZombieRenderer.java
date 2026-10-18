package joshxviii.plantz.compat.client.renderer;

import joshxviii.plantz.compat.client.model.ArmorModelSet;
import joshxviii.plantz.compat.client.model.ZombieModel;
import joshxviii.plantz.compat.client.renderer.layers.HumanoidArmorLayer;
import joshxviii.plantz.compat.client.state.ZombieRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import org.jetbrains.annotations.Nullable;

/** Backport of the 26.1 zombie renderer, with armor models built from the zombie's own model class. */
public abstract class AbstractZombieRenderer<T extends Zombie, S extends ZombieRenderState, M extends ZombieModel<S>> extends HumanoidMobRenderer<T, S, M> {
    private static final ResourceLocation ZOMBIE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    protected AbstractZombieRenderer(EntityRendererProvider.Context context, M model, M babyModel, ArmorModelSet<M> armorSet, ArmorModelSet<M> babyArmorSet) {
        this(context, model, babyModel, armorSet, babyArmorSet, null);
    }

    /** {@code modernBabyArmorSet}: 26.1's baby armor UVs, for 26.1 baby armor textures when they are available. */
    protected AbstractZombieRenderer(EntityRendererProvider.Context context, M model, M babyModel, ArmorModelSet<M> armorSet, ArmorModelSet<M> babyArmorSet,
                                     @Nullable ArmorModelSet<M> modernBabyArmorSet) {
        super(context, model, babyModel, 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, armorSet, babyArmorSet, modernBabyArmorSet));
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
