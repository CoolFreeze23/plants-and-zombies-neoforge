package joshxviii.plantz.compat.client.model;

import joshxviii.plantz.compat.client.state.EntityRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public abstract class EntityModel<T extends EntityRenderState> extends Model<T> {
    public static final float MODEL_Y_OFFSET = -1.501F;

    protected EntityModel(ModelPart root) {
        this(root, RenderType::entityCutout);
    }

    protected EntityModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        super(root, renderType);
    }
}
