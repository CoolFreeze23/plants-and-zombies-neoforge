package joshxviii.plantz.mixin.client;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

/**
 * @author Josh
 * Port: an @Accessor on the vanilla field instead of an @Invoker of the method BossHealthOverlayMixin adds
 * (an invoker must not depend on another mixin having been applied first).
 */
@Mixin(BossHealthOverlay.class)
public interface BossHealthOverlayAccessor {
    @Accessor("events")
    Map<UUID, LerpingBossEvent> plantz$getAllEvents();
}
