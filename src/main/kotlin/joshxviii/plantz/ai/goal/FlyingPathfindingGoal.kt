package joshxviii.plantz.ai.goal

import joshxviii.plantz.ai.ZombieState
import joshxviii.plantz.entity.zombie.PazZombie
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.pathfinder.Path
import net.minecraft.world.phys.Vec3
import java.util.EnumSet

open class FlyingPathfindingGoal(
    private val entity: PathfinderMob,
    val hoverGroundHeight: Double = 1.5,
    val speedModifier: Double = 1.0
) : Goal() {

    private var targetPos: Vec3? = null
    private var path: Path? = null

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        if (entity is PazZombie && !entity.isFlying) return false
        if (entity.target != null && entity.target!!.isAlive) return false
        if (!entity.navigation.isDone) return false
        if (entity.random.nextInt(reducedTickDelay(30)) != 0) return false

        val destination = findFlyingDestination() ?: return false
        path = entity.navigation.createPath(destination.x, destination.y, destination.z, 0)
        targetPos = destination
        return path != null
    }

    override fun canContinueToUse(): Boolean {
        if (entity is PazZombie && !entity.isFlying) return false
        if (entity.target != null && entity.target!!.isAlive) return false
        return !entity.navigation.isDone
    }

    override fun start() {
        path?.let { entity.navigation.moveTo(it, speedModifier) }
    }

    override fun stop() {
        entity.navigation.stop()
        targetPos = null
        path = null
    }

    private fun findFlyingDestination(): Vec3? {
        val level = entity.level()
        for (i in 0..9) {
            val rx = entity.x + (entity.random.nextDouble() * 16.0 - 8.0)
            val rz = entity.z + (entity.random.nextDouble() * 16.0 - 8.0)
            val blockX = rx.toInt()
            val blockZ = rz.toInt()

            val targetHover = (if (hoverGroundHeight > 0.0) hoverGroundHeight else 1.5)
            val surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ).toDouble()
            val ry = surfaceY + targetHover + (entity.random.nextDouble() * 1.0 - 0.5) - 3.0

            if (ry <= level.minY + 2 || ry >= level.maxY - 2) continue

            val destBlockPos = BlockPos.containing(rx, ry, rz)
            if (level.isEmptyBlock(destBlockPos)) {
                return Vec3(rx, ry, rz)
            }
        }
        return null
    }
}
