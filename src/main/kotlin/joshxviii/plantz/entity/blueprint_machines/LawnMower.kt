package joshxviii.plantz.entity.blueprint_machines

import com.mojang.serialization.Codec
import joshxviii.plantz.compat.ValueInput
import joshxviii.plantz.compat.ValueOutput
import joshxviii.plantz.item.BlueprintItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import java.util.EnumMap

class LawnMower(type: EntityType<out LawnMower>, level: Level) : LivingEntity(type, level) {
    companion object {
        // 26.1 EntityEquipment.CODEC
        private val EQUIPMENT_CODEC: Codec<Map<EquipmentSlot, ItemStack>> = Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC)
    }

    // 26.1 LivingEntity keeps (and saves) equipment itself; 1.21.1 leaves storage to subclasses
    private val equipment = EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot::class.java)

    override fun getPickResult(): ItemStack = BlueprintItem.stackFor(this.type)

    override fun hurt(
        source: DamageSource,
        damage: Float
    ): Boolean {
        if (level() !is ServerLevel) return false
        setRemoved(RemovalReason.KILLED)
        return true
    }

    override fun getArmorSlots(): Iterable<ItemStack> =
        listOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD).map { getItemBySlot(it) }

    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = equipment[slot] ?: ItemStack.EMPTY

    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {
        verifyEquippedItem(stack)
        onEquipItem(slot, equipment.put(slot, stack) ?: ItemStack.EMPTY, stack)
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        val items = equipment.filterValues { !it.isEmpty }
        if (items.isNotEmpty()) ValueOutput.of(tag, registryAccess()).store("equipment", EQUIPMENT_CODEC, items)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        equipment.clear()
        ValueInput.of(tag, registryAccess()).read("equipment", EQUIPMENT_CODEC).ifPresent { equipment.putAll(it) }
    }

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT
}
