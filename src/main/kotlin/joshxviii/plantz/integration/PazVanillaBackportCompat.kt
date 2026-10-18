package joshxviii.plantz.integration

import joshxviii.plantz.pazResource
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.sounds.SoundEvent
import net.neoforged.fml.ModList
import net.neoforged.neoforge.event.AddPackFindersEvent

/**
 * Vanilla Backport (`vanillabackport`) registers newer vanilla content (pale garden, leaf litter, firefly bushes,
 * creaking, happy ghast, ...) under the `minecraft` namespace on 1.21.1. When it is installed, this restores what the
 * port had to drop from the 26.1 original:
 * - the built-in data pack `resourcepacks/vanillabackport_compat` (graveyard pale moss / leaf litter / firefly bushes,
 *   pale oak dead trees with moss and litter, gravestones on pale moss);
 * - the 26.1 sounds, through [sound] (falls back to the 1.21.1 substitute without the mod).
 * Optional tag entries such as `minecraft:pale_moss_carpet` or `minecraft:pale_garden` resolve on their own.
 */
object PazVanillaBackportCompat {
    const val MOD_ID = "vanillabackport"

    @JvmField val LOADED: Boolean = ModList.get().isLoaded(MOD_ID)

    private val PACK_LOCATION: ResourceLocation = pazResource("resourcepacks/vanillabackport_compat")

    fun addPackFinders(event: AddPackFindersEvent) {
        if (!LOADED || event.packType != PackType.SERVER_DATA) return
        event.addPackFinders(
            PACK_LOCATION,
            PackType.SERVER_DATA,
            Component.literal("Plants & Zombies: Vanilla Backport features"),
            PackSource.BUILT_IN,
            true,
            Pack.Position.TOP
        )
    }

    /** A sound event registered by Vanilla Backport (26.1 vanilla id), or [fallback] when it isn't there. */
    @JvmStatic
    fun sound(id: String, fallback: SoundEvent): SoundEvent =
        BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.withDefaultNamespace(id)).orElse(fallback)
}
