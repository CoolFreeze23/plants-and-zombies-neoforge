package joshxviii.plantz.effect

import joshxviii.plantz.PazEffects.HYPNOTIZED_GOAL_ATTACHMENT
import joshxviii.plantz.entity.plant.Plant
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Enemy

/**
 * A [MobEffect] that causes the user to attack entities of the same type.
 */
class HypnotizedMobEffect(category: MobEffectCategory, color: Int) : MobEffect(category, color), PazMobEffectHooks {
    companion object {
        const val CHECK_INTERVAL: Int = 100
    }

    override fun shouldApplyEffectTickThisTick(tickCount: Int, amplification: Int): Boolean {
        val interval = CHECK_INTERVAL shr amplification
        return if (interval > 0) tickCount % interval == 0 else true
    }

    override fun applyEffectTick(mob: LivingEntity, amplification: Int): Boolean {
        tryAddGoal(mob)
        return true
    }

    override fun onEffectStarted(effectInstance: MobEffectInstance, entity: LivingEntity) {
        super<PazMobEffectHooks>.onEffectStarted(effectInstance, entity)
        tryAddGoal(entity)
    }

    override fun onEffectRemoved(effectInstance: MobEffectInstance, entity: LivingEntity) {
        super<PazMobEffectHooks>.onEffectRemoved(effectInstance, entity)
        if (entity !is Plant) {
            if(entity is Mob) {
                val goal = HYPNOTIZED_GOAL_ATTACHMENT.remove(entity)
                if (goal != null) entity.targetSelector.removeGoal(goal)
            }
        }
    }

    private fun tryAddGoal(entity: LivingEntity) {
        if (entity.level().isClientSide) return
        if (entity !is Plant) {
            if(entity is Mob) {
                val oldGoal = HYPNOTIZED_GOAL_ATTACHMENT[entity]
                if (oldGoal != null) return

                val goal = NearestAttackableTargetGoal(entity, LivingEntity::class.java, true) { target ->
                    target.type == entity.type || (entity is Enemy && target is Enemy)
                }

                entity.targetSelector.addGoal(0, goal)
                HYPNOTIZED_GOAL_ATTACHMENT[entity] = goal
            }
        }
    }
}