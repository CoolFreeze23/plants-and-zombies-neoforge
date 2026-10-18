package joshxviii.plantz.compat.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import joshxviii.plantz.compat.client.model.EntityModel;
import joshxviii.plantz.compat.client.renderer.SubmitNodeCollector;
import joshxviii.plantz.compat.client.state.HumanoidRenderState;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 26.1 {@code WingsLayer} (elytra). Runs 1.21.1's {@link ElytraLayer}, which keeps NeoForge's elytra hooks; it only reads
 * the baby / riding / attack properties of its parent model, so a placeholder parent model carries those.
 */
public class WingsLayer<S extends HumanoidRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    private final PropertiesModel propertiesModel = new PropertiesModel();
    private final ElytraLayer<LivingEntity, PropertiesModel> elytraLayer;

    public WingsLayer(RenderLayerParent<S, M> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.elytraLayer = new ElytraLayer<>(new net.minecraft.client.renderer.entity.RenderLayerParent<>() {
            @Override
            public PropertiesModel getModel() {
                return WingsLayer.this.propertiesModel;
            }

            @Override
            public ResourceLocation getTextureLocation(LivingEntity entity) {
                return MissingTextureAtlasSprite.getLocation();
            }
        }, modelSet);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        if (!(state.compatEntity instanceof LivingEntity entity)) return;
        this.propertiesModel.young = state.isBaby;
        this.propertiesModel.riding = state.isPassenger;
        this.propertiesModel.attackTime = state.attackTime;
        this.elytraLayer.render(poseStack, submitNodeCollector.bufferSource(), lightCoords, entity,
            state.walkAnimationPos, state.walkAnimationSpeed, state.compatPartialTick, state.ageInTicks, yRot, xRot);
    }

    private static final class PropertiesModel extends net.minecraft.client.model.EntityModel<LivingEntity> {
        @Override
        public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        }
    }
}
