package joshxviii.plantz

import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.neoforged.neoforge.event.LootTableLoadEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.storage.loot.BuiltInLootTables
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.NestedLootTable
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator

object PazLootTables {
    // seed packets
    @JvmField
    val SUN = registerLootTable("sun")

    @JvmField
    val GRAVESTONE_TREASURE = registerLootTable("chests/gravestone_treasure")

    @JvmField val MAIL_REWARDS_BONUS = registerLootTable("raid/mail_reward_bonus")
    @JvmField val MAIL_REWARD_DEFAULT_EASY = registerLootTable("raid/mail_reward_default_easy")
    @JvmField val MAIL_REWARD_DEFAULT_HARD = registerLootTable("raid/mail_reward_default_hard")
    @JvmField val MAIL_REWARD_BUCKET = registerLootTable("raid/mail_reward_bucket")
    @JvmField val MAIL_REWARD_HALFTIME = registerLootTable("raid/mail_reward_halftime")
    @JvmField val MAIL_REWARD_WINTER = registerLootTable("raid/mail_reward_winter")
    @JvmField val MAIL_REWARD_ZOMBOWEEN = registerLootTable("raid/mail_reward_zomboween")
    @JvmField val MAIL_REWARD_PIRATE = registerLootTable("raid/mail_reward_pirate")
    @JvmField val MAIL_REWARD_ARMY = registerLootTable("raid/mail_reward_army")
    @JvmField val MAIL_REWARD_LEAGUE = registerLootTable("raid/mail_reward_league")
    @JvmField val MAIL_REWARD_ZOMBOSS = registerLootTable("raid/mail_reward_zomboss")

    private fun registerLootTable(
        name: String
    ) : ResourceKey<LootTable> {
        return ResourceKey.create(Registries.LOOT_TABLE, pazResource(name) )
    }

    /** 26.1 BuiltInLootTables.CARVE_PUMPKIN injection; 1.21.1 has no carving loot table, see PumpkinBlockCarveMixin. */
    @JvmField
    val CARVE_PUMPKIN = registerLootTable("injection/carve/pumpkin")

    fun initialize() {}

    /** Drops the [CARVE_PUMPKIN] loot like the [seeds] that carving just dropped (26.1 drops all carve loot that way). */
    @JvmStatic
    fun dropCarveLoot(level: ServerLevel, state: BlockState, player: Player, seeds: ItemEntity) {
        val params = LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .withParameter(LootContextParams.ORIGIN, seeds.position())
            .withParameter(LootContextParams.BLOCK_STATE, state)
            .create(LootContextParamSets.BLOCK_USE)
        for (stack in level.server.reloadableRegistries().getLootTable(CARVE_PUMPKIN).getRandomItems(params)) {
            val drop = ItemEntity(level, seeds.x, seeds.y, seeds.z, stack)
            drop.deltaMovement = seeds.deltaMovement
            level.addFreshEntity(drop)
        }
    }

    /** Fabric's LootTableEvents.MODIFY → NeoForge LootTableLoadEvent. */
    fun onLootTableLoad(event: LootTableLoadEvent) {
        LootInjector.attemptInjection(event.name) { pool -> event.table.addPool(pool.build()) }
    }

}

object LootInjector {
    private const val PREFIX = "injection/"

    private val villageHouseBuiltInLootTables = hashSetOf(
        BuiltInLootTables.VILLAGE_DESERT_HOUSE,
        BuiltInLootTables.VILLAGE_PLAINS_HOUSE,
        BuiltInLootTables.VILLAGE_SAVANNA_HOUSE,
        BuiltInLootTables.VILLAGE_SNOWY_HOUSE,
        BuiltInLootTables.VILLAGE_TAIGA_HOUSE,
    )
    private val injections = hashSetOf(
        BuiltInLootTables.FISHING_FISH,
        BuiltInLootTables.FISHING_TREASURE,
        BuiltInLootTables.BASTION_TREASURE,
        BuiltInLootTables.BASTION_OTHER,
    ).apply { addAll(villageHouseBuiltInLootTables) }

    private val injectionIds = injections.map {it.location()}.toSet()
    private val villageInjectionIds = villageHouseBuiltInLootTables.map { it.location() }.toSet()

    fun attemptInjection(key: ResourceLocation, provider: (LootPool.Builder) -> Unit): Boolean {
        if (!this.injectionIds.contains(key)) {
            return false
        }
        val resulting = convertToPotentialInjected(key)
        PazMain.LOGGER.debug("{}: Injected {} to {}", this::class.simpleName, resulting, key)
        provider(this.injectLootPool(resulting))
        return true
    }

    private fun convertToPotentialInjected(key: ResourceLocation): ResourceLocation {
        return if (this.villageInjectionIds.contains(key))
            pazResource("${PREFIX}chests/village_house")
        else
            pazResource("${PREFIX}${key.path}")
    }

    private fun injectLootPool(resulting: ResourceLocation): LootPool.Builder {
        return LootPool.lootPool()
            .add(
                NestedLootTable
                    .lootTableReference(ResourceKey.create(Registries.LOOT_TABLE, resulting))
                    .setWeight(1)
            )
            .setBonusRolls(UniformGenerator.between(0F, 1F))
    }
}