package joshxviii.plantz.compat.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * 26.1 model element rotations (CuboidRotation) on 1.21.1, whose block model format only allows a single axis at
 * -45/-22.5/0/22.5/45 degrees. 26.1 also allows any angle and x/y/z (Euler) rotations, which the upstream models use.
 * {@code BlockElementDeserializerMixin} reads them into a 1.21.1 {@code BlockElementRotation} carrying this transform
 * ({@link Holder}), and {@code FaceBakeryMixin} applies it the 26.1 way.
 */
public final class CuboidRotation {
    private CuboidRotation() {
    }

    /** Implemented on 1.21.1's BlockElementRotation by mixin. */
    public interface Holder {
        @Nullable
        Matrix4fc plantz$transform();

        void plantz$setTransform(Matrix4fc transform);
    }

    /** True when 1.21.1 can't read this rotation itself (Euler x/y/z, or an angle off the 22.5 degree steps). */
    public static boolean needsCompat(JsonObject rotation) {
        if (!rotation.has("axis") && !rotation.has("angle")) return true;
        float angle = GsonHelper.getAsFloat(rotation, "angle", 0.0F);
        float abs = Math.abs(angle);
        return angle != 0.0F && abs != 22.5F && abs != 45.0F;
    }

    /** 26.1 CuboidModelElement.Deserializer#getRotation + CuboidRotation#computeTransform. */
    public static Matrix4f readTransform(JsonObject rotation) {
        Matrix4f result;
        if (!rotation.has("axis") && !rotation.has("angle")) {
            if (!rotation.has("x") && !rotation.has("y") && !rotation.has("z")) {
                throw new JsonParseException("Missing rotation value, expected either 'axis' and 'angle' or 'x', 'y' and 'z'");
            }
            float x = GsonHelper.getAsFloat(rotation, "x", 0.0F);
            float y = GsonHelper.getAsFloat(rotation, "y", 0.0F);
            float z = GsonHelper.getAsFloat(rotation, "z", 0.0F);
            result = new Matrix4f().rotationZYX(z * (float) (Math.PI / 180.0), y * (float) (Math.PI / 180.0), x * (float) (Math.PI / 180.0));
        } else {
            Direction.Axis axis = axis(rotation);
            float angle = GsonHelper.getAsFloat(rotation, "angle");
            result = new Matrix4f();
            if (angle != 0.0F) result.rotation(angle * (float) (Math.PI / 180.0), Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE).step());
        }
        if (GsonHelper.getAsBoolean(rotation, "rescale", false) && !isIdentity(result)) {
            result.scale(scaleFactor(result, Direction.Axis.X), scaleFactor(result, Direction.Axis.Y), scaleFactor(result, Direction.Axis.Z));
        }
        return result;
    }

    /** The axis 1.21.1 stores alongside the transform (only used when nothing reads the transform). */
    public static Direction.Axis axisOrY(JsonObject rotation) {
        return rotation.has("axis") ? axis(rotation) : Direction.Axis.Y;
    }

    /** Moves a vertex (block units, 0..1) by the 26.1 transform around the rotation origin. */
    public static void apply(Vector3f vertex, Vector3f origin, Matrix4fc transform) {
        Vector3f local = new Vector3f(vertex).sub(origin);
        transform.transformPosition(local);
        vertex.set(local.add(origin));
    }

    private static Direction.Axis axis(JsonObject rotation) {
        String name = GsonHelper.getAsString(rotation, "axis");
        Direction.Axis axis = Direction.Axis.byName(name.toLowerCase(Locale.ROOT));
        if (axis == null) throw new JsonParseException("Invalid rotation axis: " + name);
        return axis;
    }

    private static float scaleFactor(Matrix4fc rotation, Direction.Axis axis) {
        Vector3f transformed = rotation.transformDirection(new Vector3f(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE).step()));
        float max = Math.max(Math.max(Math.abs(transformed.x), Math.abs(transformed.y)), Math.abs(transformed.z));
        return 1.0F / max;
    }

    private static boolean isIdentity(Matrix4fc matrix) {
        return (matrix.properties() & Matrix4fc.PROPERTY_IDENTITY) != 0 || matrix.equals(new Matrix4f(), 1.0E-6F);
    }
}
