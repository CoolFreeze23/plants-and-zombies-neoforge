package joshxviii.plantz.gametest

import net.minecraft.world.phys.AABB
import net.minecraft.tags.EnchantmentTags
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.phys.BlockHitResult
import joshxviii.plantz.pazResource
import joshxviii.plantz.item.entityDataOf
import joshxviii.plantz.item.entityDataType
import joshxviii.plantz.PazLootTables
import joshxviii.plantz.item.component.LetterDataComponent
import joshxviii.plantz.inventory.LetterMenu
import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazCreativeTab
import joshxviii.plantz.PazEffects
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.block.CollectionBoxState
import joshxviii.plantz.block.GardenGnomeBlock
import joshxviii.plantz.block.GardenGnomePose
import joshxviii.plantz.block.MailboxBlock
import joshxviii.plantz.block.MailboxState
import joshxviii.plantz.block.MailCollectionBoxBlock
import joshxviii.plantz.block.PlantPotBlock
import joshxviii.plantz.block.SunBatteryBlock
import joshxviii.plantz.block.TimeMachineBlock
import joshxviii.plantz.block.TimeMachineState
import joshxviii.plantz.block.entity.FlagBlockEntity
import joshxviii.plantz.block.entity.MailCollectionBoxEntity
import joshxviii.plantz.block.entity.MailboxBlockEntity
import joshxviii.plantz.block.entity.MailboxManager
import joshxviii.plantz.block.entity.SunBatteryBlockEntity
import joshxviii.plantz.block.entity.TimeMachineBlockEntity
import joshxviii.plantz.block.entity.getMailboxMailQueue
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.projectile.PaintBall
import joshxviii.plantz.entity.projectile.ThrownSunBottle
import joshxviii.plantz.gametest.blocks.check
import joshxviii.plantz.gametest.blocks.entitiesNear
import joshxviii.plantz.gametest.blocks.hold
import joshxviii.plantz.gametest.blocks.mockPlayer
import joshxviii.plantz.gametest.blocks.useItemOn
import joshxviii.plantz.inventory.MailCollectionBoxMenu
import joshxviii.plantz.item.BlueprintItem
import joshxviii.plantz.item.SeedPacketCooldowns
import joshxviii.plantz.item.SeedPacketItem
import joshxviii.plantz.item.component.StoredSun
import joshxviii.plantz.item.component.StoredWater
import joshxviii.plantz.item.component.UseCooldown
import joshxviii.plantz.networking.MailboxSendResult
import joshxviii.plantz.networking.SendMailRequestPayload
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.village.poi.PoiTypes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DispenserBlock
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.DispenserBlockEntity
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

/**
 * Blocks and items: mailboxes, sun battery, time machine, flags, gravestone, garden gnome, plant pots, watering can,
 * gardening glove, seed packets, wearables, recipes and fuel. All tests use the `platform` template and run in their
 * own batch, away from the other areas' zombies and players. On 1.21.1 the template sits one block above the test
 * origin, so its grass floor is at relative y=1.
 */
@GameTestHolder("plantz")
@PrefixGameTestTemplate(false)
object BlockItemGameTests {
    private const val PLATFORM = CoreGameTests.PLATFORM
    private const val BATCH = "plantz_blocks"

    // --- mailboxes ---

    /** A collection box lists the mailboxes nearest first and, on a redstone pulse, mails its first stack; the mailbox then ejects it. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH, timeoutTicks = 100)
    fun collectionBoxSendsMailOnRedstone(helper: GameTestHelper) {
        val near = BlockPos(4, 2, 7)
        val far = BlockPos(12, 2, 12)
        val box = BlockPos(7, 2, 7)
        helper.setBlock(near, PazBlocks.MAILBOX)
        helper.setBlock(far, PazBlocks.RED_MAILBOX)
        helper.setBlock(box, PazBlocks.MAIL_COLLECTION_BOX)
        val player = helper.mockPlayer(Vec3(7.5, 2.0, 4.5))

        helper.startSequence()
            .thenIdle(2) // block entities register on their first level tick (onLoad)
            .thenExecute {
                val registered = MailboxManager.getMailboxesInLevel(helper.level).map { it.blockPos }
                helper.check(helper.absolutePos(near) in registered && helper.absolutePos(far) in registered) { "mailboxes not registered: $registered" }

                helper.useBlock(box, player)
                val menu = player.containerMenu as? MailCollectionBoxMenu ?: throw GameTestAssertException("collection box menu did not open")
                helper.check(menu.availableMailboxes.map { it.blockPos }.take(2) == listOf(helper.absolutePos(near), helper.absolutePos(far))) {
                    "mailboxes not sorted by distance: ${menu.availableMailboxes.map { it.blockPos }}"
                }
                helper.assertBlockProperty(box, MailCollectionBoxBlock.STATE, CollectionBoxState.OPEN)
                player.closeContainer()

                val boxEntity = helper.getBlockEntity<MailCollectionBoxEntity>(box)
                boxEntity.setItem(0, ItemStack(Items.DIAMOND, 5))
                boxEntity.selectedMailbox = helper.absolutePos(near)
                helper.setBlock(box.east(), Blocks.REDSTONE_BLOCK)
            }
            .thenExecute {
                val mailbox = helper.getBlockEntity<MailboxBlockEntity>(near)
                helper.check(mailbox.getItem(0).`is`(Items.DIAMOND) && mailbox.getItem(0).count == 5) { "mail not delivered: ${mailbox.getItem(0)}" }
                helper.check(helper.getBlockEntity<MailCollectionBoxEntity>(box).isEmpty) { "collection box kept the mail" }
                helper.assertBlockProperty(near, MailboxBlock.STATE, MailboxState.HAS_MAIL)

                helper.useBlock(near, player)
                helper.check(mailbox.isEmpty) { "mailbox kept its mail after being opened" }
                helper.assertBlockProperty(near, MailboxBlock.STATE, MailboxState.EJECTING)
                helper.assertItemEntityPresent(Items.DIAMOND, near, 2.0)
            }
            .thenIdle(26)
            .thenExecute { helper.assertBlockProperty(near, MailboxBlock.STATE, MailboxState.INACTIVE) }
            .thenSucceed()
    }

    /** Mail to a full mailbox fails and stays in the collection box; mail to a missing (loaded) mailbox is discarded and deselects it. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun mailboxFullAndDiscard(helper: GameTestHelper) {
        val mailboxPos = BlockPos(3, 2, 3)
        val box = BlockPos(7, 2, 7)
        helper.setBlock(mailboxPos, PazBlocks.MAILBOX)
        helper.setBlock(box, PazBlocks.MAIL_COLLECTION_BOX)
        val level = helper.level
        val mailbox = helper.getBlockEntity<MailboxBlockEntity>(mailboxPos)
        val fillers = listOf(Items.STONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.COBBLESTONE)
        fillers.forEachIndexed { i, item -> mailbox.setItem(i, ItemStack(item, 64)) }

        helper.check(SendMailRequestPayload.trySendStack(level, ItemStack(Items.DIAMOND), helper.absolutePos(mailboxPos)) == MailboxSendResult.FULL) { "full mailbox accepted mail" }
        // a partial stack still fits when it merges into a matching slot
        mailbox.setItem(0, ItemStack(Items.STONE, 60))
        helper.check(SendMailRequestPayload.trySendStack(level, ItemStack(Items.STONE, 4), helper.absolutePos(mailboxPos)) == MailboxSendResult.SUCCESS) { "matching stack not merged" }
        helper.check(mailbox.getItem(0).count == 64) { "merged count ${mailbox.getItem(0).count}" }

        val boxEntity = helper.getBlockEntity<MailCollectionBoxEntity>(box)
        boxEntity.setItem(0, ItemStack(Items.DIAMOND))
        boxEntity.selectedMailbox = helper.absolutePos(mailboxPos)
        boxEntity.trySendMail()
        helper.check(boxEntity.getItem(0).`is`(Items.DIAMOND)) { "mail to a full mailbox left the collection box" }

        val missing = helper.absolutePos(BlockPos(10, 2, 10))
        level.getMailboxMailQueue().queue(missing, ItemStack(Items.EMERALD))
        boxEntity.selectedMailbox = missing
        boxEntity.trySendMail()
        helper.check(boxEntity.selectedMailbox == null) { "discarded target still selected" }
        helper.check(boxEntity.getItem(0).`is`(Items.DIAMOND)) { "discarded mail left the collection box" }
        helper.check(!level.getMailboxMailQueue().discardFor(missing)) { "queued mail for a missing mailbox was not discarded" }
        helper.succeed()
    }

    /** Mail queued for a mailbox is delivered when the mailbox loads and by its periodic tick. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH, timeoutTicks = 160)
    fun mailboxQueuedDelivery(helper: GameTestHelper) {
        val first = BlockPos(3, 2, 3)
        val second = BlockPos(10, 2, 10)
        val queue = helper.level.getMailboxMailQueue()
        queue.queue(helper.absolutePos(first), ItemStack(Items.EMERALD, 3))
        helper.setBlock(first, PazBlocks.BLUE_MAILBOX)
        helper.setBlock(second, PazBlocks.MAILBOX)

        helper.startSequence()
            .thenIdle(2)
            .thenExecute {
                val mailbox = helper.getBlockEntity<MailboxBlockEntity>(first)
                helper.check(mailbox.getItem(0).`is`(Items.EMERALD) && mailbox.getItem(0).count == 3) { "queued mail not delivered on load: ${mailbox.getItem(0)}" }
                helper.assertBlockProperty(first, MailboxBlock.STATE, MailboxState.HAS_MAIL)
                queue.queue(helper.absolutePos(second), ItemStack(Items.GOLD_INGOT))
            }
            .thenWaitUntil {
                helper.check(helper.getBlockEntity<MailboxBlockEntity>(second).getItem(0).`is`(Items.GOLD_INGOT)) { "queued mail not delivered by the mailbox tick" }
            }
            .thenSucceed()
    }

    /** Breaking a mailbox drops its mail; picking it gives the mailbox of its colour. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun mailboxDropsContents(helper: GameTestHelper) {
        val pos = BlockPos(5, 2, 5)
        helper.setBlock(pos, PazBlocks.GREEN_MAILBOX)
        helper.getBlockEntity<MailboxBlockEntity>(pos).setItem(2, ItemStack(Items.APPLE, 7))
        helper.level.destroyBlock(helper.absolutePos(pos), false)
        helper.assertBlockNotPresent(PazBlocks.GREEN_MAILBOX, pos)
        helper.assertItemEntityCountIs(Items.APPLE, pos, 2.0, 7)
        helper.succeed()
    }

    /** The 26.1 transmute dye recipe recolours a mailbox and keeps its components (name, lore, ...). */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun mailboxDyeRecipeKeepsData(helper: GameTestHelper) {
        val level = helper.level
        val mailbox = ItemStack(PazBlocks.MAILBOX)
        mailbox.set(DataComponents.CUSTOM_NAME, Component.literal("Crazy Dave"))
        mailbox.set(DataComponents.LORE, ItemLore(listOf(Component.literal("taco inside"))))

        fun craft(vararg stacks: ItemStack): ItemStack? {
            val input = CraftingInput.of(2, 1, stacks.toList())
            val recipe = level.recipeManager.getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null) ?: return null
            return recipe.value().assemble(input, level.registryAccess())
        }

        val red = craft(mailbox, ItemStack(Items.RED_DYE)) ?: throw GameTestAssertException("no recipe for mailbox + red dye")
        helper.check(red.`is`(PazBlocks.RED_MAILBOX.asItem()) && red.count == 1) { "wrong result $red" }
        helper.check(red.get(DataComponents.CUSTOM_NAME)?.string == "Crazy Dave") { "custom name lost" }
        helper.check(red.get(DataComponents.LORE)?.lines()?.firstOrNull()?.string == "taco inside") { "lore lost" }

        val lime = craft(ItemStack(Items.LIME_DYE), red) ?: throw GameTestAssertException("no recipe for lime dye + mailbox")
        helper.check(lime.`is`(PazBlocks.LIME_MAILBOX.asItem())) { "lime dye made $lime" } // upstream's lime recipe gave a red mailbox
        helper.check(craft(red, ItemStack(Items.RED_DYE)) == null) { "re-dyeing to the same colour matched" }
        helper.check(craft(mailbox, mailbox.copy()) == null) { "two mailboxes matched" }
        helper.succeed()
    }

    // --- sun battery and time machine ---

    /** Sun charges a placed battery (light level and comparator follow); a battery on a dispenser pays for planting; breaking it keeps the sun. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH, timeoutTicks = 60)
    fun sunBatteryChargesAndPowersDispenser(helper: GameTestHelper) {
        val batteryPos = BlockPos(3, 2, 3)
        helper.setBlock(batteryPos, PazBlocks.SUN_BATTERY_BLOCK)
        val player = helper.mockPlayer(Vec3(3.5, 2.0, 5.5))
        val sun = player.hold(ItemStack(PazItems.SUN, 3))
        repeat(3) { helper.useBlock(batteryPos, player) }
        val battery = helper.getBlockEntity<SunBatteryBlockEntity>(batteryPos)
        helper.check(battery.hasSun(3) && !battery.hasSun(4)) { "battery holds ${battery.theItem.get(PazComponents.STORED_SUN)}" }
        helper.check(sun.isEmpty) { "sun not consumed: $sun" }
        val level = StoredSun(3).getLevel()
        helper.assertBlockProperty(batteryPos, SunBatteryBlock.LEVEL, level)
        helper.check(helper.level.getBlockState(helper.absolutePos(batteryPos)).getAnalogOutputSignal(helper.level, helper.absolutePos(batteryPos)) == level) { "comparator signal" }

        // battery sitting on a dispenser: the dispenser plants seed packets and the battery pays
        val dispenserPos = BlockPos(8, 2, 7)
        helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST))
        helper.setBlock(dispenserPos.above(), PazBlocks.SUN_BATTERY_BLOCK)
        val dispenserBattery = helper.getBlockEntity<SunBatteryBlockEntity>(dispenserPos.above())
        dispenserBattery.addSun(StoredSun().max)
        val packet = SeedPacketItem.stackFor(PazEntities.SUNFLOWER)
        helper.getBlockEntity<DispenserBlockEntity>(dispenserPos).setItem(0, packet)
        helper.setBlock(dispenserPos.west(), Blocks.REDSTONE_BLOCK)

        helper.startSequence()
            .thenWaitUntil {
                helper.check(helper.entitiesNear(Plant::class.java, dispenserPos.east(), 1.0).any { it.type == PazEntities.SUNFLOWER }) {
                    "no dispensed sunflower; dispenser holds ${helper.getBlockEntity<DispenserBlockEntity>(dispenserPos).getItem(0)}, battery ${dispenserBattery.theItem.get(PazComponents.STORED_SUN)}, " +
                        "near ${helper.entitiesNear(net.minecraft.world.entity.Entity::class.java, dispenserPos.east(), 2.0).map { it.type.toShortString() + "@" + helper.relativeVec(it.position()) }}"
                }
            }
            .thenExecute {
                helper.check(!dispenserBattery.isFull()) { "dispensed plant was free" }
                helper.check(helper.getBlockEntity<DispenserBlockEntity>(dispenserPos).isEmpty) { "seed packet not used" }

                helper.level.destroyBlock(helper.absolutePos(batteryPos), false)
                val drops = helper.entitiesNear(ItemEntity::class.java, batteryPos).map { it.item }
                helper.check(drops.any { it.`is`(PazItems.SUN_BATTERY) && it.get(PazComponents.STORED_SUN)?.storedSun == 3 }) { "battery drop: $drops" }
            }
            .thenSucceed()
    }

    /** A charged battery in a powered time machine activates it and drains one sun every 38 ticks; without power it idles. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH, timeoutTicks = 140)
    fun timeMachineRunsOnSunAndRedstone(helper: GameTestHelper) {
        val pos = BlockPos(5, 2, 5)
        helper.setBlock(pos, PazBlocks.TIME_MACHINE)
        val player = helper.mockPlayer(Vec3(5.5, 2.0, 8.5))
        val battery = ItemStack(PazItems.SUN_BATTERY)
        battery.set(PazComponents.STORED_SUN, StoredSun(10))
        player.hold(battery)
        helper.useItemOn(player, pos)
        val machine = helper.getBlockEntity<TimeMachineBlockEntity>(pos)
        helper.check(machine.item.`is`(PazItems.SUN_BATTERY) && player.mainHandItem.isEmpty) { "battery not inserted" }
        player.hold(ItemStack(PazItems.SUN_BATTERY))
        helper.check(helper.useItemOn(player, pos) == InteractionResult.FAIL) { "second battery accepted" }

        helper.startSequence()
            .thenExecuteAfter(2) { helper.assertBlockProperty(pos, TimeMachineBlock.STATE, TimeMachineState.BATTERY) }
            .thenExecute { helper.setBlock(pos.north(), Blocks.REDSTONE_BLOCK) }
            .thenExecuteAfter(2) { helper.assertBlockProperty(pos, TimeMachineBlock.STATE, TimeMachineState.ACTIVE) }
            .thenWaitUntil {
                val stored = machine.item.get(PazComponents.STORED_SUN)?.storedSun ?: -1
                helper.check(stored < 10) { "active time machine used no sun ($stored)" }
            }
            .thenExecute { helper.setBlock(pos.north(), Blocks.AIR) }
            .thenExecuteAfter(2) {
                helper.assertBlockProperty(pos, TimeMachineBlock.STATE, TimeMachineState.BATTERY)
                helper.level.destroyBlock(helper.absolutePos(pos), false)
                helper.assertItemEntityPresent(PazItems.SUN_BATTERY, pos, 2.0)
            }
            .thenSucceed()
    }

    // --- flags, gravestone, gnome ---

    /** A hurt flag heals 1% per second after its cooldown; a destroyed plantz flag drops a brainz flag; it turns bad omen into zombie omen. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH, timeoutTicks = 400)
    fun flagHealthResetAndOmen(helper: GameTestHelper) {
        val healing = BlockPos(3, 2, 3)
        val dying = BlockPos(11, 2, 11)
        helper.setBlock(healing, PazBlocks.BRAINZ_FLAG)
        helper.setBlock(dying, PazBlocks.PLANTZ_FLAG)
        val player = helper.mockPlayer(Vec3(11.5, 2.0, 8.5))
        player.addEffect(MobEffectInstance(MobEffects.BAD_OMEN, 1200, 1))

        val flag = helper.getBlockEntity<FlagBlockEntity>(healing)
        flag.hurt(10f)
        helper.check(flag.health == flag.maxHealth - 10f) { "hurt flag: ${flag.health} / ${flag.maxHealth}" }
        helper.getBlockEntity<FlagBlockEntity>(dying).hurt(FlagBlockEntity.MAX_HEALTH)

        helper.startSequence()
            .thenExecuteAfter(2) {
                helper.assertBlockNotPresent(PazBlocks.PLANTZ_FLAG, dying)
                helper.assertItemEntityPresent(PazBlocks.BRAINZ_FLAG.asItem(), dying, 2.0)
                helper.check(player.hasEffect(PazEffects.ZOMBIE_OMEN) && !player.hasEffect(MobEffects.BAD_OMEN)) { "bad omen not replaced" }
                helper.check(player.getEffect(PazEffects.ZOMBIE_OMEN)?.amplifier == 1) { "omen level lost" }
            }
            .thenWaitUntil { helper.check(flag.health > flag.maxHealth - 10f) { "flag not healing (${flag.health})" } }
            .thenExecute { helper.check(flag.health == flag.maxHealth - 10f + flag.maxHealth * 0.01f) { "heal step ${flag.health}" } }
            .thenSucceed()
    }

    /** A gravestone in the dark spawns a zombie near it while a survival player is close. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH, timeoutTicks = 560)
    fun gravestoneSpawnsZombie(helper: GameTestHelper) {
        val grave = BlockPos(7, 2, 7)
        // a closed stone room keeps the sky light out
        for (x in 3..11) for (z in 3..11) for (y in 2..5) {
            if (x == 3 || x == 11 || z == 3 || z == 11 || y == 5) helper.setBlock(BlockPos(x, y, z), Blocks.STONE)
        }
        helper.setBlock(grave, PazBlocks.GRAVESTONE)
        helper.mockPlayer(Vec3(1.5, 2.0, 1.5))
        helper.succeedWhen {
            helper.check(helper.entitiesNear(Zombie::class.java, grave, 4.0).isNotEmpty()) { "no zombie from the gravestone" }
        }
    }

    /** Using a garden gnome cycles its pose; it breaks when its support goes. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun gardenGnomePosesAndSupport(helper: GameTestHelper) {
        val pos = BlockPos(5, 3, 5)
        helper.setBlock(pos.below(), Blocks.STONE)
        helper.setBlock(pos, PazBlocks.RED_GARDEN_GNOME)
        val player = helper.mockPlayer(Vec3(5.5, 2.0, 8.5))
        for (pose in listOf(GardenGnomePose.WAVE, GardenGnomePose.RELAX, GardenGnomePose.SIT, GardenGnomePose.THINK, GardenGnomePose.NONE)) {
            helper.useBlock(pos, player)
            helper.assertBlockProperty(pos, GardenGnomeBlock.POSE, pose)
        }
        helper.setBlock(pos.below(), Blocks.AIR)
        helper.assertBlockNotPresent(PazBlocks.RED_GARDEN_GNOME, pos)
        helper.succeed()
    }

    // --- planting ---

    /** A seed packet plants on a plant pot (facing the pot), using sun; the plant in a zen pot can then be watered with a filled watering can. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun plantPotsAndWateringCan(helper: GameTestHelper) {
        val pot = BlockPos(3, 2, 3)
        val zenPot = BlockPos(10, 2, 3)
        helper.setBlock(pot, PazBlocks.PLANT_POT.defaultBlockState().setValue(PlantPotBlock.FACING, Direction.EAST))
        helper.setBlock(zenPot, PazBlocks.ZEN_PLANT_POT)
        val player = helper.mockPlayer(Vec3(6.5, 2.0, 6.5))
        player.inventory.setItem(20, ItemStack(PazItems.SUN, 64))
        val sunBefore = player.inventory.countItem(PazItems.SUN)

        player.hold(SeedPacketItem.stackFor(PazEntities.PEA_SHOOTER).copyWithCount(2))
        helper.check(helper.useItemOn(player, pot, Direction.NORTH).consumesAction()) { "planting on the plant pot failed: ${player.messages}" }
        val potted = helper.entitiesNear(Plant::class.java, pot.above(), 0.5).firstOrNull() ?: throw GameTestAssertException("no plant on the pot")
        helper.check(potted.type == PazEntities.PEA_SHOOTER && potted.isTame) { "potted plant ${potted.type} tame=${potted.isTame}" }
        helper.check(Mth.degreesDifferenceAbs(potted.yRot, Direction.EAST.toYRot()) < 1f) { "plant does not face the pot's way (${potted.yRot})" }
        helper.check(player.mainHandItem.count == 1) { "seed packet not consumed" }
        helper.check(player.inventory.countItem(PazItems.SUN) < sunBefore) { "no sun paid" }
        helper.check(!helper.useItemOn(player, pot, Direction.UP).consumesAction()) { "planted twice in one pot" }

        helper.check(helper.useItemOn(player, zenPot).consumesAction()) { "planting on the zen plant pot failed" }
        val zenPlant = helper.entitiesNear(Plant::class.java, zenPot.above(), 0.5).firstOrNull() ?: throw GameTestAssertException("no plant on the zen pot")

        // fill the watering can from a water source, looking straight down at it
        val water = BlockPos(6, 2, 10)
        for (d in Direction.Plane.HORIZONTAL) helper.setBlock(water.relative(d), Blocks.STONE)
        helper.setBlock(water, Blocks.WATER)
        val can = player.hold(ItemStack(PazItems.WATERING_CAN))
        val eye = helper.absoluteVec(Vec3(6.5, 3.2, 10.5))
        player.teleportTo(helper.level, eye.x, eye.y, eye.z, 0f, 90f)
        can.use(helper.level, player, InteractionHand.MAIN_HAND)
        val filled = can.get(PazComponents.STORED_WATER)?.storedWater ?: 0
        helper.check(filled == StoredWater().addWater().storedWater) { "watering can holds $filled" }

        player.interactOn(zenPlant, InteractionHand.MAIN_HAND)
        helper.check(zenPlant.receivedWater == 2) { "plant got ${zenPlant.receivedWater} water" }
        helper.check(can.get(PazComponents.STORED_WATER)?.storedWater == filled - 2) { "can not emptied" }

        // farmland gets wet
        val farmland = BlockPos(12, 1, 12)
        helper.setBlock(farmland, Blocks.FARMLAND)
        helper.useItemOn(player, farmland)
        helper.assertBlockProperty(farmland, FarmBlock.MOISTURE, 7)
        helper.succeed()
    }

    /** Sneak-using a gardening glove on your plant picks it up; using the glove on the ground puts it back down, costing durability. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun gardeningGloveMovesPlant(helper: GameTestHelper) {
        val from = BlockPos(3, 2, 7)
        val to = BlockPos(10, 1, 7)
        val player = helper.mockPlayer(Vec3(6.5, 2.0, 7.5))
        val plant = helper.spawn(PazEntities.WALL_NUT, from)
        plant.tame(player)
        val glove = player.hold(ItemStack(PazItems.GARDENING_GLOVE))
        player.isShiftKeyDown = true
        player.interactOn(plant, InteractionHand.MAIN_HAND)
        helper.check(plant.isRemoved) { "plant not picked up" }
        helper.check(glove.has(DataComponents.ENTITY_DATA)) { "glove holds nothing" }
        player.isShiftKeyDown = false

        helper.check(helper.useItemOn(player, to).consumesAction()) { "glove could not put the plant down" }
        helper.assertEntityPresent(PazEntities.WALL_NUT, to.above())
        helper.check(!glove.has(DataComponents.ENTITY_DATA)) { "glove still holds the plant" }
        helper.check(glove.damageValue == 1) { "glove damage ${glove.damageValue}" }
        helper.check(!PazItems.GARDENING_GLOVE.canAttackBlock(Blocks.STONE.defaultBlockState(), helper.level, BlockPos.ZERO, helper.mockPlayer(Vec3(1.5, 2.0, 1.5), GameType.CREATIVE))) { "creative glove breaks blocks" }
        helper.succeed()
    }

    /** Seed packet cooldown groups: a stamped packet is blocked while its plant's group cools down, other packets are not. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun seedPacketCooldownGroups(helper: GameTestHelper) {
        val player = helper.mockPlayer(Vec3(7.5, 2.0, 7.5))
        player.inventory.setItem(20, ItemStack(PazItems.SUN, 64))
        val group = SeedPacketCooldowns.groupFor(PazEntities.PEA_SHOOTER)
        val stamped = SeedPacketItem.stackFor(PazEntities.PEA_SHOOTER).copyWithCount(4)
        stamped.set(PazComponents.USE_COOLDOWN, UseCooldown(5f, java.util.Optional.of(group)))
        val fresh = SeedPacketItem.stackFor(PazEntities.PEA_SHOOTER).copyWithCount(4)
        SeedPacketCooldowns.addCooldown(player, group, 100)

        helper.check(SeedPacketCooldowns.isOnCooldown(player, stamped)) { "stamped packet not on cooldown" }
        helper.check(!SeedPacketCooldowns.isOnCooldown(player, fresh)) { "unstamped packet shares the group" }
        player.hold(stamped)
        helper.check(!helper.useItemOn(player, BlockPos(3, 1, 3)).consumesAction()) { "planted during the cooldown" }
        player.hold(fresh)
        helper.check(helper.useItemOn(player, BlockPos(11, 1, 11)).consumesAction()) { "unstamped packet blocked: ${player.messages}" }

        player.tickCount += 100
        helper.check(!SeedPacketCooldowns.isOnCooldown(player, stamped)) { "cooldown did not end" }
        player.hold(stamped)
        helper.check(helper.useItemOn(player, BlockPos(3, 1, 3)).consumesAction()) { "planting after the cooldown failed" }
        helper.succeed()
    }

    // --- wearables ---

    /** Right-click equips helmets, the cone (one off a stack) and the ducky tube; flags only fit the off hand; an empty bucket counts as headgear. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun wearablesEquip(helper: GameTestHelper) {
        val player = helper.mockPlayer(Vec3(7.5, 2.0, 7.5))
        val level = helper.level
        fun use(stack: ItemStack): ItemStack {
            player.hold(stack)
            val result = stack.use(level, player, InteractionHand.MAIN_HAND)
            player.setItemInHand(InteractionHand.MAIN_HAND, result.`object`)
            return result.`object`
        }

        use(ItemStack(PazItems.FOOTBALL_HELMET))
        helper.check(player.getItemBySlot(EquipmentSlot.HEAD).`is`(PazItems.FOOTBALL_HELMET) && player.mainHandItem.isEmpty) { "football helmet not worn" }
        use(ItemStack(PazItems.PLANT_POT_HELMET))
        helper.check(player.getItemBySlot(EquipmentSlot.HEAD).`is`(PazItems.PLANT_POT_HELMET) && player.mainHandItem.`is`(PazItems.FOOTBALL_HELMET)) { "helmets not swapped" }
        use(ItemStack(PazBlocks.CONE, 4))
        helper.check(player.getItemBySlot(EquipmentSlot.HEAD).let { it.`is`(PazBlocks.CONE.asItem()) && it.count == 1 }) { "cone not worn singly" }
        helper.check(player.mainHandItem.count == 3 && player.inventory.countItem(PazItems.PLANT_POT_HELMET) == 1) { "cone stack / old helmet not kept" }
        use(ItemStack(PazItems.DUCKY_TUBE))
        helper.check(player.getItemBySlot(EquipmentSlot.LEGS).`is`(PazItems.DUCKY_TUBE)) { "ducky tube not on the legs" }

        val flag = ItemStack(PazBlocks.BRAINZ_FLAG)
        use(flag)
        helper.check(player.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty) { "flag equipped by right-click" }
        helper.check(player.getEquipmentSlotForItem(flag) == EquipmentSlot.OFFHAND) { "flag slot ${player.getEquipmentSlotForItem(flag)}" }

        val bucket = ItemStack(Items.BUCKET)
        helper.check(player.getEquipmentSlotForItem(bucket) == EquipmentSlot.HEAD) { "bucket is not headgear" }
        helper.check(player.getEquipmentSlotForItem(ItemStack(Items.WATER_BUCKET)) == EquipmentSlot.MAINHAND) { "water bucket is headgear" }
        val headSlot = player.inventoryMenu.getSlot(5)
        helper.check(headSlot.mayPlace(bucket) && !headSlot.mayPlace(ItemStack(Items.WATER_BUCKET))) { "helmet slot bucket rules" }
        helper.check(bucket.get(PazComponents.PROTECTS_HEAD) != null) { "bucket does not protect the head" }
        helper.check(bucket.get(DataComponents.ATTRIBUTE_MODIFIERS)?.modifiers()?.any { it.attribute() == Attributes.ARMOR } == true) { "bucket gives no armor" }

        // shift-click an empty bucket from the inventory into the free helmet slot
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY)
        player.inventory.setItem(9, bucket)
        player.inventoryMenu.quickMoveStack(player, 9)
        helper.check(player.getItemBySlot(EquipmentSlot.HEAD).`is`(Items.BUCKET)) { "bucket not shift-clicked onto the head" }
        helper.succeed()
    }

    /** Dispensers put wearables (helmet, flag) on the living entity in front, like 26.1 equippables, and hatch spawn eggs. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH, timeoutTicks = 40)
    fun dispensersEquipAndHatch(helper: GameTestHelper) {
        val helmetDispenser = BlockPos(3, 2, 3)
        val flagDispenser = BlockPos(3, 2, 8)
        val eggDispenser = BlockPos(3, 2, 12)
        val stand = helper.spawn(EntityType.ARMOR_STAND, helmetDispenser.east())
        val flagStand = helper.spawn(EntityType.ARMOR_STAND, flagDispenser.east())
        flagStand.isShowArms = true // armor stands only hold items with arms
        for ((pos, stack) in listOf(
            helmetDispenser to ItemStack(PazItems.FOOTBALL_HELMET),
            flagDispenser to ItemStack(PazBlocks.PLANTZ_FLAG),
            eggDispenser to ItemStack(PazItems.BROWN_COAT_SPAWN_EGG),
        )) {
            helper.setBlock(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST))
            helper.getBlockEntity<DispenserBlockEntity>(pos).setItem(0, stack)
            helper.setBlock(pos.west(), Blocks.REDSTONE_BLOCK)
        }
        helper.succeedWhen {
            helper.check(stand.getItemBySlot(EquipmentSlot.HEAD).`is`(PazItems.FOOTBALL_HELMET)) { "helmet not dispensed onto the armor stand" }
            helper.check(flagStand.getItemBySlot(EquipmentSlot.OFFHAND).`is`(PazBlocks.PLANTZ_FLAG.asItem())) { "flag not dispensed into the off hand" }
            helper.check(helper.entitiesNear(Zombie::class.java, eggDispenser.east(), 8.0).any { it.type == PazEntities.BROWN_COAT }) {
                "spawn egg not dispensed: dispenser holds ${helper.getBlockEntity<DispenserBlockEntity>(eggDispenser).getItem(0)}"
            }
        }
    }

    // --- other items ---

    /** Blueprints build their machine for brainz alloy and a point of durability. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun blueprintBuildsMachine(helper: GameTestHelper) {
        val player = helper.mockPlayer(Vec3(7.5, 2.0, 3.5))
        player.inventory.setItem(20, ItemStack(PazItems.BRAINZ_ALLOY, 64))
        val blueprint = player.hold(BlueprintItem.stackFor(PazEntities.ZOMBIE_TURRET))
        val target = BlockPos(7, 1, 9)
        helper.check(helper.useItemOn(player, target).consumesAction()) { "blueprint did not build: ${player.messages}" }
        helper.assertEntityPresent(PazEntities.ZOMBIE_TURRET, target.above())
        helper.check(blueprint.damageValue == 1) { "blueprint damage ${blueprint.damageValue}" }
        helper.check(player.inventory.countItem(PazItems.BRAINZ_ALLOY) < 64) { "no alloy paid" }
        helper.check(blueprint.item.getBreakingSound() == SoundEvents.WOLF_ARMOR_BREAK) { "blueprint break sound" }
        helper.succeed()
    }

    /** Dye blaster shoots paint balls while used; sun bottles are thrown; the newspaper is raised like a shield; the lawn flamingo is a weapon. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun handheldItems(helper: GameTestHelper) {
        val level = helper.level
        val player = helper.mockPlayer(Vec3(7.5, 2.0, 3.5))

        val blaster = player.hold(ItemStack(PazItems.DYE_BLASTER))
        player.inventory.setItem(20, ItemStack(Items.RED_DYE, 16))
        blaster.use(level, player, InteractionHand.MAIN_HAND)
        helper.check(player.isUsingItem) { "dye blaster not in use" }
        blaster.onUseTick(level, player, blaster.getUseDuration(player) - 1) // loads a dye
        helper.check(blaster.has(DataComponents.CHARGED_PROJECTILES) && blaster.has(DataComponents.DYED_COLOR)) { "dye blaster not loaded" }
        blaster.onUseTick(level, player, blaster.getUseDuration(player) - 4)
        helper.check(helper.entitiesNear(PaintBall::class.java, BlockPos(7, 3, 3), 3.0).isNotEmpty()) { "no paint ball" }
        helper.check(blaster.damageValue == 2) { "blaster damage ${blaster.damageValue}" } // 1 per use tick + 1 per shot (ProjectileWeaponItem.shoot)
        player.stopUsingItem()

        val bottles = player.hold(ItemStack(PazItems.SUN_BOTTLE, 2))
        bottles.use(level, player, InteractionHand.MAIN_HAND)
        helper.check(bottles.count == 1) { "sun bottle not used" }
        helper.check(helper.entitiesNear(ThrownSunBottle::class.java, BlockPos(7, 3, 3), 3.0).isNotEmpty()) { "no thrown sun bottle" }

        val newspaper = player.hold(ItemStack(PazItems.NEWSPAPER))
        newspaper.use(level, player, InteractionHand.MAIN_HAND)
        helper.check(player.isUsingItem && player.useItem.`is`(PazItems.NEWSPAPER)) { "newspaper not raised" }
        helper.check(newspaper.item.getBreakingSound() == SoundEvents.SHIELD_BREAK) { "newspaper break sound" }
        player.stopUsingItem()

        val flamingo = player.hold(ItemStack(PazBlocks.LAWN_FLAMINGO))
        val target = helper.spawn(EntityType.ARMOR_STAND, BlockPos(7, 2, 5))
        helper.check(flamingo.hurtEnemy(target, player)) { "lawn flamingo is not a weapon" }
        helper.succeed()
    }

    /** Fuel values, item registrations that 26.1 data drove (POIs) and the creative tab contents. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun registrationsFuelAndCreativeTab(helper: GameTestHelper) {
        helper.check(ItemStack(PazItems.SUN).getBurnTime(RecipeType.SMELTING) == 100) { "sun burn time" }
        helper.check(ItemStack(PazItems.NEWSPAPER).getBurnTime(RecipeType.SMELTING) == 200) { "newspaper burn time" }
        helper.check(AbstractFurnaceBlockEntity.isFuel(ItemStack(PazItems.SUN))) { "sun is not fuel" }

        helper.check(PoiTypes.forState(PazBlocks.PLANTZ_FLAG.defaultBlockState()).map { it.value() == PazBlocks.PLANTZ_FLAG_POI }.orElse(false)) { "plantz flag POI" }
        helper.check(PoiTypes.forState(PazBlocks.SUN_BATTERY_BLOCK.defaultBlockState()).map { it.value() == PazBlocks.SUN_BATTERY_POI }.orElse(false)) { "sun battery POI" }

        val tab = PazCreativeTab.PAZ_TAB
        tab.buildContents(CreativeModeTab.ItemDisplayParameters(helper.level.enabledFeatures(), false, helper.level.registryAccess()))
        val items = tab.displayItems.toList()
        helper.check(items.firstOrNull()?.let { SeedPacketItem.typeFromStack(it) } == PazEntities.SUNFLOWER) { "tab starts with ${items.firstOrNull()}" }
        helper.check(items.any { it.`is`(PazItems.WATERING_CAN) } && items.any { it.`is`(PazBlocks.MAIL_COLLECTION_BOX.asItem()) }) { "tab misses items" }
        helper.check(items.count { it.`is`(PazItems.SEED_PACKET) } == 30) { "tab has ${items.count { it.`is`(PazItems.SEED_PACKET) }} seed packets" }
        helper.succeed()
    }

    // --- letters, carving, doors, enchanting ---

    /** A letter opens its menu; an attachment put in it is stored on the letter, and a signed letter takes none. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun letterMenuStoresAttachment(helper: GameTestHelper) {
        val level = helper.level
        val player = helper.mockPlayer(Vec3(7.5, 2.0, 7.5))
        val letter = player.hold(ItemStack(PazItems.LETTER))
        letter.use(level, player, InteractionHand.MAIN_HAND)
        val menu = player.containerMenu as? LetterMenu ?: throw GameTestAssertException("letter menu not open: ${player.containerMenu}")
        helper.check(menu.editable) { "new letter is not editable" }
        menu.attachmentSlot.set(ItemStack(Items.DIAMOND))
        val data = letter.get(PazComponents.LETTER_DATA)
        helper.check(data != null && data.item.`is`(Items.DIAMOND)) { "attachment not stored: $data" }
        // components need value equality: the same data with a copied attachment is equal
        val copy = data!!.copy(item = data.item.copy())
        helper.check(copy == data && copy.hashCode() == data.hashCode()) { "letter data equality" }
        player.closeContainer()

        val signed = player.hold(ItemStack(PazItems.LETTER).apply {
            set(PazComponents.LETTER_DATA, LetterDataComponent(pages = listOf("Hi"), signed = true, author = "Crazy Dave"))
        })
        signed.use(level, player, InteractionHand.MAIN_HAND)
        val signedMenu = player.containerMenu as? LetterMenu ?: throw GameTestAssertException("signed letter menu not open")
        helper.check(!signedMenu.editable && !signedMenu.attachmentSlot.mayPlace(ItemStack(Items.DIAMOND))) { "signed letter takes attachments" }
        player.closeContainer()
        helper.succeed()
    }

    /** Carving a pumpkin still drops its seeds and rolls the carve loot table, which sometimes gives a Pumpkin seed packet. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun pumpkinCarvingLoot(helper: GameTestHelper) {
        val level = helper.level
        val pos = BlockPos(7, 2, 7)
        helper.setBlock(pos, Blocks.PUMPKIN)
        val player = helper.mockPlayer(Vec3(7.5, 2.0, 4.5))
        player.hold(ItemStack(Items.SHEARS))
        val abs = helper.absolutePos(pos)
        val hit = BlockHitResult(Vec3.atCenterOf(abs).relative(Direction.NORTH, 0.5), Direction.NORTH, abs, false)
        val result = level.getBlockState(abs).useItemOn(player.mainHandItem, level, player, InteractionHand.MAIN_HAND, hit)
        helper.check(result.consumesAction()) { "pumpkin not carved: $result" }
        helper.assertBlockPresent(Blocks.CARVED_PUMPKIN, pos)
        helper.check(helper.entitiesNear(ItemEntity::class.java, pos, 2.0).any { it.item.`is`(Items.PUMPKIN_SEEDS) }) { "no pumpkin seeds" }

        val table = level.server.reloadableRegistries().getLootTable(PazLootTables.CARVE_PUMPKIN)
        val params = LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(abs))
            .withParameter(LootContextParams.BLOCK_STATE, Blocks.PUMPKIN.defaultBlockState())
            .create(LootContextParamSets.BLOCK_USE)
        val drops = (1..1000).flatMap { table.getRandomItems(params) }
        helper.check(drops.all { it.`is`(PazItems.SEED_PACKET) && it.entityDataType() == PazEntities.PUMPKIN }) { "carve loot: $drops" }
        helper.check(drops.size in 15..80) { "${drops.size} pumpkin packets in 1000 carvings (4% chance)" }
        helper.succeed()
    }

    /** The brainz alloy door and trapdoor open by hand, like copper ones. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun brainzAlloyDoorAndTrapdoorOpen(helper: GameTestHelper) {
        val player = helper.mockPlayer(Vec3(7.5, 2.0, 4.5))
        val door = BlockPos(5, 2, 7)
        val lower = PazBlocks.BRAINZ_ALLOY_DOOR.defaultBlockState()
        helper.setBlock(door, lower)
        helper.setBlock(door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER))
        val trapdoor = BlockPos(9, 2, 7)
        helper.setBlock(trapdoor, PazBlocks.BRAINZ_ALLOY_TRAPDOOR)
        helper.useBlock(door, player)
        helper.useBlock(trapdoor, player)
        helper.assertBlockProperty(door, DoorBlock.OPEN, true)
        helper.assertBlockProperty(door.above(), DoorBlock.OPEN, true)
        helper.assertBlockProperty(trapdoor, TrapDoorBlock.OPEN, true)
        helper.succeed()
    }

    /** Enchantability (26.1 enchantable(n)); the gardening glove takes Lobbing at the enchanting table, and a lobbing glove throws the plant it holds. */
    @JvmStatic
    @GameTest(template = PLATFORM, batch = BATCH)
    fun gardeningGloveLobbing(helper: GameTestHelper) {
        val level = helper.level
        val values = mapOf(PazItems.GARDENING_GLOVE to 10, PazItems.DUCKY_TUBE to 1, PazItems.DYE_BLASTER to 1, PazItems.PLANT_POT_HELMET to 1)
        for ((item, value) in values) helper.check(ItemStack(item).enchantmentValue == value) { "$item enchantability ${ItemStack(item).enchantmentValue}" }

        val lobbing = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT, pazResource("lobbing")))
        val glove = ItemStack(PazItems.GARDENING_GLOVE)
        helper.check(glove.isEnchantable && lobbing.value().isSupportedItem(glove)) { "glove can't take lobbing" }
        helper.check(lobbing.`is`(EnchantmentTags.IN_ENCHANTING_TABLE)) { "lobbing not at the enchanting table" }

        val player = helper.mockPlayer(Vec3(7.5, 2.0, 3.5))
        glove.enchant(lobbing, 1)
        glove.set(DataComponents.ENTITY_DATA, entityDataOf(PazEntities.PEA_SHOOTER))
        player.hold(glove)
        glove.use(level, player, InteractionHand.MAIN_HAND)
        helper.check(player.isUsingItem) { "glove with a plant not in use" }
        glove.onUseTick(level, player, glove.getUseDuration(player) - 1)
        helper.check(!glove.has(DataComponents.ENTITY_DATA)) { "plant still in the glove" }
        val thrown = level.getEntities(PazEntities.PEA_SHOOTER, AABB(helper.absolutePos(BlockPos(7, 2, 3))).inflate(4.0)) { true }
        helper.check(thrown.isNotEmpty()) { "no plant thrown" }
        player.stopUsingItem()
        thrown.forEach { it.discard() }
        helper.succeed()
    }
}
