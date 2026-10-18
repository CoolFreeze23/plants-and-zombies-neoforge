package joshxviii.plantz.item

import joshxviii.plantz.getItemCount
import joshxviii.plantz.removeItemFromInventory
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.Tool
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CropBlock
import java.util.List

class GardeningGloveItem(properties: Properties) : Item(properties) {

    companion object {

        fun createToolProperties(): Tool {
            return Tool(
                listOf(Tool.Rule.overrideSpeed(BlockTags.CROPS, 15.0f)), 1.0f, 1
            )
        }

        fun addToTooltip(consumer: MutableList<Component>, components: ItemStack, shiftKey: Component) {
            val data = components.entityDataType()

            if (data == null) {
                consumer.add(1, Component.translatable("item.plantz.gardening_glove.description", shiftKey)
                    .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC))
                return
            }

            val id = BuiltInRegistries.ENTITY_TYPE.getKey(data)
            val entityName = Component.translatable("entity.${id.namespace}.${id.path}")
                .withStyle(ChatFormatting.DARK_GREEN)

            consumer.add(1, Component.translatable("item.plantz.gardening_glove.entity", entityName)
                .withStyle(ChatFormatting.GRAY))
        }

        fun hurtAndDropPlant(item: ItemStack, owner: LivingEntity, hand: InteractionHand) {
            if (item.nextDamageWillBreak()) {
                item.entityDataType()?.let { data ->
                    val breakItem = SeedPacketItem.stackFor(data)
                    ItemEntity(owner.level(), owner.x, owner.eyeY, owner.z, breakItem).let {
                        owner.level().addFreshEntity(it)
                    }
                }
            }
            item.hurtAndBreak(1, owner, LivingEntity.getSlotForHand(hand))
        }
    }

    override fun getUseAnimation(itemStack: ItemStack): UseAnim {
        return UseAnim.BRUSH
    }

    // 26.1 repairable(LEATHER)
    override fun isValidRepairItem(stack: ItemStack, repairCandidate: ItemStack): Boolean = repairCandidate.`is`(Items.LEATHER)

    override fun useOn(context: UseOnContext): InteractionResult {
        val stack = context.itemInHand
        val level = context.level
        val player = context.player
        val isHolding = stack.has(DataComponents.ENTITY_DATA)

        val blockState = level.getBlockState(context.clickedPos)
        val cropAge = blockState.getOptionalValue(CropBlock.AGE).orElse(0)
        val seedItem = blockState.block.getCloneItemStack(level, context.clickedPos, blockState).item
        if (cropAge >= 7 && (player?.hasInfiniteMaterials() ?: false || (player?.getItemCount(seedItem) ?: 0) > 0)) {
            if (isHolding) {
                player?.sendOverlayMessage(Component.translatable("message.plantz.glove_full").withStyle(ChatFormatting.RED))
                return InteractionResult.PASS
            }
            val blockPos = context.clickedPos
            val blockState = level.getBlockState(blockPos)
            if (!level.isClientSide) {
                if(level.destroyBlock(blockPos, true, player)) player?.let {
                    GardeningGloveItem.hurtAndDropPlant(stack, player, context.hand)
                    if (player.hasInfiniteMaterials() || it.removeItemFromInventory(seedItem) > 0) level.setBlockAndUpdate(blockPos, blockState.setValue(CropBlock.AGE, 0))
                }
            }
            return InteractionResult.SUCCESS
        }
        if (!isHolding) return InteractionResult.PASS // 26.1 TRY_WITH_EMPTY_HAND

        if (level !is ServerLevel) return InteractionResult.SUCCESS

        val result = SeedPacketItem.tryPlant(
            level = level,
            player = context.player,
            itemStack = stack,
            pos = context.clickedPos,
            face = context.clickedFace,
            horizontalDir = context.horizontalDirection,
            ignoreSunRequirement = true,
            consumeItem = false
        )

        if (result == InteractionResult.SUCCESS) {
            stack.remove(DataComponents.ENTITY_DATA)
            player?.let {
                GardeningGloveItem.hurtAndDropPlant(stack, it, context.hand)
            }
        }
        return result
    }

}
