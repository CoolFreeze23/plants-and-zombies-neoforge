package joshxviii.plantz.compat.client.renderer;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * {@link RenderType#create} typed as {@link RenderType}: its declared return type (CompositeRenderType) is not visible
 * outside its package, which Kotlin flags when it infers it (KTLC-14).
 */
public final class CompatRenderType {
    private CompatRenderType() {
    }

    public static RenderType create(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                    boolean affectsCrumbling, boolean sortOnUpload, RenderType.CompositeState state) {
        return RenderType.create(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, state);
    }
}
