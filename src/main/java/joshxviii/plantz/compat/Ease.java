package joshxviii.plantz.compat;

import net.minecraft.util.Mth;

/** 26.1 {@code net.minecraft.util.Ease} (the curves the spear animations use). */
public final class Ease {
    private Ease() {
    }

    public static float inOutSine(float x) {
        return -(Mth.cos((float) Math.PI * x) - 1.0F) / 2.0F;
    }

    public static float outBack(float x) {
        return 1.0F + 2.70158F * cube(x - 1.0F) + 1.70158F * Mth.square(x - 1.0F);
    }

    public static float outQuart(float x) {
        return 1.0F - Mth.square(Mth.square(1.0F - x));
    }

    public static float outCubic(float x) {
        return 1.0F - cube(1.0F - x);
    }

    public static float inOutExpo(float x) {
        if (x < 0.5F) {
            return x == 0.0F ? 0.0F : (float) (Math.pow(2.0, 20.0 * (double) x - 10.0) / 2.0);
        } else {
            return x == 1.0F ? 1.0F : (float) ((2.0 - Math.pow(2.0, -20.0 * (double) x + 10.0)) / 2.0);
        }
    }

    public static float inQuad(float x) {
        return x * x;
    }

    public static float outCirc(float x) {
        return (float) Math.sqrt(1.0F - Mth.square(x - 1.0F));
    }

    public static float inOutElastic(float x) {
        if (x == 0.0F) {
            return 0.0F;
        } else if (x == 1.0F) {
            return 1.0F;
        } else {
            double sin = Math.sin((20.0 * (double) x - 11.125) * (float) Math.PI * 4.0F / 9.0F);
            return x < 0.5F ? (float) (-(Math.pow(2.0, 20.0 * (double) x - 10.0) * sin) / 2.0) : (float) (Math.pow(2.0, -20.0 * (double) x + 10.0) * sin / 2.0 + 1.0);
        }
    }

    public static float inCirc(float x) {
        return (float) (-Math.sqrt(1.0F - x * x)) + 1.0F;
    }

    private static float cube(float x) {
        return x * x * x;
    }
}
