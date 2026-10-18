package joshxviii.plantz.effect

import net.minecraft.core.particles.ParticleOptions
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob

/**
 *
 */
class ButteredMobEffect(
    category: MobEffectCategory,
    color: Int,
    particleOptions: ParticleOptions
) : MobEffect(category, color, particleOptions), PazMobEffectHooks {
    companion object {

    }

    override fun onEffectStarted(effectInstance: MobEffectInstance, entity: LivingEntity) {
        super<PazMobEffectHooks>.onEffectStarted(effectInstance, entity)
    }

    override fun onEffectRemoved(effectInstance: MobEffectInstance, entity: LivingEntity) {
        super<PazMobEffectHooks>.onEffectRemoved(effectInstance, entity)
    }
}