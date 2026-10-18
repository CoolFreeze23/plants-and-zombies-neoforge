package joshxviii.plantz.particles

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureManager

/** 26.1 `SpriteSet.first()`. */
fun SpriteSet.first(): TextureAtlasSprite = get(0, 1)

/**
 * Stand-in for 26.1 particle groups that submit custom geometry (beams, electric arcs).
 * The particle engine batches particles per [ParticleRenderType]; this type hands out an empty buffer
 * so the engine still calls `Particle.render`, and each particle draws its own geometry immediately
 * with real [RenderType]s through [CustomGeometryParticles.draw].
 */
class CustomGeometryParticleRenderType(private val name: String) : ParticleRenderType {
    override fun begin(tesselator: Tesselator, textureManager: TextureManager): BufferBuilder =
        tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE)

    override fun isTranslucent(): Boolean = true

    override fun toString(): String = name
}

object CustomGeometryParticles {
    private val byteBuffer by lazy { ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE) }
    private val bufferSource by lazy { MultiBufferSource.immediate(byteBuffer) }

    /** Draws [geometry] (camera-relative world coordinates) right now with [renderType]. */
    fun draw(renderType: RenderType, geometry: (VertexConsumer) -> Unit) {
        geometry(bufferSource.getBuffer(renderType))
        bufferSource.endBatch(renderType)
    }
}
