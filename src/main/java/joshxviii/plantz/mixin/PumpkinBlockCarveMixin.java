package joshxviii.plantz.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import joshxviii.plantz.PazLootTables;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.1 rolls the {@code minecraft:carve/pumpkin} loot table for carving drops; 1.21.1 drops 4 seeds directly, so ours rolls next to them. */
@Mixin(PumpkinBlock.class)
public abstract class PumpkinBlockCarveMixin {
    @Inject(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", shift = At.Shift.AFTER))
    private void plantz$carveLoot(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
                                  CallbackInfoReturnable<ItemInteractionResult> cir, @Local ItemEntity seeds) {
        if (level instanceof ServerLevel serverLevel) PazLootTables.dropCarveLoot(serverLevel, state, player, seeds);
    }
}
