package joshxviii.plantz.ai.pathfinding

import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation
import net.minecraft.world.level.Level
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator
import net.minecraft.world.level.pathfinder.NodeEvaluator
import net.minecraft.world.level.pathfinder.Path
import net.minecraft.world.level.pathfinder.PathFinder
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * Advances the path once the mob is horizontally on top of the next node, ignoring the vertical distance.
 * Vanilla only advances when the mob is within 1 block vertically, so mobs that can't control their height
 * (used for floating in water or flying with balloons etc)
 */
fun Path.advanceIfHorizontallyReached(mob: Mob) {
    if (isDone) return
    val maxDistanceToWaypoint = if (mob.bbWidth > 0.75f) mob.bbWidth / 2.0f else 0.75f - mob.bbWidth / 2.0f
    val nodePos = nextNodePos
    if (abs(mob.x - (nodePos.x + 0.5)) < maxDistanceToWaypoint && abs(mob.z - (nodePos.z + 0.5)) < maxDistanceToWaypoint) advance()
}

open class PazGroundPathNavigation(mob: Mob, level: Level) : GroundPathNavigation(mob, level) {
    override fun followThePath() {
        val path = path ?: return
        if (mob.isInWater) {
            path.advanceIfHorizontallyReached(mob)
            if (path.isDone) return
        }
        super.followThePath()
    }
}

open class PazFlyingPathNavigation(mob: Mob, level: Level) : FlyingPathNavigation(mob, level) {
    var passThroughBlocks: Boolean = false
        set(value) {
            field = value
            (nodeEvaluator as? GhostNodeEvaluator)?.passThroughBlocks = value
        }

    override fun createPathFinder(maxVisitedNodes: Int): PathFinder {
        this.nodeEvaluator = GhostNodeEvaluator().also { it.passThroughBlocks = passThroughBlocks }
        return PathFinder(this.nodeEvaluator, maxVisitedNodes)
    }
    override fun canMoveDirectly(startPos: Vec3, stopPos: Vec3): Boolean {
        if (passThroughBlocks) return true
        return super.canMoveDirectly(startPos, stopPos)
    }
    override fun followThePath() {
        val path = path ?: return
        path.advanceIfHorizontallyReached(mob)
        if (path.isDone) return
        super.followThePath()
    }
}
