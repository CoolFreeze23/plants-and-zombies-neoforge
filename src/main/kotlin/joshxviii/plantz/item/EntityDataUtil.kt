package joshxviii.plantz.item

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

/*
 * 1.21.1 stores DataComponents.ENTITY_DATA as CustomData with an "id" key, where 26.1 uses TypedEntityData.
 * These helpers stand in for 26.1's `TypedEntityData.of(type, tag)` and `entityData.type()`.
 */

/** 26.1 `itemStack.get(DataComponents.ENTITY_DATA)?.type()`. */
fun ItemStack.entityDataType(): EntityType<*>? {
    val data = get(DataComponents.ENTITY_DATA) ?: return null
    return entityTypeOf(data)
}

fun entityTypeOf(data: CustomData): EntityType<*>? {
    if (data.isEmpty) return null
    val id = data.unsafe.getString("id")
    if (id.isEmpty()) return null
    return EntityType.byString(id).orElse(null)
}

/** 26.1 `TypedEntityData.of(type, tag)`. */
fun entityDataOf(type: EntityType<*>, tag: CompoundTag = CompoundTag()): CustomData {
    val copy = tag.copy()
    copy.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString())
    return CustomData.of(copy)
}
