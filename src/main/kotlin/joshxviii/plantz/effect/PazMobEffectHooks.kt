package joshxviii.plantz.effect

import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity

/**
 * The instance-aware effect callbacks the 26.1 Fabric build got from Fabric API
 * (`onEffectStarted/onEffectAdded/onEffectRemoved(MobEffectInstance, LivingEntity)`).
 * 1.21.1 has no equivalent, so `mixin.LivingEntityMixin` calls these for any effect implementing this interface:
 * - [onEffectAdded]: a new effect instance was added (LivingEntity.onEffectAdded)
 * - [onEffectStarted]: an effect was applied or re-applied (LivingEntity.addEffect -> MobEffectInstance.onEffectStarted)
 * - [onEffectRemoved]: an effect was removed or expired (LivingEntity.onEffectRemoved)
 */
interface PazMobEffectHooks {
    fun onEffectStarted(effectInstance: MobEffectInstance, entity: LivingEntity) {}
    fun onEffectAdded(effectInstance: MobEffectInstance, entity: LivingEntity) {}
    fun onEffectRemoved(effectInstance: MobEffectInstance, entity: LivingEntity) {}
}
