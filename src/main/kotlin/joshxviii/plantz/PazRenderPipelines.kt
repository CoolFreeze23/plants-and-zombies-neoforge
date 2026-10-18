package joshxviii.plantz

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import joshxviii.plantz.integration.PazIrisCompat
import net.minecraft.Util
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.client.event.RegisterShadersEvent
import java.util.function.Function

/**
 * 26.1 `RenderPipeline`s ported to 1.21.1 [RenderType]s. Custom pipelines use core shaders under
 * `assets/plantz/shaders/core`, registered from [registerShaders] (`RegisterShadersEvent`).
 * Iris: 26.1 assigned `PAINT_OVERLAY` to Iris' entity program. On 1.21.1, [paintOverlay] switches to the vanilla
 * translucent-entity shader while a shader pack is active, which Iris replaces with that same entity program.
 * The other world render types already use vanilla shader states.
 */
object PazRenderPipelines {

    private var paintOverlayShader: ShaderInstance? = null
    private var headMarkerShader: ShaderInstance? = null
    private var headMarkerOutlineShader: ShaderInstance? = null

    @JvmField val PAINT_OVERLAY_SHADER = RenderStateShard.ShaderStateShard { paintOverlayShader }
    @JvmField val HEAD_MARKER_SHADER = RenderStateShard.ShaderStateShard { headMarkerShader }
    @JvmField val HEAD_MARKER_OUTLINE_SHADER = RenderStateShard.ShaderStateShard { headMarkerOutlineShader }

    /** For GUI code that draws with `RenderSystem.setShader`. Null until shaders have loaded. */
    @JvmStatic fun headMarkerShader(): ShaderInstance? = headMarkerShader
    @JvmStatic fun headMarkerOutlineShader(): ShaderInstance? = headMarkerOutlineShader

    fun registerShaders(event: RegisterShadersEvent) {
        event.registerShader(ShaderInstance(event.resourceProvider, pazResource("paint_overlay"), DefaultVertexFormat.NEW_ENTITY)) { paintOverlayShader = it }
        event.registerShader(ShaderInstance(event.resourceProvider, pazResource("head_marker"), DefaultVertexFormat.NEW_ENTITY)) { headMarkerShader = it }
        event.registerShader(ShaderInstance(event.resourceProvider, pazResource("head_marker_outline"), DefaultVertexFormat.NEW_ENTITY)) { headMarkerOutlineShader = it }
    }

    /** Texture unit the paint noise is bound to (Sampler3); units 1/2 are taken by the overlay and lightmap states. */
    const val PAINT_NOISE_UNIT = 3

    private class PaintNoiseShard(noise: ResourceLocation) : RenderStateShard.TexturingStateShard(
        "plantz_paint_noise",
        { RenderSystem.setShaderTexture(PAINT_NOISE_UNIT, noise) },
        { RenderSystem.setShaderTexture(PAINT_NOISE_UNIT, 0) }
    )

    /**
     * 26.1 `ELECTRIC_ARC`: additive, emissive, alpha-cutout entity pipeline (energy swirl without the texture scroll).
     */
    @JvmField
    val ELECTRIC_ARC: Function<ResourceLocation, RenderType> = Util.memoize { texture: ResourceLocation ->
        RenderType.create(
            "plantz_electric_arc", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENERGY_SWIRL_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
                .setTexturingState(RenderStateShard.OffsetTexturingStateShard(0f, 0f))
                .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .createCompositeState(false)
        )
    }

    /**
     * 26.1 `PAINT_OVERLAY`: the entity texture's alpha masks a noise texture tinted by the vertex colour
     * (translucent, view-offset layered, no cull). Shader: `plantz:paint_overlay`.
     */
    @JvmStatic
    fun paintOverlay(entityTexture: ResourceLocation, noiseTexture: ResourceLocation): RenderType =
        if (PazIrisCompat.isShaderPackInUse()) PAINT_OVERLAY_IRIS_TYPES.apply(entityTexture)
        else PAINT_OVERLAY_TYPES.apply(entityTexture to noiseTexture)

    /**
     * Shader-pack variant of the paint overlay: same states, but the vanilla translucent-entity shader, so Iris draws it
     * with its entity program. Like 26.1 under Iris, that program samples only the entity texture (tinted by the paint
     * colour) and not the noise.
     */
    private val PAINT_OVERLAY_IRIS_TYPES: Function<ResourceLocation, RenderType> = Util.memoize { texture: ResourceLocation ->
        RenderType.create(
            "plantz_paint_overlay_iris", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(true)
        )
    }

    private val PAINT_OVERLAY_TYPES: Function<Pair<ResourceLocation, ResourceLocation>, RenderType> = Util.memoize { (texture, noise) ->
        RenderType.create(
            "plantz_paint_overlay", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            RenderType.CompositeState.builder()
                .setShaderState(PAINT_OVERLAY_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
                .setTexturingState(PaintNoiseShard(noise))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(true)
        )
    }

    /**
     * 26.1 `HEAD_MARKER_BAKE` (TODO upstream): emissive alpha-cutout model rendering without overlay or
     * cardinal lighting. Shader: `plantz:head_marker`.
     */
    @JvmField
    val HEAD_MARKER_BAKE: Function<ResourceLocation, RenderType> = Util.memoize { texture: ResourceLocation ->
        RenderType.create(
            "plantz_head_marker_bake", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                .setShaderState(HEAD_MARKER_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .createCompositeState(false)
        )
    }

    /** 26.1 `HEAD_MARKER` (TODO upstream): plain textured GUI quad. */
    @JvmField
    val HEAD_MARKER: Function<ResourceLocation, RenderType> = Util.memoize { texture: ResourceLocation ->
        RenderType.create(
            "plantz_head_marker", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                .setShaderState(HEAD_MARKER_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .createCompositeState(false)
        )
    }

    /** 26.1 `HEAD_MARKER_OUTLINE`: draws the texture's silhouette in black. Shader: `plantz:head_marker_outline`. */
    @JvmField
    val HEAD_MARKER_OUTLINE: Function<ResourceLocation, RenderType> = Util.memoize { texture: ResourceLocation ->
        RenderType.create(
            "plantz_head_marker_outline", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                .setShaderState(HEAD_MARKER_OUTLINE_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .createCompositeState(false)
        )
    }

    /**
     * Sun quad used by Sun entities (26.1 `RenderPipelines.ENERGY_SWIRL` with lightmap/overlay, item-entity target).
     */
    @JvmStatic
    fun emissiveSun(texture: ResourceLocation): RenderType = RenderType.create(
        "plantz_sun", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_ENERGY_SWIRL_SHADER)
            .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
            .setTexturingState(RenderStateShard.OffsetTexturingStateShard(0f, 0f))
            .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
            .setCullState(RenderStateShard.NO_CULL)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setOverlayState(RenderStateShard.OVERLAY)
            .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
            .createCompositeState(true)
    )

    /**
     * Sun glow used by sun batteries / time machines (26.1 `RenderPipelines.BEACON_BEAM_TRANSLUCENT`:
     * translucent, no depth write, item-entity target). Uses the BLOCK vertex format of the beacon shader.
     */
    @JvmStatic
    fun translucentBeam(texture: ResourceLocation): RenderType = RenderType.create(
        "plantz_sun_beam", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1536, true, true,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER)
            .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
            .createCompositeState(true)
    )
}
