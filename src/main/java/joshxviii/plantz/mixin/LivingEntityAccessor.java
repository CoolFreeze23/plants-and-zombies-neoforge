package joshxviii.plantz.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

/** Reads the effect flags {@link LivingEntityMixin} syncs to clients. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Invoker("plantz$hypnoFlag")
    boolean plantz$getHypnoId();

    @Invoker("plantz$chillFlag")
    boolean plantz$getChillId();

    @Invoker("plantz$frozenFlag")
    boolean plantz$getFrozenId();

    @Invoker("plantz$butterFlag")
    boolean plantz$getButterId();

    @Invoker("plantz$paintedColors")
    Map<Integer, Integer> plantz$getPaintedColors();

}
