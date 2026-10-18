package joshxviii.plantz.compat.client.animation;

import joshxviii.plantz.compat.AnimationState;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backport of the 1.21.2+ baked keyframe animation. 26.1 code calls {@code definition.bake(root)};
 * here that is {@code KeyframeAnimation.bake(definition, root)}.
 */
public class KeyframeAnimation {
    private final AnimationDefinition definition;
    private final List<Entry> entries;

    private KeyframeAnimation(AnimationDefinition definition, List<Entry> entries) {
        this.definition = definition;
        this.entries = entries;
    }

    public static KeyframeAnimation bake(AnimationDefinition definition, ModelPart root) {
        Map<String, ModelPart> parts = createPartLookup(root);
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
            String partName = entry.getKey();
            ModelPart part = parts.get(partName);
            if (part == null) {
                throw new IllegalArgumentException("Cannot animate " + partName + ", which does not exist in model");
            }
            for (AnimationChannel channel : entry.getValue()) {
                entries.add(new Entry(part, channel.target(), channel.keyframes()));
            }
        }
        return new KeyframeAnimation(definition, List.copyOf(entries));
    }

    private static Map<String, ModelPart> createPartLookup(ModelPart root) {
        Map<String, ModelPart> lookup = new HashMap<>();
        Deque<ModelPart> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            ModelPart part = queue.poll();
            for (Map.Entry<String, ModelPart> child : part.children.entrySet()) {
                lookup.putIfAbsent(child.getKey(), child.getValue());
                queue.add(child.getValue());
            }
        }
        return lookup;
    }

    public void applyStatic() {
        this.apply(0L, 1.0F);
    }

    public void applyWalk(float animationPos, float animationSpeed, float speedFactor, float scaleFactor) {
        long time = (long) (animationPos * 50.0F * speedFactor);
        float scale = Math.min(animationSpeed * scaleFactor, 1.0F);
        this.apply(time, scale);
    }

    public void apply(AnimationState animationState, float currentTime) {
        this.apply(animationState, currentTime, 1.0F);
    }

    public void apply(AnimationState animationState, float currentTime, float speedFactor) {
        animationState.ifStarted(state -> this.apply((long) ((float) state.getTimeInMillis(currentTime) * speedFactor), 1.0F));
    }

    public void apply(long millisSinceStart, float targetScale) {
        float secondsSinceStart = this.getElapsedSeconds(millisSinceStart);
        Vector3f scratchVector = new Vector3f();
        for (Entry entry : this.entries) {
            entry.apply(secondsSinceStart, targetScale, scratchVector);
        }
    }

    private float getElapsedSeconds(long millisSinceStart) {
        float secondsSinceStart = (float) millisSinceStart / 1000.0F;
        return this.definition.looping() ? secondsSinceStart % this.definition.lengthInSeconds() : secondsSinceStart;
    }

    private record Entry(ModelPart part, AnimationChannel.Target target, Keyframe[] keyframes) {
        public void apply(float secondsSinceStart, float targetScale, Vector3f scratchVector) {
            int prev = Math.max(0, Mth.binarySearch(0, this.keyframes.length, i -> secondsSinceStart <= this.keyframes[i].timestamp()) - 1);
            int next = Math.min(this.keyframes.length - 1, prev + 1);
            Keyframe previousFrame = this.keyframes[prev];
            Keyframe nextFrame = this.keyframes[next];
            float keyframeTimeDelta = secondsSinceStart - previousFrame.timestamp();
            float lerpAlpha;
            if (next != prev) {
                lerpAlpha = Mth.clamp(keyframeTimeDelta / (nextFrame.timestamp() - previousFrame.timestamp()), 0.0F, 1.0F);
            } else {
                lerpAlpha = 0.0F;
            }
            nextFrame.interpolation().apply(scratchVector, lerpAlpha, this.keyframes, prev, next, targetScale);
            this.target.apply(this.part, scratchVector);
        }
    }
}
