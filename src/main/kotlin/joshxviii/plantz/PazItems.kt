package joshxviii.plantz

import joshxviii.plantz.PazEntities.ALL_STAR
import joshxviii.plantz.PazEntities.BACKUP_DANCER
import joshxviii.plantz.PazEntities.BROWN_COAT
import joshxviii.plantz.PazEntities.DIGGER_ZOMBIE
import joshxviii.plantz.PazEntities.DISCO_ZOMBIE
import joshxviii.plantz.PazEntities.ENGINEER_ZOMBIE
import joshxviii.plantz.PazEntities.GARGANTUAR
import joshxviii.plantz.PazEntities.GNOME
import joshxviii.plantz.PazEntities.GRAVE_DIGGER
import joshxviii.plantz.PazEntities.IMP
import joshxviii.plantz.PazEntities.NEWSPAPER_ZOMBIE
import joshxviii.plantz.PazEntities.PIRATE_CAPTAIN
import joshxviii.plantz.PazEntities.ROBO_ZOMBIE
import joshxviii.plantz.PazEntities.SOLDIER_ZOMBIE
import joshxviii.plantz.PazEntities.SUPER_BRAINZ
import joshxviii.plantz.PazEntities.ZOMBIE_YETI
import joshxviii.plantz.entity.zombie.PazZombie
import joshxviii.plantz.item.*
import joshxviii.plantz.item.component.*
import net.minecraft.core.Direction
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.dispenser.BlockSource
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.*
import net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID
import net.minecraft.world.item.Item.BASE_ATTACK_SPEED_ID
import net.minecraft.world.item.Items.GLASS_BOTTLE
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.level.block.ComposterBlock
import net.minecraft.world.level.block.DispenserBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent
import java.util.function.Function

object PazItems {
    @JvmField
    val SUN: Item = registerItem(
        "sun", ::SunItem,
        properties = Item.Properties()
    )
    @JvmField
    val SUN_BOTTLE: Item = registerItem(
        "sun_bottle", ::SunBottleItem,
        properties = Item.Properties().craftRemainder(GLASS_BOTTLE)
    )
    @JvmField
    val SUN_BATTERY: Item = registerItem(
        "sun_battery", ::SunBatteryItem,
        properties = Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.RARE)
            .component(PazComponents.STORED_SUN, StoredSun())
    )
    @JvmField
    val WATERING_CAN: Item = registerItem(
        "watering_can", ::WateringCanItem,
        properties = Item.Properties()
            .stacksTo(1)
            .component(PazComponents.STORED_WATER, StoredWater())
    )
    @JvmField
    val GARDENING_GLOVE: Item = registerItem(
        "gardening_glove", ::GardeningGloveItem, // repairable with leather: GardeningGloveItem.isValidRepairItem
        properties = Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.UNCOMMON)
            .durability(325)
            .component(DataComponents.TOOL, GardeningGloveItem.createToolProperties())
            .attributes(
                ItemAttributeModifiers.builder()
                    .add(Attributes.ENTITY_INTERACTION_RANGE, AttributeModifier(BASE_ATTACK_DAMAGE_ID, 1.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
                    .add(Attributes.ATTACK_SPEED, AttributeModifier(BASE_ATTACK_SPEED_ID, -0.1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
                    .build()
            )
    )
    @JvmField
    val BRAINZIUM: Item = registerItem(
        "brainzium",
        properties = Item.Properties()
            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
    )
    @JvmField
    val BRAINZ_ALLOY: Item = registerItem(
        "brainz_alloy",
        properties = Item.Properties()
    )
    @JvmField
    val NEWSPAPER: Item = registerItem(
        "newspaper", ::NewspaperItem,
        properties = Item.Properties()
            .component(PazComponents.BLOCKS_PROJECTILE_DAMAGE, BlocksProjectileDamage(
                slot = EquipmentSlotGroup.HAND,
                breakChance = 0.1f,
                mustBeUsing = true
            )
            ) // 26.1 BREAK_SOUND (shield break) has no 1.21.1 component; the newspaper has no durability anyway
    )
    @JvmField
    val TACO: Item = registerItem(
        "taco",
        properties = Item.Properties().food(FoodProperties.Builder().nutrition(9).saturationModifier(1.1f).build())
    )
    const val DUCKY_TUBE_DAMAGE_INTERVAL = 45
    @JvmField
    val DUCKY_TUBE: Item = registerItem(
        "ducky_tube", ::DuckyTubeItem, // equips to LEGS (Equipable), repairable with phantom membrane
        properties = Item.Properties()
            .durability(200)
            .attributes(
                ItemAttributeModifiers.builder()
                    .add(
                        Attributes.WATER_MOVEMENT_EFFICIENCY,
                        AttributeModifier(pazResource("ducky_tube"), 1.5, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.LEGS
                    ).build())
    )
    @JvmField val WHITE_BALLOON: Item = registerItem("white_balloon", {BalloonItem(it, DyeColor.WHITE)}, properties = Item.Properties())
    @JvmField val LIGHT_GRAY_BALLOON: Item = registerItem("light_gray_balloon", {BalloonItem(it, DyeColor.LIGHT_GRAY)}, properties = Item.Properties())
    @JvmField val GRAY_BALLOON: Item = registerItem("gray_balloon", {BalloonItem(it, DyeColor.GRAY)}, properties = Item.Properties())
    @JvmField val BLACK_BALLOON: Item = registerItem("black_balloon", {BalloonItem(it, DyeColor.BLACK)}, properties = Item.Properties())
    @JvmField val BROWN_BALLOON: Item = registerItem("brown_balloon", {BalloonItem(it, DyeColor.BROWN)}, properties = Item.Properties())
    @JvmField val RED_BALLOON: Item = registerItem("red_balloon", {BalloonItem(it, DyeColor.RED)}, properties = Item.Properties())
    @JvmField val ORANGE_BALLOON: Item = registerItem("orange_balloon", {BalloonItem(it, DyeColor.ORANGE)}, properties = Item.Properties())
    @JvmField val YELLOW_BALLOON: Item = registerItem("yellow_balloon", {BalloonItem(it, DyeColor.YELLOW)}, properties = Item.Properties())
    @JvmField val LIME_BALLOON: Item = registerItem("lime_balloon", {BalloonItem(it, DyeColor.LIME)}, properties = Item.Properties())
    @JvmField val GREEN_BALLOON: Item = registerItem("green_balloon", {BalloonItem(it, DyeColor.GREEN)}, properties = Item.Properties())
    @JvmField val CYAN_BALLOON: Item = registerItem("cyan_balloon", {BalloonItem(it, DyeColor.CYAN)}, properties = Item.Properties())
    @JvmField val LIGHT_BLUE_BALLOON: Item = registerItem("light_blue_balloon", {BalloonItem(it, DyeColor.LIGHT_BLUE)}, properties = Item.Properties())
    @JvmField val BLUE_BALLOON: Item = registerItem("blue_balloon", {BalloonItem(it, DyeColor.BLUE)}, properties = Item.Properties())
    @JvmField val PURPLE_BALLOON: Item = registerItem("purple_balloon", {BalloonItem(it, DyeColor.PURPLE)}, properties = Item.Properties())
    @JvmField val MAGENTA_BALLOON: Item = registerItem("magenta_balloon", {BalloonItem(it, DyeColor.MAGENTA)}, properties = Item.Properties())
    @JvmField val PINK_BALLOON: Item = registerItem("pink_balloon", {BalloonItem(it, DyeColor.PINK)}, properties = Item.Properties())
    val balloonByColor = mapOf(
        DyeColor.WHITE to      WHITE_BALLOON,
        DyeColor.LIGHT_GRAY to LIGHT_GRAY_BALLOON,
        DyeColor.GRAY to       GRAY_BALLOON,
        DyeColor.BLACK to      BLACK_BALLOON,
        DyeColor.BROWN to      BROWN_BALLOON,
        DyeColor.RED to        RED_BALLOON,
        DyeColor.ORANGE to     ORANGE_BALLOON,
        DyeColor.YELLOW to     YELLOW_BALLOON,
        DyeColor.LIME to       LIME_BALLOON,
        DyeColor.GREEN to      GREEN_BALLOON,
        DyeColor.CYAN to       CYAN_BALLOON,
        DyeColor.LIGHT_BLUE to LIGHT_BLUE_BALLOON,
        DyeColor.BLUE to       BLUE_BALLOON,
        DyeColor.PURPLE to     PURPLE_BALLOON,
        DyeColor.MAGENTA to    MAGENTA_BALLOON,
        DyeColor.PINK to       PINK_BALLOON
    )

    @JvmField
    val DYE_BLASTER: Item = registerItem(
        "dye_blaster", ::DyeBlasterItem, // repairable with brainz alloy: DyeBlasterItem.isValidRepairItem
        properties = Item.Properties()
            .durability(435)
            .stacksTo(1)
    )
    @JvmField
    val FOOTBALL_HELMET: Item = registerItem(
        "football_helmet", ::FootballHelmetItem, // head Equipable; 26.1 humanoidArmor(CHAINMAIL, HELMET) durability/enchantability/repair kept
        properties = Item.Properties()
            .durability(FootballHelmetItem.DURABILITY)
            .component(PazComponents.BLOCKS_PROJECTILE_DAMAGE, BlocksProjectileDamage(
                breakChance = 0.025f)
            )
            .component(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                    .add(
                        Attributes.ARMOR,
                        AttributeModifier(pazResource("football_armor"), 4.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HEAD
                    ).add(
                        Attributes.KNOCKBACK_RESISTANCE,
                        AttributeModifier(pazResource("football_knockback_resistance"), 0.1, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HEAD
                    ).build()
            )
    )
    @JvmField
    val BLUEPRINT: Item = registerItem(
        "blueprint", ::BlueprintItem, // break sound (wolf armor) played by BlueprintItem itself
        properties = Item.Properties()
            .stacksTo(1)
            .durability(20)
            .component(PazComponents.BRAINZ_ALLOY_COST, BrainzAlloyCost())
    )
    @JvmField
    val LETTER: Item = registerItem(
        "letter",
        properties = Item.Properties()
    )
    @JvmField
    val SEED_PACKET: Item = registerItem(
        "seed_packet", ::SeedPacketItem, // per-plant cooldowns: SeedPacketCooldowns (no USE_COOLDOWN component in 1.21.1)
        properties = Item.Properties()
            .component(PazComponents.SUN_COST, SunCost())
    )
    @JvmField
    val PLANT_POT_MINECART: Item = registerItem(
        "plant_pot_minecart", ::PlantPotMinecartItem,
        properties = Item.Properties().stacksTo(1)
    )
    @JvmField
    val PLANT_POT_HELMET: Item = registerItem(
        "plant_pot_helmet", ::PlantPotHelmetItem, // head Equipable, repairable with brick
        properties = Item.Properties()
            .durability(185)
            .rarity(Rarity.UNCOMMON)
    )
    @JvmField
    val MUSIC_DISC_GRASSY_GROOVE: Item = registerItem(
        "music_disc_grassy_groove",
        properties = Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(PazJukeboxSongs.GRASSY_GROOVE)
    )

    @JvmField val BROWN_COAT_SPAWN_EGG: Item = registerSpawnEgg(BROWN_COAT, 0x7A5230, 0x799C65)
    @JvmField val NEWSPAPER_ZOMBIE_SPAWN_EGG: Item = registerSpawnEgg(NEWSPAPER_ZOMBIE, 0xD8D2C4, 0x799C65)
    @JvmField val DIGGER_ZOMBIE_SPAWN_EGG: Item = registerSpawnEgg(DIGGER_ZOMBIE, 0x8A6A3B, 0xF2C230)
    @JvmField val ENGINEER_ZOMBIE_SPAWN_EGG: Item = registerSpawnEgg(ENGINEER_ZOMBIE, 0xE0A526, 0x4F6E8F)
    @JvmField val ZOMBIE_YETI_SPAWN_EGG: Item = registerSpawnEgg(ZOMBIE_YETI, 0xEFF4F7, 0x8FB6D9)
    @JvmField val DISCO_ZOMBIE_SPAWN_EGG: Item = registerSpawnEgg(DISCO_ZOMBIE, 0x5B2A86, 0xF2D33A)
    @JvmField val BACKUP_DANCER_SPAWN_EGG: Item = registerSpawnEgg(BACKUP_DANCER, 0xC23B3B, 0x799C65)
    @JvmField val ALL_STAR_SPAWN_EGG: Item = registerSpawnEgg(ALL_STAR, 0xB22222, 0xF0F0F0)
    @JvmField val GRAVE_DIGGER_SPAWN_EGG: Item = registerSpawnEgg(GRAVE_DIGGER, 0x4B4B4B, 0x8A7356)
    @JvmField val SOLDIER_ZOMBIE_SPAWN_EGG: Item = registerSpawnEgg(SOLDIER_ZOMBIE, 0x556B2F, 0x799C65)
    @JvmField val ROBO_ZOMBIE_SPAWN_EGG: Item = registerSpawnEgg(ROBO_ZOMBIE, 0x9AA3AB, 0xD94A2B)
    @JvmField val PIRATE_CAPTAIN_SPAWN_EGG: Item = registerSpawnEgg(PIRATE_CAPTAIN, 0x2B2B2B, 0xC9A23F)
    @JvmField val SUPER_BRAINZ_SPAWN_EGG: Item = registerSpawnEgg(SUPER_BRAINZ, 0x2E5AAC, 0xD62828)
    @JvmField val IMP_SPAWN_EGG: Item = registerSpawnEgg(IMP, 0x8DB06A, 0xC94C4C)
    @JvmField val GARGANTUAR_SPAWN_EGG: Item = registerSpawnEgg(GARGANTUAR, 0x6E5A44, 0x799C65)

    @JvmField val GNOME_SPAWN_EGG: Item = registerSpawnEgg(GNOME, 0x2F6FD0, 0xE9D8B4)

    private fun registerItem(
        name: String,
        itemFactory: Function<Item.Properties, Item> = Function { p: Item.Properties -> Item(p) },
        properties: Item.Properties = Item.Properties()
    ) : Item {
        val key = ResourceKey.create(Registries.ITEM, pazResource(name) )
        val item = itemFactory.apply(properties)
        Registry.register(BuiltInRegistries.ITEM, key, item)
        return item
    }

    private fun registerSpawnEgg(type: EntityType<out Mob>, backgroundColor: Int, highlightColor: Int): Item {
        val entityId = EntityType.getKey(type).path
        return registerItem(
           "${entityId}_spawn_egg", { SpawnEggItem(type, backgroundColor, highlightColor, it) },
            Item.Properties()
        )
    }

    /**
     * Replaces Fabric's `DefaultItemComponentEvents.MODIFY`. Lead: call from the mod-bus `ModifyDefaultComponentsEvent`.
     * 26.1 also made the bucket head-equippable (EQUIPPABLE); 1.21.1 cannot give a vanilla item an equipment slot via
     * components, so mobs can still wear it (setItemSlot) and get these attributes, but players cannot right-click equip it.
     */
    @JvmStatic
    fun modifyDefaultComponents(event: ModifyDefaultComponentsEvent) {
        event.modify(Items.BUCKET) { builder ->
            builder.set(PazComponents.BLOCKS_PROJECTILE_DAMAGE, BlocksProjectileDamage(breakChance = .05f))

            val armorModifier = ItemAttributeModifiers.builder()
                .add(
                    Attributes.ARMOR,
                    AttributeModifier(pazResource("bucket_armor"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD
                ).add(
                    Attributes.KNOCKBACK_RESISTANCE,
                    AttributeModifier(pazResource("bucket_knockback_resistance"), 0.05, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD
                ).build()

            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, armorModifier)
        }
    }

    fun initialize() {
        // Fuel (Fabric FuelValueEvents): SunItem / NewspaperItem override getBurnTime.

        ComposterBlock.COMPOSTABLES.put(SEED_PACKET.asItem(), 1.0f)

        // Component tooltips (Fabric ItemComponentTooltipProviderRegistry): PazComponents.addComponentTooltips, wired by the lead.
        // Default components (Fabric DefaultItemComponentEvents): modifyDefaultComponents, wired by the lead.

        // Dispenser behavior
        DispenserBlock.registerBehavior(
            SEED_PACKET, object : DefaultDispenseItemBehavior() {
            public override fun execute(source: BlockSource, dispensed: ItemStack): ItemStack {
                val level = source.level

                val dispenserPos = source.pos
                val facing = source.state().getValue(DispenserBlock.FACING)
                val plantPos = dispenserPos.relative(facing)

                val result = SeedPacketItem.tryPlantFromDispenser(
                    level = level,
                    itemStack = dispensed,
                    dispenserPos = dispenserPos,
                    pos = plantPos,
                    face = facing,
                    horizontalDir = if (facing.axis.isHorizontal) facing else Direction.getRandom(level.random),
                )

                if (result) {
                    dispensed.shrink(1)
                    playAnimation(source, facing)
                    return dispensed
                }
                else return dispensed
            }
        })

        balloonByColor.values.forEach {
            DispenserBlock.registerBehavior(
                it, object : DefaultDispenseItemBehavior() {
                    public override fun execute(source: BlockSource, dispensed: ItemStack): ItemStack {
                        val level = source.level

                        val dispenserPos = source.pos
                        val facing = source.state().getValue(DispenserBlock.FACING)
                        val balloonPos = dispenserPos.relative(facing)

                        val balloonItem = dispensed.item as? BalloonItem ?: return super.execute(source, dispensed)
                        val balloonEntity = PazEntities.BALLOON.create(level)?: return dispensed
                        balloonEntity.dyeColor = balloonItem.color
                        val center = balloonPos.center
                        balloonEntity.moveTo(center.x, center.y, center.z)
                        balloonEntity.applyImpulse(Vec3.atLowerCornerOf(facing.normal), pow = 0.15f, uncertainty = 20.0f)

                        //TODO make the balloon attach to any entity in front of the dispenser

                        if (level.addFreshEntity(balloonEntity)) {
                            balloonEntity.playSound(PazSounds.BALLOON_INFLATE)
                            dispensed.shrink(1)
                            playAnimation(source, facing)

                            for (entity in level.getEntitiesOfClass(
                                LivingEntity::class.java,
                                AABB(balloonPos),
                                EntitySelector.NO_SPECTATORS
                            )) {
                                if (entity is PazZombie && !entity.canEquipBalloons()) continue
                                balloonEntity.setLeashedTo(entity, true)
                                break
                            }

                            return dispensed
                        } else {
                            balloonEntity.discard()
                            return dispensed
                        }
                    }
                }
            )
        }

        DispenserBlock.registerProjectileBehavior(SUN_BOTTLE)

        DispenserBlock.registerBehavior(PLANT_POT_MINECART, PlantPotMinecartItem.DISPENSE_ITEM_BEHAVIOR)

        // 26.1 Equippable items are dispensable onto mobs/players by default
        listOf(FOOTBALL_HELMET, PLANT_POT_HELMET, DUCKY_TUBE, PazBlocks.CONE.asItem()).forEach {
            DispenserBlock.registerBehavior(it, ArmorItem.DISPENSE_ITEM_BEHAVIOR)
        }
    }
}
