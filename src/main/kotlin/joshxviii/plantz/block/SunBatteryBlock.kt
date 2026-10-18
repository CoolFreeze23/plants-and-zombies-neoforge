package joshxviii.plantz.block

import com.mojang.math.OctahedralGroup
import net.minecraft.world.Containers
import net.minecraft.world.ItemInteractionResult
import com.mojang.serialization.MapCodec
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazItems
import joshxviii.plantz.block.entity.SunBatteryBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.function.ToIntFunction

class SunBatteryBlock(properties: Properties) : BaseEntityBlock(properties), SimpleWaterloggedBlock  {
    // 1.21.1 BaseEntityBlock defaults to INVISIBLE; 26.1 renders the block model
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    companion object {
        val CODEC: MapCodec<SunBatteryBlock> = simpleCodec(::SunBatteryBlock)
        val FACE: EnumProperty<AttachFace> = BlockStateProperties.ATTACH_FACE
        val FACING: EnumProperty<Direction> = HorizontalDirectionalBlock.FACING
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED
        val LEVEL: IntegerProperty = BlockStateProperties.LEVEL
        val LIGHT_EMISSION: ToIntFunction<BlockState> = { it.getValue(LightBlock.LEVEL) }

        val SHAPE: VoxelShape = PazShapes.rotate(column(8.0, 0.0, 11.0), OctahedralGroup.ROT_90_REF_X_NEG)
        var SHAPES: Map<AttachFace, Map<Direction, VoxelShape>> = PazShapes.rotateAttachFace(SHAPE)
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.FLOOR).setValue(WATERLOGGED, false).setValue(LEVEL, 0))
    }

    override fun useItemOn(
        itemStack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        if (itemStack.`is`(PazItems.SUN)) {
            val blockEntity = level.getBlockEntity(pos)
            (blockEntity as? SunBatteryBlockEntity)?.let {
                if (it.isFull()) return ItemInteractionResult.FAIL
                it.addSun(1)
                itemStack.consume(1, player)
                return ItemInteractionResult.SUCCESS
            }
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return SHAPES[state.getValue(FACE)]!![state.getValue(FACING)]!!
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return rotate(state, mirror.getRotation(state.getValue(FACING)))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, FACE, WATERLOGGED, LEVEL)
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val level = context.level
        val clicked = context.clickedFace
        val pos = context.clickedPos
        val fluid = level.getFluidState(pos)
        val sunLevel = context.itemInHand.get(PazComponents.STORED_SUN)?.getLevel() ?: 0

        for (dir in context.nearestLookingDirections) {
            val state = if (dir.axis === Direction.Axis.Y) {
                defaultBlockState()
                    .setValue(FACE, if (dir == Direction.UP) AttachFace.CEILING else AttachFace.FLOOR)
                    .setValue(FACING, context.horizontalDirection)
            } else {
                defaultBlockState()
                    .setValue(FACE, AttachFace.WALL)
                    .setValue(FACING, dir.opposite)
            }
                .setValue(WATERLOGGED, fluid.`is`(Fluids.WATER))
                .setValue(LEVEL, sunLevel)

            if (state.canSurvive(level, pos)) {
                return state
            }
        }
        return null
    }

    override fun newBlockEntity(worldPosition: BlockPos, blockState: BlockState): BlockEntity {
        return SunBatteryBlockEntity(worldPosition, blockState)
    }

    // 26.1 BlockEntity.preRemoveSideEffects dropped container contents (the battery item); 1.21.1 does it from the block
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        Containers.dropContentsOnDestroy(state, newState, level, pos)
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        val blockEntity = level.getBlockEntity(pos)
        (blockEntity as? SunBatteryBlockEntity)?.let {
            if (player.hasInfiniteMaterials()) it.clearContent()
        }
        return super.playerWillDestroy(level, pos, state, player)
    }

    override fun isPathfindable(state: BlockState, type: PathComputationType): Boolean = false

    override fun updateShape(
        state: BlockState,
        directionToNeighbour: Direction,
        neighbourState: BlockState,
        level: LevelAccessor,
        pos: BlockPos,
        neighbourPos: BlockPos
    ): BlockState {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        return if (getConnectedDirection(state).opposite == directionToNeighbour && !state.canSurvive(level, pos)) {
            Blocks.AIR.defaultBlockState()
        } else
            super.updateShape(state, directionToNeighbour, neighbourState, level, pos, neighbourPos)
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        return canAttach(level, pos, getConnectedDirection(state).opposite)
    }

    fun getConnectedDirection(state: BlockState): Direction {
        return when (state.getValue(FACE)) {
            AttachFace.CEILING -> Direction.DOWN
            AttachFace.FLOOR -> Direction.UP
            else -> state.getValue(FACING)
        }
    }

    private fun canAttach(level: LevelReader, pos: BlockPos, supportDir: Direction): Boolean {
        val supportPos = pos.relative(supportDir)
        return canSupportCenter(level, supportPos, supportDir.opposite)
    }

    override fun hasAnalogOutputSignal(state: BlockState): Boolean = state.getValue(LEVEL) > 0
    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos): Int {
        return state.getValue(LEVEL)
    }

    override fun getCloneItemStack(state: BlockState, target: HitResult, level: LevelReader, pos: BlockPos, player: Player): ItemStack {
        // copy: upstream handed out the block entity's own stack, so the picked item and the block shared their sun
        return (level.getBlockEntity(pos) as? SunBatteryBlockEntity)?.theItem?.copy()?:
        super.getCloneItemStack(state, target, level, pos, player)
    }

    override fun codec(): MapCodec<out SunBatteryBlock> { return CODEC }
}