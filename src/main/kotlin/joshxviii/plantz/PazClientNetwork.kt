package joshxviii.plantz

import joshxviii.plantz.PazNetwork.ZombieRaidClientCache
import joshxviii.plantz.inventory.AbstractMailboxMenu
import joshxviii.plantz.inventory.MailCollectionBoxMenu
import joshxviii.plantz.networking.MailboxListResponsePayload
import joshxviii.plantz.networking.SendMailResponsePayload
import joshxviii.plantz.networking.ServerConfigResponsePayload
import joshxviii.plantz.networking.ZombieRaidResponsePayload
import joshxviii.plantz.raid.ZombieRaid
import net.neoforged.neoforge.network.handling.IPayloadContext

object PazClientNetwork {
    private var initialized = false

    fun onClientTick() {
        if (!initialized) {
            ZombieRaidClientCache.clear()
            initialized = true
        }
        RaidMusicManager.tick()
    }

    fun handleZombieRaid(payload: ZombieRaidResponsePayload, context: IPayloadContext) {
        if (ZombieRaidClientCache.get(payload.data.id) == null) RaidMusicManager.start()
        ZombieRaidClientCache.put(payload.data)
        if (payload.data.status == ZombieRaid.ZombieRaidStatus.TERMINATE) ZombieRaidClientCache.remove(payload.data.id)
    }

    fun handleSendMailResponse(payload: SendMailResponsePayload, context: IPayloadContext) {
        val menu = context.player().containerMenu as? AbstractMailboxMenu ?: return
        menu.responseMessage = payload.message
        menu.responseTimeout = 30
    }

    fun handleMailboxList(payload: MailboxListResponsePayload, context: IPayloadContext) {
        val menu = context.player().containerMenu as? AbstractMailboxMenu ?: return
        if (menu is MailCollectionBoxMenu) {
            menu.selectedMailboxPos = menu.data.selectedMailbox
        }
        // Rebuild list from positions
        menu.availableMailboxes = payload.mailboxes
        menu.updateFilteredMailboxes()
    }

    fun handleServerConfig(payload: ServerConfigResponsePayload, context: IPayloadContext) {
        try {
            PazConfig.server = PazConfig.GSON.fromJson(payload.json, ServerConfig::class.java)
            PazMain.LOGGER.info("Server config synced!")
        } catch (e: Exception) {
            PazMain.LOGGER.error("Failed to parse server config", e)
        }
    }
}
