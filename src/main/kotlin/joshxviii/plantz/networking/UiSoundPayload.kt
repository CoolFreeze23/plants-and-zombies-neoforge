package joshxviii.plantz.networking

import joshxviii.plantz.pazResource
import net.minecraft.core.Holder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.sounds.SoundEvent

/**
 * 26.1 `ClientboundSoundPacket(sound, SoundSource.UI, ...)` at the player. 1.21.1 has no UI sound category, so the
 * client plays these on Master scaled by the port's UI volume slider (`PazConfig.UI_VOLUME`).
 */
data class UiSoundPayload(
    val sound: Holder<SoundEvent>,
    val volume: Float,
    val pitch: Float,
    val seed: Long
) : CustomPacketPayload {
    companion object {
        val ID: CustomPacketPayload.Type<UiSoundPayload> = CustomPacketPayload.Type(pazResource("ui_sound"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, UiSoundPayload> = StreamCodec.composite(
            SoundEvent.STREAM_CODEC, UiSoundPayload::sound,
            ByteBufCodecs.FLOAT, UiSoundPayload::volume,
            ByteBufCodecs.FLOAT, UiSoundPayload::pitch,
            ByteBufCodecs.VAR_LONG, UiSoundPayload::seed,
            ::UiSoundPayload
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
