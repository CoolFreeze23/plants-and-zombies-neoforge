package joshxviii.plantz.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ModelEvent;

import static joshxviii.plantz.UtilsKt.pazResource;

/**
 * The bucket worn as a helmet. Upstream gives the vanilla bucket item a "head" model (26.1 item definition), which any
 * resource pack that retextures the bucket replaces; here the head layers draw the mod's own
 * {@code plantz:item/bucket_head} model instead, so the helmet survives such packs and the bucket keeps their look elsewhere.
 */
public final class BucketHelmet {
    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(pazResource("item/bucket_head"));

    private BucketHelmet() {
    }

    public static void registerModel(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    /** Draws a worn bucket at the head (already translated to the head); false for any other item. */
    public static boolean render(ItemStack headItem, PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (!headItem.is(Items.BUCKET)) return false;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().render(headItem, ItemDisplayContext.HEAD, false, poseStack, buffer, light, OverlayTexture.NO_OVERLAY,
            minecraft.getModelManager().getModel(MODEL));
        return true;
    }
}
