package joshxviii.plantz.particles

import joshxviii.plantz.NukeBlastParticleOptions
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.TextureSheetParticle
import net.minecraft.client.particle.SpriteSet
import kotlin.math.floor

class NukeBlastParticle(
    level: ClientLevel, x: Double, y: Double, z: Double, xa: Double, ya: Double, za: Double,
    private val scale: Float,
    private val sprites: SpriteSet
) : TextureSheetParticle(level, x, y, z, 0.0, 0.0, 0.0) {
    init { setSprite(sprites.first()) }

    init {
        this.quadSize = scale
        this.lifetime = (12 + (floor(this.quadSize / 5))).toInt()
        this.setParticleSpeed(xa, ya, za)
    }

    override fun tick() {
        super.tick()
        this.setSpriteFromAge(sprites);
    }

    override fun getRenderType(): ParticleRenderType = ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT

    override fun getLightColor(tint: Float): Int = 15728880

    class Provider(private val sprite: SpriteSet) : ParticleProvider<NukeBlastParticleOptions> {
        override fun createParticle(
            options: NukeBlastParticleOptions,
            level: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xAux: Double,
            yAux: Double,
            zAux: Double
        ): Particle {
            val particle = NukeBlastParticle(level, x, y, z, xAux, yAux, zAux, options.scale, sprite)
            val color = options.getColor()
            particle.setColor(color.x, color.y, color.z)
            return particle
        }
    }
}