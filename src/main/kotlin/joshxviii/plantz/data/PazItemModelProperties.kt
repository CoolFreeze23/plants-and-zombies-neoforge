package joshxviii.plantz.data

import joshxviii.plantz.PazComponents
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
import net.minecraft.util.FastColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent

/**
 * Item model predicates used by the 1.21.1 item models in assets/plantz/models/item (the 26.1 item model
 * definitions `select` / `condition` / `range_dispatch` were converted to "overrides"). The `plantz:plant_icon`
 * composites (seed packet, gardening glove) are baked models instead, see PlantIconItemModel.
 *
 * CLIENT ONLY. Call [register] from FMLClientSetupEvent.enqueueWork { }.
 *
 * Values are kept in [0, 1] so they survive clamping. The JSON override thresholds use the same encoding:
 *  - plantz:blueprint_type = (index in [BLUEPRINT_TYPES] + 1) / 8 (blueprint)
 */
object PazItemModelProperties {

    /** Blueprint entity ids, in the order of the blueprint.json overrides. */
    @JvmField
    val BLUEPRINT_TYPES: List<String> = listOf(
        "plantz:zombie_turret", "plantz:zombie_drone", "plantz:electro_turret", "plantz:lawn_mower",
    )

    private val BLUEPRINT_TYPE = pazResource("blueprint_type")
    private val HAS_STORED_SUN = pazResource("has_stored_sun")
    private val HAS_STORED_WATER = pazResource("has_stored_water")
    private val NEWSPAPER_ZOMBIE = pazResource("newspaper_zombie")
    private val USING = pazResource("using")
    private val MOON_PHASE = pazResource("moon_phase")
    private val DYED = pazResource("dyed")
    private val OPENED = pazResource("opened")

    fun register() {
        val blueprint = item("blueprint")
        val sunBattery = item("sun_battery")
        val wateringCan = item("watering_can")
        val newspaper = item("newspaper")
        val storedSun = BuiltInRegistries.DATA_COMPONENT_TYPE.get(pazResource("stored_sun"))
        val storedWater = BuiltInRegistries.DATA_COMPONENT_TYPE.get(pazResource("stored_water"))

        // 26.1: select component minecraft:entity_data -> id
        ItemProperties.register(blueprint, BLUEPRINT_TYPE, fn { stack, _, _ -> indexValue(entityId(stack), BLUEPRINT_TYPES, 8f) })
        // 26.1: condition has_component plantz:stored_sun / plantz:stored_water (ignore_default)
        // 26.1 range_dispatch battery_fullness (1 - stored / max) with the empty model at 1
        ItemProperties.register(sunBattery, HAS_STORED_SUN, fn { stack, _, _ -> bool((stack.get(PazComponents.STORED_SUN)?.storedSun ?: 0) > 0) })
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
        // 26.1: condition has_component minecraft:dyed_color / minecraft:writable_book_content (ignore_default)
        ItemProperties.register(item("dye_blaster"), DYED, fn { stack, _, _ -> bool(isDyed(stack)) })
        ItemProperties.register(item("letter"), OPENED, fn { stack, _, _ -> bool(isOpenedLetter(stack)) })
        // 26.1 baron_staff: condition using_item
        ItemProperties.register(item("baron_staff"), USING, fn { stack, _, entity ->
            bool(entity != null && entity.isUsingItem && entity.useItem == stack)
        })
    }

    @JvmStatic
    fun isDyed(stack: ItemStack): Boolean = hasNonDefault(stack, DataComponents.DYED_COLOR)

    @JvmStatic
    fun isOpenedLetter(stack: ItemStack): Boolean = hasNonDefault(stack, DataComponents.WRITABLE_BOOK_CONTENT)

    /**
     * 26.1 `"tints": [{"type": "minecraft:dye", "default": ...}]` on the dyed dye blaster and the closed letter models.
     * 1.21.1 colors per item, so the other models of these items (empty blaster, opened letter) get no tint here.
     */
    fun registerItemColors(event: RegisterColorHandlersEvent.Item) {
        event.register({ stack, tintIndex ->
            if (tintIndex == 0 && isDyed(stack)) FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, 0xFFFFFF)) else -1
        }, item("dye_blaster"))
        event.register({ stack, tintIndex ->
            if (tintIndex == 0 && !isOpenedLetter(stack)) FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, -3523278)) else -1
        }, item("letter"))
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
