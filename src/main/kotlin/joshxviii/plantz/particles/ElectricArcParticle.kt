package joshxviii.plantz.particles
import com.mojang.blaze3d.vertex.VertexConsumer
import joshxviii.plantz.ElectricArcParticleOptions
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

class ElectricArcParticle private constructor(
    world: ClientLevel,
    x: Double, y: Double, z: Double,
    val targetPos: Vec3,
    var thickness: Float,
    val color: Int,
) : Particle(world, x, y, z) {
    var startPos: Vec3 = Vec3(x, y, z)
    val particleAge: Int
        get() = (age)

    init {
        lifetime = 6 + world.random.nextInt(4)
        hasPhysics = false
        alpha = 1.0f
    }

    override fun tick() {
        super.tick()
    }

    // 26.1 ParticleGroup extract + submit, drawn per particle (see ElectricArcParticleGroup)
    override fun render(buffer: VertexConsumer, camera: Camera, partialTicks: Float) {
        val cameraPos = camera.position
        ElectricArcParticleGroup.render(
            ElectricArcParticleGroup.ElectricArcParticleRenderState(
                startPos = startPos.subtract(cameraPos),
                targetPos = Vec3(targetPos.x, targetPos.y, targetPos.z).subtract(cameraPos),
                thickness = thickness,
                color = color,
                alpha = alpha,
                age = particleAge,
                lifetime = lifetime,
                random = RandomSource.create(hashCode().toLong())
            )
        )
    }

    override fun getRenderType(): ParticleRenderType = PazParticles.ELECTRIC_ARC

    override fun getRenderBoundingBox(partialTicks: Float): AABB = AABB(startPos, targetPos).inflate(1.0)

    override fun getLightColor(a: Float): Int = LightCoordsUtil.addSmoothBlockEmission(super.getLightColor(a), 1.0f)

    class Provider : ParticleProvider<ElectricArcParticleOptions> {
        override fun createParticle(
            options: ElectricArcParticleOptions,
            level: ClientLevel,
            x: Double, y: Double, z: Double,
            vx: Double, vy: Double, vz: Double
        ): Particle {
            return ElectricArcParticle(
                level, x, y, z, options.targetPos, options.width, options.color
            )
        }
    }
}
