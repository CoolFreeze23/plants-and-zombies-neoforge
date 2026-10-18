package joshxviii.plantz.particles
import com.mojang.blaze3d.vertex.VertexConsumer
import joshxviii.plantz.BeamParticleOptions
import joshxviii.plantz.PazParticles
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import joshxviii.plantz.compat.LightCoordsUtil
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

class BeamParticle private constructor(
    val world: ClientLevel,
    x: Double, y: Double, z: Double,
    val targetPos: Vec3,
    var width: Float,
    val color: Int,
    val lifeTime: Int = 1
) : Particle(world, x, y, z) {
    var startPos: Vec3 = Vec3(x, y, z)
    val particleAge: Int
        get() = (age)
    var gameTime: Long = 0

    init {
        lifetime = lifeTime
        hasPhysics = false
        alpha = 1.0f
    }

    override fun tick() {
        super.tick()
        gameTime = world.gameTime
    }

    // 26.1 ParticleGroup extract + submit, drawn per particle (see BeamParticleGroup)
    override fun render(buffer: VertexConsumer, camera: Camera, partialTicks: Float) {
        val cameraPos = camera.position
        BeamParticleGroup.render(
            BeamParticleGroup.BeamParticleRenderState(
                startPos = startPos.subtract(cameraPos),
                targetPos = Vec3(targetPos.x, targetPos.y, targetPos.z).subtract(cameraPos),
                width = width,
                color = color,
                alpha = alpha,
                age = particleAge,
                lifetime = lifetime,
                gameTime = gameTime.toFloat(),
                partialTicks = partialTicks,
                random = RandomSource.create(hashCode().toLong())
            )
        )
    }

    override fun getRenderType(): ParticleRenderType = PazParticles.BEAM

    override fun getRenderBoundingBox(partialTicks: Float): AABB = AABB(startPos, targetPos).inflate(width.toDouble())

    override fun getLightColor(a: Float): Int = LightCoordsUtil.addSmoothBlockEmission(super.getLightColor(a), 1.0f)

    class Provider : ParticleProvider<BeamParticleOptions> {
        override fun createParticle(
            options: BeamParticleOptions,
            level: ClientLevel,
            x: Double, y: Double, z: Double,
            vx: Double, vy: Double, vz: Double
        ): Particle {
            return BeamParticle(
                level, x, y, z, options.targetPos, options.width, options.color, options.lifeTime
            )
        }
    }
}
