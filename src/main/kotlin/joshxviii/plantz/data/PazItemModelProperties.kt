package joshxviii.plantz.data

import joshxviii.plantz.pazResource
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

/**
 * Item model predicates used by the 1.21.1 item models in assets/plantz/models/item (the 26.1 item model
 * definitions `select` / `condition` / `range_dispatch` / `plantz:plant_icon` were converted to "overrides").
 *
 * CLIENT ONLY. Call [register] from FMLClientSetupEvent.enqueueWork { }.
 *
 * Values are kept in [0, 1] so they survive clamping. The JSON override thresholds use the same encoding:
 *  - plantz:plant_icon     = (index in [PLANT_ICONS] + 1) / 64   (seed_packet, gardening_glove)
 *  - plantz:blueprint_type = (index in [BLUEPRINT_TYPES] + 1) / 8 (blueprint)
 */
object PazItemModelProperties {

    /** Plants that have models/item/plant_icon/<id>.json; ORDER MUST MATCH the JSON overrides (alphabetical). */
    @JvmField
    val PLANT_ICONS: List<String> = listOf(
        "bonkchoy", "cabbagepult", "cactus", "cherrybomb", "chomper", "coffeebean", "doomshroom",
        "electric_peashooter", "explode_o_nut", "fire_peashooter", "fumeshroom", "grave_buster", "hypnoshroom",
        "ice_peashooter", "iceshroom", "kernelpult", "lightning_reed", "melonpult", "peashooter", "potatomine",
        "puffshroom", "repeater", "scaredyshroom", "seashroom", "sunflower", "sunshroom", "tanglekelp", "wallnut",
        "winter_melon",
    ).map { "plantz:$it" }

    /** Blueprint entity ids, in the order of the blueprint.json overrides. */
    @JvmField
    val BLUEPRINT_TYPES: List<String> = listOf(
        "plantz:zombie_turret", "plantz:zombie_drone", "plantz:electro_turret", "plantz:lawn_mower",
    )

    private val PLANT_ICON = pazResource("plant_icon")
    private val BLUEPRINT_TYPE = pazResource("blueprint_type")
    private val HAS_ENTITY_DATA = pazResource("has_entity_data")
    private val HAS_STORED_SUN = pazResource("has_stored_sun")
    private val HAS_STORED_WATER = pazResource("has_stored_water")
    private val NEWSPAPER_ZOMBIE = pazResource("newspaper_zombie")
    private val USING = pazResource("using")
    private val MOON_PHASE = pazResource("moon_phase")

    fun register() {
        val seedPacket = item("seed_packet")
        val glove = item("gardening_glove")
        val blueprint = item("blueprint")
        val sunBattery = item("sun_battery")
        val wateringCan = item("watering_can")
        val newspaper = item("newspaper")
        val storedSun = BuiltInRegistries.DATA_COMPONENT_TYPE.get(pazResource("stored_sun"))
        val storedWater = BuiltInRegistries.DATA_COMPONENT_TYPE.get(pazResource("stored_water"))

        val plantIcon = fn { stack, _, _ -> indexValue(entityId(stack), PLANT_ICONS, 64f) }
        ItemProperties.register(seedPacket, PLANT_ICON, plantIcon)
        ItemProperties.register(glove, PLANT_ICON, plantIcon)
        // 26.1: condition has_component minecraft:entity_data (ignore_default)
        ItemProperties.register(glove, HAS_ENTITY_DATA, fn { stack, _, _ -> bool(hasNonDefault(stack, DataComponents.ENTITY_DATA)) })
        // 26.1: select component minecraft:entity_data -> id
        ItemProperties.register(blueprint, BLUEPRINT_TYPE, fn { stack, _, _ -> indexValue(entityId(stack), BLUEPRINT_TYPES, 8f) })
        // 26.1: condition has_component plantz:stored_sun / plantz:stored_water (ignore_default)
        if (storedSun != null) ItemProperties.register(sunBattery, HAS_STORED_SUN, fn { stack, _, _ -> bool(hasNonDefault(stack, storedSun)) })
        if (storedWater != null) ItemProperties.register(wateringCan, HAS_STORED_WATER, fn { stack, _, _ -> bool(hasNonDefault(stack, storedWater)) })
        // 26.1 newspaper: select context_entity_type == plantz:newspaper_zombie / condition using_item / range_dispatch time(moon_phase)
        ItemProperties.register(newspaper, NEWSPAPER_ZOMBIE, fn { _, _, entity ->
            bool(entity != null && BuiltInRegistries.ENTITY_TYPE.getKey(entity.type) == pazResource("newspaper_zombie"))
        })
        ItemProperties.register(newspaper, USING, fn { stack, _, entity ->
            bool(entity != null && entity.isUsingItem && entity.useItem == stack)
        })
        ItemProperties.register(newspaper, MOON_PHASE, fn { _, level, _ ->
            val l = level ?: Minecraft.getInstance().level
            if (l == null) 0f else l.moonPhase / 8f
        })
    }

    private fun item(path: String) = BuiltInRegistries.ITEM.get(pazResource(path))

    private inline fun fn(crossinline f: (ItemStack, ClientLevel?, LivingEntity?) -> Float): ClampedItemPropertyFunction =
        ClampedItemPropertyFunction { stack, level, entity, _ -> f(stack, level, entity) }

    private fun bool(b: Boolean) = if (b) 1f else 0f

    private fun indexValue(id: String?, list: List<String>, scale: Float): Float {
        if (id == null) return 0f
        val i = list.indexOf(id)
        return if (i < 0) 0f else (i + 1) / scale
    }

    @Suppress("DEPRECATION")
    private fun entityId(stack: ItemStack): String? {
        val data = stack.get(DataComponents.ENTITY_DATA) ?: return null
        val tag = data.unsafe
        if (!tag.contains("id")) return null
        return ResourceLocation.tryParse(tag.getString("id"))?.toString()
    }

    /** 26.1 `has_component` with `ignore_default: true`: present in the stack's patch (not just the prototype). */
    private fun hasNonDefault(stack: ItemStack, type: DataComponentType<*>): Boolean =
        stack.componentsPatch.get(type)?.isPresent == true
}
