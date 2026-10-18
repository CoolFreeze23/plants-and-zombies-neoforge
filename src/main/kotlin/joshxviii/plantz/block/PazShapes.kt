package joshxviii.plantz.block

import com.mojang.math.OctahedralGroup
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.EnumMap

/**
 * 26.1 `Block.column(...)` helpers (absent in 1.21.1). Sizes are in 1/16 block units, centred on the block.
 */
fun column(sizeXZ: Double, minY: Double, maxY: Double): VoxelShape = column(sizeXZ, sizeXZ, minY, maxY)

fun column(sizeX: Double, sizeZ: Double, minY: Double, maxY: Double): VoxelShape {
    val halfX = sizeX / 2.0
    val halfZ = sizeZ / 2.0
    return Block.box(8.0 - halfX, minY, 8.0 - halfZ, 8.0 + halfX, maxY, 8.0 + halfZ)
}

/**
 * Backport of the 26.1 `Shapes.rotate*` helpers. Same semantics as 26.1 `Shapes.rotate(shape, group)` about the
 * block centre: the new X/Y/Z coordinates come from the old axis the group permutes onto, flipped when the group
 * inverts that axis. Works box by box, which is exact for the axis-aligned 90-degree rotations used here.
 */
object PazShapes {
    // 26.1 BLOCK_ROT_* aliases, expressed with the 1.21.1 enum names
    @JvmField val BLOCK_ROT_X_90: OctahedralGroup = OctahedralGroup.ROT_90_X_NEG
    @JvmField val BLOCK_ROT_X_270: OctahedralGroup = OctahedralGroup.ROT_90_X_POS
    @JvmField val BLOCK_ROT_Y_90: OctahedralGroup = OctahedralGroup.ROT_90_Y_NEG
    @JvmField val BLOCK_ROT_Y_180: OctahedralGroup = OctahedralGroup.ROT_180_FACE_XZ
    @JvmField val BLOCK_ROT_Y_270: OctahedralGroup = OctahedralGroup.ROT_90_Y_POS

    private fun sourceAxis(rotation: OctahedralGroup, axis: Direction.Axis): Direction.Axis =
        rotation.rotate(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE)).axis

    fun rotate(shape: VoxelShape, rotation: OctahedralGroup): VoxelShape {
        if (rotation == OctahedralGroup.IDENTITY || shape.isEmpty) return shape
        val srcX = sourceAxis(rotation, Direction.Axis.X)
        val srcY = sourceAxis(rotation, Direction.Axis.Y)
        val srcZ = sourceAxis(rotation, Direction.Axis.Z)
        val flipX = rotation.inverts(Direction.Axis.X)
        val flipY = rotation.inverts(Direction.Axis.Y)
        val flipZ = rotation.inverts(Direction.Axis.Z)
        var result = Shapes.empty()
        for (box in shape.toAabbs()) {
            fun range(axis: Direction.Axis, flip: Boolean): Pair<Double, Double> {
                val min = box.min(axis)
                val max = box.max(axis)
                return if (flip) (1.0 - max) to (1.0 - min) else min to max
            }
            val (x0, x1) = range(srcX, flipX)
            val (y0, y1) = range(srcY, flipY)
            val (z0, z1) = range(srcZ, flipZ)
            result = Shapes.or(result, Shapes.box(x0, y0, z0, x1, y1, z1))
        }
        return result.optimize()
    }

    fun rotateHorizontalAxis(zAxis: VoxelShape): MutableMap<Direction.Axis, VoxelShape> {
        val map = EnumMap<Direction.Axis, VoxelShape>(Direction.Axis::class.java)
        map[Direction.Axis.Z] = zAxis
        map[Direction.Axis.X] = rotate(zAxis, BLOCK_ROT_Y_90)
        return map
    }

    fun rotateHorizontal(north: VoxelShape, initial: OctahedralGroup = OctahedralGroup.IDENTITY): MutableMap<Direction, VoxelShape> {
        val map = EnumMap<Direction, VoxelShape>(Direction::class.java)
        map[Direction.NORTH] = rotate(north, initial)
        map[Direction.EAST] = rotate(north, BLOCK_ROT_Y_90.compose(initial))
        map[Direction.SOUTH] = rotate(north, BLOCK_ROT_Y_180.compose(initial))
        map[Direction.WEST] = rotate(north, BLOCK_ROT_Y_270.compose(initial))
        return map
    }

    fun rotateAttachFace(north: VoxelShape, initial: OctahedralGroup = OctahedralGroup.IDENTITY): Map<AttachFace, Map<Direction, VoxelShape>> {
        return mapOf(
            AttachFace.WALL to rotateHorizontal(north, initial),
            AttachFace.FLOOR to rotateHorizontal(north, BLOCK_ROT_X_270.compose(initial)),
            AttachFace.CEILING to rotateHorizontal(north, BLOCK_ROT_Y_180.compose(BLOCK_ROT_X_90).compose(initial)),
        )
    }
}
