package joshxviii.plantz.block.entity

import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazComponents
import joshxviii.plantz.TimeMachineData
import joshxviii.plantz.block.TimeMachineBlock
import joshxviii.plantz.block.TimeMachineBlock.Companion.STATE
import joshxviii.plantz.block.TimeMachineState
import joshxviii.plantz.inventory.TimeMachineMenu
import joshxviii.plantz.compat.menu.ExtendedMenuProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import joshxviii.plantz.compat.ValueInput
import joshxviii.plantz.compat.ValueOutput
import net.minecraft.world.ticks.ContainerSingleItem.BlockContainerSingleItem

class GardenGnomeBlockEntity(
    worldPosition: BlockPos,
    blockState: BlockState
) : BlockEntity(PazBlocks.GARDEN_GNOME_ENTITY, worldPosition, blockState) {
    companion object {
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        val output = ValueOutput.of(tag, registries)

    }
}