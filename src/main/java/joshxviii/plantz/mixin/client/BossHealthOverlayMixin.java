package joshxviii.plantz.mixin.client;

import joshxviii.plantz.gui.ZombieRaidOverlay;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Josh
 * Hides the zombie raid bars (ZombieRaidOverlay draws them). plantz$getAllEvents lives in BossHealthOverlayAccessor.
 */
@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {
    @Redirect(method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    private Collection<LerpingBossEvent> plantz$filterEvents(Map<UUID, LerpingBossEvent> map) {
        // a list, not a set, so the remaining bars keep their order
        return map.values().stream().filter(it -> !ZombieRaidOverlay.isZombieRaid(it)).collect(Collectors.toList());
    }

}
