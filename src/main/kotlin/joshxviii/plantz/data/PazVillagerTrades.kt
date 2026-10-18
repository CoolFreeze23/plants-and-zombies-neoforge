package joshxviii.plantz.data

import joshxviii.plantz.pazResource
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.entity.npc.VillagerTrades
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import java.util.Optional

/**
 * Upstream (26.1) adds farmer trades through data-driven `villager_trade` entries tagged into
 * `#minecraft:farmer/level_N` trade sets. 1.21.1 has no such registry, so the same trades are
 * appended here through NeoForge's [net.neoforged.neoforge.event.village.VillagerTradesEvent].
 * Vanilla still picks 2 offers per level from the combined list, like the 26.1 trade sets do.
 *
 * Wire with `NeoForge.EVENT_BUS.addListener(PazVillagerTrades::onVillagerTrades)`.
 */
object PazVillagerTrades {

    /** A count that is either fixed or uniformly rolled in [min, max] (26.1 `count` number provider). */
    private data class Count(val min: Int, val max: Int = min) {
        fun roll(random: RandomSource): Int = Mth.randomBetweenInclusive(random, min, max)
    }

    private data class Cost(val item: ResourceLocation, val count: Count)

    /** Mirrors a 26.1 `villager_trade` JSON entry. */
    private class Trade(
        val wants: Cost,
        val additionalWants: Cost?,
        val gives: ResourceLocation,
        val givesCount: Int = 1,
        val givesEntity: ResourceLocation? = null,
        val maxUses: Int,
        val reputationDiscount: Float,
        val xp: Int,
    ) : VillagerTrades.ItemListing {
        override fun getOffer(trader: Entity, random: RandomSource): MerchantOffer? {
            val result = ItemStack(item(gives), givesCount)
            if (givesEntity != null) {
                result.set(DataComponents.ENTITY_DATA, CustomData.of(CompoundTag().apply { putString("id", givesEntity.toString()) }))
            }
            val cost = toItemCost(wants, random) ?: return null
            val extra = additionalWants?.let { toItemCost(it, random) ?: return null }
            return MerchantOffer(cost, Optional.ofNullable(extra), result, maxOf(maxUses, 1), maxOf(xp, 0), maxOf(reputationDiscount, 0f))
        }

        private fun toItemCost(cost: Cost, random: RandomSource): ItemCost? {
            val item = item(cost.item)
            val count = Mth.clamp(cost.count.roll(random), 0, item.defaultMaxStackSize)
            return if (count < 1) null else ItemCost(item, count)
        }
    }

    private fun item(id: ResourceLocation): Item = BuiltInRegistries.ITEM.get(id)
    private fun mc(path: String) = ResourceLocation.withDefaultNamespace(path)

    // data/plantz/villager_trade/farmer/*.json (upstream e92d5937)
    private val SUN = Trade(
        wants = Cost(pazResource("sun"), Count(32)), additionalWants = null,
        gives = mc("emerald"), givesCount = 1,
        maxUses = 10, reputationDiscount = 0.1f, xp = 5,
    )
    private val WATERING_CAN = Trade(
        wants = Cost(mc("emerald"), Count(10, 15)), additionalWants = Cost(pazResource("sun"), Count(10, 16)),
        gives = pazResource("watering_can"),
        maxUses = 3, reputationDiscount = 0.3f, xp = 25,
    )
    private val POTATOMINE = Trade(
        wants = Cost(mc("emerald"), Count(1, 2)), additionalWants = null,
        gives = pazResource("seed_packet"), givesEntity = pazResource("potatomine"),
        maxUses = 10, reputationDiscount = 0.2f, xp = 8,
    )
    private val GARDENING_GLOVE = Trade(
        wants = Cost(mc("emerald"), Count(12, 20)), additionalWants = Cost(pazResource("sun"), Count(12, 22)),
        gives = pazResource("gardening_glove"),
        maxUses = 1, reputationDiscount = 0.1f, xp = 15,
    )
    private val CHERRYBOMB = Trade(
        wants = Cost(mc("emerald"), Count(8, 16)), additionalWants = null,
        gives = pazResource("seed_packet"), givesEntity = pazResource("cherrybomb"),
        maxUses = 10, reputationDiscount = 0.2f, xp = 8,
    )

    /**
     * Defined upstream (villager_trade/farmer/plant_pot_helmet.json) but not in any farmer level tag,
     * so it is not offered. Kept for parity.
     */
    @Suppress("unused")
    private val PLANT_POT_HELMET = Trade(
        wants = Cost(mc("emerald"), Count(20, 32)), additionalWants = Cost(pazResource("sun"), Count(26, 38)),
        gives = pazResource("plant_pot_helmet"),
        maxUses = 1, reputationDiscount = 0.1f, xp = 10,
    )

    // tags/villager_trade/farmer/level_N.json (levels 4 and 5 were empty)
    private val FARMER_TRADES: Map<Int, List<VillagerTrades.ItemListing>> = mapOf(
        1 to listOf(SUN),
        2 to listOf(WATERING_CAN, POTATOMINE),
        3 to listOf(GARDENING_GLOVE, CHERRYBOMB),
    )

    fun onVillagerTrades(event: net.neoforged.neoforge.event.village.VillagerTradesEvent) {
        if (event.type != VillagerProfession.FARMER) return
        val trades = event.trades
        for ((level, listings) in FARMER_TRADES) {
            val list: MutableList<VillagerTrades.ItemListing> =
                trades.get(level) ?: ArrayList<VillagerTrades.ItemListing>().also { trades.put(level, it) }
            list.addAll(listings)
        }
    }
}
