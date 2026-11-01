package joshxviii.plantz

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent

/**
 * The 26.1 vanilla sounds upstream plays that 1.21.1 doesn't have. Each gets a `plantz:compat.<26.1 id>` event whose
 * definition (assets/plantz/sounds.json) plays the closest 1.21.1 sound with the 26.1 subtitle. When a 26.1+ game is
 * installed on the machine, the built-in "26.1 assets" pack (`client.ModernAssets`) swaps in the real 26.1 sound.
 * A backport that registers the vanilla event itself (Vanilla Backport, Copper Age Backport) is used first.
 */
enum class PazCompatSounds(
    /** The 26.1 sound event id (minecraft namespace). */
    val id: String,
    /** The 1.21.1 sound event used when no 26.1 sound is available (also in sounds.json). */
    val fallback: String,
    /** Other ids a backport registers the same sound under. */
    private vararg val backportIds: String
) {
    COPPER_CHEST_OPEN("block.copper_chest.open", "block.copper_trapdoor.open"),
    COPPER_CHEST_CLOSE("block.copper_chest.close", "block.copper_trapdoor.close"),
    COPPER_GOLEM_STEP("entity.copper_golem.step", "block.copper.step"),
    COPPER_GOLEM_BECOME_STATUE("entity.copper_golem_become_statue", "block.decorated_pot.insert", "entity.copper_golem.become_statue"),
    CREAKING_ACTIVATE("entity.creaking.activate", "block.hanging_roots.break"),
    HAPPY_GHAST_UNEQUIP("entity.happy_ghast.unequip", "item.armor.equip_generic"),
    UI_HUD_BUBBLE_POP("ui.hud.bubble_pop", "block.bubble_column.bubble_pop"),
    LEAD_BREAK("item.lead.break", "entity.leash_knot.break");

    /** The port's stand-in event, `plantz:compat.<id>`. */
    lateinit var compatEvent: SoundEvent
        private set

    /** The sound to play: a backport's own 26.1 event when one is installed, else the port's stand-in. */
    fun get(): SoundEvent {
        for (backportId in listOf(id, *backportIds)) {
            BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.withDefaultNamespace(backportId)).orElse(null)?.let { return it }
        }
        return compatEvent
    }

    companion object {
        fun initialize() {
            for (sound in entries) {
                val location = pazResource("compat." + sound.id)
                sound.compatEvent = Registry.register(BuiltInRegistries.SOUND_EVENT, location, SoundEvent.createVariableRangeEvent(location))
            }
        }
    }
}
