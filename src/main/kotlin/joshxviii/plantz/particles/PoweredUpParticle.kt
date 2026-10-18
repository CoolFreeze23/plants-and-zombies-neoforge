package joshxviii.plantz.particles

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.RisingParticle
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType
import joshxviii.plantz.compat.LightCoordsUtil

class PoweredUpParticle private constructor(
    level: ClientLevel, x: Double, y: Double, z: Double, xd: Double, yd: Double, zd: Double,
    private val sprites: SpriteSet
) : RisingParticle(level, x, y, z, xd, yd, zd) {
    init { setSprite(sprites.first()) }

    init {
        quadSize = 0.2f
        alpha = 0.5f
        lifetime = random.nextInt(4, 9)
        setSpriteFromAge(sprites)
    }

    override fun tick() {
        xo = x
        yo = y
        zo = z
        xd = 0.0
        zd = 0.0
        yd += 0.001
        move(xd, yd, zd)
        setSpriteFromAge(sprites)
        if (age++ >= lifetime) remove()
    }

    override fun getRenderType(): ParticleRenderType = ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT

    override fun move(xa: Double, ya: Double, za: Double) {
        this.boundingBox = this.boundingBox.move(xa, ya, za)
        this.setLocationFromBoundingbox()
    }

    override fun getQuadSize(a: Float): Float {
        return this.quadSize
    }

    public override fun getLightColor(a: Float): Int {
        return LightCoordsUtil.addSmoothBlockEmission(super.getLightColor(a), 1.0f - (this.age + a) / this.lifetime)
    }

    class Provider(private val sprites: SpriteSet) : ParticleProvider<SimpleParticleType> {
        override fun createParticle(
            options: SimpleParticleType,
            level: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xAux: Double,
            yAux: Double,
            zAux: Double
        ): Particle {
            return PoweredUpParticle(level, x, y, z, xAux, yAux, zAux, sprites)
        }
    }
}