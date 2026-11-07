package joshxviii.plantz.item.component

import com.mojang.serialization.Codec
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazItems
import joshxviii.plantz.name
import net.minecraft.ChatFormatting
import joshxviii.plantz.item.entityDataType
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.ItemStack
import java.util.function.Consumer

data class SunCost(
    private val sunCost: Int = -1,
) {
    fun addToTooltip(
        context: Item.TooltipContext,
        consumer: Consumer<Component>,
        flag: TooltipFlag,
        components: ItemStack
    ) {
        val type = components.entityDataType()
        consumer.accept(Component.translatable("component.plantz.item_cost", PazItems.SUN.name(), getSunCost(type)).withStyle(ChatFormatting.GOLD))
    }

    fun getSunCost(forType: EntityType<*>?): Int {
        return if (sunCost > 0) sunCost
        else PazConfig.getSunCost(forType).let { if(it == -1) 0 else it }
    }

    companion object {
        fun getSunCost(item: ItemStack): Int {
            return item.get(PazComponents.SUN_COST)?.getSunCost(item.entityDataType())?: 0
        }

        val CODEC = Codec.INT.xmap(
            { SunCost(it) },
            { it.sunCost }
        )

        val STREAM_CODEC = ByteBufCodecs.INT.map(
            { SunCost(it) },
            { it.sunCost }
        )
    }
}