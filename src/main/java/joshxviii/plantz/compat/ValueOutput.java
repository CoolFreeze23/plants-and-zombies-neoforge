package joshxviii.plantz.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

/**
 * Backport shim of the 1.21.6+ {@code ValueOutput} API, writing straight into a {@link CompoundTag}.
 * Lets the 26.1 save code run unchanged on 1.21.1's CompoundTag-based save hooks.
 */
public final class ValueOutput {
    private final CompoundTag tag;
    private final DynamicOps<Tag> ops;

    private ValueOutput(CompoundTag tag, DynamicOps<Tag> ops) {
        this.tag = tag;
        this.ops = ops;
    }

    public static ValueOutput of(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        return new ValueOutput(tag, registries == null ? NbtOps.INSTANCE : RegistryOps.create(NbtOps.INSTANCE, registries));
    }

    public CompoundTag tag() { return tag; }

    public <T> void store(String key, Codec<T> codec, T value) {
        codec.encodeStart(ops, value).ifSuccess(encoded -> tag.put(key, encoded));
    }

    public <T> void storeNullable(String key, Codec<T> codec, @Nullable T value) {
        if (value != null) store(key, codec, value);
    }

    public void putBoolean(String key, boolean value) { tag.putBoolean(key, value); }
    public void putByte(String key, byte value) { tag.putByte(key, value); }
    public void putShort(String key, short value) { tag.putShort(key, value); }
    public void putInt(String key, int value) { tag.putInt(key, value); }
    public void putLong(String key, long value) { tag.putLong(key, value); }
    public void putFloat(String key, float value) { tag.putFloat(key, value); }
    public void putDouble(String key, double value) { tag.putDouble(key, value); }
    public void putString(String key, String value) { tag.putString(key, value); }
    public void putIntArray(String key, int[] value) { tag.putIntArray(key, value); }

    public ValueOutput child(String key) {
        CompoundTag child = new CompoundTag();
        tag.put(key, child);
        return new ValueOutput(child, ops);
    }

    public void discard(String key) { tag.remove(key); }

    public boolean isEmpty() { return tag.isEmpty(); }
}
