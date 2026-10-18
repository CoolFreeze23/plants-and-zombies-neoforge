package joshxviii.plantz.compat.client.renderer.layers;

import joshxviii.plantz.compat.client.model.EntityModel;
import joshxviii.plantz.compat.client.state.EntityRenderState;

public interface RenderLayerParent<S extends EntityRenderState, M extends EntityModel<? super S>> {
    M getModel();
}
