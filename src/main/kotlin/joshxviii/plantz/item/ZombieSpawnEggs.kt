package joshxviii.plantz.item

import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.entity.ZombieEquipment.BUCKET
import joshxviii.plantz.entity.ZombieEquipment.COFFIN
import joshxviii.plantz.entity.ZombieEquipment.CONE
import joshxviii.plantz.entity.ZombieEquipment.DOOR
import joshxviii.plantz.entity.zombie.AllStarVariant
import joshxviii.plantz.entity.zombie.BrownCoatVariant
import joshxviii.plantz.entity.zombie.GargantuarVariant
import joshxviii.plantz.entity.zombie.ImpVariant
import joshxviii.plantz.entity.zombie.RoboZombieVariant
import joshxviii.plantz.entity.zombie.SuperBrainzVariant
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.Component
import net.minecraft.util.StringRepresentable
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomModelData

object ZombieSpawnEggs {
    class Loadout(val id: String, val items: Map<EquipmentSlot, () -> ItemStack>)

    class EggBuilder(private val type: EntityType<*>, private val item: Item) {
        private val tag = CompoundTag()
        private val equipment = mutableMapOf<EquipmentSlot, ItemStack>()
        private var name: Component? = null
        private var customModelData = 0

        private val typeKey get() = EntityType.getKey(type).path

        fun variant(variant: StringRepresentable) {
            tag.putString("variant", variant.serializedName)
            name = Component.translatable("entity.plantz.$typeKey.variant.${variant.serializedName}")
            // 26.1 custom_model_data strings: the variant's ordinal, which the egg's model overrides test
            customModelData = (variant as Enum<*>).ordinal
        }

        fun nbt(block: CompoundTag.() -> Unit) = tag.block()

        fun loadout(loadout: Loadout) {
            loadout.items.forEach { (slot, stack) ->
                equipment[slot] = stack()
            }
            name = Component.translatable("entity.plantz.$typeKey.loadout.${loadout.id}")
        }

        fun build(): ItemStack {
            // 26.1 "equipment" tag -> 1.21.1 ArmorItems / HandItems
            if (equipment.isNotEmpty()) {
                tag.put("ArmorItems", itemList(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD))
                tag.put("HandItems", itemList(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND))
            }
            val egg = item.defaultInstance
            egg.set(DataComponents.ENTITY_DATA, entityDataOf(type, tag))
            name?.let { egg.set(DataComponents.ITEM_NAME, it) }
            if (customModelData != 0) egg.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(customModelData))
            return egg
        }

        private fun itemList(vararg slots: EquipmentSlot) = ListTag().also { list ->
            for (slot in slots) list.add(ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, equipment[slot] ?: ItemStack.EMPTY).getOrThrow())
        }
    }

    inline fun egg(type: EntityType<*>, item: Item, configure: EggBuilder.() -> Unit): ItemStack =
        EggBuilder(type, item).apply(configure).build()

    fun <V> variantEggs(
        type: EntityType<*>,
        item: Item,
        values: Collection<V>,
        extras: EggBuilder.(V) -> Unit = {}
    ): List<ItemStack> where V : Enum<V>, V : StringRepresentable =
        values.drop(1).map { v -> egg(type, item) { variant(v); extras(v) } }

    fun loadoutEggs(type: EntityType<*>, item: Item, loadouts: List<Loadout>): List<ItemStack> =
        loadouts.map { l -> egg(type, item) { loadout(l) } }

    val BROWN_COAT_LOADOUTS = listOf(
        Loadout("cone", mapOf(EquipmentSlot.HEAD to CONE)),
        Loadout("bucket", mapOf(EquipmentSlot.HEAD to BUCKET)),
        Loadout("door", mapOf(EquipmentSlot.OFFHAND to DOOR)),
        Loadout("coffin", mapOf(EquipmentSlot.CHEST to COFFIN)),
        Loadout("full", mapOf(EquipmentSlot.HEAD to CONE, EquipmentSlot.OFFHAND to DOOR, EquipmentSlot.CHEST to COFFIN)),
    )

    fun brownCoat() = loadoutEggs(PazEntities.BROWN_COAT, PazItems.BROWN_COAT_SPAWN_EGG, BROWN_COAT_LOADOUTS) +
        variantEggs(PazEntities.BROWN_COAT, PazItems.BROWN_COAT_SPAWN_EGG, BrownCoatVariant.entries)
    fun imp() = variantEggs(PazEntities.IMP, PazItems.IMP_SPAWN_EGG, ImpVariant.entries) { if (it == ImpVariant.PIRATE) nbt { putBoolean("hasBarrel", true) } }
    fun gargantuar() = variantEggs(PazEntities.GARGANTUAR, PazItems.GARGANTUAR_SPAWN_EGG, GargantuarVariant.entries)
    fun superBrainz() = variantEggs(PazEntities.SUPER_BRAINZ, PazItems.SUPER_BRAINZ_SPAWN_EGG, SuperBrainzVariant.entries)
    fun allStar() = variantEggs(PazEntities.ALL_STAR, PazItems.ALL_STAR_SPAWN_EGG, AllStarVariant.entries)
    fun roboZombie() = variantEggs(PazEntities.ROBO_ZOMBIE, PazItems.ROBO_ZOMBIE_SPAWN_EGG, RoboZombieVariant.entries)
}
