package joshxviii.plantz.block.entity

import joshxviii.plantz.PazBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class GardenGnomeBlockEntity(
    worldPosition: BlockPos,
    blockState: BlockState
) : BlockEntity(PazBlocks.GARDEN_GNOME_ENTITY, worldPosition, blockState)
