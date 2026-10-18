package joshxviii.plantz

import joshxviii.plantz.item.component.BrainzAlloyCost
import joshxviii.plantz.item.component.BlocksProjectileDamage
import joshxviii.plantz.item.component.StoredSun
import joshxviii.plantz.item.component.StoredWater
import joshxviii.plantz.item.component.SunCost
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
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

    private fun <T : Any> register(name: String, builder: UnaryOperator<DataComponentType.Builder<T>>): DataComponentType<T> {
        val key = ResourceKey.create(Registries.DATA_COMPONENT_TYPE, pazResource(name))
        return Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            key,
            builder.apply(DataComponentType.builder()).build()
        )
    }

    /**
     * Replaces Fabric's `ItemComponentTooltipProviderRegistry.addLast(...)` for our components: appends their tooltip
     * lines (in upstream order) to [lines]. Call from NeoForge's `ItemTooltipEvent`:
     * `PazComponents.addComponentTooltips(event.itemStack, event.context, event.toolTip, event.flags)`.
     */
    @JvmStatic
    fun addComponentTooltips(stack: ItemStack, context: Item.TooltipContext, lines: MutableList<Component>, flag: TooltipFlag) {
        val consumer = java.util.function.Consumer<Component> { lines.add(it) }
        stack.get(STORED_WATER)?.addToTooltip(context, consumer, flag, stack)
        stack.get(STORED_SUN)?.addToTooltip(context, consumer, flag, stack)
        stack.get(SUN_COST)?.addToTooltip(context, consumer, flag, stack)
        stack.get(BRAINZ_ALLOY_COST)?.addToTooltip(context, consumer, flag, stack)
        stack.get(BLOCKS_PROJECTILE_DAMAGE)?.addToTooltip(context, consumer, flag, stack)
    }

    fun initialize() {}
}
