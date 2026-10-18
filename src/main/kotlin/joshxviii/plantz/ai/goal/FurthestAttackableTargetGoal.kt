package joshxviii.plantz.ai.goal

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.target.TargetGoal
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import java.util.*
import java.util.function.Predicate

open class FurthestAttackableTargetGoal<T : LivingEntity>(
    usingEntity: PathfinderMob,
    protected val targetType: Class<T>,
    randomInterval: Int,
    mustSee: Boolean,
    mustReach: Boolean,
    selector: Predicate<LivingEntity>?
) : TargetGoal(usingEntity, mustSee, mustReach) {
    protected val randomInterval: Int = reducedTickDelay(randomInterval)
    protected var target: LivingEntity? = null
    protected val targetConditions: TargetingConditions

    init {
        this.setFlags(EnumSet.of<Flag>(Flag.TARGET))
        this.targetConditions = TargetingConditions.forCombat().range(this.followDistance).selector(selector)
    }

    override fun canUse(): Boolean {
        if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
            return false
        } else {
            this.findTarget()
            return this.target != null
        }
    }

    protected open fun getTargetSearchArea(followDistance: Double): AABB {
        return this.mob.boundingBox.inflate(followDistance, followDistance, followDistance)
    }

    protected fun findTarget() {
        val level = this.mob.level() as? ServerLevel ?: return
        if (this.targetType != Player::class.java && this.targetType != ServerPlayer::class.java) {
            this.target = getFurthestEntities(
                this.mob.level().getEntitiesOfClass<T>(
                    this.targetType,
                    this.getTargetSearchArea(this.followDistance),
                    Predicate { entity: T? -> true }),
                this.targetConditions,
                this.mob,
                this.mob.x,
                this.mob.eyeY,
                this.mob.z
            )
        } else {
            this.target = level.getNearestPlayer(
                this.targetConditions,
                this.mob,
                this.mob.x,
                this.mob.eyeY,
                this.mob.z
            )
        }
    }

    override fun start() {
        this.mob.target = this.target
        super.start()
    }

    // joshxviii.plantz.getFurthestEntities (26.1 ServerEntityGetter receiver), inlined for 1.21.1
    private fun <E : LivingEntity> getFurthestEntities(
        entities: List<E>,
        targetConditions: TargetingConditions,
        source: LivingEntity?,
        x: Double,
        y: Double,
        z: Double
    ): E? {
        var best = -1.0
        var result: E? = null
        for (entity in entities) {
            if (targetConditions.test(source, entity)) {
                val dist = entity.distanceToSqr(x, y, z)
                if (best == -1.0 || dist > best) {
                    best = dist
                    result = entity
                }
            }
        }
        return result
    }
}