package joshxviii.plantz.networking

import joshxviii.plantz.pazResource
import joshxviii.plantz.raid.CompletedWave
import joshxviii.plantz.raid.ZombieRaid
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import java.util.*

class ZombieRaidResponsePayload(
    val data: ZombieRaidClientData
): CustomPacketPayload {
    companion object {
        val ID: CustomPacketPayload.Type<ZombieRaidResponsePayload> = CustomPacketPayload.Type(pazResource("zombie_raid_response"))
        private val WAVES = CompletedWave.STREAM_CODEC.apply(ByteBufCodecs.list())

        // 26.1 StreamCodec.composite takes up to 12 fields; 1.21.1 stops at 6, so encode by hand (same wire layout).
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ZombieRaidResponsePayload> = StreamCodec.of(
            { buf, payload ->
                val data = payload.data
                UUIDUtil.STREAM_CODEC.encode(buf, data.id)
                ByteBufCodecs.VAR_INT.encode(buf, data.status.ordinal)
                WAVES.encode(buf, data.waves)
                ByteBufCodecs.VAR_INT.encode(buf, data.numWaves)
                ByteBufCodecs.VAR_INT.encode(buf, data.waveTimer)
                ByteBufCodecs.VAR_INT.encode(buf, data.activeTime)
                ByteBufCodecs.FLOAT.encode(buf, data.plantProgressBar)
                ByteBufCodecs.FLOAT.encode(buf, data.zombieProgressBar)
                ByteBufCodecs.FLOAT.encode(buf, data.bossProgressBar)
                ByteBufCodecs.BOOL.encode(buf, data.seenCredits)
                ByteBufCodecs.FLOAT.encode(buf, data.bossChance)
                RaidDetails.STREAM_CODEC.encode(buf, data.details)
            },
            { buf ->
                ZombieRaidResponsePayload(
                    ZombieRaidClientData(
                        UUIDUtil.STREAM_CODEC.decode(buf),
                        ZombieRaid.ZombieRaidStatus.entries[ByteBufCodecs.VAR_INT.decode(buf)],
                        WAVES.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.FLOAT.decode(buf),
                        RaidDetails.STREAM_CODEC.decode(buf)
                    )
                )
            }
        )
    }

    override fun type() = ID

}

/**
 * Raid data for the client
 */
data class ZombieRaidClientData(
    val id: UUID = UUID.randomUUID(),
    val status: ZombieRaid.ZombieRaidStatus = ZombieRaid.ZombieRaidStatus.STOPPED,
    val waves: List<CompletedWave> = listOf(),
    val numWaves: Int = 0,
    val waveTimer: Int = 0,
    val activeTime: Int = 0,
    val plantProgressBar: Float = 0f,
    val zombieProgressBar: Float = 0f,
    val bossProgressBar: Float = 0f,
    val seenCredits: Boolean = false,
    val bossChance: Float = 0f,
    val details: RaidDetails = RaidDetails(),
) {
    fun currentWaveNumber(): Int = waves.size
    fun currentWaveType(): CompletedWave? = waves.lastOrNull()
    fun isFinalWave(): Boolean = waves.size == numWaves
    fun bossIsAlive(): Boolean = bossProgressBar > 0f
}

/**
 * More detailed information that is not necessary for rendering the main UI.
 */
data class RaidDetails(
    val flagHealth: Float = 0f,
    val flagMaxHealth: Float = 0f,
    val remainingRaiders: Int = 0,
    val remainingBossHealth: Float = 0f,
    val totalBossHealth: Float = 0f,
) {
    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, RaidDetails> = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RaidDetails::flagHealth,
            ByteBufCodecs.FLOAT, RaidDetails::flagMaxHealth,
            ByteBufCodecs.VAR_INT, RaidDetails::remainingRaiders,
            ByteBufCodecs.FLOAT, RaidDetails::remainingBossHealth,
            ByteBufCodecs.FLOAT, RaidDetails::totalBossHealth,
            ::RaidDetails,
        )
    }
}