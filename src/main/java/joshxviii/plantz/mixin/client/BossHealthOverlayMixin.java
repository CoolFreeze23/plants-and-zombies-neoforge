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
 * Port: 26.1 extractRenderState(GuiGraphicsExtractor) is 1.21.1 render(GuiGraphics).
 */
@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {
    // (upstream also added plantz$getAllEvents() here; BossHealthOverlayAccessor now provides it as an @Accessor)

    @Redirect(method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    private Collection<LerpingBossEvent> plantz$filterEvents(Map<UUID, LerpingBossEvent> map) {
        // list (not set like upstream) so the remaining bars keep their order
        return map.values().stream().filter(it -> !ZombieRaidOverlay.isZombieRaid(it)).collect(Collectors.toList());
    }

}
