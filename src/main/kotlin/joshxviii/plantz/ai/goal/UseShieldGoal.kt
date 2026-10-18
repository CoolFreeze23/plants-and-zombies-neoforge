package joshxviii.plantz.ai.goal

import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.goal.Goal
import net.neoforged.neoforge.common.ItemAbilities
import java.util.*


class UseShieldGoal<T : Mob>(
    private val mob: T,
    val speedModifier: Double = 0.5
) : Goal() {

    init {
        setFlags(EnumSet.of<Flag>(Flag.MOVE, Flag.LOOK))
    }

    override fun canUse(): Boolean {
        return ableToBlock() && !mob.isUsingItem
    }

    private fun ableToBlock(): Boolean {
        return this.mob.target != null && mob.mainHandItem.canPerformAction(ItemAbilities.SHIELD_BLOCK)// 26.1 BLOCKS_ATTACKS component
    }

    private val blockDelay: Float
        get() {
            // 1.21.1 shields have a fixed 5 tick (0.25 s) block delay instead of BlocksAttacks.blockDelaySeconds
            return if (mob.mainHandItem.canPerformAction(ItemAbilities.SHIELD_BLOCK)) 0.25f else 0f
        }

    override fun canContinueToUse(): Boolean {
        return ableToBlock()
    }

    override fun start() {
        super.start()
        mob.setAggressive(true)
        mob.startUsingItem(InteractionHand.MAIN_HAND)
    }

    override fun stop() {
        super.stop()
        mob.getNavigation().stop()
        mob.setAggressive(false)
        mob.stopUsingItem()
    }

    override fun tick() {
        val target = this.mob.target?: return
        this.mob.lookAt(target, 30.0f, 30.0f)
        this.mob.getLookControl().setLookAt(target, 30.0f, 30.0f)

        this.mob.getNavigation().moveTo(target, speedModifier)
    }
}