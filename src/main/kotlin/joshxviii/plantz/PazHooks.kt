package joshxviii.plantz

import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent

/** NeoForge event wiring for hooks that live in other areas (items, data, ...). Filled in at integration. */
object PazHooks {
    fun registerModBus(modBus: IEventBus) {
        modBus.addListener { event: ModifyDefaultComponentsEvent -> PazItems.modifyDefaultComponents(event) }
    }

    fun registerGameBus(bus: IEventBus) {
        bus.addListener { event: net.neoforged.neoforge.event.village.VillagerTradesEvent -> joshxviii.plantz.data.PazVillagerTrades.onVillagerTrades(event) }
    }
}
