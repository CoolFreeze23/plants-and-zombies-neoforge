package joshxviii.plantz.integration

import joshxviii.plantz.pazResource
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.world.item.Item
import net.neoforged.fml.ModList
import net.neoforged.neoforge.event.AddPackFindersEvent

/**
 * Optional Copper Age Backport support. It adds 1.21.9's copper tools, copper nuggets, copper chests and copper golem
 * under the `minecraft` namespace, so with it installed the 26.1 copper content comes back: gnomes carry copper tools
 * and the relocation advancement shows a copper shovel; its copper chest, statue and step sound events are preferred by
 * [joshxviii.plantz.PazCompatSounds].
 */
object PazCopperAgeBackportCompat {
    const val MOD_ID = "copperagebackport"

    @JvmField val LOADED: Boolean = ModList.get().isLoaded(MOD_ID)

    private val PACK_LOCATION: ResourceLocation = pazResource("resourcepacks/copperagebackport_compat")

    fun addPackFinders(event: AddPackFindersEvent) {
        if (!LOADED || event.packType != PackType.SERVER_DATA) return
        event.addPackFinders(
            PACK_LOCATION, PackType.SERVER_DATA, Component.literal("Plants & Zombies: Copper Age Backport features"),
            PackSource.BUILT_IN, true, Pack.Position.TOP
        )
    }

    /** The `minecraft:` item [id] when a backport registers it, else [fallback]. */
    @JvmStatic
    fun item(id: String, fallback: Item): Item =
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.withDefaultNamespace(id)).orElse(fallback)
}
