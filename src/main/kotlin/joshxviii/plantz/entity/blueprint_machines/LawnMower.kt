package joshxviii.plantz.entity.blueprint_machines

import joshxviii.plantz.item.BlueprintItem
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class LawnMower(type: EntityType<out LawnMower>, level: Level) : LivingEntity(type, level) {
    companion object {

    }

    override fun getPickResult(): ItemStack = BlueprintItem.stackFor(this.type)

    // 26.1 hurtServer -> 1.21.1 hurt (server side only, like hurtServer)
    override fun hurt(
        source: DamageSource,
        damage: Float
    ): Boolean {
        if (level() !is ServerLevel) return false
        setRemoved(RemovalReason.KILLED)
        return true
    }

    // 1.21.1 LivingEntity has no built-in equipment storage (26.1 does), so keep a plain per-slot store
    private val equipment = HashMap<EquipmentSlot, ItemStack>()
    override fun getArmorSlots(): Iterable<ItemStack> =
        listOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD).map { getItemBySlot(it) }
    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = equipment[slot] ?: ItemStack.EMPTY
    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {
        verifyEquippedItem(stack)
        equipment[slot] = stack
    }

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT
}