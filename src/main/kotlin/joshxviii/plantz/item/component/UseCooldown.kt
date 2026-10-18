package joshxviii.plantz.item.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import java.util.Optional

/**
 * 26.1 `minecraft:use_cooldown`: after a successful instant use the item's [cooldownGroup] (the item id when empty)
 * goes on cooldown for [seconds]. Seed packets carry it; [joshxviii.plantz.item.SeedPacketCooldowns] holds the groups.
 */
data class UseCooldown(
    val seconds: Float,
    val cooldownGroup: Optional<ResourceLocation> = Optional.empty(),
) {
    fun ticks(): Int = Math.round(seconds * 20.0f)

    companion object {
        val CODEC: Codec<UseCooldown> = RecordCodecBuilder.create { i ->
            i.group(
                Codec.floatRange(0.0f, Float.MAX_VALUE).fieldOf("seconds").forGetter(UseCooldown::seconds),
                ResourceLocation.CODEC.optionalFieldOf("cooldown_group").forGetter(UseCooldown::cooldownGroup)
            ).apply(i, ::UseCooldown)
        }

        val STREAM_CODEC: StreamCodec<ByteBuf, UseCooldown> = StreamCodec.composite(
            ByteBufCodecs.FLOAT, UseCooldown::seconds,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), UseCooldown::cooldownGroup,
            ::UseCooldown
        )
    }
}
