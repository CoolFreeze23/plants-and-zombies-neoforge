package joshxviii.plantz.compat.client.state;

import joshxviii.plantz.compat.client.renderer.ModelFeatureRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BlockEntityRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    @Nullable public BlockEntityType<?> blockEntityType;
    public int lightCoords;
    @Nullable public ModelFeatureRenderer.CrumblingOverlay breakProgress;

    public BlockState blockState() {
        return blockState;
    }

    public static void extractBase(BlockEntity blockEntity, BlockEntityRenderState state, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        state.blockPos = blockEntity.getBlockPos();
        state.blockState = blockEntity.getBlockState();
        state.blockEntityType = blockEntity.getType();
        state.lightCoords = blockEntity.getLevel() != null ? LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos()) : 15728880;
        state.breakProgress = breakProgress;
    }
}
