package joshxviii.plantz

import joshxviii.plantz.integration.PazVanillaBackportCompat
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.AddPackFindersEvent
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent

/** NeoForge event wiring for hooks that live in other areas (items, data, ...). Filled in at integration. */
object PazHooks {
    fun registerModBus(modBus: IEventBus) {
        modBus.addListener { event: AddPackFindersEvent -> PazVanillaBackportCompat.addPackFinders(event) }
        modBus.addListener { event: ModifyDefaultComponentsEvent -> PazItems.modifyDefaultComponents(event) }
    }

    fun registerGameBus(bus: IEventBus) {
        bus.addListener { event: net.neoforged.neoforge.event.village.VillagerTradesEvent -> joshxviii.plantz.data.PazVillagerTrades.onVillagerTrades(event) }
    }
}
