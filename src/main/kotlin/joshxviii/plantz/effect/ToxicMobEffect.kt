package joshxviii.plantz.effect

import joshxviii.plantz.PazDamageTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity


class ToxicMobEffect(category: MobEffectCategory, color: Int) : MobEffect(category, color) {
    companion object {
        const val DAMAGE_INTERVAL: Int = 15
        const val HURT_PERCENT: Float = 0.25f
    }

    override fun applyEffectTick(level: ServerLevel, mob: LivingEntity, amplification: Int): Boolean {
        if (mob.health > 0) {
            mob.hurtServer(level, mob.damageSources().source(PazDamageTypes.TOXIC), (mob.health*HURT_PERCENT).coerceAtLeast(0.25f))
        }
        return true
    }

    override fun shouldApplyEffectTickThisTick(tickCount: Int, amplification: Int): Boolean {
        val interval = DAMAGE_INTERVAL shr amplification
        return if (interval > 0) tickCount % interval == 0 else true
    }
}