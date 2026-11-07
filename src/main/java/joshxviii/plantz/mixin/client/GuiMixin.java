package joshxviii.plantz.mixin.client;

import joshxviii.plantz.PazEffects;
import joshxviii.plantz.PazModels;
import joshxviii.plantz.gui.GuiUtilsKt;
import joshxviii.plantz.effect.PaintedMobEffect;
import joshxviii.plantz.gui.SunInfoHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import joshxviii.plantz.compat.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Josh
 * Port: 26.1 extractCameraOverlays/extractTextureOverlay(GuiGraphicsExtractor, ...) are 1.21.1
 * renderCameraOverlays/renderTextureOverlay(GuiGraphics, ...) (the latter private).
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow
    private void renderTextureOverlay(GuiGraphics graphics, ResourceLocation texture, float alpha) {
        throw new AssertionError();
    }

    @Unique
    private final ResourceLocation FREEZE_OUTLINE_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private ItemStack lastToolHighlight;

    @Shadow
    private int toolHighlightTimer;

    // NeoForge draws the hotbar as its own GUI layer and never calls renderHotbarAndDecorations
    @Inject(method = "renderItemHotbar", at = @At("HEAD"))
    public void displayStoredSun(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SunInfoHud.INSTANCE.extractRenderState(graphics, deltaTracker, this.lastToolHighlight, this.toolHighlightTimer);
    }

    @Inject(method = "renderCameraOverlays(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"))
    public void extractCameraOverlays(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) return;
        var effects = PaintedMobEffect.getPaintEffects(player, null);
        effects.forEach( it -> {
            var effect = it.getEffect().value();
            if (effect instanceof PaintedMobEffect paintedMobEffect) {
                extractPaintOverlay(graphics, paintedMobEffect.getRandomness(), paintedMobEffect.getPaintColor(), it.getAmplifier(), (it.getDuration()/80f));
            }
        });
        if (player.hasEffect(PazEffects.CHILLED)) renderTextureOverlay(graphics, FREEZE_OUTLINE_LOCATION, 0.15f);
        if (player.hasEffect(PazEffects.FROZEN)) renderTextureOverlay(graphics, FREEZE_OUTLINE_LOCATION, 1.0f);
    }

    @Unique
    private void extractPaintOverlay(final GuiGraphics graphics, RandomSource random, int color, int amplifier, float alpha) {
        float srcWidth = Math.min(graphics.guiWidth(), graphics.guiHeight());

        for (int i = 0; i < amplifier+1; i++) {
            float scale = 0.2f * (random.nextFloat() + 1.5f);
            float ratio = Math.min(graphics.guiWidth() / srcWidth, graphics.guiHeight() / srcWidth) * scale;
            int width = Mth.floor(srcWidth * ratio * .5);
            int height = Mth.floor(srcWidth * ratio * .5);
            int x = random.nextInt(graphics.guiWidth() - width);
            int y = random.nextInt(graphics.guiHeight() - height);
            GuiUtilsKt.pazBlit(graphics, PazModels.INSTANCE.getOverlayTexture(random.nextFloat()), x, y, 0.0F, 0.0F, width, height, width, height, ARGB.multiplyAlpha(ARGB.opaque(color), alpha));
        }

        //graphics.fill(RenderPipelines.GUI, 0, bottom, graphics.guiWidth(), graphics.guiHeight(), -16777216);
        //graphics.fill(RenderPipelines.GUI, 0, 0, graphics.guiWidth(), top, -16777216);
        //graphics.fill(RenderPipelines.GUI, 0, top, left, bottom, -16777216);
        //graphics.fill(RenderPipelines.GUI, right, top, graphics.guiWidth(), bottom, -16777216);
    }
}
