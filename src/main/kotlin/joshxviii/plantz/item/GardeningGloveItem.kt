package joshxviii.plantz.item

import net.minecraft.world.InteractionResultHolder
import joshxviii.plantz.PlantHeadAttachment
import joshxviii.plantz.PazEnchantments
import joshxviii.plantz.PazItems
import joshxviii.plantz.applyImpulse
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.plant.WallNut
import joshxviii.plantz.removeItemFromInventory
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.Tool
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.item.enchantment.ItemEnchantments
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.level.block.entity.BeaconBlockEntity.playSound

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

        fun removePlant(item: ItemStack, owner: LivingEntity?, hand: InteractionHand) {
            item.remove(DataComponents.ENTITY_DATA)
            owner?.let{
                hurtAndDropPlant(item, owner, hand)
            }
        }
    }

    override fun getUseAnimation(itemStack: ItemStack): UseAnim {
        return UseAnim.BRUSH
    }

    // 26.1 repairable(LEATHER)
    override fun isValidRepairItem(stack: ItemStack, repairCandidate: ItemStack): Boolean = repairCandidate.`is`(Items.LEATHER)

    // 26.1 enchantable(10)
    override fun getEnchantmentValue(stack: ItemStack): Int = 10

    // 26.1 Tool(canDestroyBlocksInCreative = false)
    override fun canAttackBlock(state: BlockState, level: Level, pos: BlockPos, player: Player): Boolean = !player.abilities.instabuild

    override fun useOn(context: UseOnContext): InteractionResult {
        val stack = context.itemInHand
        val level = context.level
        val player = context.player
        val isHolding = stack.has(DataComponents.ENTITY_DATA)

        val blockState = level.getBlockState(context.clickedPos)
        val cropAge = blockState.getOptionalValue(CropBlock.AGE).orElse(0)
        val hit = BlockHitResult(context.clickLocation, context.clickedFace, context.clickedPos, context.isInside)
        val seedItem = player?.let { blockState.getCloneItemStack(hit, level, context.clickedPos, it).item } ?: Items.AIR
        if (cropAge >= 7 && player != null) {
            if (isHolding) {
                player.sendOverlayMessage(Component.translatable("message.plantz.glove_full").withStyle(ChatFormatting.RED))
                return InteractionResult.PASS
            }
            val blockPos = context.clickedPos
            val blockState = level.getBlockState(blockPos)
            if (!level.isClientSide) {
                if(level.destroyBlock(blockPos, true, player)) player.let {
                    hurtAndDropPlant(stack, player, context.hand)
                    if (player.hasInfiniteMaterials() || it.removeItemFromInventory(seedItem) > 0) level.setBlockAndUpdate(blockPos, blockState.setValue(CropBlock.AGE, 0))
                }
            }
            return InteractionResult.SUCCESS
        }
        if (!isHolding) {
            player?.let{
                return PlantHeadAttachment.potHelmetPlaceInteraction(level, context.clickedPos, player)
            }
            return InteractionResult.PASS // 26.1 TRY_WITH_EMPTY_HAND
        }

        // Match SeedPacketItem.useOn: predict the interaction as handled on the client.
        if (level.isClientSide) return InteractionResult.SUCCESS

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
            removePlant(stack, context.player, context.hand)
        }
        return result
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (player.getItemInHand(hand).has(DataComponents.ENTITY_DATA)) player.startUsingItem(hand)
        return super.use(level, player, hand)
    }

    // 26.1 ticks onUseTick even for a 0 tick use (until released); 1.21.1 only while ticks remain
    override fun getUseDuration(itemStack: ItemStack, user: LivingEntity): Int = 72000

    override fun onUseTick(level: Level, livingEntity: LivingEntity, itemStack: ItemStack, ticksRemaining: Int) {
        super.onUseTick(level, livingEntity, itemStack, ticksRemaining)
        if (level.isClientSide || livingEntity !is Player) return

        val stack = livingEntity.getItemInHand(livingEntity.usedItemHand)
        val plantData = stack.get(DataComponents.ENTITY_DATA) ?: return
        val lobbingLevel = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
            .entrySet()
            .firstOrNull { it.key.value().effects().has(PazEnchantments.LOB_PLANT) }
            ?.intValue ?: 0
        if (lobbingLevel == 0) return

        val lookAngle = livingEntity.lookAngle
        val pos = livingEntity.eyePosition.add(lookAngle)
        val plantEntity = entityTypeOf(plantData)?.create(level) ?: return
        plantData.loadInto(plantEntity)
        plantEntity.moveTo(pos)
        plantEntity.playSound(SoundEvents.SNOWBALL_THROW,  0.4f, 0.6f)
        if (plantEntity is WallNut) plantEntity.roll(lookAngle, power = 0.7f * lobbingLevel)
        else plantEntity.applyImpulse(lookAngle.scale(0.65 + 0.5 * lobbingLevel))
        if (!level.addFreshEntity(plantEntity)) {
            plantEntity.discard()
            return
        }
        removePlant(stack, livingEntity, livingEntity.usedItemHand)
    }

}
