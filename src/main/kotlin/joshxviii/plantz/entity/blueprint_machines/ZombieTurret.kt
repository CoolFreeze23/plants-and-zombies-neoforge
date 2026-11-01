package joshxviii.plantz.entity.blueprint_machines

import joshxviii.plantz.PazDamageTypes
import joshxviii.plantz.PazSounds
import joshxviii.plantz.ai.goal.ProjectileAttackGoal
import joshxviii.plantz.entity.projectile.LaserBullet
import joshxviii.plantz.entity.zombie.ZombieRobot
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec2

class ZombieTurret(type: EntityType<out ZombieTurret>, level: Level) : ZombieRobot(type, level) {

    override fun tick() {
        super.tick()

        if (actionTime>0) {
            actionAnimation.startIfStopped(tickCount)
            if (actionTime++>14) {
                actionAnimation.stop()
                actionTime=0
            }
        }
    }

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(1, ProjectileAttackGoal(
            usingEntity = this,
            velocity = 1.1,
            actionDelay = 9,
            inaccuracy = 5.2f,
            leadShots = false,
            soundEvent = PazSounds.ZOMBIE_TURRET_SHOOT,
            projectileFactory = {
                LaserBullet(
                    level(),
                    this,
                    spawnOffset = Vec2(0.2f, 0.4f),
                    damageType = if (owner is Player) PazDamageTypes.ENERGY else PazDamageTypes.ZOMBIE_ENERGY,
                )
            },
            usePredicate = { actionTime<=0 },
            actionStartEffect = { actionTime=1 },
        ))
    }

}