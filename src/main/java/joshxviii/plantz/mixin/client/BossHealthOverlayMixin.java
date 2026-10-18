package joshxviii.plantz.mixin.client;

import joshxviii.plantz.gui.ZombieRaidOverlay;
import joshxviii.plantz.raid.ZombieRaids;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Josh
 */
@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {
    @Shadow
    @Final
    private Map<UUID, LerpingBossEvent> events;

    @Unique
    public Map<UUID, LerpingBossEvent> plantz$getAllEvents() {
        return events;
    }

//    @Inject(method = "extractRenderState",at = @At("HEAD"), cancellable = true)
//    private void plantz$skipOurBar(GuiGraphicsExtractor graphics, CallbackInfo ci) {
//        events.forEach((uuid, event) -> {
//            if (event instanceof LerpingBossEvent lerpingBossEvent) {
//                if (isZombieRaid(lerpingBossEvent)) {
//                    ci.cancel();
//                }
//            }
//        });
//    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    private Collection<LerpingBossEvent> plantz$filterEvents(Map<UUID, LerpingBossEvent> map) {
        return map.values().stream().filter(it -> !ZombieRaidOverlay.isZombieRaid(it)).collect(Collectors.toSet());
    }

}
