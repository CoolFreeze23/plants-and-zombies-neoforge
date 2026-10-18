package joshxviii.plantz.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import joshxviii.plantz.compat.ARGB;
import joshxviii.plantz.mixin.LivingEntityAccessor;
import joshxviii.plantz.renderer.VanillaPaintLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Port addition: upstream's LivingEntityRendererMixin tinted every living entity (26.1 has one renderer pipeline).
 * On 1.21.1, vanilla renderers (players, vanilla mobs) still use 1.21.1's LivingEntityRenderer, so this applies the
 * same hypno / chill / frozen / butter tint to the model color there, and adds the paint overlay layer. (The other
 * extra render layers and the frozen-pose freeze from LivingEntityRendererMixin only apply to the mod's compat renderers.)
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererVanillaMixin<T extends LivingEntity, M extends EntityModel<T>> implements RenderLayerParent<T, M> {

    @Shadow
    protected abstract boolean addLayer(RenderLayer<T, M> layer);

    /** Upstream's paint overlay on vanilla renderers too (players, vanilla mobs). */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void plantz$addPaintLayer(EntityRendererProvider.Context context, M model, float shadowRadius, CallbackInfo ci) {
        this.addLayer(new VanillaPaintLayer<>(this));
    }

    @Unique
    private static final int PLANTZ_HYPNO_TINT = ARGB.opaque(0xD036FF);
    @Unique
    private static final int PLANTZ_FREEZE_TINT = ARGB.opaque(0x0098DC);
    @Unique
    private static final int PLANTZ_BUTTER_TINT = ARGB.opaque(0xdbdc8c);

    @ModifyArg(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
        index = 4
    )
    private int plantz$applyHypnoTint(int color, @Local(argsOnly = true) LivingEntity entity) {
        LivingEntityAccessor accessor = (LivingEntityAccessor) entity;
        int finalColor = color;
        if (accessor.plantz$getHypnoId()) finalColor = ARGB.multiply(finalColor, PLANTZ_HYPNO_TINT);
        if (accessor.plantz$getChillId() || accessor.plantz$getFrozenId()) finalColor = ARGB.multiply(finalColor, PLANTZ_FREEZE_TINT);
        if (accessor.plantz$getButterId()) finalColor = ARGB.multiply(finalColor, PLANTZ_BUTTER_TINT);
        return finalColor;
    }
}
