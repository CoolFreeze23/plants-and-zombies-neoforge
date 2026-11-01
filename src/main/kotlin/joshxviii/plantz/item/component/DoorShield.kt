package joshxviii.plantz.item.component

import com.mojang.serialization.Codec
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import java.util.function.Consumer

data class DoorShield(
    val doorType: Block = Blocks.OAK_DOOR,
) {
    fun addToTooltip(
        context: Item.TooltipContext,
        consumer: Consumer<Component>,
        flag: TooltipFlag,
        components: ItemStack
    ) {
        consumer.accept(Component.translatable("component.door_shield", doorType.name).withColor(0x7B44A3))
    }


    companion object {
        val CODEC: Codec<DoorShield> = BuiltInRegistries.BLOCK.byNameCodec().xmap(
            ::DoorShield,
            DoorShield::doorType
        )

        val STREAM_CODEC = ByteBufCodecs.registry(Registries.BLOCK).map(
            ::DoorShield,
            DoorShield::doorType
        )
    }
}