package joshxviii.plantz.item.crafting

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import joshxviii.plantz.pazResource
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

/**
 * 26.1 `minecraft:crafting_transmute` (with its default single material), as `plantz:crafting_transmute`: one [input]
 * plus one [material] gives [result] carrying over the input's components (name, contents, ...). The mailbox dye
 * recipes use it.
 */
class TransmuteRecipe(
    private val groupName: String,
    private val bookCategory: CraftingBookCategory,
    val input: Ingredient,
    val material: Ingredient,
    val result: ItemStack,
) : CraftingRecipe {

    override fun matches(input: CraftingInput, level: Level): Boolean {
        if (input.ingredientCount() != 2) return false
        var foundInput: ItemStack? = null
        for (slot in 0 until input.size()) {
            val stack = input.getItem(slot)
            if (stack.isEmpty) continue
            if (this.input.test(stack)) {
                if (foundInput != null) return false
                foundInput = stack
            } else if (!material.test(stack)) return false
        }
        if (foundInput == null) return false
        return result.count != 1 || !ItemStack.isSameItemSameComponents(foundInput, transmute(foundInput))
    }

    override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
        for (slot in 0 until input.size()) {
            val stack = input.getItem(slot)
            if (!stack.isEmpty && this.input.test(stack)) return transmute(stack)
        }
        return ItemStack.EMPTY
    }

    /** 26.1 `TransmuteRecipe.createWithOriginalComponents`. */
    private fun transmute(inputStack: ItemStack): ItemStack = result.copy().also { it.applyComponents(inputStack.componentsPatch) }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2
    override fun getResultItem(registries: HolderLookup.Provider): ItemStack = result
    override fun getIngredients(): NonNullList<Ingredient> = NonNullList.of(Ingredient.EMPTY, input, material)
    override fun getGroup(): String = groupName
    override fun category(): CraftingBookCategory = bookCategory
    override fun getSerializer(): RecipeSerializer<*> = SERIALIZER

    companion object {
        val CODEC: MapCodec<TransmuteRecipe> = RecordCodecBuilder.mapCodec { i ->
            i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(TransmuteRecipe::groupName),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(TransmuteRecipe::bookCategory),
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(TransmuteRecipe::input),
                Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(TransmuteRecipe::material),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(TransmuteRecipe::result)
            ).apply(i, ::TransmuteRecipe)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TransmuteRecipe::groupName,
            CraftingBookCategory.STREAM_CODEC, TransmuteRecipe::bookCategory,
            Ingredient.CONTENTS_STREAM_CODEC, TransmuteRecipe::input,
            Ingredient.CONTENTS_STREAM_CODEC, TransmuteRecipe::material,
            ItemStack.STREAM_CODEC, TransmuteRecipe::result,
            ::TransmuteRecipe
        )

        @JvmField
        val SERIALIZER: RecipeSerializer<TransmuteRecipe> = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            pazResource("crafting_transmute"),
            object : RecipeSerializer<TransmuteRecipe> {
                override fun codec(): MapCodec<TransmuteRecipe> = CODEC
                override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> = STREAM_CODEC
            }
        )

        fun initialize() {}
    }
}
