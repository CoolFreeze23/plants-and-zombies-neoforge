package joshxviii.plantz.entity.projectile

import joshxviii.plantz.PazSounds
import joshxviii.plantz.createFallingBlock
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.item.FallingBlockEntity
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.ExplosionDamageCalculator
import net.minecraft.world.level.Level
import net.minecraft.world.level.SimpleExplosionDamageCalculator
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.GameRules
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import java.util.*

class PowderSnowChunk(
    val level: Level,
    val mob: Mob
) : ThrowableItemProjectile(EntityType.SNOWBALL, mob, level) {

    companion object {
        val EXPLOSION_DAMAGE_CALCULATOR: ExplosionDamageCalculator = SimpleExplosionDamageCalculator(false, true, Optional.ofNullable(null), Optional.ofNullable(null))
    }

    private val block : FallingBlockEntity? = createFallingBlock(this.level(), this.blockPosition().above().center, Blocks.POWDER_SNOW.defaultBlockState())

    init {
        item = Items.AIR.defaultInstance
        block?.dropItem = false
        this.setOwner(block)
        if(!(level as ServerLevel).gameRules.getBoolean(GameRules.RULE_MOBGRIEFING)) block?.disableDrop()
    }

    override fun baseTick() {
        super.baseTick()
        if(tickCount<=1) {
            block?.deltaMovement = deltaMovement
            block?.hasImpulse = true
            block?.startRiding(this)
        }
    }

    override fun tick() {

        super.tick()
        if(tickCount % 2 == 0)
        if (level is ServerLevel) {
            if (block!=null) level.sendParticles(
                ParticleTypes.SNOWFLAKE,
                block.x, block.y+0.5, block.z,
                3,
                0.1, 0.1, 0.1,
                0.02
            )
        }
        if (block !=null && (block.isRemoved || !block.isAlive)) {
            explode()
        }
    }

    override fun getDefaultItem(): Item = Items.AIR

    override fun onHitEntity(hitResult: EntityHitResult) {
        super.onHitEntity(hitResult)
        val entity = hitResult.entity
        val damage = 5
        entity.hurt(this.damageSources().thrown(this, this.getOwner()), damage.toFloat())
    }

    override fun onHit(hitResult: HitResult) {
        super.onHit(hitResult)
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, 3.toByte())
            explode()
        }
    }

    private fun explode() {
        if (level is ServerLevel) {
            if (block!=null) level.sendParticles(
                ParticleTypes.SNOWFLAKE,
                block.x, block.y, block.z,
                80,
                0.13, 0.13, 0.13,
                0.1
            )
        }
        discard()
        this.level()
            .explode(
                this,
                null,
                EXPLOSION_DAMAGE_CALCULATOR,
                x,
                y,
                z,
                1.0f,
                false,
                Level.ExplosionInteraction.MOB,
                ParticleTypes.SNOWFLAKE,
                ParticleTypes.SMOKE,
                PazSounds.SNOWCHUNK_HIT
            )
    }
}