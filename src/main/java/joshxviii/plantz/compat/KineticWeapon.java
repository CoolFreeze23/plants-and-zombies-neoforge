package joshxviii.plantz.compat;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The parts of 26.1's {@code minecraft:kinetic_weapon} and {@code minecraft:swing_animation} item components that the
 * spear AI and animations use. 1.21.1 has neither; a spear backport (Backported Spears) registers both under the same
 * ids with the same record accessors, so they are read reflectively and nothing links against the backport.
 */
public record KineticWeapon(int delayTicks, int dismountTicks, int knockbackTicks, int damageTicks, float forwardMovement) {
    private static final ResourceLocation KINETIC_WEAPON_ID = ResourceLocation.withDefaultNamespace("kinetic_weapon");
    private static final ResourceLocation SWING_ANIMATION_ID = ResourceLocation.withDefaultNamespace("swing_animation");

    /** 26.1 {@code KineticWeapon.computeDamageUseDuration()}. */
    public int computeDamageUseDuration() {
        return this.delayTicks + this.damageTicks;
    }

    @Nullable
    public static KineticWeapon of(ItemStack stack) {
        DataComponentType<?> type = Components.KINETIC_WEAPON;
        Object component = type == null || stack.isEmpty() ? null : stack.get(type);
        if (component == null) return null;
        Accessors a = Components.ACCESSORS.get(component.getClass()).orElse(null);
        if (a == null) return null;
        try {
            return new KineticWeapon(
                (Integer) a.delayTicks.invoke(component),
                maxDuration(a.dismountConditions.invoke(component)),
                maxDuration(a.knockbackConditions.invoke(component)),
                maxDuration(a.damageConditions.invoke(component)),
                (Float) a.forwardMovement.invoke(component));
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    /** 26.1 {@code ItemStack.getSwingAnimation().type() == SwingAnimationType.STAB}. */
    public static boolean isStab(ItemStack stack) {
        DataComponentType<?> type = Components.SWING_ANIMATION;
        Object component = type == null || stack.isEmpty() ? null : stack.get(type);
        if (component == null) return false;
        try {
            Object swingType = component.getClass().getMethod("swingType").invoke(component);
            return "stab".equalsIgnoreCase(String.valueOf(swingType));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /** 26.1 {@code LivingEntity.getTicksSinceLastKineticHitFeedback(partialTicks)}; large when the entity never hit. */
    public static float ticksSinceLastKineticHitFeedback(Entity entity, float partialTicks) {
        Method method = Components.timeSinceKineticAttack(entity.getClass());
        if (method == null) return Float.MAX_VALUE;
        try {
            return (Float) method.invoke(entity, partialTicks);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return Float.MAX_VALUE;
        }
    }

    private record Accessors(Method delayTicks, Method dismountConditions, Method knockbackConditions, Method damageConditions, Method forwardMovement) {
    }

    private static int maxDuration(Object optionalCondition) throws ReflectiveOperationException {
        if (!(optionalCondition instanceof Optional<?> optional) || optional.isEmpty()) return 0;
        Object condition = optional.get();
        return (Integer) condition.getClass().getMethod("maxDurationTicks").invoke(condition);
    }

    /** Looked up on first use, after registration. */
    private static final class Components {
        @Nullable
        static final DataComponentType<?> KINETIC_WEAPON = BuiltInRegistries.DATA_COMPONENT_TYPE.get(KINETIC_WEAPON_ID);
        @Nullable
        static final DataComponentType<?> SWING_ANIMATION = BuiltInRegistries.DATA_COMPONENT_TYPE.get(SWING_ANIMATION_ID);
        static final ClassValue<Optional<Accessors>> ACCESSORS = new ClassValue<>() {
            @Override
            protected Optional<Accessors> computeValue(Class<?> type) {
                try {
                    return Optional.of(new Accessors(type.getMethod("delayTicks"), type.getMethod("dismountConditions"),
                        type.getMethod("knockbackConditions"), type.getMethod("damageConditions"), type.getMethod("forwardMovement")));
                } catch (NoSuchMethodException e) {
                    return Optional.empty();
                }
            }
        };
        private static final ClassValue<Optional<Method>> TIME_SINCE_KINETIC_ATTACK = new ClassValue<>() {
            @Override
            protected Optional<Method> computeValue(Class<?> type) {
                try {
                    return Optional.of(type.getMethod("getTimeSinceLastKineticAttack", float.class));
                } catch (NoSuchMethodException e) {
                    return Optional.empty();
                }
            }
        };

        @Nullable
        static Method timeSinceKineticAttack(Class<?> type) {
            return TIME_SINCE_KINETIC_ATTACK.get(type).orElse(null);
        }
    }
}
