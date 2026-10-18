package joshxviii.plantz.compat.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;

/** 26.1 {@code HeadedModel} (with the {@code translateToHead} default). */
public interface HeadedModel {
    ModelPart getHead();

    default void translateToHead(PoseStack poseStack) {
        this.getHead().translateAndRotate(poseStack);
    }
}
