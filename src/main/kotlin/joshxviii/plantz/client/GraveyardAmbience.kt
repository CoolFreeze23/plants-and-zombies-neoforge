package joshxviii.plantz.client

import joshxviii.plantz.PazWorldGen
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.FogRenderer
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.material.FogType
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.client.event.ViewportEvent
import org.joml.Vector3f

/**
 * The graveyard biome's 26.1 environment attributes that 1.21.1 biome JSON can't hold: sky fog end distance 64,
 * water fog start 4, cloud colour, sunrise colour, sky light colour/factor and ambient light colour.
 * [blend] is how much graveyard surrounds the camera (sampled like vanilla's biome fog blending, eased over time),
 * so the effects fade in and out at biome borders like 26.1's interpolated attributes.
 */
object GraveyardAmbience {
    private const val SKY_FOG_END = 64f
    private const val WATER_FOG_START = 4f
    private val CLOUD_COLOR = rgb(7437954)
    private val SUNRISE_COLOR = rgb(10165001)
    private val SKY_LIGHT_COLOR = rgb(0xB694D1)
    private const val SKY_LIGHT_FACTOR = 0.7f
    private val AMBIENT_LIGHT_COLOR = rgb(0x221D29)

    @JvmStatic
    var blend: Float = 0f
        private set

    private fun rgb(color: Int) = Vector3f((color shr 16 and 0xFF) / 255f, (color shr 8 and 0xFF) / 255f, (color and 0xFF) / 255f)

    fun tick() {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level
        val camera = minecraft.gameRenderer.mainCamera
        if (level == null || !camera.isInitialized) {
            blend = 0f
            return
        }
        val center = camera.blockPosition
        var inGraveyard = 0
        for (dx in -2..2) for (dz in -2..2) {
            if (level.getBiome(BlockPos(center.x + dx * 4, center.y, center.z + dz * 4)).`is`(PazWorldGen.GRAVEYARD)) inGraveyard++
        }
        blend += (inGraveyard / 25f - blend) * 0.05f
        if (blend < 0.001f) blend = 0f
    }

    fun onRenderFog(event: ViewportEvent.RenderFog) {
        if (blend <= 0f) return
        when {
            event.mode == FogRenderer.FogMode.FOG_SKY && event.type == FogType.NONE -> {
                event.farPlaneDistance = Mth.lerp(blend, event.farPlaneDistance, minOf(event.farPlaneDistance, SKY_FOG_END))
                event.isCanceled = true
            }
            event.type == FogType.WATER -> {
                event.nearPlaneDistance = Mth.lerp(blend, event.nearPlaneDistance, WATER_FOG_START)
                event.isCanceled = true
            }
        }
    }

    @JvmStatic
    fun cloudColor(vanilla: Vec3): Vec3 {
        if (blend <= 0f) return vanilla
        // 26.1 swaps the cloud base colour (white in vanilla) for the biome's; the day/rain/thunder shading stays
        return vanilla.lerp(vanilla.multiply(CLOUD_COLOR.x.toDouble(), CLOUD_COLOR.y.toDouble(), CLOUD_COLOR.z.toDouble()), blend.toDouble())
    }

    @JvmStatic
    fun sunriseColor(vanilla: FloatArray?): FloatArray? {
        if (vanilla == null || blend <= 0f) return vanilla
        return floatArrayOf(
            Mth.lerp(blend, vanilla[0], SUNRISE_COLOR.x),
            Mth.lerp(blend, vanilla[1], SUNRISE_COLOR.y),
            Mth.lerp(blend, vanilla[2], SUNRISE_COLOR.z),
            vanilla[3]
        )
    }

    /**
     * 26.1's lightmap is `ambient + skyLightColor * skyBrightness + blockLight`. 1.21.1 builds the same pixel with a
     * fixed blue-white sky colour, so swap that sky term for the graveyard's (scaled by its sky light factor) and add
     * the ambient colour.
     */
    @JvmStatic
    fun adjustLightmap(skyDarken: Float, skyLight: Float, color: Vector3f) {
        if (blend <= 0f) return
        val vanillaSky = Vector3f(skyDarken, skyDarken, 1.0f).lerp(Vector3f(1.0f, 1.0f, 1.0f), 0.35f).mul(skyLight)
        val graveyardSky = Vector3f(SKY_LIGHT_COLOR).mul(skyLight * SKY_LIGHT_FACTOR)
        val target = Vector3f(color).sub(vanillaSky).add(graveyardSky).add(AMBIENT_LIGHT_COLOR)
        target.set(Mth.clamp(target.x, 0f, 1f), Mth.clamp(target.y, 0f, 1f), Mth.clamp(target.z, 0f, 1f))
        color.lerp(target, blend)
    }
}
