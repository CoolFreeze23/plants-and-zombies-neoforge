package joshxviii.plantz.compat.client.model;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.function.Function;

/** 26.1 {@code ArmorModelSet}: one value per armor slot. */
public record ArmorModelSet<T>(T head, T chest, T legs, T feet) {
    public T get(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> this.head;
            case CHEST -> this.chest;
            case LEGS -> this.legs;
            case FEET -> this.feet;
            default -> throw new IllegalStateException("No model for slot: " + slot);
        };
    }

    public <U> ArmorModelSet<U> map(Function<? super T, ? extends U> mapper) {
        return new ArmorModelSet<>(mapper.apply(this.head), mapper.apply(this.chest), mapper.apply(this.legs), mapper.apply(this.feet));
    }

    public static <M> ArmorModelSet<M> bake(ArmorModelSet<ModelLayerLocation> locations, EntityModelSet modelSet, Function<ModelPart, M> factory) {
        return locations.map(location -> factory.apply(modelSet.bakeLayer(location)));
    }
}
