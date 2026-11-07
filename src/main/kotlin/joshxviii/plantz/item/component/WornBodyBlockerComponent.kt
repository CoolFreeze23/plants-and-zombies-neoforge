package joshxviii.plantz.item.component

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.ChatFormatting
import net.minecraft.world.item.ItemStack
import net.minecraft.network.codec.StreamCodec
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import java.util.function.Consumer

/**
 * An item with this component that is worn in the chest slot
 * blocks attacks at all times (using its [DataComponents.BLOCKS_ATTACKS])
 * and is rendered on the wearer's body.
 */
data object WornBodyBlockerComponent{
    fun addToTooltip(
        context: Item.TooltipContext,
        consumer: Consumer<Component>,
        flag: TooltipFlag,
        components: ItemStack
    ) {
        consumer.accept(Component.translatable("component.plantz.worn_body_blocker").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC))
    }

    @JvmField
    val CODEC: Codec<WornBodyBlockerComponent> = MapCodec.unit(WornBodyBlockerComponent).codec()

    @JvmField
    val STREAM_CODEC: StreamCodec<Any, WornBodyBlockerComponent> = StreamCodec.unit(WornBodyBlockerComponent)
}
