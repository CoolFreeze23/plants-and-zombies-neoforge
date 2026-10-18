package joshxviii.plantz

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import joshxviii.plantz.compat.client.renderer.CompatRenderType
import joshxviii.plantz.integration.PazIrisCompat
import net.minecraft.Util
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.client.event.RegisterShadersEvent
import java.util.function.Function

/**
 * 26.1 `RenderPipeline`s as 1.21.1 [RenderType]s. The custom ones use the core shaders in `assets/plantz/shaders/core`,
 * registered by [registerShaders].
 * Iris: 26.1 assigned `PAINT_OVERLAY` to Iris' entity program. Iris on 1.21.1 replaces vanilla core shaders instead, so
 * [paintOverlay] uses the vanilla translucent-entity shader while a shader pack is active.
 */
object PazRenderPipelines {

    private var paintOverlayShader: ShaderInstance? = null
    private var headMarkerShader: ShaderInstance? = null
    private var headMarkerOutlineShader: ShaderInstance? = null

    @JvmField val PAINT_OVERLAY_SHADER = RenderStateShard.ShaderStateShard { paintOverlayShader }
    @JvmField val HEAD_MARKER_SHADER = RenderStateShard.ShaderStateShard { headMarkerShader }
    @JvmField val HEAD_MARKER_OUTLINE_SHADER = RenderStateShard.ShaderStateShard { headMarkerOutlineShader }

    fun registerShaders(event: RegisterShadersEvent) {
        event.registerShader(ShaderInstance(event.resourceProvider, pazResource("paint_overlay"), DefaultVertexFormat.NEW_ENTITY)) { paintOverlayShader = it }
        event.registerShader(ShaderInstance(event.resourceProvider, pazResource("head_marker"), DefaultVertexFormat.NEW_ENTITY)) { headMarkerShader = it }
        event.registerShader(ShaderInstance(event.resourceProvider, pazResource("head_marker_outline"), DefaultVertexFormat.NEW_ENTITY)) { headMarkerOutlineShader = it }
    }

    /** Texture unit of the paint noise (26.1 `Sampler1`); units 1 and 2 are the overlay and lightmap here. */
    private const val PAINT_NOISE_UNIT = 3

    private class PaintNoiseShard(noise: ResourceLocation) : RenderStateShard.TexturingStateShard(
        "plantz_paint_noise",
        { RenderSystem.setShaderTexture(PAINT_NOISE_UNIT, noise) },
        { RenderSystem.setShaderTexture(PAINT_NOISE_UNIT, 0) }
    )

    /** 26.1 `ELECTRIC_ARC`: additive, emissive, alpha-cutout entity pipeline. Unused upstream as well. */
    @JvmField
    val ELECTRIC_ARC: Function<ResourceLocation, RenderType> = Util.memoize { texture: ResourceLocation ->
        CompatRenderType.create(
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

    private val PAINT_OVERLAY_TYPES: Function<Pair<ResourceLocation, ResourceLocation>, RenderType> = Util.memoize { (texture, noise) ->
        CompatRenderType.create(
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
     * Shader-pack variant: same states with the vanilla translucent-entity shader, so Iris draws it with its entity
     * program. Like 26.1 under Iris, that program samples only the entity texture tinted by the paint colour.
     */
    private val PAINT_OVERLAY_IRIS_TYPES: Function<ResourceLocation, RenderType> = Util.memoize { texture: ResourceLocation ->
        CompatRenderType.create(
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

    /**
     * 26.1 `HEAD_MARKER_BAKE`: the zombie head on the raid HUD. Entity snippet with alpha cutout, emissive, no overlay,
     * no cardinal lighting, no cull (shader `plantz:head_marker`). Takes entity-format vertices (`ModelPart.render`).
     */
    @JvmField
    val HEAD_MARKER_BAKE: Function<ResourceLocation, RenderType> = Util.memoize { texture: ResourceLocation ->
        CompatRenderType.create(
            "plantz_head_marker_bake", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                .setShaderState(HEAD_MARKER_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
                .setCullState(RenderStateShard.NO_CULL)
                .createCompositeState(false)
        )
    }

    /** 26.1 `HEAD_MARKER` (GUI textured snippet, unused upstream as well). */
    @JvmField
    val HEAD_MARKER: Function<ResourceLocation, RenderType> = Util.memoize { texture: ResourceLocation ->
        CompatRenderType.create(
            "plantz_head_marker", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                .setShaderState(HEAD_MARKER_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .createCompositeState(false)
        )
    }

    /**
     * 26.1 `HEAD_MARKER_OUTLINE`: the texture's silhouette in black, drawn scaled up behind [HEAD_MARKER_BAKE] on the raid
     * HUD (shader `plantz:head_marker_outline`). Takes entity-format vertices (`ModelPart.render`).
     */
    @JvmField
    val HEAD_MARKER_OUTLINE: Function<ResourceLocation, RenderType> = Util.memoize { texture: ResourceLocation ->
        CompatRenderType.create(
            "plantz_head_marker_outline", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                .setShaderState(HEAD_MARKER_OUTLINE_SHADER)
                .setTextureState(RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .createCompositeState(false)
        )
    }

    /** Sun entity quad: 26.1 `RenderPipelines.ENERGY_SWIRL` render setup with lightmap and overlay, item-entity target. */
    @JvmStatic
    fun emissiveSun(texture: ResourceLocation): RenderType = CompatRenderType.create(
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
     * Sun glow of sun batteries and time machines: 26.1 `RenderPipelines.BEACON_BEAM_TRANSLUCENT` (translucent, no depth
     * write, item-entity target), with the beacon shader's BLOCK vertex format.
     */
    @JvmStatic
    fun translucentBeam(texture: ResourceLocation): RenderType = CompatRenderType.create(
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
