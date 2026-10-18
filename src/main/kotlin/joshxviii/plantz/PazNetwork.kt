package joshxviii.plantz

import joshxviii.plantz.networking.MailboxListResponsePayload
import joshxviii.plantz.networking.SendMailRequestPayload
import joshxviii.plantz.networking.SendMailRequestPayload.Companion.handleSendMailPacket
import joshxviii.plantz.networking.SendMailResponsePayload
import joshxviii.plantz.networking.ServerConfigResponsePayload
import joshxviii.plantz.networking.UpdateCollectionBoxPayload
import joshxviii.plantz.networking.UpdateCollectionBoxPayload.Companion.handleUpdateCollectionBoxPacket
import joshxviii.plantz.networking.ZombieRaidClientData
import joshxviii.plantz.networking.ZombieRaidResponsePayload
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import java.util.UUID

object PazNetwork {

    object ZombieRaidClientCache {
        private val active = mutableMapOf<UUID, ZombieRaidClientData>()

        fun get() = active.values.firstOrNull()
        fun get(id: UUID) = active[id]
        fun put(data: ZombieRaidClientData) { active[data.id] = data }
        fun remove(id: UUID) { active.remove(id) }
        fun clear() { active.clear() }

        fun isActive() = active.isNotEmpty()
    }

    /**
     * Payload registration. NeoForge runs handlers on the main thread by default, like Fabric's play receivers.
     * Client handlers live in [PazClientNetwork] and are only reached on the physical client.
     */
    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1")

        // serverbound
        registrar.playToServer(SendMailRequestPayload.ID, SendMailRequestPayload.STREAM_CODEC, ::handleSendMailPacket)
        registrar.playToServer(UpdateCollectionBoxPayload.ID, UpdateCollectionBoxPayload.STREAM_CODEC, ::handleUpdateCollectionBoxPacket)

        // clientbound
        registrar.playToClient(SendMailResponsePayload.ID, SendMailResponsePayload.STREAM_CODEC) { payload, context -> PazClientNetwork.handleSendMailResponse(payload, context) }
        registrar.playToClient(MailboxListResponsePayload.ID, MailboxListResponsePayload.STREAM_CODEC) { payload, context -> PazClientNetwork.handleMailboxList(payload, context) }
        registrar.playToClient(ServerConfigResponsePayload.ID, ServerConfigResponsePayload.STREAM_CODEC) { payload, context -> PazClientNetwork.handleServerConfig(payload, context) }
        registrar.playToClient(ZombieRaidResponsePayload.ID, ZombieRaidResponsePayload.STREAM_CODEC) { payload, context -> PazClientNetwork.handleZombieRaid(payload, context) }
    }
}
