package joshxviii.plantz.block.entity

import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazItems
import joshxviii.plantz.block.SunBatteryBlock
import joshxviii.plantz.item.component.StoredSun
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import joshxviii.plantz.compat.ValueInput
import joshxviii.plantz.compat.ValueOutput
import net.minecraft.world.ticks.ContainerSingleItem.BlockContainerSingleItem

class SunBatteryBlockEntity(
    worldPosition: BlockPos,
    blockState: BlockState,
) : BlockEntity(
    PazBlocks.SUN_BATTERY_BLOCK_ENTITY, worldPosition, blockState
), BlockContainerSingleItem {

    companion object {
        fun findAttachedSunBattery(level: Level, targetPos: BlockPos, hasSunAmount: Int = 0): SunBatteryBlockEntity? {
            for (dir in Direction.entries) {
                val batteryPos = targetPos.relative(dir)
                val be = level.getBlockEntity(batteryPos) as? SunBatteryBlockEntity ?: continue
                val state = be.blockState
                if (!state.`is`(PazBlocks.SUN_BATTERY_BLOCK)) continue

                val supportDir = (state.block as SunBatteryBlock).getConnectedDirection(state).opposite
                val supportPos = batteryPos.relative(supportDir)

                if (hasSunAmount > 0 && !be.hasSun(hasSunAmount)) continue

                if (supportPos == targetPos) return be
            }
            return null
        }
    }

    private var batteryItem: ItemStack = PazItems.SUN_BATTERY.defaultInstance

    fun getStoredSunPercent(): Float {
        return batteryItem.get(PazComponents.STORED_SUN)?.storagePercentage() ?: 0f
    }

    fun addSun(amount: Int) {
        val newStoredSun = batteryItem.get(PazComponents.STORED_SUN)?.addSun(amount) ?: StoredSun(amount)
        batteryItem.set(PazComponents.STORED_SUN, newStoredSun)
        level?.let {
            it.playSound(null, blockPos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.6f, getStoredSunPercent()+0.5f+(it.random.nextFloat()*0.1f))
        }
        updateLevel(newStoredSun.getLevel())
    }

    fun removeSun(amount: Int) {
        val newStoredSun = batteryItem.get(PazComponents.STORED_SUN)?.removeSun(amount) ?: StoredSun()
        batteryItem.set(PazComponents.STORED_SUN, newStoredSun)
        updateLevel(newStoredSun.getLevel())
    }

    fun hasSun(amount: Int): Boolean = batteryItem.get(PazComponents.STORED_SUN)?.hasSun(amount) ?: false
    fun isFull(): Boolean = batteryItem.get(PazComponents.STORED_SUN)?.isFull() ?: false

    fun updateLevel(sunLevel: Int) {
        level?.setBlock(blockPos, blockState.setValue(SunBatteryBlock.LEVEL, sunLevel), 3)
    }

    override fun canTakeItem(into: Container, slot: Int, itemStack: ItemStack): Boolean {
        return false
    }

    override fun applyImplicitComponents(components: DataComponentInput) {
        super.applyImplicitComponents(components)
        batteryItem.set(PazComponents.STORED_SUN, components.get(PazComponents.STORED_SUN) ?: StoredSun())
    }

    override fun getContainerBlockEntity(): BlockEntity = this

    // 26.1 preRemoveSideEffects dropped the battery item: done by SunBatteryBlock.onRemove on 1.21.1

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        val output = ValueOutput.of(tag, registries)
        if (!batteryItem.isEmpty) output.store("BatteryItem", ItemStack.CODEC, batteryItem)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        val input = ValueInput.of(tag, registries)
        batteryItem = input.read("BatteryItem", ItemStack.CODEC).orElse(ItemStack.EMPTY)?: ItemStack.EMPTY
    }

    override fun getTheItem(): ItemStack {
        return batteryItem
    }

    override fun setTheItem(itemStack: ItemStack) {
        if (!itemStack.`is`(PazItems.SUN_BATTERY)) return
        batteryItem = itemStack
    }

}