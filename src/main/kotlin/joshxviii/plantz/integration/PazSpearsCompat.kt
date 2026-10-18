package joshxviii.plantz.integration

import joshxviii.plantz.compat.ai.SpearUseGoal
import net.minecraft.world.entity.ai.goal.GoalSelector
import net.minecraft.world.entity.monster.Zombie

/**
 * Optional Backported Spears support. It adds 1.21.11's spears under the `minecraft` namespace (the copper spear only
 * together with Copper Age Backport) with the `minecraft:kinetic_weapon` component that [SpearUseGoal] charges with,
 * so with it installed gnomes carry spears and gnomes and zombies fight with them like in 26.1.
 */
object PazSpearsCompat {
    const val MOD_ID = "spears"

    private const val MOD_SPEAR_GOAL = "com.notunanancyowen.spears.goals.UseSpearGoal"

    /**
     * Backported Spears' mixin adds its own copy of the spear goal to vanilla `Zombie.addBehaviourGoals` (26.1 adds
     * SpearUseGoal there too). That copy never resets after its first charge, so zombies get [SpearUseGoal] in its place.
     */
    fun replaceModSpearGoal(zombie: Zombie, goalSelector: GoalSelector) {
        val modGoal = goalSelector.availableGoals.firstOrNull { it.goal.javaClass.name == MOD_SPEAR_GOAL } ?: return
        goalSelector.removeGoal(modGoal.goal)
        goalSelector.addGoal(modGoal.priority, SpearUseGoal(zombie, 1.0, 1.0, 10.0f, 2.0f))
    }
}
