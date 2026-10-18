package joshxviii.plantz.compat.client;

import net.minecraft.world.item.ItemDisplayContext;

/**
 * 26.1's {@code on_shelf} item display context, added to 1.21.1 by enum extension (META-INF/enumextensions.json) as
 * {@code plantz:on_shelf}, falling back to {@code fixed}. 1.21.1 has no shelves; Copper Age Backport's shelves render
 * with it (CopperShelfRendererMixin), so items with an on_shelf transform sit on them like in 26.1.
 */
public final class OnShelfDisplay {
    public static final ItemDisplayContext ON_SHELF = ItemDisplayContext.valueOf("PLANTZ_ON_SHELF");

    private OnShelfDisplay() {
    }
}
