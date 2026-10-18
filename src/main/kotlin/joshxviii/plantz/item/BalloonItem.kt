package joshxviii.plantz.item

import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems.balloonByColor
import joshxviii.plantz.PazSounds
import joshxviii.plantz.entity.Balloon
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.LeadItem
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

class BalloonItem(
    properties: Properties,
    val color: DyeColor = DyeColor.WHITE
) : Item(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level: Level = context.level
        val pos: BlockPos = context.clickedPos
        val face: Direction = context.clickedFace
        val state: BlockState = level.getBlockState(pos)
        val player: Player? = context.player
        val itemStack: ItemStack = context.itemInHand


        if (level !is ServerLevel || player == null) return InteractionResult.PASS

        val spawnPos = if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty) pos
        else pos.relative(face)

        val balloonEntity: Balloon = PazEntities.BALLOON.create(
            level,
            EntityType.createDefaultStackConfig(level, itemStack, player),
            spawnPos,
            EntitySpawnReason.SPAWN_ITEM_USE,
            true,
            face == Direction.UP
        )?: return InteractionResult.FAIL
        balloonEntity.dyeColor = color

        if (!level.addFreshEntity(balloonEntity)) {
            balloonEntity.discard()
            return InteractionResult.FAIL
        }
        balloonEntity.playSound(PazSounds.BALLOON_INFLATE)
        itemStack.consume(1, player)

        balloonEntity.setLeashedTo(player, true)
        if (state.`is`(BlockTags.FENCES)) {
            return LeadItem.bindPlayerMobs(player, level, pos)
        }

        return InteractionResult.SUCCESS_SERVER
    }

    companion object {
        fun stackFor(color: DyeColor): ItemStack {
            return balloonByColor[color]?.defaultInstance?: ItemStack.EMPTY
        }
    }


}