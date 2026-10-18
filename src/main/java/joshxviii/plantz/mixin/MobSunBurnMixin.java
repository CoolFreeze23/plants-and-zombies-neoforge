package joshxviii.plantz.mixin;

import joshxviii.plantz.PazWorldGen;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Port-only: the 26.1 graveyard biome sets the environment attribute "monsters burn" to false. 1.21.1 biomes
 * have no such setting, so undead never burn in daylight while standing in the graveyard.
 */
@Mixin(Mob.class)
public abstract class MobSunBurnMixin {
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void plantz$noSunBurnInGraveyard(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (self.level().getBiome(self.blockPosition()).is(PazWorldGen.GRAVEYARD)) {
            cir.setReturnValue(false);
        }
    }
}
