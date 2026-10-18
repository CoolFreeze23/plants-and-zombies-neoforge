package joshxviii.plantz.networking

import joshxviii.plantz.pazResource
import joshxviii.plantz.raid.WaveType
import joshxviii.plantz.raid.ZombieRaid
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import java.util.UUID

class ZombieRaidResponsePayload(
    val data: ZombieRaidClientData
): CustomPacketPayload {
    companion object {
        val ID: CustomPacketPayload.Type<ZombieRaidResponsePayload> = CustomPacketPayload.Type(pazResource("zombie_raid_response"))

        // 26.1 StreamCodec.composite takes up to 12 fields; 1.21.1 stops at 6, so encode by hand (same wire layout).
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ZombieRaidResponsePayload> = StreamCodec.of(
            { buf, payload ->
                val data = payload.data
                UUIDUtil.STREAM_CODEC.encode(buf, data.id)
                ByteBufCodecs.VAR_INT.encode(buf, data.status.ordinal)
                ByteBufCodecs.VAR_INT.encode(buf, data.currentWaveType.ordinal)
                ByteBufCodecs.VAR_INT.encode(buf, data.wavesSpawned)
                ByteBufCodecs.VAR_INT.encode(buf, data.numWaves)
                ByteBufCodecs.VAR_INT.encode(buf, data.waveTimer)
                ByteBufCodecs.VAR_INT.encode(buf, data.activeTime)
                ByteBufCodecs.FLOAT.encode(buf, data.zombieHealth)
                ByteBufCodecs.FLOAT.encode(buf, data.zombieHealthMax)
                ByteBufCodecs.FLOAT.encode(buf, data.flagHealth)
                ByteBufCodecs.BOOL.encode(buf, data.seenCredits)
            },
            { buf ->
                ZombieRaidResponsePayload(
                    ZombieRaidClientData(
                        UUIDUtil.STREAM_CODEC.decode(buf),
                        ZombieRaid.ZombieRaidStatus.entries[ByteBufCodecs.VAR_INT.decode(buf)],
                        WaveType.entries[ByteBufCodecs.VAR_INT.decode(buf)],
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf)
                    )
                )
            }
        )
    }

    override fun type() = ID

}

data class ZombieRaidClientData(
    val id: UUID = UUID.randomUUID(),
    val status: ZombieRaid.ZombieRaidStatus = ZombieRaid.ZombieRaidStatus.STOPPED,
    val currentWaveType: WaveType = WaveType.DEFAULT,
    val wavesSpawned: Int = 0,
    val numWaves: Int = 0,
    val waveTimer: Int = 0,
    val activeTime: Int = 0,
    val zombieHealth: Float = 0f,
    val zombieHealthMax: Float = 0f,
    val flagHealth: Float = 0f,
    val seenCredits: Boolean = false
)