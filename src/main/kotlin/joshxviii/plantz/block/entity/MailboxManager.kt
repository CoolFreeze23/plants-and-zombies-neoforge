package joshxviii.plantz.block.entity

import joshxviii.plantz.MailboxData
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import java.util.Locale

object MailboxManager {

    private val mailboxes: MutableMap<ResourceKey<Level>, MutableSet<BlockPos>> = mutableMapOf()

    fun clearMailboxes() {
        mailboxes.clear()
    }

    fun registerMailbox(level: Level, blockEntity: MailboxBlockEntity) {
        val levelKey = level.dimension()
        val pos = blockEntity.blockPos
        mailboxes.getOrPut(levelKey) { mutableSetOf() }.add(pos)
    }

    fun unregisterMailbox(level: Level, pos: BlockPos) {
        val levelKey = level.dimension()
        mailboxes[levelKey]?.remove(pos)
    }

    fun getMailboxesInLevel(level: Level): List<MailboxData> {
        val levelKey = level.dimension()

        return mailboxes[levelKey]?.mapNotNull { pos ->
            (level.getBlockEntity(pos) as? MailboxBlockEntity)?.asMailBoxData()
        } ?: emptyList()
    }

    fun searchMailboxes(mailboxes: List<MailboxData>, search: String): List<MailboxData> {
        if (search.isEmpty()) return mailboxes

        return mailboxes.filter { mailbox ->
            mailbox.name.string.lowercase(Locale.getDefault())
                .contains(search.lowercase(Locale.getDefault()))
        }
    }
}