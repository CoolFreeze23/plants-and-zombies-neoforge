package joshxviii.plantz.compat.client;

import java.util.function.Supplier;

/** Backport of Fabric's {@code RenderStateDataKey}: a typed key for extra data attached to a render state. */
public final class RenderStateDataKey<T> {
    private final Supplier<String> name;

    private RenderStateDataKey(Supplier<String> name) {
        this.name = name;
    }

    public static <T> RenderStateDataKey<T> create(Supplier<String> name) {
        return new RenderStateDataKey<>(name);
    }

    @Override
    public String toString() {
        return "RenderStateDataKey[" + name.get() + "]";
    }
}
