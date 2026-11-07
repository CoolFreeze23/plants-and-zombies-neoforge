package joshxviii.plantz

import joshxviii.plantz.PazNetwork.ZombieRaidClientCache
import joshxviii.plantz.inventory.AbstractMailboxMenu
import joshxviii.plantz.inventory.MailCollectionBoxMenu
import joshxviii.plantz.item.SeedPacketCooldowns
import joshxviii.plantz.gui.PazOptionsScreen
import joshxviii.plantz.networking.MailboxListResponsePayload
import joshxviii.plantz.networking.SeedPacketCooldownPayload
import joshxviii.plantz.networking.OpenOptionsScreenPayload
import joshxviii.plantz.networking.SendMailResponsePayload
import joshxviii.plantz.networking.ServerConfigResponsePayload
import joshxviii.plantz.networking.UiSoundPayload
import joshxviii.plantz.networking.ZombieRaidResponsePayload
import joshxviii.plantz.raid.ZombieRaid
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
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
        if (!ZombieRaidClientCache.isActive()) RaidMusicManager.restart()
        ZombieRaidClientCache.put(payload.data)
        if (payload.data.status == ZombieRaid.ZombieRaidStatus.TERMINATE) ZombieRaidClientCache.remove(payload.data.id)
    }

    fun handleOpenOptionsScreen(payload: OpenOptionsScreenPayload, context: IPayloadContext) {
        Minecraft.getInstance().setScreen(PazOptionsScreen())
    }

    /** 26.1 played these on SoundSource.UI at the player; see [UiSoundPayload]. */
    fun handleUiSound(payload: UiSoundPayload, context: IPayloadContext) {
        val player = context.player()
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance(payload.sound.value(), SoundSource.MASTER,
            payload.volume * PazConfig.UI_VOLUME, payload.pitch, RandomSource.create(payload.seed), player.x, player.y, player.z))
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

    fun handleSeedPacketCooldown(payload: SeedPacketCooldownPayload, context: IPayloadContext) {
        SeedPacketCooldowns.addCooldown(context.player(), payload.group, payload.ticks)
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
