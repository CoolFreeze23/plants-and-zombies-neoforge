package joshxviii.plantz

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.client.model.BakedModelWrapper

/**
 * 26.1 `plantz:door_shield_model` item model (the item looks like the door its DOOR_SHIELD component names) ported to
 * 1.21.1 baked models: the door shield's inventory model is wrapped ([modifyBakingResult]) with [ItemOverrides] that
 * pick, per stack, the door item's own model (oak door when the component is missing or the door has no item).
 */
object DoorShieldItemModel {
    private val DOOR_SHIELD = pazResource("door_shield")

    fun modifyBakingResult(event: ModelEvent.ModifyBakingResult) {
        val key = ModelResourceLocation.inventory(DOOR_SHIELD)
        event.models[key]?.let { event.models[key] = DoorShieldModel(it) }
    }

    private class DoorShieldModel(base: BakedModel) : BakedModelWrapper<BakedModel>(base) {
        private val overrides = object : ItemOverrides() {
            override fun resolve(model: BakedModel, stack: ItemStack, level: ClientLevel?, entity: LivingEntity?, seed: Int): BakedModel {
                val door = stack.get(PazComponents.DOOR_SHIELD)?.doorType?.asItem()?.takeIf { it != net.minecraft.world.item.Items.AIR }
                    ?: Blocks.OAK_DOOR.asItem()
                val doorModel = Minecraft.getInstance().itemRenderer.itemModelShaper.getItemModel(door) ?: return model
                return doorModel.overrides.resolve(doorModel, stack, level, entity, seed) ?: doorModel
            }
        }

        override fun getOverrides(): ItemOverrides = overrides
    }
}
