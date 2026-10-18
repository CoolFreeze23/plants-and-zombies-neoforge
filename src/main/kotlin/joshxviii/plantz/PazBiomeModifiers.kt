package joshxviii.plantz

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.Registry
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.MobSpawnSettings
import net.neoforged.neoforge.common.world.BiomeModifier
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo
import net.neoforged.neoforge.registries.NeoForgeRegistries

/**
 * Fabric's `BiomeModifications.addSpawn(selector, category, ...)` puts spawns into an explicit category
 * (upstream adds plants as AMBIENT). NeoForge's built-in `add_spawns` always uses the entity type's own
 * category, so this modifier (`plantz:add_spawns`, data/plantz/neoforge/biome_modifier/) keeps upstream's.
 */
data class AddSpawnsInCategoryBiomeModifier(
    val biomes: HolderSet<Biome>,
    val category: MobCategory,
    val spawners: List<MobSpawnSettings.SpawnerData>,
) : BiomeModifier {

    override fun modify(biome: Holder<Biome>, phase: BiomeModifier.Phase, builder: ModifiableBiomeInfo.BiomeInfo.Builder) {
        if (phase == BiomeModifier.Phase.ADD && biomes.contains(biome)) {
            val spawns = builder.mobSpawnSettings
            spawners.forEach { spawns.addSpawn(category, it) }
        }
    }

    override fun codec(): MapCodec<out BiomeModifier> = CODEC

    companion object {
        val CODEC: MapCodec<AddSpawnsInCategoryBiomeModifier> = RecordCodecBuilder.mapCodec { i ->
            i.group(
                Biome.LIST_CODEC.fieldOf("biomes").forGetter { it.biomes },
                MobCategory.CODEC.fieldOf("category").forGetter { it.category },
                Codec.withAlternative(
                    MobSpawnSettings.SpawnerData.CODEC.listOf(),
                    MobSpawnSettings.SpawnerData.CODEC.xmap({ listOf(it) }, { it.first() })
                ).fieldOf("spawners").forGetter { it.spawners },
            ).apply(i, ::AddSpawnsInCategoryBiomeModifier)
        }
    }
}

object PazBiomeModifiers {
    @JvmField val ADD_SPAWNS: MapCodec<AddSpawnsInCategoryBiomeModifier> =
        Registry.register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, pazResource("add_spawns"), AddSpawnsInCategoryBiomeModifier.CODEC)

    fun initialize() {}
}
