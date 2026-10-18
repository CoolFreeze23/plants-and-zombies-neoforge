package joshxviii.plantz.item

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block

/** A block item with a 26.1 `WEAPON` component: hits count as item use and cost [damagePerAttack] durability. */
class WeaponBlockItem(
    block: Block,
    properties: Properties,
    private val damagePerAttack: Int = 1,
) : BlockItem(block, properties) {
    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean = true

    override fun postHurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity) {
        stack.hurtAndBreak(damagePerAttack, attacker, EquipmentSlot.MAINHAND)
    }
}
