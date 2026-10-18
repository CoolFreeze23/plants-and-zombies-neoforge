package joshxviii.plantz.block

import com.mojang.serialization.MapCodec
import joshxviii.plantz.PazBlocks
import joshxviii.plantz.block.entity.GravestoneBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.Util
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class GravestoneBlock(properties: Properties) : BaseEntityBlock(properties), SimpleWaterloggedBlock {
    // 1.21.1 BaseEntityBlock defaults to INVISIBLE; 26.1 renders the block model
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    companion object {
        val CODEC: MapCodec<GravestoneBlock> = simpleCodec(::GravestoneBlock)
        val SHAPE: VoxelShape = Util.make {
            Shapes.or(
                column(16.0, 6.0, 0.0, 2.0),
                column(12.0, 4.0, 2.0, 16.0),
            )
        }
        val SHAPES: MutableMap<Direction.Axis, VoxelShape> = PazShapes.rotateHorizontalAxis(SHAPE)
        val FACING: EnumProperty<Direction> = HorizontalDirectionalBlock.FACING
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED
    }

    override fun isPathfindable(state: BlockState, type: PathComputationType): Boolean {
        return when (type) {
            PathComputationType.LAND -> (false)
            PathComputationType.WATER -> (state.getValue(TrapDoorBlock.WATERLOGGED))
            PathComputationType.AIR -> (false)
        }
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false))
    }

    override fun <T : BlockEntity> getTicker(level: Level, blockState: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (type == PazBlocks.GRAVESTONE_BLOCK_ENTITY) {
            BlockEntityTicker { level, pos, state, blockEntity ->
                GravestoneBlockEntity.tick(level, pos, state, blockEntity as GravestoneBlockEntity)
            }
        } else null
    }

    override fun newBlockEntity(worldPosition: BlockPos, blockState: BlockState): BlockEntity {
        return GravestoneBlockEntity(worldPosition, blockState)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        return SHAPES[facing.axis] as VoxelShape
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue<Direction, Direction>(FACING, rotation.rotate(state.getValue<Direction>(FACING)))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, WATERLOGGED)
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val replacedFluidState = context.level.getFluidState(context.clickedPos)
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
            .setValue(WATERLOGGED, replacedFluidState.`is`(Fluids.WATER))
    }

    override fun updateShape(
        state: BlockState,
        directionToNeighbour: Direction,
        neighbourState: BlockState,
        level: LevelAccessor,
        pos: BlockPos,
        neighbourPos: BlockPos
    ): BlockState {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        return super.updateShape(state, directionToNeighbour, neighbourState, level, pos, neighbourPos)
    }

    override fun codec(): MapCodec<out GravestoneBlock> { return CODEC }
}