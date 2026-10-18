package joshxviii.plantz.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

/**
 * Backport shim of the 1.21.6+ {@code ValueInput} API, reading from a {@link CompoundTag}.
 */
public final class ValueInput {
    private final CompoundTag tag;
    private final DynamicOps<Tag> ops;
    @Nullable private final HolderLookup.Provider registries;

    private ValueInput(CompoundTag tag, DynamicOps<Tag> ops, @Nullable HolderLookup.Provider registries) {
        this.tag = tag;
        this.ops = ops;
        this.registries = registries;
    }

    public static ValueInput of(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        return new ValueInput(tag, registries == null ? NbtOps.INSTANCE : RegistryOps.create(NbtOps.INSTANCE, registries), registries);
    }

    public CompoundTag tag() { return tag; }

    @Nullable public HolderLookup.Provider lookup() { return registries; }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        Tag value = tag.get(key);
        if (value == null) return Optional.empty();
        return codec.parse(ops, value).result();
    }

    public boolean contains(String key) { return tag.contains(key); }

    public Set<String> keySet() { return tag.getAllKeys(); }

    public Optional<Integer> getInt(String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? Optional.of(tag.getInt(key)) : Optional.empty();
    }

    public Optional<Long> getLong(String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? Optional.of(tag.getLong(key)) : Optional.empty();
    }

    public Optional<String> getString(String key) {
        return tag.contains(key, Tag.TAG_STRING) ? Optional.of(tag.getString(key)) : Optional.empty();
    }

    public Optional<int[]> getIntArray(String key) {
        return tag.contains(key, Tag.TAG_INT_ARRAY) ? Optional.of(tag.getIntArray(key)) : Optional.empty();
    }

    public boolean getBooleanOr(String key, boolean fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback; }
    public byte getByteOr(String key, byte fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getByte(key) : fallback; }
    public short getShortOr(String key, short fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getShort(key) : fallback; }
    public int getIntOr(String key, int fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback; }
    public long getLongOr(String key, long fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : fallback; }
    public float getFloatOr(String key, float fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : fallback; }
    public double getDoubleOr(String key, double fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(key) : fallback; }
    public String getStringOr(String key, String fallback) { return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback; }

    public Optional<ValueInput> child(String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? Optional.of(new ValueInput(tag.getCompound(key), ops, registries)) : Optional.empty();
    }

    public ValueInput childOrEmpty(String key) {
        return child(key).orElseGet(() -> new ValueInput(new CompoundTag(), ops, registries));
    }
}
