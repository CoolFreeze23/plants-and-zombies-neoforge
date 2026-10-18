package joshxviii.plantz.particles

import com.mojang.blaze3d.vertex.VertexConsumer
import joshxviii.plantz.pazResource
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3

/**
 * 26.1 `ParticleGroup<ElectricArcParticle>` render half. 1.21.1 has no particle groups, so
 * [ElectricArcParticle.render] builds the state and calls [render], which draws immediately (see [CustomGeometryParticles]).
 */
object ElectricArcParticleGroup {
    // 26.1 RenderPipelines.BEACON_BEAM_TRANSLUCENT == 1.21.1 RenderType.beaconBeam(texture, true)
    val RENDER_TYPE: RenderType = RenderType.beaconBeam(pazResource("textures/particle/electric_arc.png"), true)

    @JvmRecord
    data class ElectricArcParticleRenderState(
        val startPos: Vec3,
        val targetPos: Vec3,
        val thickness: Float,
        val color: Int,
        val alpha: Float,
        val age: Int,
        val lifetime: Int,
        val random: RandomSource
    )

    fun render(state: ElectricArcParticleRenderState) {
        CustomGeometryParticles.draw(RENDER_TYPE) { buffer -> renderElectricArc(buffer, state) }
    }

    private fun renderElectricArc(buffer: VertexConsumer, state: ElectricArcParticleRenderState) {
        val ageFactor = 1-(state.age.toFloat() / state.lifetime)
        val mainAlpha = state.alpha * ageFactor
        val width = state.thickness * ageFactor

        renderSegmentedArc(buffer, state, width, mainAlpha, 10, 1.05f, randomInt = state.random.nextFloat())

        if (ageFactor < 0.75f) renderSegmentedArc(buffer, state, width * 0.55f, mainAlpha, 8, 1.35f, 0.08, state.random.nextFloat())
        if (ageFactor < 0.74f) renderSegmentedArc(buffer, state, width * 0.35f, mainAlpha, 7, 1.6f, -0.10, state.random.nextFloat())
    }

    private fun renderSegmentedArc(
        buffer: VertexConsumer,
        state: ElectricArcParticleRenderState,
        width: Float,
        alpha: Float,
        segments: Int,
        jitterMultiplier: Float = 1.0f,
        yOffset: Double = 0.0,
        randomInt: Float = 0f
    ) {
        var current = state.startPos
        val dir = state.targetPos.subtract(state.startPos)
        val twist = Mth.TWO_PI * randomInt

        for (i in 0 until segments) {
            val t = (i + 1.0) / segments
            var next = state.startPos.add(dir.scale(t))

            if (i in 1 until segments - 1) {
                val progress = i.toDouble() / segments
                val jitter = 0.42 * jitterMultiplier * (1.0 - progress * 0.4) * (1.0 - state.age.toDouble() / state.lifetime)

                val rnd = RandomSource.create((state.age * 31 + i * 19).toLong())

                next = next.add(
                    rnd.nextDouble() * jitter - jitter * 0.5,
                    rnd.nextDouble() * jitter * 0.8 - jitter * 0.35 + yOffset,
                    rnd.nextDouble() * jitter - jitter * 0.5
                )
            }

            drawSegment(buffer, current, next, width, state.color, alpha, twist)
            current = next
        }
    }

    private fun drawSegment(
        buffer: VertexConsumer,
        from: Vec3,
        to: Vec3,
        width: Float,
        color: Int,
        alpha: Float,
        twist: Float
    ) {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f

        val hw = width * 0.5

        repeat(2) {
            val diff = to.subtract(from).normalize()
            val neg = if (it == 0) 1 else -1
            var perp = Vec3(-diff.z * neg, 0.0, diff.x * neg).normalize()
            perp = perp.zRot(twist)

            val p1 = from.add(perp.scale(hw))
            val p2 = from.subtract(perp.scale(hw))
            val p3 = to.subtract(perp.scale(hw))
            val p4 = to.add(perp.scale(hw))

            vertex(buffer, p1, r, g, b, alpha)
            vertex(buffer, p2, r, g, b, alpha)
            vertex(buffer, p3, r, g, b, alpha)
            vertex(buffer, p4, r, g, b, alpha)
        }
    }

    private fun vertex(buffer: VertexConsumer, pos: Vec3, r: Float, g: Float, b: Float, a: Float) {
        buffer.addVertex(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
            .setColor(r, g, b, a)
            .setUv(0f, 0f)
            .setUv1(0, 0)
            .setNormal(0f, 1f, 0f)
            .setLight(15728880)
    }
}
