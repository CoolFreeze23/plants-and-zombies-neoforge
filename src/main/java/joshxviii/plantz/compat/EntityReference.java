package joshxviii.plantz.compat;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Backport of the 1.21.5+ {@code EntityReference}: a UUID that resolves to a loaded entity (cached while alive). */
public final class EntityReference<T extends Entity> {
    private final UUID uuid;
    @Nullable private T cached;

    private EntityReference(UUID uuid) {
        this.uuid = uuid;
    }

    private EntityReference(T entity) {
        this.uuid = entity.getUUID();
        this.cached = entity;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> Codec<EntityReference<T>> codec() {
        return UUIDUtil.CODEC.xmap(EntityReference::new, EntityReference::getUUID);
    }

    public static <T extends Entity> StreamCodec<ByteBuf, EntityReference<T>> streamCodec() {
        return UUIDUtil.STREAM_CODEC.map(EntityReference::new, EntityReference::getUUID);
    }

    @Nullable
    public static <T extends Entity> EntityReference<T> of(@Nullable T entity) {
        return entity == null ? null : new EntityReference<>(entity);
    }

    public static <T extends Entity> EntityReference<T> of(UUID uuid) {
        return new EntityReference<>(uuid);
    }

    public UUID getUUID() {
        return uuid;
    }

    @Nullable
    public T getEntity(Level level, Class<T> clazz) {
        if (cached != null && !cached.isRemoved()) return cached;
        Entity entity = level.getEntities().get(uuid);
        if (entity == null) entity = level.getPlayerByUUID(uuid);
        if (clazz.isInstance(entity)) {
            cached = clazz.cast(entity);
            return cached;
        }
        return null;
    }

    public boolean matches(T entity) {
        return uuid.equals(entity.getUUID());
    }

    public void store(ValueOutput output, String key) {
        output.store(key, UUIDUtil.CODEC, uuid);
    }

    public static void store(@Nullable EntityReference<?> reference, ValueOutput output, String key) {
        if (reference != null) reference.store(output, key);
    }

    @Nullable
    public static <T extends Entity> T get(@Nullable EntityReference<T> reference, Level level, Class<T> clazz) {
        return reference != null ? reference.getEntity(level, clazz) : null;
    }

    @Nullable
    public static Entity getEntity(@Nullable EntityReference<Entity> reference, Level level) {
        return get(reference, level, Entity.class);
    }

    @Nullable
    public static LivingEntity getLivingEntity(@Nullable EntityReference<LivingEntity> reference, Level level) {
        return get(reference, level, LivingEntity.class);
    }

    @Nullable
    public static Player getPlayer(@Nullable EntityReference<Player> reference, Level level) {
        return get(reference, level, Player.class);
    }

    @Nullable
    public static <T extends Entity> EntityReference<T> read(ValueInput input, String key) {
        Optional<UUID> uuid = input.read(key, UUIDUtil.CODEC);
        return uuid.<EntityReference<T>>map(EntityReference::new).orElse(null);
    }

    /** 1.21.1 saves owners as a plain UUID (or an old-style string), both of which this accepts. */
    @Nullable
    public static <T extends Entity> EntityReference<T> readWithOldOwnerConversion(ValueInput input, String key, Level level) {
        EntityReference<T> reference = read(input, key);
        if (reference != null) return reference;
        Optional<String> legacy = input.getString(key);
        if (legacy.isPresent()) {
            try {
                return new EntityReference<>(UUID.fromString(legacy.get()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof EntityReference<?> other && other.uuid.equals(this.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}
