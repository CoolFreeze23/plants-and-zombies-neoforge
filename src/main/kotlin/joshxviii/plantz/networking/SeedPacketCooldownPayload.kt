package joshxviii.plantz.networking

import joshxviii.plantz.pazResource
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Port-only: 26.1 synced per-plant seed packet cooldowns through vanilla cooldown groups. 1.21.1 cooldowns are per
 * item, so [joshxviii.plantz.item.SeedPacketCooldowns] tracks them and this payload mirrors them to the client.
 */
data class SeedPacketCooldownPayload(val group: ResourceLocation, val ticks: Int) : CustomPacketPayload {
    companion object {
        val ID: CustomPacketPayload.Type<SeedPacketCooldownPayload> = CustomPacketPayload.Type(pazResource("seed_packet_cooldown"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SeedPacketCooldownPayload> = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SeedPacketCooldownPayload::group,
            ByteBufCodecs.VAR_INT, SeedPacketCooldownPayload::ticks,
            ::SeedPacketCooldownPayload
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
