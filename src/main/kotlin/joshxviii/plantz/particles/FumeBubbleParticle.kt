package joshxviii.plantz.particles

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.TextureSheetParticle
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType
import kotlin.math.pow

class FumeBubbleParticle private constructor(
    level: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    xa: Double,
    ya: Double,
    za: Double,
    private val sprites: SpriteSet
) : TextureSheetParticle(level, x, y, z) {
    init { setSprite(sprites.first()) }
    init {
        lifetime = 8
        gravity = 0.008f
        xd = xa
        yd = ya
        zd = za
        setSpriteFromAge(sprites)
    }

    override fun tick() {
        xo = x
        yo = y
        zo = z
        if (age++ >= lifetime) remove()
        else {
            yd = yd - gravity
            move(xd, yd, zd)

            updateEasedSprite()
        }
    }

    override fun getRenderType(): ParticleRenderType = ParticleRenderType.PARTICLE_SHEET_OPAQUE

    private fun updateEasedSprite() {
        if (removed) return

        val progress = age.toFloat() / lifetime.toFloat()
        val eased = progress.pow(3.7f)// set easeIn here

        val frameIndex = (eased * (lifetime - 1)).toInt().coerceIn(0, lifetime - 1)
        setSprite(sprites.get(frameIndex, lifetime))
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
            return FumeBubbleParticle(level, x, y, z, xAux, yAux, zAux, sprites)
        }
    }
}