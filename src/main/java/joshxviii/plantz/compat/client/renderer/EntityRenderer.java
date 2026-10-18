package joshxviii.plantz.compat.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.compat.ARGB;
import joshxviii.plantz.compat.client.CameraRenderState;
import joshxviii.plantz.compat.client.state.EntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import org.jetbrains.annotations.Nullable;

/**
 * Backport of the 1.21.2+ two-phase entity renderer (extract state, then submit) on top of
 * 1.21.1's immediate {@code render(...)}. One state object is reused per renderer, like vanilla 26.1.
 * The default {@link #submit} runs 1.21.1's base render, so leashes and name tags (with NeoForge's
 * RenderNameTagEvent) keep working.
 */
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> extends net.minecraft.client.renderer.entity.EntityRenderer<T> {
    @Nullable private S reusedState;
    @Nullable private T currentEntity;
    private float currentYaw;
    private float currentPartialTick;
    @Nullable private MultiBufferSource currentBufferSource;

    protected EntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public abstract S createRenderState();

    protected final S getReusedState() {
        if (reusedState == null) reusedState = createRenderState();
        return reusedState;
    }

    public void extractRenderState(T entity, S state, float partialTicks) {
        state.entityType = entity.getType();
        state.x = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        state.y = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        state.z = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        state.isInvisible = entity.isInvisible();
        state.ageInTicks = (float) entity.tickCount + partialTicks;
        state.boundingBoxWidth = entity.getBbWidth();
        state.boundingBoxHeight = entity.getBbHeight();
        state.eyeHeight = entity.getEyeHeight();
        state.passengerOffset = null;
        if (this.entityRenderDispatcher.camera != null) {
            state.distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr(entity);
            boolean shouldShowName = state.distanceToCameraSq < 4096.0 && this.shouldShowName(entity, state.distanceToCameraSq);
            if (shouldShowName) {
                state.nameTag = this.getNameTag(entity);
                state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTicks));
            } else {
                state.nameTag = null;
            }
        }
        state.isDiscrete = entity.isDiscrete();
        state.displayFireAnimation = entity.displayFireAnimation();
        boolean appearsGlowing = Minecraft.getInstance().shouldEntityAppearGlowing(entity);
        state.outlineColor = appearsGlowing ? ARGB.opaque(entity.getTeamColor()) : 0;
        state.lightCoords = this.getPackedLightCoords(entity, partialTicks);
        state.shadowRadius = this.shadowRadius;
    }

    /** Draws leash and name tag through 1.21.1's base renderer. */
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (currentEntity != null && currentBufferSource != null) {
            super.render(currentEntity, currentYaw, currentPartialTick, poseStack, currentBufferSource, state.lightCoords);
        }
    }

    @Override
    public final void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        S state = getReusedState();
        currentEntity = entity;
        currentYaw = entityYaw;
        currentPartialTick = partialTick;
        currentBufferSource = bufferSource;
        try {
            state.compatEntity = entity;
            state.compatPartialTick = partialTick;
            extractRenderState(entity, state, partialTick);
            state.lightCoords = packedLight;
            submit(state, poseStack, new SubmitNodeCollector(bufferSource), CameraRenderState.current());
        } finally {
            state.compatEntity = null;
            currentEntity = null;
            currentBufferSource = null;
        }
    }

    protected float getShadowRadius(S state) {
        return this.shadowRadius;
    }

    @Override
    protected float getShadowRadius(T entity) {
        return reusedState != null ? getShadowRadius(reusedState) : this.shadowRadius;
    }

    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        return entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity;
    }

    @Override
    protected boolean shouldShowName(T entity) {
        return shouldShowName(entity, this.entityRenderDispatcher.distanceToSqr(entity));
    }

    protected Component getNameTag(T entity) {
        return entity.getDisplayName();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return MissingTextureAtlasSprite.getLocation();
    }
}
