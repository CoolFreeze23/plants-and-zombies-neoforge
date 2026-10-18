package joshxviii.plantz.renderer

import joshxviii.plantz.PazEntities.MAGIC_NAMES
import joshxviii.plantz.pazResource
import joshxviii.plantz.renderer.entity.ProjectileRenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.model.geom.ModelPart
import joshxviii.plantz.compat.client.state.BlockEntityRenderState
import joshxviii.plantz.compat.client.state.EntityRenderState
import joshxviii.plantz.compat.client.state.LivingEntityRenderState
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import joshxviii.plantz.compat.ARGB
import net.minecraft.util.FormattedCharSequence
import kotlin.collections.component1
import kotlin.collections.component2

fun GuiGraphics.outlineText(font: Font, text: String, x: Int = 0, y: Int = 0, color: Int = 0xFFFFFF, outlineColor: Int = ARGB.multiply(color, 0x333333)) {
    val c = Component.literal(text)
    outlineText(font, c, x, y, color, outlineColor)
}

fun GuiGraphics.outlineText(font: Font, text: Component, x: Int = 0, y: Int = 0, color: Int = 0xFFFFFF, outlineColor: Int = ARGB.multiply(color, 0x333333)) {
    outlineText(font, text.visualOrderText, x, y, color, outlineColor)
}

fun GuiGraphics.outlineText(font: Font, text: FormattedCharSequence, x: Int = 0, y: Int = 0, color: Int = 0xFFFFFF, outlineColor: Int = ARGB.multiply(color, 0x333333)) {
    drawString(font, text, x+1, y, ARGB.opaque(outlineColor), false)
    drawString(font, text, x-1, y, ARGB.opaque(outlineColor), false)
    drawString(font, text, x, y+1, ARGB.opaque(outlineColor), false)
    drawString(font, text, x, y-1, ARGB.opaque(outlineColor), false)
    drawString(font, text, x, y, ARGB.opaque(color), false)
}

// MODEL RENDERING
fun ModelPart.getChildOrNull(name: String): ModelPart? = if (this.hasChild(name)) this.getChild(name) else null

fun List<String>.permutationsDescending(): List<String> = buildList {
    add(this@permutationsDescending.joinToString("_"))
    for (i in size - 1 downTo 1) {
        add(this@permutationsDescending.subList(0, i).joinToString(""))
    }
}

fun resolveTextureLocation(base: String, rm: ResourceManager, suffixes: List<String> = listOf()): ResourceLocation? {
    for (suffix in suffixes.permutationsDescending()) {
        if (suffix.isEmpty()) break
        val candidate = pazResource("${base}_${suffix}.png")
        if (rm.getResource(candidate).isPresent) return candidate
    }
    return null
}

fun LivingEntityRenderState.isMagicName(name: String): String {
    val type = this.entityType
    MAGIC_NAMES.forEach { (entityType, magicName) ->
        if (entityType == type && magicName == name.lowercase()) return magicName
    }
    return ""
}

fun BlockEntityRenderState.getTextureLocation(path: String): ResourceLocation {
    return pazResource("${path}.png")
}

fun EntityRenderState.getProjectileTextureLocation(emissive: Boolean = false): ResourceLocation? {
    val entityName = entityType.toShortString().lowercase()
    val path = "${ProjectileRenderState.TEXTURE_PATH}/${entityName}${if (emissive) "_emissive" else ""}.png"
    val texture = pazResource(path)
    val isValid = Minecraft.getInstance().resourceManager.getResource(texture).isPresent
    return if (isValid) texture else null
}

fun EntityRenderState.getTextureLocation(basePath: String, suffixes: MutableList<String> = mutableListOf()): ResourceLocation {
    val entityName = entityType.toShortString().lowercase()
    val base = "${basePath}/${entityName}/${entityName}"
    val rm = Minecraft.getInstance().resourceManager

    val textureLocation = resolveTextureLocation(base, rm, suffixes)
    return textureLocation?: pazResource("${base}.png")
}

fun EntityRenderState.getEmissiveTextureLocation(basePath: String, suffixes: MutableList<String> = mutableListOf()): ResourceLocation? {
    val entityName = entityType.toShortString().lowercase()
    val base = "${basePath}/${entityName}/${entityName}"
    val rm = Minecraft.getInstance().resourceManager

    return resolveTextureLocation(base, rm, suffixes.apply { add("emissive") })
}