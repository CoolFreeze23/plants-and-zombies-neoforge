package joshxviii.plantz.compat.client.state;

import joshxviii.plantz.compat.client.RenderStateDataKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/** Backport of the 1.21.2+ entity render state, filled by {@code compat.client.renderer.EntityRenderer}. */
public class EntityRenderState {
    public static final int NO_OUTLINE = 0;
    public EntityType<?> entityType;
    public double x;
    public double y;
    public double z;
    public float ageInTicks;
    public float boundingBoxWidth;
    public float boundingBoxHeight;
    public float eyeHeight;
    public double distanceToCameraSq;
    public boolean isInvisible;
    public boolean isDiscrete;
    public boolean displayFireAnimation;
    public int lightCoords = 15728880;
    public int outlineColor = 0;
    @Nullable public Vec3 passengerOffset;
    @Nullable public Component nameTag;
    @Nullable public Component scoreText;
    @Nullable public Vec3 nameTagAttachment;
    public float shadowRadius;

    /** The entity this state was extracted from during the current frame (immediate-mode rendering only). */
    @Nullable public transient Entity compatEntity;
    public float compatPartialTick;

    private final Map<RenderStateDataKey<?>, Object> data = new IdentityHashMap<>();

    public boolean appearsGlowing() {
        return this.outlineColor != 0;
    }

    public <T> void setData(RenderStateDataKey<T> key, @Nullable T value) {
        if (value == null) data.remove(key); else data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getData(RenderStateDataKey<T> key) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getDataOrDefault(RenderStateDataKey<T> key, T fallback) {
        Object value = data.get(key);
        return value == null ? fallback : (T) value;
    }

    public void clearExtraData() {
        data.clear();
    }
}
