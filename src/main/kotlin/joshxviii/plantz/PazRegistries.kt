package joshxviii.plantz

import joshxviii.plantz.api.SeedMutationData
import net.neoforged.neoforge.registries.DataPackRegistryEvent
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.core.registries.Registries.ROOT_REGISTRY_NAME
import net.minecraft.resources.ResourceLocation
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceKey.InternKey

object PazRegistries {
    val PAZ_ROOT_REGISTRY: ResourceLocation = pazResource("root")

    @JvmField val SEED_MUTATION: ResourceKey<Registry<SeedMutationData>> = register("seed_mutation")

    fun <T : Any> register(name: String): ResourceKey<Registry<T>> {
        return ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace(name))
    }

    /** Fabric's DynamicRegistries.register (server-only, not synced) → NeoForge datapack registry without a network codec. */
    fun registerDataPackRegistries(event: DataPackRegistryEvent.NewRegistry) {
        event.dataPackRegistry(SEED_MUTATION, SeedMutationData.CODEC)
    }
}