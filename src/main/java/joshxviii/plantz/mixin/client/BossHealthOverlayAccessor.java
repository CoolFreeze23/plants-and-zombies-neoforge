package joshxviii.plantz.mixin.client;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;
import java.util.UUID;

/**
 * @author Josh
 */
@Mixin(BossHealthOverlay.class)
public interface BossHealthOverlayAccessor {
    @Invoker("plantz$getAllEvents")
    Map<UUID, LerpingBossEvent> plantz$getAllEvents();
}
