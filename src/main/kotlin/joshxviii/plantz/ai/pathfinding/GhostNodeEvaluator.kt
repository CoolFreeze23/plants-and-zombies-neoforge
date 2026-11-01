package joshxviii.plantz.ai.pathfinding

import joshxviii.plantz.PazTags
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Mob
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator
import net.minecraft.world.level.pathfinder.Node
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.level.pathfinder.PathfindingContext
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator

class GhostNodeEvaluator : FlyNodeEvaluator() {
    var passThroughBlocks: Boolean = false

    override fun getPathType(context: PathfindingContext, x: Int, y: Int, z: Int): PathType {
        val pos = BlockPos(x, y, z)
        var type = super.getPathType(context, x, y, z)

        if (passThroughBlocks && isSolid(context, pos)) type = PathType.OPEN

        return type
    }

    private fun isSolid(context: PathfindingContext, pos: BlockPos): Boolean {
        val blockState = context.getBlockState(pos)
        return !blockState.getCollisionShape(context.level(), pos).isEmpty
    }

    override fun isDiagonalValid(diagonal: Node?): Boolean {
        return super.isDiagonalValid(diagonal)
    }

    override fun getPathType(mob: Mob, pos: BlockPos): PathType {
        return super.getPathType(mob, pos)
    }

    override fun getNeighbors(neighbors: Array<out Node>, pos: Node): Int {
        return super.getNeighbors(neighbors, pos)
    }
}