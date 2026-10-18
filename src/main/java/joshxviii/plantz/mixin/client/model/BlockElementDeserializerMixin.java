package joshxviii.plantz.mixin.client.model;

import com.google.gson.JsonObject;
import joshxviii.plantz.compat.client.model.CuboidRotation;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reads 26.1 element rotations that 1.21.1 rejects (any angle, x/y/z rotations); everything 1.21.1 can read itself
 * goes through the vanilla code unchanged.
 */
@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockElement$Deserializer")
public class BlockElementDeserializerMixin {
    @Inject(method = "getRotation", at = @At("HEAD"), cancellable = true)
    private void plantz$readCuboidRotation(JsonObject json, CallbackInfoReturnable<BlockElementRotation> cir) {
        if (!json.has("rotation")) return;
        JsonObject rotation = GsonHelper.getAsJsonObject(json, "rotation");
        if (!CuboidRotation.needsCompat(rotation)) return;
        Vector3f origin = GsonHelper.isArrayNode(rotation, "origin") ? readOrigin(rotation) : new Vector3f(8.0F, 8.0F, 8.0F);
        BlockElementRotation result = new BlockElementRotation(
            origin.mul(0.0625F), CuboidRotation.axisOrY(rotation), 0.0F, GsonHelper.getAsBoolean(rotation, "rescale", false));
        ((CuboidRotation.Holder) (Object) result).plantz$setTransform(CuboidRotation.readTransform(rotation));
        cir.setReturnValue(result);
    }

    private static Vector3f readOrigin(JsonObject rotation) {
        var array = GsonHelper.getAsJsonArray(rotation, "origin");
        if (array.size() != 3) throw new com.google.gson.JsonParseException("Expected 3 origin values, found: " + array.size());
        return new Vector3f(GsonHelper.convertToFloat(array.get(0), "origin[0]"), GsonHelper.convertToFloat(array.get(1), "origin[1]"),
            GsonHelper.convertToFloat(array.get(2), "origin[2]"));
    }
}
