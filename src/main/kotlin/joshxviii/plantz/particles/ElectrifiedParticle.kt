package joshxviii.plantz.particles

import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.TextureSheetParticle
import net.minecraft.client.particle.SpriteSet
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.core.particles.SimpleParticleType
import joshxviii.plantz.compat.LightCoordsUtil
import net.minecraft.util.Mth
import org.joml.Quaternionf


class ElectrifiedParticle private constructor(
    world: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    private val sprites: SpriteSet
) : TextureSheetParticle(world, x, y, z) {
    init { setSprite(sprites.first()) }
    init {
        this.lifetime = 6 + this.random.nextInt(3)
        this.setParticleSpeed(0.0, 0.0, 0.0)
        this.scale(1.25f)
        oRoll = random.nextFloat() * Mth.TWO_PI
    }

    override fun render(buffer: VertexConsumer, camera: Camera, partialTickTime: Float) {
        val quaternion = Quaternionf()
        facingCameraMode.setRotation(quaternion, camera, partialTickTime)
        quaternion.rotateZ(oRoll)
        this.renderRotatedQuad(buffer, camera, quaternion, partialTickTime)
    }

    override fun tick() {
        if (this.age++ >= this.lifetime) {
            this.remove()
        } else {
            setSprite(sprites.get(random))
        }
    }



    override fun getRenderType(): ParticleRenderType = ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT

    override fun getLightColor(a: Float): Int {
        return LightCoordsUtil.addSmoothBlockEmission(super.getLightColor(a), 1.0f)
    }

    class Provider(val sprites: SpriteSet) : ParticleProvider<SimpleParticleType> {
        override fun createParticle(
            parameters: SimpleParticleType,
            world: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            velocityX: Double,
            velocityY: Double,
            velocityZ: Double
        ): Particle {
            return ElectrifiedParticle(world, x, y, z, sprites)
        }
    }
}