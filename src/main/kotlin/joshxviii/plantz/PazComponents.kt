package joshxviii.plantz

import joshxviii.plantz.item.component.BrainzAlloyCost
import joshxviii.plantz.item.component.BlocksProjectileDamage
import joshxviii.plantz.item.component.StoredSun
import joshxviii.plantz.item.component.StoredWater
import joshxviii.plantz.item.component.SunCost
import joshxviii.plantz.item.component.UseCooldown
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.contents.TranslatableContents
import java.util.function.Consumer
import java.util.function.UnaryOperator

object PazComponents {

    @JvmField
    val SUN_COST: DataComponentType<SunCost> = register(
        "sun_cost"
    ) { it.persistent(SunCost.CODEC)
        .networkSynchronized(SunCost.STREAM_CODEC)
        .cacheEncoding()
    }

    @JvmField
    val BRAINZ_ALLOY_COST: DataComponentType<BrainzAlloyCost> = register(
        "brainz_alloy_cost"
    ) { it.persistent(BrainzAlloyCost.CODEC)
        .networkSynchronized(BrainzAlloyCost.STREAM_CODEC)
        .cacheEncoding()
    }

    @JvmField
    val STORED_WATER: DataComponentType<StoredWater> = register(
        "stored_water"
    ) { it.persistent(StoredWater.CODEC)
        .networkSynchronized(StoredWater.STREAM_CODEC)
        .cacheEncoding()
    }

    @JvmField
    val STORED_SUN: DataComponentType<StoredSun> = register(
        "stored_sun"
    ) { it.persistent(StoredSun.CODEC)
        .networkSynchronized(StoredSun.STREAM_CODEC)
        .cacheEncoding()
    }

    @JvmField
    val BLOCKS_PROJECTILE_DAMAGE: DataComponentType<BlocksProjectileDamage> = register(
        "blocks_projectile_damage"
    ) { it.persistent(BlocksProjectileDamage.CODEC)
        .networkSynchronized(BlocksProjectileDamage.STREAM_CODEC)
        .cacheEncoding()
    }

    /** 26.1 `minecraft:use_cooldown` (1.21.1 has no such component). */
    @JvmField
    val USE_COOLDOWN: DataComponentType<UseCooldown> = register(
        "use_cooldown"
    ) { it.persistent(UseCooldown.CODEC)
        .networkSynchronized(UseCooldown.STREAM_CODEC)
        .cacheEncoding()
    }

    private fun <T : Any> register(name: String, builder: UnaryOperator<DataComponentType.Builder<T>>): DataComponentType<T> {
        val key = ResourceKey.create(Registries.DATA_COMPONENT_TYPE, pazResource(name))
        return Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            key,
            builder.apply(DataComponentType.builder()).build()
        )
    }

    /**
     * Fabric's `ItemComponentTooltipProviderRegistry.addLast(...)` for our components, called from NeoForge's
     * `ItemTooltipEvent`. Like on 26.1 the lines go after vanilla's component lines (lore, attribute modifiers,
     * unbreakable) and before the can-break / can-place-on sections and the advanced (F3+H) lines.
     */
    @JvmStatic
    fun addComponentTooltips(stack: ItemStack, context: Item.TooltipContext, lines: MutableList<Component>, flag: TooltipFlag) {
        val added = mutableListOf<Component>()
        val consumer = Consumer<Component> { added.add(it) }
        stack.get(STORED_WATER)?.addToTooltip(context, consumer, flag, stack)
        stack.get(STORED_SUN)?.addToTooltip(context, consumer, flag, stack)
        stack.get(SUN_COST)?.addToTooltip(context, consumer, flag, stack)
        stack.get(BRAINZ_ALLOY_COST)?.addToTooltip(context, consumer, flag, stack)
        stack.get(BLOCKS_PROJECTILE_DAMAGE)?.addToTooltip(context, consumer, flag, stack)
        if (added.isNotEmpty()) lines.addAll(tooltipInsertIndex(stack, lines), added)
    }

    private fun tooltipInsertIndex(stack: ItemStack, lines: List<Component>): Int {
        val itemId = BuiltInRegistries.ITEM.getKey(stack.item).toString()
        for (i in 1 until lines.size) {
            val line = lines[i]
            val key = (line.contents as? TranslatableContents)?.key
            when {
                key == "item.canBreak" || key == "item.canPlace" -> return if (lines[i - 1].string.isEmpty()) i - 1 else i
                key == "item.durability" || key == "item.components" || key == "item.disabled" -> return i
                line.string == itemId -> return i
            }
        }
        return lines.size
    }

    fun initialize() {}
}
