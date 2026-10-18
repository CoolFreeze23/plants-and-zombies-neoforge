package joshxviii.plantz.integration

import net.irisshaders.iris.api.v0.IrisApi
import net.neoforged.fml.ModList

/**
 * Iris integration (client only). Upstream 26.1 assigned its paint overlay pipeline to Iris' entity program with
 * `IrisApi.assignPipeline`. Iris for 1.21.1 swaps vanilla core shaders instead, so while a shader pack is active the
 * mod renders through vanilla shader states that Iris already replaces (see `PazRenderPipelines.paintOverlay`).
 */
object PazIrisCompat {
    @JvmField val LOADED: Boolean = ModList.get().isLoaded("iris")

    /** True while an Iris shader pack is in use. Always false without Iris. */
    @JvmStatic
    fun isShaderPackInUse(): Boolean = LOADED && IrisAccess.isShaderPackInUse()

    /** True while Iris renders its shadow pass. Always false without Iris. */
    @JvmStatic
    fun isRenderingShadowPass(): Boolean = LOADED && IrisAccess.isRenderingShadowPass()
}

/** Only touched when Iris is loaded, so its classes are never resolved without it. */
private object IrisAccess {
    fun isShaderPackInUse(): Boolean = IrisApi.getInstance().isShaderPackInUse
    fun isRenderingShadowPass(): Boolean = IrisApi.getInstance().isRenderingShadowPass
}
