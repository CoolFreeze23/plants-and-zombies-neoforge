package joshxviii.plantz.entity.plant

import joshxviii.plantz.*
import joshxviii.plantz.PazCompatSounds
import joshxviii.plantz.compat.ValueOutput
import joshxviii.plantz.item.SeedPacketItem
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3


object PlantUtils {
}

// PLANT ITEM INTERACTIONS
// sun interaction
fun Plant.processSunItem(player: Player, item: ItemStack, hand: InteractionHand, growNeeds: PlantGrowNeeds): Boolean {
    val hasStoredSun = item.get(PazComponents.STORED_SUN)?.hasSun() == true
    val isSunItem = item.`is`(PazItems.SUN)
    if (!hasStoredSun && !isSunItem) return false
    val level = level() as? ServerLevel?: return false
    var success = false

    if (isTame && health < maxHealth) {// heal
        sunHeal(1)
        success = true
    }
    else if (!isTame) {// try to tame
        if (random.nextFloat() < PazConfig.getTameChance(type)) {
            tame(player)
            level.broadcastEntityEvent(this, 7.toByte())
        } else level.broadcastEntityEvent(this, 6.toByte())
        success = true
    }
    else if (growNeeds == PlantGrowNeeds.SUN && verifyOwner(player)) {// grow seeds
        playSound(
            PazCompatSounds.UI_HUD_BUBBLE_POP.get(), 1.0f,
            receivedSun.toFloat()/sunRequiredForSeeds() + 0.9f
        )
        if (receivedSun++ >= sunRequiredForSeeds()) awardSeedPacket(player)
        success = true
    }

    if (success) {
        if (hasStoredSun) item.set(PazComponents.STORED_SUN, item.get(PazComponents.STORED_SUN)?.removeSun(1))
        else item.consume(1, player)
    }
    return success
}

// watering interaction
fun Plant.processWateringItem(player: Player, item: ItemStack, hand: InteractionHand, growNeeds: PlantGrowNeeds): Boolean {
    if (growNeeds != PlantGrowNeeds.WATER) return false
    val isWaterBottle = item.components.get(DataComponents.POTION_CONTENTS)?.`is`(Potions.WATER) == true
    val isWaterBucket = item.`is`(Items.WATER_BUCKET)
    val hasStoredWater = item.get(PazComponents.STORED_WATER)?.hasWater() == true
    val waterAmount = when (true) {
        isWaterBottle -> {
            player.setItemInHand(hand, ItemUtils.createFilledResult(item, player, ItemStack(Items.GLASS_BOTTLE)))
            this.playSound(SoundEvents.BOTTLE_EMPTY)
            1
        }
        isWaterBucket -> {
            player.setItemInHand(hand, ItemStack(Items.BUCKET))
            this.playSound(SoundEvents.BUCKET_EMPTY, 1.0f, 1.0f)
            8
        }
        (hasStoredWater) -> {
            this.playSound(PazSounds.WATERING_CAN)
            item.set(PazComponents.STORED_WATER, item.get(PazComponents.STORED_WATER)?.removeWater(2))
            2
        }
        else -> 0
    }
    this.receivedWater+=waterAmount
    if (waterAmount>0) {
        addParticlesAroundSelf()
        funnyBounce()
        return true
    }
    return false
}

// glove interaction
fun Plant.processGloveItem(player: Player, item: ItemStack, hand: InteractionHand): Boolean {
    if (!item.`is`(PazItems.GARDENING_GLOVE)) return false
    if (isAttached()) return false
    when {
        // owner check
        !verifyOwner(player) -> return false
        // roll wallnut
        (this is WallNut && this.isRolling) -> {
            deltaMovement = Vec3.ZERO
        }
        // hold plant
        (player.isSecondaryUseActive) -> {
            if (item.has(DataComponents.ENTITY_DATA)) {
                player.displayClientMessage(Component.translatable("message.plantz.glove_full").withStyle(ChatFormatting.RED), true)
                return true
            }

            val data = this.saveAsCompoundTag()
            // 26.1 TypedEntityData(type, tag): the tag with an "id" field
            data.putString("id", EntityType.getKey(this.type).toString())
            item.set(DataComponents.ENTITY_DATA, CustomData.of(data))

            playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value())
            addParticlesAroundSelf(level(), ParticleTypes.DUST_PLUME, verticalSpreadScale = 0.5, height = 0f)
            this.discard()
            return true
        }
        // exit early when holding plant
        (item.has(DataComponents.ENTITY_DATA)) -> return false
        // pet :)
        else -> {
            this.funnyBounce()
            addParticlesAroundSelf(level(), ParticleTypes.HEART, amount = 0..0, height = eyeHeight)
        }
    }
    return true
}

fun Entity.saveAsCompoundTag(): CompoundTag {
    val tag = CompoundTag()
    saveWithoutId(tag)
    if (this is Plant) plantSaveData(ValueOutput.of(tag, registryAccess()))
    return tag.apply {
        remove("UUID")
        remove("Pos")
    }
}

// seed packet interaction
fun Plant.processSeedPacketInteraction(player: Player, itemStack: ItemStack, blockState: BlockState? = null): PacketInteractionResult {
    val type = itemStack.get(DataComponents.ENTITY_DATA)?.let { EntityType.by(it.copyTag()).orElse(null) }
    val availableSun = player.getTotalSun()
    val sunCost = itemStack.get(PazComponents.SUN_COST)?.getSunCost(type)?: 0
    val cantAfford = sunCost > availableSun && !player.hasInfiniteMaterials()

    val result = when (type) {
        PazEntities.COFFEE_BEAN -> {
            when {
                isGrowingSeeds -> {
                    player.displayClientMessage(Component.translatable("message.plantz.growing", name.copy().withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.DARK_RED), true)
                    PacketInteractionResult.FAIL
                }
                cantAfford -> PacketInteractionResult.CANT_AFFORD
                coffeeBuff>0 -> PacketInteractionResult.FAIL
                else -> {
                    applyCoffeeBuff()
                    PacketInteractionResult.SUCCESS
                }
            }
        }
        else -> PacketInteractionResult.NO_INTERACTION
    }
    // show message
    if (result == PacketInteractionResult.CANT_AFFORD) player.displayClientMessage(Component.translatable("message.plantz.not_enough_sun", availableSun, sunCost).withStyle(ChatFormatting.RED), true)
    // remove used sun
    if (result == PacketInteractionResult.SUCCESS && !player.hasInfiniteMaterials()) {
        player.removeSunFromStorageAndInventory(sunCost)
        SeedPacketItem.applyCooldown(itemStack, player)
    }
    return result
}

enum class PacketInteractionResult {
    SUCCESS,
    FAIL,
    CANT_AFFORD,
    NO_INTERACTION
}

enum class PlantGrowNeeds {
    CANNOT_GROW,
    SOIL,
    SUN,
    WATER,
    TIME;
}