package joshxviii.plantz

import net.neoforged.bus.api.IEventBus

/** Client-side NeoForge event wiring for hooks that live in other areas. Filled in at integration. */
object PazClientHooks {
    fun registerModBus(modBus: IEventBus) {
    }

    fun registerGameBus(bus: IEventBus) {
    }

    /** Runs on the main thread during FMLClientSetupEvent. */
    fun clientSetup() {
        joshxviii.plantz.data.PazItemModelProperties.register()
    }
}
