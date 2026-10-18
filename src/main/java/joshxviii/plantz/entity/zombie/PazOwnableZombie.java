package joshxviii.plantz.entity.zombie;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import joshxviii.plantz.compat.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/** 1.21.1 OwnableEntity lacks the 26.1 getOwnerReference()/getRootOwner() and only resolves player owners, so they live here. */
public abstract class PazOwnableZombie extends PazZombie implements OwnableEntity {
    public PazOwnableZombie(EntityType<? extends PazZombie> type, Level level) {
        super(type, level);
    }

    @Nullable
    public abstract EntityReference<LivingEntity> getOwnerReference();

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        EntityReference<LivingEntity> reference = this.getOwnerReference();
        return reference == null ? null : reference.getUUID();
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        return EntityReference.getLivingEntity(this.getOwnerReference(), this.level());
    }

    /** 26.1 OwnableEntity#getRootOwner. */
    @Nullable
    public LivingEntity getRootOwner() {
        Set<Object> seen = new ObjectArraySet<>();
        LivingEntity owner = this.getOwner();
        seen.add(this);

        while (owner instanceof OwnableEntity ownableOwner) {
            LivingEntity ownersOwner = ownableOwner.getOwner();
            if (seen.contains(ownersOwner)) {
                return null;
            }

            seen.add(owner);
            owner = ownableOwner.getOwner();
        }

        return owner;
    }
}
