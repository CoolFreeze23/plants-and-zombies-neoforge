package joshxviii.plantz.block

import com.mojang.serialization.MapCodec
import joshxviii.plantz.PazBlocks
import joshxviii.plantz.block.entity.FlagBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.RotationSegment
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class FlagBlock(properties: Properties) : BaseEntityBlock(properties) {
    // 1.21.1 BaseEntityBlock defaults to INVISIBLE; 26.1 renders the block model
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    companion object {
        val SHAPE: VoxelShape = column(8.0, 0.0, 16.0)
        //val FACING: EnumProperty<Direction> = HorizontalDirectionalBlock.FACING
        val ROTATION: IntegerProperty = BlockStateProperties.ROTATION_16
        val CODEC: MapCodec<FlagBlock> = simpleCodec(::FlagBlock)
    }
    override fun codec(): MapCodec<out FlagBlock> = CODEC

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return SHAPE
    }

    override fun newBlockEntity(worldPosition: BlockPos, blockState: BlockState): BlockEntity {
        return FlagBlockEntity(worldPosition, blockState)
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0))
    }

    override fun <T : BlockEntity> getTicker(level: Level, blockState: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (type == PazBlocks.FLAG_BLOCK_ENTITY) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as FlagBlockEntity).tick(lvl, pos, st)
            }
        } else null
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16))
    }
    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.setValue(ROTATION,mirror.mirror(state.getValue(ROTATION), 16))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(ROTATION)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return this.defaultBlockState().setValue(ROTATION, RotationSegment.convertToSegment(context.rotation + 180.0f))
    }

    override fun updateShape(
        state: BlockState,
        directionToNeighbour: Direction,
        neighbourState: BlockState,
        level: LevelAccessor,
        pos: BlockPos,
        neighbourPos: BlockPos
    ): BlockState {
        return if (directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos))
            Blocks.AIR.defaultBlockState()
        else
            super.updateShape(state, directionToNeighbour, neighbourState, level, pos, neighbourPos)
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        val direction = Direction.DOWN
        return canSupportCenter(level, pos.relative(direction), direction.opposite)
    }
}