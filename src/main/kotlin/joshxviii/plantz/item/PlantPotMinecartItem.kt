package joshxviii.plantz.item

import joshxviii.plantz.PazEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.dispenser.BlockSource
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.BaseRailBlock
import net.minecraft.world.level.block.DispenserBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.RailShape
import net.minecraft.world.level.gameevent.GameEvent

/**
 * 26.1 `MinecartItem(PazEntities.PLANT_POT_MINECART, props)`: 1.21.1 MinecartItem only takes the vanilla
 * `AbstractMinecart.Type` enum, so this re-implements its placement and dispenser logic for our entity type.
 */
class PlantPotMinecartItem(properties: Properties) : Item(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val state = level.getBlockState(pos)
        if (!state.`is`(BlockTags.RAILS)) return InteractionResult.FAIL
        val stack = context.itemInHand
        if (level is ServerLevel) {
            val shape = railShape(state, level, pos)
            val yOffset = if (shape.isAscending) 0.5 else 0.0
            val placed = spawnMinecart(level, pos.x + 0.5, pos.y + 0.0625 + yOffset, pos.z + 0.5, stack, context.player)
                ?: return InteractionResult.FAIL
            level.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.player, level.getBlockState(pos.below())))
            if (placed.isRemoved) return InteractionResult.FAIL
        }
        stack.shrink(1)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    companion object {
        private fun railShape(state: BlockState, level: net.minecraft.world.level.Level, pos: BlockPos): RailShape =
            (state.block as? BaseRailBlock)?.getRailDirection(state, level, pos, null) ?: RailShape.NORTH_SOUTH

        fun spawnMinecart(level: ServerLevel, x: Double, y: Double, z: Double, stack: ItemStack, player: Player?): net.minecraft.world.entity.Entity? {
            val minecart = PazEntities.PLANT_POT_MINECART.create(level) ?: return null
            minecart.setPos(x, y, z)
            EntityType.createDefaultStackConfig<net.minecraft.world.entity.Entity>(level, stack, player).accept(minecart)
            level.addFreshEntity(minecart)
            return minecart
        }

        @JvmField
        val DISPENSE_ITEM_BEHAVIOR: DispenseItemBehavior = object : DefaultDispenseItemBehavior() {
            private val defaultBehavior = DefaultDispenseItemBehavior()

            override fun execute(source: BlockSource, stack: ItemStack): ItemStack {
                val direction = source.state().getValue(DispenserBlock.FACING)
                val level = source.level()
                val center = source.center()
                val x = center.x() + direction.stepX * 1.125
                val y = Math.floor(center.y()) + direction.stepY
                val z = center.z() + direction.stepZ * 1.125
                val pos = source.pos().relative(direction)
                val state = level.getBlockState(pos)
                val yOffset: Double
                if (state.`is`(BlockTags.RAILS)) {
                    yOffset = if (railShape(state, level, pos).isAscending) 0.6 else 0.1
                } else {
                    if (!state.isAir || !level.getBlockState(pos.below()).`is`(BlockTags.RAILS)) {
                        return defaultBehavior.dispense(source, stack)
                    }
                    val below = level.getBlockState(pos.below())
                    yOffset = if (direction != Direction.DOWN && railShape(below, level, pos.below()).isAscending) -0.4 else -0.9
                }
                spawnMinecart(level, x, y + yOffset, z, stack, null)
                stack.shrink(1)
                return stack
            }

            override fun playSound(source: BlockSource) {
                source.level().levelEvent(1000, source.pos(), 0)
            }
        }
    }
}
