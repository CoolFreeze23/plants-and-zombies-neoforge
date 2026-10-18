package joshxviii.plantz.particles

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.TextureSheetParticle
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType
import joshxviii.plantz.compat.LightCoordsUtil

class PopParticle private constructor(
    level: ClientLevel, x: Double, y: Double, z: Double, xd: Double, yd: Double, zd: Double,
    private val sprites: SpriteSet
) : TextureSheetParticle(level, x, y, z, xd, yd, zd) {
    init { setSprite(sprites.first()) }

    init {
        lifetime = 2
        gravity = 0f
        quadSize = 0.75f
        hasPhysics = false
    }

    override fun tick() {
        xo = x
        yo = y
        zo = z

        if (age++ >= lifetime) {
            remove()
            return
        }

        setSpriteFromAge(sprites)
    }

    override fun getRenderType(): ParticleRenderType = ParticleRenderType.PARTICLE_SHEET_OPAQUE

    override fun getQuadSize(a: Float): Float {
        return quadSize
    }

    public override fun getLightColor(a: Float): Int {
        return LightCoordsUtil.addSmoothBlockEmission(super.getLightColor(a), 1.0f)
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
            return PopParticle(level, x, y, z, xAux, yAux, zAux, sprites)
        }
    }
}