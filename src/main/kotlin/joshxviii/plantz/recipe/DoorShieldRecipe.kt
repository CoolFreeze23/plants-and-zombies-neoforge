package joshxviii.plantz.recipe

import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazItems
import joshxviii.plantz.item.component.DoorShieldComponent
import joshxviii.plantz.pazResource
import net.minecraft.core.HolderLookup
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DoorBlock

class DoorShieldRecipe(category: CraftingBookCategory) : CustomRecipe(category) {
    override fun matches(input: CraftingInput, level: Level): Boolean {
        return input.ingredientCount() == 2 && doorBlock(input) != null &&
            input.items().count { it.`is`(Items.SHIELD) } == 1
    }

    override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
        val door = doorBlock(input) ?: return ItemStack.EMPTY
        return ItemStack(PazItems.DOOR_SHIELD).also {
            it.set(PazComponents.DOOR_SHIELD, DoorShieldComponent(door))
        }
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2

    override fun getSerializer(): RecipeSerializer<DoorShieldRecipe> = SERIALIZER

    private fun doorBlock(input: CraftingInput): Block? {
        if (input.ingredientCount() != 2) return null

        val doorItem = input.items().singleOrNull { it.`is`(ItemTags.DOORS) } ?: return null
        val doorBlock = (doorItem.item as? BlockItem)?.block ?: return null
        return doorBlock.takeIf { it is DoorBlock }
    }

    companion object {
        val SERIALIZER: RecipeSerializer<DoorShieldRecipe> = SimpleCraftingRecipeSerializer(::DoorShieldRecipe)

        fun register() {
            Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, pazResource("door_shield"), SERIALIZER)
        }
    }
}
