package joshxviii.plantz.gametest

import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazRegistries
import joshxviii.plantz.PazTags
import joshxviii.plantz.integration.PazCopperAgeBackportCompat
import joshxviii.plantz.pazResource
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.locale.Language
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.BlockTags
import net.minecraft.tags.DamageTypeTags
import net.minecraft.tags.EntityTypeTags
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.item.SpawnEggItem
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.entity.npc.VillagerTrades
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.DebugLevelSource
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
import net.minecraft.world.level.dimension.LevelStem
import net.minecraft.resources.RegistryOps
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3
import net.neoforged.fml.ModList
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

/**
 * Data parity tests: recipes, loot tables, advancements, tags, worldgen, trades and data registries behave like the
 * upstream 26.1 data. The recipe and advancement tables are generated from the untouched upstream JSON (e92d5937).
 * They pass with and without Vanilla Backport and Copper Age Backport (`./gradlew runGameTestServer -PwithVanillaBackport
 * -PwithCopperAgeBackport`).
 */
@GameTestHolder("plantz")
@PrefixGameTestTemplate(false)
object DataGameTests {
    private const val PLATFORM = CoreGameTests.PLATFORM
    private val vanillaBackport: Boolean get() = ModList.get().isLoaded("vanillabackport")
    private val copperAgeBackport: Boolean get() = ModList.get().isLoaded("copperagebackport")

    private fun rl(id: String) = ResourceLocation.parse(id)
    private fun item(id: String): Item {
        check(BuiltInRegistries.ITEM.containsKey(rl(id))) { "unknown item $id" }
        return BuiltInRegistries.ITEM.get(rl(id))
    }
    private fun block(id: String): Block {
        check(BuiltInRegistries.BLOCK.containsKey(rl(id))) { "unknown block $id" }
        return BuiltInRegistries.BLOCK.get(rl(id))
    }
    private fun itemId(stack: ItemStack) = BuiltInRegistries.ITEM.getKey(stack.item).toString()
    private fun entityData(stack: ItemStack): String? =
        stack.get(DataComponents.ENTITY_DATA)?.copyTag()?.getString("id")?.takeIf { it.isNotEmpty() }

    // ---------------------------------------------------------------- recipes

    /** One upstream recipe: grid cells ("" empty, "#tag" = first tag member) and the expected result. */
    private class R(val id: String, val shaped: Boolean, val w: Int, val h: Int, val cells: List<String>, val result: String, val count: Int)

    // generated from upstream data/plantz/recipe/**.json; mailbox transmutes start from a differently dyed mailbox.
    private val RECIPES = listOf(
        R("balloon/balloon", true, 3, 3, listOf("", "minecraft:phantom_membrane", "", "", "minecraft:wind_charge", "", "", "minecraft:string", ""), "plantz:white_balloon", 8),
        R("balloon/black_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:black_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:black_balloon", 8),
        R("balloon/blue_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:blue_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:blue_balloon", 8),
        R("balloon/brown_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:brown_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:brown_balloon", 8),
        R("balloon/cyan_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:cyan_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:cyan_balloon", 8),
        R("balloon/gray_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:gray_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:gray_balloon", 8),
        R("balloon/green_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:green_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:green_balloon", 8),
        R("balloon/light_blue_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:light_blue_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:light_blue_balloon", 8),
        R("balloon/light_gray_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:light_gray_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:light_gray_balloon", 8),
        R("balloon/lime_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:lime_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:lime_balloon", 8),
        R("balloon/magenta_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:magenta_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:magenta_balloon", 8),
        R("balloon/orange_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:orange_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:orange_balloon", 8),
        R("balloon/pink_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:pink_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:pink_balloon", 8),
        R("balloon/purple_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:purple_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:purple_balloon", 8),
        R("balloon/red_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:red_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:red_balloon", 8),
        R("balloon/white_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:white_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:white_balloon", 8),
        R("balloon/yellow_balloon", false, 9, 1, listOf("#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "minecraft:yellow_dye", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons", "#plantz:balloons"), "plantz:yellow_balloon", 8),
        R("brainz_alloy", false, 9, 1, listOf("plantz:brainzium", "minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot"), "plantz:brainz_alloy", 16),
        R("brainz_alloy_block", true, 3, 3, listOf("plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy"), "plantz:brainz_alloy_block", 1),
        R("brainz_alloy_by_block", false, 1, 1, listOf("plantz:brainz_alloy_block"), "plantz:brainz_alloy", 9),
        R("brainz_alloy_fence", true, 3, 2, listOf("plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy"), "plantz:brainz_alloy_fence", 16),
        R("brainz_alloy_slab", true, 3, 1, listOf("plantz:brainz_alloy_block", "plantz:brainz_alloy_block", "plantz:brainz_alloy_block"), "plantz:brainz_alloy_slab", 6),
        R("brainz_alloy_stairs", true, 3, 3, listOf("plantz:brainz_alloy_block", "", "", "plantz:brainz_alloy_block", "plantz:brainz_alloy_block", "", "plantz:brainz_alloy_block", "plantz:brainz_alloy_block", "plantz:brainz_alloy_block"), "plantz:brainz_alloy_stairs", 4),
        R("mail_collection_box", true, 3, 3, listOf("plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:brainz_alloy", "plantz:newspaper", "plantz:brainz_alloy", "minecraft:redstone", "#plantz:mailboxes", "minecraft:redstone"), "plantz:mail_collection_box", 1),
        R("mailbox/black_mailbox", false, 2, 1, listOf("plantz:mailbox", "minecraft:black_dye"), "plantz:black_mailbox", 1),
        R("mailbox/blue_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:blue_dye"), "plantz:blue_mailbox", 1),
        R("mailbox/brown_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:brown_dye"), "plantz:brown_mailbox", 1),
        R("mailbox/cyan_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:cyan_dye"), "plantz:cyan_mailbox", 1),
        R("mailbox/gray_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:gray_dye"), "plantz:gray_mailbox", 1),
        R("mailbox/green_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:green_dye"), "plantz:green_mailbox", 1),
        R("mailbox/light_blue_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:light_blue_dye"), "plantz:light_blue_mailbox", 1),
        R("mailbox/light_gray_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:light_gray_dye"), "plantz:light_gray_mailbox", 1),
        R("mailbox/lime_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:lime_dye"), "plantz:lime_mailbox", 1),
        R("mailbox/magenta_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:magenta_dye"), "plantz:magenta_mailbox", 1),
        R("mailbox/mailbox", true, 3, 2, listOf("minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "plantz:newspaper", "minecraft:iron_trapdoor"), "plantz:mailbox", 1),
        R("mailbox/orange_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:orange_dye"), "plantz:orange_mailbox", 1),
        R("mailbox/pink_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:pink_dye"), "plantz:pink_mailbox", 1),
        R("mailbox/purple_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:purple_dye"), "plantz:purple_mailbox", 1),
        R("mailbox/red_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:red_dye"), "plantz:red_mailbox", 1),
        R("mailbox/white_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:white_dye"), "plantz:mailbox", 1),
        R("mailbox/yellow_mailbox", false, 2, 1, listOf("plantz:black_mailbox", "minecraft:yellow_dye"), "plantz:yellow_mailbox", 1),
        R("plant_pot", true, 3, 3, listOf("", "plantz:sun", "", "minecraft:brick", "minecraft:dirt", "minecraft:brick", "", "minecraft:brick", ""), "plantz:plant_pot", 1),
        R("plant_pot_minecart", false, 2, 1, listOf("plantz:plant_pot", "minecraft:minecart"), "plantz:plant_pot_minecart", 1),
        R("plantz_flag", true, 3, 3, listOf("plantz:sun", "plantz:sun", "plantz:sun", "plantz:sun", "plantz:brainz_flag", "plantz:sun", "plantz:sun", "plantz:sun", "plantz:sun"), "plantz:plantz_flag", 1),
        R("reinforced_brainz_alloy_block", true, 3, 3, listOf("minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "plantz:brainz_alloy_block", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot"), "plantz:reinforced_brainz_alloy_block", 1),
        R("sun", false, 1, 1, listOf("plantz:sun_bottle"), "plantz:sun", 8),
        R("sun_battery", true, 3, 3, listOf("plantz:brainz_alloy", "minecraft:daylight_detector", "plantz:brainz_alloy", "minecraft:glass", "", "minecraft:glass", "plantz:brainz_alloy", "plantz:reinforced_brainz_alloy_block", "plantz:brainz_alloy"), "plantz:sun_battery", 1),
        R("sun_bottle", true, 3, 3, listOf("plantz:sun", "plantz:sun", "plantz:sun", "plantz:sun", "minecraft:glass_bottle", "plantz:sun", "plantz:sun", "plantz:sun", "plantz:sun"), "plantz:sun_bottle", 1),
        R("taco", false, 5, 1, listOf("#c:foods/bread", "minecraft:cooked_beef", "#c:foods/leafy_green", "#c:crops/tomato", "#c:crops/onion"), "plantz:taco", 1),
        R("time_machine", true, 3, 3, listOf("plantz:brainz_alloy", "minecraft:amethyst_shard", "plantz:brainz_alloy", "minecraft:redstone", "plantz:brainzium", "minecraft:redstone", "plantz:reinforced_brainz_alloy_block", "plantz:reinforced_brainz_alloy_block", "plantz:reinforced_brainz_alloy_block"), "plantz:time_machine", 1),
        R("treaded_brainz_alloy_block", true, 2, 2, listOf("plantz:brainz_alloy", "plantz:brainz_alloy_block", "plantz:brainz_alloy_block", "plantz:brainz_alloy"), "plantz:treaded_brainz_alloy_block", 2),
        R("zen_plant_pot", true, 3, 3, listOf("", "", "", "", "plantz:plant_pot", "", "minecraft:bamboo", "minecraft:bamboo_slab", "minecraft:bamboo"), "plantz:zen_plant_pot", 1),
    )

    private fun tagItems(tag: String): List<Item> =
        BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, rl(tag))).map { set -> set.map { it.value() } }.orElse(emptyList())

    /** Every upstream recipe id exists, and crafting the upstream ingredients gives the upstream result and count. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun recipesMatchUpstream(helper: GameTestHelper) {
        val level = helper.level
        val manager = level.recipeManager
        val problems = mutableListOf<String>()
        val loaded = manager.recipes.filter { it.id().namespace == "plantz" }.map { it.id().path }.toSet()
        val expected = RECIPES.map { it.id }.toSet()
        if (loaded != expected) problems += "recipe ids differ: missing=${expected - loaded} extra=${loaded - expected}"
        for (r in RECIPES) {
            if (r.cells.any { it.startsWith("#") && tagItems(it.substring(1)).isEmpty() }) continue // needs another mod's tag (taco)
            val stacks = r.cells.map { c ->
                when {
                    c.isEmpty() -> ItemStack.EMPTY
                    c.startsWith("#") -> ItemStack(tagItems(c.substring(1)).first())
                    else -> ItemStack(item(c))
                }
            }
            val input = if (r.shaped) CraftingInput.of(r.w, r.h, stacks)
                else CraftingInput.of(3, 3, stacks + List(9 - stacks.size) { ItemStack.EMPTY })
            val found = manager.getRecipeFor(RecipeType.CRAFTING, input, level)
            if (found.isEmpty) { problems += "${r.id}: nothing matches"; continue }
            val out = found.get().value().assemble(input, level.registryAccess())
            if (itemId(out) != r.result || out.count != r.count)
                problems += "${r.id}: got ${out.count}x ${itemId(out)} (via ${found.get().id()}), want ${r.count}x ${r.result}"
        }
        helper.assertTrue(problems.isEmpty(), problems.joinToString("; "))
        helper.succeed()
    }

    /** The taco needs conventional food tags other mods fill; the recipe still loads with all five ingredients. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun tacoRecipeLoads(helper: GameTestHelper) {
        val recipe = helper.level.recipeManager.byKey(pazResource("taco"))
        helper.assertTrue(recipe.isPresent, "plantz:taco recipe missing")
        val ingredients = recipe.get().value().ingredients
        helper.assertTrue(ingredients.size == 5, "taco has ${ingredients.size} ingredients, want 5")
        helper.assertTrue(ingredients.any { ing -> ing.items.any { it.`is`(Items.COOKED_BEEF) } }, "taco lost its cooked beef")
        helper.succeed()
    }

    // ---------------------------------------------------------------- loot

    private fun origin(helper: GameTestHelper) = Vec3.atCenterOf(helper.absolutePos(BlockPos(7, 1, 7)))

    private fun chestParams(helper: GameTestHelper) = LootParams.Builder(helper.level)
        .withParameter(LootContextParams.ORIGIN, origin(helper))
        .create(LootContextParamSets.CHEST)

    private fun roll(helper: GameTestHelper, id: String, times: Int, params: LootParams): List<ItemStack> {
        val table = helper.level.server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, rl(id)))
        helper.assertTrue(table !== LootTable.EMPTY, "loot table $id missing")
        val out = mutableListOf<ItemStack>()
        // independent seeds (consecutive seeds give correlated first rolls); empty stacks are dropped like containers do
        repeat(times) { out += table.getRandomItems(params, helper.level.random.nextLong() or 1L) }
        return out.filter { !it.isEmpty }
    }

    /** Gravestone treasure rolls only upstream's items (its copper nuggets are copper ingots without Copper Age Backport). */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun gravestoneTreasureRollsUpstreamItems(helper: GameTestHelper) {
        val copper = if (copperAgeBackport) "minecraft:copper_nugget" else "minecraft:copper_ingot"
        val allowed = setOf(
            copper, "minecraft:copper_ingot", "minecraft:emerald", "minecraft:gold_ingot", "minecraft:gold_nugget",
            "minecraft:iron_ingot", "minecraft:iron_nugget", "minecraft:ominous_bottle", "minecraft:rotten_flesh",
            "plantz:brainz_alloy", "plantz:cone", "plantz:ducky_tube", "plantz:newspaper",
        )
        val drops = roll(helper, "plantz:chests/gravestone_treasure", 400, chestParams(helper))
        val ids = drops.map(::itemId).toSet()
        helper.assertTrue(allowed.containsAll(ids), "unexpected treasure: ${ids - allowed}")
        helper.assertTrue(ids.containsAll(listOf("minecraft:rotten_flesh", copper, "plantz:cone", "plantz:brainz_alloy", "minecraft:ominous_bottle")),
            "treasure never rolled: ${allowed - ids}")
        if (copperAgeBackport) helper.assertTrue(drops.filter { itemId(it) == copper }.all { it.count in 1..4 }, "copper nuggets outside 1-4")
        val bottle = drops.first { it.`is`(Items.OMINOUS_BOTTLE) }
        helper.assertTrue(bottle.has(DataComponents.OMINOUS_BOTTLE_AMPLIFIER), "ominous bottle without an amplifier")
        helper.succeed()
    }

    /** Every raid mail reward table (and the tables it nests) yields items. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun raidMailRewardsRoll(helper: GameTestHelper) {
        val params = chestParams(helper)
        for (name in listOf("army", "bonus", "bucket", "default_easy", "default_hard", "halftime", "league", "pirate", "winter", "zomboss")) {
            helper.assertTrue(roll(helper, "plantz:raid/mail_reward_$name", 20, params).isNotEmpty(), "raid/mail_reward_$name rolled nothing in 20 tries")
        }
        helper.succeed()
    }

    /** The robot/boss zombies drop brainz alloy through their own loot tables, like upstream. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun zombieEntityDrops(helper: GameTestHelper) {
        for (type in listOf("gargantuar", "robo_zombie", "super_brainz", "engineer_zombie")) {
            @Suppress("UNCHECKED_CAST")
            val mob = helper.spawnWithNoFreeWill(BuiltInRegistries.ENTITY_TYPE.get(pazResource(type)) as EntityType<Mob>, 7, 1, 7)
            helper.assertTrue(mob.lootTable.location() == pazResource("entities/$type"), "$type uses ${mob.lootTable.location()}")
            val params = LootParams.Builder(helper.level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, helper.level.damageSources().generic())
                .create(LootContextParamSets.ENTITY)
            val ids = roll(helper, "plantz:entities/$type", 60, params).map(::itemId).toSet()
            helper.assertTrue(ids == setOf("plantz:brainz_alloy"), "$type drops $ids")
            mob.discard()
        }
        helper.succeed()
    }

    /** Plantz pools are injected into village house, bastion and fishing loot (upstream's LootTableEvents.MODIFY). */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun injectedVanillaLoot(helper: GameTestHelper) {
        val chest = chestParams(helper)
        val village = roll(helper, "minecraft:chests/village/village_plains_house", 300, chest).filter { itemId(it).startsWith("plantz:") }
        helper.assertTrue(village.isNotEmpty(), "no plantz loot in village houses")
        helper.assertTrue(village.all { itemId(it) == "plantz:watering_can" || entityData(it) != null }, "village seed packet without a plant")
        for (bastion in listOf("bastion_other", "bastion_treasure")) {
            val packets = roll(helper, "minecraft:chests/$bastion", 300, chest).filter { itemId(it) == "plantz:seed_packet" }.mapNotNull(::entityData).toSet()
            helper.assertTrue(packets.isNotEmpty() && packets.all { it == "plantz:cherrybomb" || it == "plantz:doomshroom" }, "$bastion seed packets: $packets")
        }
        val fishing = LootParams.Builder(helper.level)
            .withParameter(LootContextParams.ORIGIN, origin(helper))
            .withParameter(LootContextParams.TOOL, ItemStack(Items.FISHING_ROD))
            .create(LootContextParamSets.FISHING)
        val fish = roll(helper, "minecraft:gameplay/fishing/fish", 400, fishing).filter { itemId(it).startsWith("plantz:") }
        helper.assertTrue(fish.isNotEmpty() && fish.all { entityData(it) == "plantz:seashroom" }, "fishing/fish plantz loot: ${fish.map(::entityData)}")
        val treasure = roll(helper, "minecraft:gameplay/fishing/treasure", 200, fishing).map(::itemId).filter { it.startsWith("plantz:") }.toSet()
        helper.assertTrue(treasure == setOf("plantz:ducky_tube", "plantz:watering_can"), "fishing/treasure plantz loot: $treasure")
        helper.succeed()
    }

    /** Block tables: a player-broken Plantz flag drops itself; the gravestone needs silk touch. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun blockLootTables(helper: GameTestHelper) {
        fun params(block: Block, tool: ItemStack) = LootParams.Builder(helper.level)
            .withParameter(LootContextParams.ORIGIN, origin(helper))
            .withParameter(LootContextParams.TOOL, tool)
            .withParameter(LootContextParams.BLOCK_STATE, block.defaultBlockState())
            .create(LootContextParamSets.BLOCK)
        helper.assertTrue(roll(helper, "plantz:blocks/plantz_flag", 5, params(block("plantz:plantz_flag"), ItemStack.EMPTY)).all { itemId(it) == "plantz:plantz_flag" }, "plantz flag drop")
        val grave = block("plantz:gravestone")
        helper.assertTrue(roll(helper, "plantz:blocks/gravestone", 5, params(grave, ItemStack(Items.IRON_PICKAXE))).isEmpty(), "gravestone dropped without silk touch")
        val silk = ItemStack(Items.IRON_PICKAXE)
        silk.enchant(helper.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), 1)
        val silkDrops = roll(helper, "plantz:blocks/gravestone", 3, params(grave, silk))
        helper.assertTrue(silkDrops.isNotEmpty() && silkDrops.all { itemId(it) == "plantz:gravestone" }, "silk touch gravestone drops ${silkDrops.map(::itemId)}")
        helper.succeed()
    }

    // ---------------------------------------------------------------- advancements

    private class A(val id: String, val parent: String, val criteria: List<String>, val requirements: Int, val recipes: List<String>, val loot: List<String>, val xp: Int)

    // generated from upstream data/plantz/advancement/**.json
    private val ADVANCEMENTS = listOf(
        A("plantz:big_three", "plantz:cone_head", listOf("plantz:all_star", "plantz:disco_zombie", "plantz:zombie_yeti"), 3, listOf(), listOf(), 0),
        A("plantz:bigger_three", "plantz:big_three", listOf("plantz:all_star", "plantz:disco_zombie", "plantz:zombie_yeti"), 3, listOf(), listOf(), 0),
        A("plantz:cone_head", "plantz:root", listOf("wearing_cone"), 1, listOf(), listOf(), 0),
        A("plantz:craft_plant_flag", "plantz:root", listOf("craft_flag"), 1, listOf(), listOf(), 0),
        A("plantz:disco_hypno", "plantz:big_three", listOf("disco_hypno"), 1, listOf(), listOf(), 0),
        A("plantz:grave_buster_bust", "plantz:grow_plant_seeds", listOf("grave_buster_bust"), 1, listOf(), listOf(), 0),
        A("plantz:grow_plant_seeds", "plantz:relocation", listOf("grow_seeds"), 1, listOf(), listOf(), 0),
        A("plantz:plant_all_plants", "plantz:grave_buster_bust", listOf("plantz:bonkchoy", "plantz:cabbagepult", "plantz:cactus", "plantz:cherrybomb", "plantz:chomper", "plantz:coffeebean", "plantz:doomshroom", "plantz:electric_peashooter", "plantz:explode_o_nut", "plantz:fire_peashooter", "plantz:fumeshroom", "plantz:grave_buster", "plantz:hypnoshroom", "plantz:ice_peashooter", "plantz:iceshroom", "plantz:kernelpult", "plantz:lightning_reed", "plantz:melonpult", "plantz:peashooter", "plantz:potatomine", "plantz:puffshroom", "plantz:repeater", "plantz:scaredyshroom", "plantz:seashroom", "plantz:sunflower", "plantz:sunshroom", "plantz:tanglekelp", "plantz:wallnut", "plantz:winter_melon"), 29, listOf(), listOf(), 0),
        A("plantz:plant_pot_minecart", "plantz:grow_plant_seeds", listOf("plant_pot_minecart"), 1, listOf(), listOf(), 0),
        A("plantz:read_newspaper", "plantz:root", listOf("read_newspaper"), 1, listOf(), listOf(), 0),
        A("plantz:receive_hero_mail", "plantz:send_mail", listOf("receive_hero_mail"), 1, listOf(), listOf(), 0),
        A("plantz:relocation", "plantz:root", listOf("relocate"), 1, listOf(), listOf(), 0),
        A("plantz:root", "", listOf("has_sun"), 1, listOf(), listOf(), 0),
        A("plantz:send_mail", "plantz:read_newspaper", listOf("send_mail"), 1, listOf(), listOf(), 0),
        A("plantz:solar_power", "plantz:plant_pot_minecart", listOf("has_sun_battery"), 1, listOf(), listOf(), 0),
        A("plantz:start_hardest_raid", "plantz:win_zombie_raid", listOf("start_hardest_raid"), 1, listOf(), listOf(), 0),
        A("plantz:taco_time", "plantz:craft_plant_flag", listOf("wave_1", "wave_10", "wave_2", "wave_3", "wave_4", "wave_5", "wave_6", "wave_7", "wave_8", "wave_9"), 10, listOf(), listOf(), 0),
        A("plantz:time_machine", "plantz:solar_power", listOf("has_time_machine"), 1, listOf(), listOf(), 0),
        A("plantz:wallnut_bowling", "plantz:plant_pot_minecart", listOf("bowl"), 1, listOf(), listOf(), 0),
        A("plantz:win_zombie_raid", "plantz:craft_plant_flag", listOf("win_zombie_raid"), 1, listOf(), listOf(), 0),
        A("plantz:recipes/balloon", "minecraft:recipes/root", listOf("has_the_recipe", "has_wind_charge"), 1, listOf("plantz:balloon/balloon"), listOf(), 0),
        A("plantz:recipes/balloon_dyes", "plantz:recipes/balloon", listOf("has_balloon", "has_the_recipe"), 1, listOf("plantz:balloon/black_balloon", "plantz:balloon/blue_balloon", "plantz:balloon/brown_balloon", "plantz:balloon/cyan_balloon", "plantz:balloon/gray_balloon", "plantz:balloon/green_balloon", "plantz:balloon/light_blue_balloon", "plantz:balloon/light_gray_balloon", "plantz:balloon/lime_balloon", "plantz:balloon/magenta_balloon", "plantz:balloon/orange_balloon", "plantz:balloon/pink_balloon", "plantz:balloon/purple_balloon", "plantz:balloon/red_balloon", "plantz:balloon/white_balloon", "plantz:balloon/yellow_balloon"), listOf(), 0),
        A("plantz:recipes/brainz_alloy", "minecraft:recipes/root", listOf("has_brainz_alloy_block", "has_the_recipe"), 1, listOf("plantz:brainz_alloy_by_block"), listOf(), 0),
        A("plantz:recipes/brainz_alloy_blocks", "minecraft:recipes/root", listOf("has_brainz_alloy", "has_the_recipe"), 1, listOf("plantz:brainz_alloy_block", "plantz:brainz_alloy_by_block", "plantz:brainz_alloy_fence", "plantz:brainz_alloy_slab", "plantz:brainz_alloy_stairs", "plantz:reinforced_brainz_alloy_block", "plantz:sun_battery", "plantz:treaded_brainz_alloy_block"), listOf(), 0),
        A("plantz:recipes/brainzium_crafting", "minecraft:recipes/root", listOf("has_brainzium", "has_the_recipe"), 1, listOf("plantz:brainz_alloy", "plantz:time_machine"), listOf(), 0),
        A("plantz:recipes/mail_collection_box", "minecraft:recipes/root", listOf("has_brainz_alloy", "has_mailbox", "has_the_recipe"), 1, listOf("plantz:mail_collection_box"), listOf(), 0),
        A("plantz:recipes/mailbox", "minecraft:recipes/root", listOf("has_newspaper", "has_the_recipe"), 1, listOf("plantz:mailbox/mailbox"), listOf(), 0),
        A("plantz:recipes/mailbox_dyes", "plantz:recipes/mailbox", listOf("has_mailbox", "has_the_recipe"), 1, listOf("plantz:mailbox/black_mailbox", "plantz:mailbox/blue_mailbox", "plantz:mailbox/brown_mailbox", "plantz:mailbox/cyan_mailbox", "plantz:mailbox/gray_mailbox", "plantz:mailbox/green_mailbox", "plantz:mailbox/light_blue_mailbox", "plantz:mailbox/light_gray_mailbox", "plantz:mailbox/lime_mailbox", "plantz:mailbox/magenta_mailbox", "plantz:mailbox/orange_mailbox", "plantz:mailbox/pink_mailbox", "plantz:mailbox/purple_mailbox", "plantz:mailbox/red_mailbox", "plantz:mailbox/white_mailbox", "plantz:mailbox/yellow_mailbox"), listOf(), 0),
        A("plantz:recipes/plant_pot", "minecraft:recipes/root", listOf("has_sun", "has_the_recipe"), 1, listOf("plantz:plant_pot"), listOf(), 0),
        A("plantz:recipes/plant_pot_minecart", "minecraft:recipes/root", listOf("has_plant_pot", "has_the_recipe"), 1, listOf("plantz:plant_pot_minecart"), listOf(), 0),
        A("plantz:recipes/plantz_flag", "minecraft:recipes/root", listOf("has_flag", "has_the_recipe"), 1, listOf("plantz:plantz_flag"), listOf(), 0),
        A("plantz:recipes/sun", "minecraft:recipes/root", listOf("has_sun_bottle", "has_the_recipe"), 1, listOf("plantz:sun"), listOf(), 0),
        A("plantz:recipes/sun_bottle", "minecraft:recipes/root", listOf("has_glass_bottle", "has_the_recipe"), 1, listOf("plantz:sun_bottle"), listOf(), 0),
        A("plantz:recipes/taco", "minecraft:recipes/root", listOf("has_cooked_beef", "has_the_recipe"), 1, listOf("plantz:taco"), listOf(), 0),
        A("plantz:recipes/zen_plant_pot", "minecraft:recipes/root", listOf("has_plant_pot", "has_the_recipe"), 1, listOf("plantz:zen_plant_pot"), listOf(), 0),
    )

    /** Every upstream advancement loads with the same parent, criteria, requirement count and rewards. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun advancementsMatchUpstream(helper: GameTestHelper) {
        val manager = helper.level.server.advancements
        val problems = mutableListOf<String>()
        val loaded = manager.allAdvancements.filter { it.id().namespace == "plantz" }.map { it.id().toString() }.toSet()
        val expected = ADVANCEMENTS.map { it.id }.toSet()
        if (loaded != expected) problems += "advancement ids differ: missing=${expected - loaded} extra=${loaded - expected}"
        for (a in ADVANCEMENTS) {
            val adv = manager.get(rl(a.id))?.value() ?: continue
            if (adv.parent().map { it.toString() }.orElse("") != a.parent) problems += "${a.id}: parent ${adv.parent()}"
            if (adv.criteria().keys.sorted() != a.criteria) problems += "${a.id}: criteria ${adv.criteria().keys.sorted()}"
            if (adv.requirements().size() != a.requirements) problems += "${a.id}: ${adv.requirements().size()} requirements"
            val rewards = adv.rewards()
            if (rewards.recipes().map { it.toString() }.sorted() != a.recipes) problems += "${a.id}: recipe rewards ${rewards.recipes()}"
            if (rewards.loot().map { it.location().toString() }.sorted() != a.loot) problems += "${a.id}: loot rewards ${rewards.loot()}"
            if (rewards.experience() != a.xp) problems += "${a.id}: xp ${rewards.experience()}"
            for (r in rewards.recipes()) if (helper.level.recipeManager.byKey(r).isEmpty) problems += "${a.id}: reward recipe $r missing"
            if (adv.display().isPresent && adv.display().get().icon.isEmpty) problems += "${a.id}: empty icon"
        }
        helper.assertTrue(problems.isEmpty(), problems.joinToString("; "))
        helper.succeed()
    }

    /**
     * Copper Age Backport brings back upstream's copper content (its pack, the copper shovel icon, copper gnome tools and
     * the copper chest / copper golem sounds); without it the 1.21.1 stand-ins are used.
     */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun copperAgeBackportContent(helper: GameTestHelper) {
        val server = helper.level.server
        val packOn = server.packRepository.selectedIds.any { it.contains("copperagebackport_compat") }
        helper.assertTrue(packOn == copperAgeBackport, "copper pack enabled=$packOn, mod loaded=$copperAgeBackport")
        val icon = server.advancements.get(pazResource("relocation"))!!.value().display().get().icon
        helper.assertTrue(itemId(icon) == if (copperAgeBackport) "minecraft:copper_shovel" else "minecraft:iron_shovel", "relocation icon ${itemId(icon)}")

        val tools = mutableSetOf<String>()
        repeat(120) {
            val gnome = helper.spawnWithNoFreeWill(PazEntities.GNOME, 7, 2, 7)
            gnome.finalizeSpawn(helper.level, helper.level.getCurrentDifficultyAt(gnome.blockPosition()), MobSpawnType.NATURAL, null)
            gnome.mainHandItem.takeIf { !it.isEmpty }?.let { tools += itemId(it) }
            gnome.discard()
        }
        val expectedTools = if (copperAgeBackport) setOf("minecraft:copper_axe", "minecraft:stone_sword", "minecraft:bow", "minecraft:copper_pickaxe")
            else setOf("minecraft:stone_axe", "minecraft:stone_sword", "minecraft:bow", "minecraft:stone_pickaxe")
        helper.assertTrue(tools == expectedTools, "gnome tools $tools, expected $expectedTools")

        for ((id, fallback) in listOf("block.copper_chest.open" to SoundEvents.COPPER_TRAPDOOR_OPEN, "block.copper_chest.close" to SoundEvents.COPPER_TRAPDOOR_CLOSE,
                "entity.copper_golem.become_statue" to SoundEvents.DECORATED_POT_INSERT)) {
            val sound = PazCopperAgeBackportCompat.sound(id, fallback).location
            helper.assertTrue(sound == if (copperAgeBackport) ResourceLocation.withDefaultNamespace(id) else fallback.location, "$id plays $sound")
        }
        helper.succeed()
    }

    // ---------------------------------------------------------------- tags

    /** Tag membership the code relies on: plantable soil, entity groups, damage groups, armor/weapon tags, biome tags. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun tagMembership(helper: GameTestHelper) {
        val problems = mutableListOf<String>()
        fun expect(ok: Boolean, what: String) { if (!ok) problems += what }
        fun blockIs(id: String, tag: TagKey<Block>) = block(id).defaultBlockState().`is`(tag)
        for (soil in listOf("minecraft:grass_block", "minecraft:podzol", "minecraft:mycelium", "minecraft:dirt", "minecraft:coarse_dirt",
                "minecraft:rooted_dirt", "minecraft:moss_block", "minecraft:mud", "minecraft:farmland", "minecraft:clay", "minecraft:dirt_path",
                "minecraft:lily_pad", "minecraft:soul_soil", "minecraft:moss_carpet", "minecraft:crimson_nylium", "plantz:plant_pot", "plantz:zen_plant_pot"))
            expect(blockIs(soil, PazTags.BlockTags.PLANTABLE), "$soil not plantable")
        for (no in listOf("minecraft:stone", "minecraft:sand", "minecraft:oak_planks")) expect(!blockIs(no, PazTags.BlockTags.PLANTABLE), "$no plantable")
        if (vanillaBackport) for (pale in listOf("minecraft:pale_moss_block", "minecraft:pale_moss_carpet")) expect(blockIs(pale, PazTags.BlockTags.PLANTABLE), "$pale not plantable")
        expect(blockIs("minecraft:farmland", PazTags.BlockTags.FARMABLE) && blockIs("plantz:zen_plant_pot", PazTags.BlockTags.FARMABLE), "farmable")
        expect(blockIs("plantz:gravestone", BlockTags.MINEABLE_WITH_PICKAXE), "gravestone not pickaxe-mineable")
        expect(blockIs("plantz:brainz_alloy_block", BlockTags.BEACON_BASE_BLOCKS), "brainz alloy block not a beacon base")

        fun entityIs(id: String, tag: TagKey<EntityType<*>>) = BuiltInRegistries.ENTITY_TYPE.get(rl(id)).`is`(tag)
        expect(entityIs("plantz:browncoat", EntityTypeTags.ZOMBIES), "browncoat not in #minecraft:zombies")
        expect(entityIs("plantz:peashooter", PazTags.EntityTypes.PLANT), "peashooter not a plant")
        expect(entityIs("plantz:electric_peashooter", PazTags.EntityTypes.IMMUNE_TO_ELECTRICITY), "electric peashooter not immune to electricity")
        expect(entityIs("plantz:zombie_yeti", EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES), "zombie yeti not freeze immune")
        expect(entityIs("minecraft:camel", PazTags.EntityTypes.CANNOT_CHOMP), "camel chompable")

        fun itemIs(id: String, tag: TagKey<Item>) = ItemStack(item(id)).`is`(tag)
        for (w in listOf("minecraft:bow", "minecraft:mace", "minecraft:iron_axe")) expect(itemIs(w, PazTags.ItemTags.GNOME_PREFERRED_WEAPONS), "$w not a gnome weapon")
        expect(itemIs("minecraft:bucket", ItemTags.HEAD_ARMOR), "bucket not head armor")
        expect(itemIs("plantz:ducky_tube", ItemTags.LEG_ARMOR), "ducky tube not leg armor")
        expect(itemIs("plantz:gardening_glove", ItemTags.DURABILITY_ENCHANTABLE), "gardening glove not durability-enchantable")

        val damage = helper.level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
        fun damageIs(id: String, tag: TagKey<DamageType>) = damage.getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, rl(id))).`is`(tag)
        expect(damageIs("plantz:plant_fume", DamageTypeTags.BYPASSES_ARMOR), "plant fume doesn't bypass armor")
        expect(damageIs("plantz:plant_freeze", DamageTypeTags.IS_FREEZING), "plant freeze isn't freezing")
        expect(damageIs("plantz:plant_fume", DamageTypeTags.NO_ANGER), "plant fume angers")

        val graveyard = helper.level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME, pazResource("graveyard")))
        expect(graveyard.`is`(PazTags.Biomes.HAS_GRAVE_BUSTER), "graveyard lacks grave busters")
        expect(graveyard.`is`(TagKey.create(Registries.BIOME, rl("c:is_spooky"))), "graveyard not #c:is_spooky")
        helper.assertTrue(problems.isEmpty(), problems.joinToString("; "))
        helper.succeed()
    }

    // ---------------------------------------------------------------- worldgen

    /** The graveyard keeps upstream's plantz features (plus the Vanilla Backport ones when present) and both plantz configured features place. */
    @JvmStatic
    @GameTest(template = PLATFORM, timeoutTicks = 100, skyAccess = true)
    fun graveyardFeaturesPlace(helper: GameTestHelper) {
        val level = helper.level
        val access = level.registryAccess()
        val biome: Biome = access.registryOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME, pazResource("graveyard")))
        val placed = access.registryOrThrow(Registries.PLACED_FEATURE)
        val steps = biome.generationSettings.features()
        val vegetal = steps[GenerationStep.Decoration.VEGETAL_DECORATION.ordinal].map { placed.getKey(it.value()).toString() }
        helper.assertTrue(vegetal.containsAll(listOf("plantz:dead_trees", "plantz:gravestones")), "graveyard vegetal features: $vegetal")
        val vbFeatures = listOf("minecraft:pale_moss_patch", "minecraft:patch_leaf_litter", "minecraft:patch_firefly_bush_near_water")
        val all = steps.flatMap { step -> step.map { placed.getKey(it.value()).toString() } }
        helper.assertTrue(if (vanillaBackport) all.containsAll(vbFeatures) else all.none { it in vbFeatures }, "graveyard Vanilla Backport features: $all")

        // the platform's grass floor sits at relative y=1 here, so features go on y=2
        val configured = access.registryOrThrow(Registries.CONFIGURED_FEATURE)
        val grave = configured.getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, pazResource("gravestone")))
        helper.assertTrue(grave.place(level, level.chunkSource.generator, level.random, helper.absolutePos(BlockPos(3, 2, 3))), "gravestone feature did not place")
        helper.assertBlockPresent(block("plantz:gravestone"), BlockPos(3, 2, 3))

        val treePos = BlockPos(10, 2, 10)
        val tree = configured.getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, pazResource("dead_tree")))
        helper.assertTrue(tree.place(level, level.chunkSource.generator, level.random, helper.absolutePos(treePos)), "dead tree did not place")
        val trunk = if (vanillaBackport) block("minecraft:pale_oak_wood") else Blocks.STRIPPED_BIRCH_WOOD
        helper.assertBlockPresent(trunk, treePos)
        helper.assertBlockPresent(trunk, treePos.above(3))
        helper.succeed()
    }

    /** Seed mutations, damage types, the jukebox song and the time-space dimension load from data like upstream. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun dataRegistriesLoad(helper: GameTestHelper) {
        val access = helper.level.registryAccess()
        val mutations = access.registryOrThrow(PazRegistries.SEED_MUTATION)
        val expected = mapOf(
            "electric_peashooter" to listOf("plantz:electric_peashooter", "plantz:peashooter"),
            "explode_o_nut" to listOf("plantz:wallnut"),
            "melonpult" to listOf("plantz:winter_melon"),
            "peashooter" to listOf("plantz:electric_peashooter", "plantz:fire_peashooter", "plantz:repeater"),
            "repeater" to listOf("plantz:peashooter"),
            "wallnut" to listOf("plantz:explode_o_nut"),
            "winter_melon" to listOf("plantz:winter_melon", "plantz:melonpult"),
        )
        helper.assertTrue(mutations.keySet().map { it.toString() }.toSet() == expected.keys.map { "plantz:$it" }.toSet(), "seed mutations: ${mutations.keySet()}")
        for ((name, results) in expected) {
            val rules = mutations.get(pazResource(name))!!.mutations
            helper.assertTrue(rules.map { it.result.unwrapKey().get().location().toString() } == results, "$name mutation results")
        }
        val pea = mutations.get(pazResource("peashooter"))!!.mutations
        helper.assertTrue(pea[0].weather.isPresent && pea[1].biomes.isPresent && pea[1].biomes.get().size() > 0 && pea[2].chance == 0.1f, "peashooter mutation rules")

        val damageNames = listOf("electric_zap", "energy", "paint", "plant", "plant_chomp", "plant_electric", "plant_explode", "plant_fire",
            "plant_freeze", "plant_fume", "toxic", "zombie_eat", "zombie_electric", "zombie_energy", "zombie_explode", "zombie_smash", "zombie_trample")
        val loadedDamage = access.registryOrThrow(Registries.DAMAGE_TYPE).keySet().filter { it.namespace == "plantz" }.map { it.path }.sorted()
        helper.assertTrue(loadedDamage == damageNames, "damage types: $loadedDamage")

        val song = access.registryOrThrow(Registries.JUKEBOX_SONG).get(pazResource("grassy_groove"))
        helper.assertTrue(song != null && song.lengthInSeconds() == 117f && song.comparatorOutput() == 0 &&
            song.soundEvent().unwrapKey().get().location() == pazResource("music_disc.grassy_groove"), "grassy groove jukebox song")

        // the GameTest server skips data pack dimensions, so decode the file the way the world loader would
        val dimFile = helper.level.server.resourceManager.getResource(rl("plantz:dimension/time_space.json"))
        helper.assertTrue(dimFile.isPresent, "data/plantz/dimension/time_space.json missing")
        val json = dimFile.get().openAsReader().use { JsonParser.parseReader(it) }
        val stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access), json).getOrThrow()
        helper.assertTrue(stem.generator() is DebugLevelSource && stem.type().`is`(BuiltinDimensionTypes.OVERWORLD), "time_space dimension")
        helper.succeed()
    }

    // ---------------------------------------------------------------- trades

    /** Farmers offer upstream's villager_trade entries at levels 1-3 (26.1 trade sets, here via VillagerTradesEvent). */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun farmerTradesMatchUpstream(helper: GameTestHelper) {
        val villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, 7, 1, 7)
        val trades = VillagerTrades.TRADES[VillagerProfession.FARMER]!!
        val random = helper.level.random
        val problems = mutableListOf<String>()
        val sun = item("plantz:sun")
        val expected = mapOf(1 to listOf("minecraft:emerald"), 2 to listOf("plantz:watering_can", "seed_packet/plantz:potatomine"),
            3 to listOf("plantz:gardening_glove", "seed_packet/plantz:cherrybomb"), 4 to emptyList(), 5 to emptyList())
        for ((lvl, want) in expected) {
            val offers = (trades[lvl] ?: emptyArray()).mapNotNull { it.getOffer(villager, random) }
                .filter { o -> itemId(o.result).startsWith("plantz:") || o.baseCostA.`is`(sun) || o.costB.`is`(sun) }
            val got = offers.map { o -> if (itemId(o.result) == "plantz:seed_packet") "seed_packet/${entityData(o.result)}" else itemId(o.result) }
            if (got.sorted() != want.sorted()) problems += "level $lvl offers $got"
            for (o in offers) {
                val r = itemId(o.result)
                fun check(ok: Boolean, what: String) { if (!ok) problems += "$r: $what" }
                when {
                    r == "minecraft:emerald" -> check(o.baseCostA.`is`(sun) && o.baseCostA.count == 32 && o.result.count == 1 && o.maxUses == 10 && o.xp == 5 && o.priceMultiplier == 0.1f, "sun trade")
                    r == "plantz:watering_can" -> check(o.baseCostA.`is`(Items.EMERALD) && o.baseCostA.count in 10..15 && o.costB.`is`(sun) && o.costB.count in 10..16 && o.maxUses == 3 && o.xp == 25 && o.priceMultiplier == 0.3f, "watering can trade")
                    r == "plantz:gardening_glove" -> check(o.baseCostA.count in 12..20 && o.costB.`is`(sun) && o.costB.count in 12..22 && o.maxUses == 1 && o.xp == 15 && o.priceMultiplier == 0.1f, "glove trade")
                    entityData(o.result) == "plantz:potatomine" -> check(o.baseCostA.count in 1..2 && o.costB.isEmpty && o.maxUses == 10 && o.xp == 8 && o.priceMultiplier == 0.2f, "potatomine trade")
                    entityData(o.result) == "plantz:cherrybomb" -> check(o.baseCostA.count in 8..16 && o.costB.isEmpty && o.maxUses == 10 && o.xp == 8 && o.priceMultiplier == 0.2f, "cherrybomb trade")
                }
            }
        }
        villager.discard()
        helper.assertTrue(problems.isEmpty(), problems.joinToString("; "))
        helper.succeed()
    }

    // ---------------------------------------------------------------- lang

    /** Names upstream's en_us never had (shown as raw keys on 26.1 too): projectile/misc entities and one block. */
    private val UPSTREAM_LANG_GAPS = setOf("block.plantz.smooth_brainz_alloy_block")

    /** en_us names every plantz item (block items use block.* keys on 1.21.1), mob, effect and damage type death message. */
    @JvmStatic
    @GameTest(template = PLATFORM)
    fun englishNamesExist(helper: GameTestHelper) {
        val lang = Language.getInstance()
        val missing = mutableListOf<String>()
        fun need(key: String) { if (!lang.has(key)) missing += key }
        BuiltInRegistries.ITEM.entrySet().filter { it.key.location().namespace == "plantz" }.forEach { need(it.value.descriptionId) }
        BuiltInRegistries.ENTITY_TYPE.entrySet()
            .filter { it.key.location().namespace == "plantz" && (it.value.category != MobCategory.MISC || SpawnEggItem.byId(it.value) != null) }
            .forEach { need(it.value.descriptionId) }
        BuiltInRegistries.MOB_EFFECT.entrySet().filter { it.key.location().namespace == "plantz" }.forEach { need(it.value.descriptionId) }
        helper.level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).entrySet()
            .filter { it.key.location().namespace == "plantz" }.forEach { need("death.attack." + it.value.msgId()) }
        need("jukebox_song.plantz.grassy_groove")
        missing.removeAll(UPSTREAM_LANG_GAPS)
        helper.assertTrue(missing.isEmpty(), "missing en_us keys: $missing")
        helper.succeed()
    }
}
