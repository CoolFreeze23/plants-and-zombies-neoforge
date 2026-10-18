package joshxviii.plantz.entity.gnome

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import joshxviii.plantz.pazResource
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ByIdMap
import net.minecraft.util.StringRepresentable
import java.util.function.IntFunction

/**
 * The Gnome Variants used for Textures.
 */
enum class GnomeVariant(val color: String, val id: Int) : StringRepresentable {
    BLUE("blue", 0),
    RED("red", 1),
    GREEN("green", 2),
    YELLOW("yellow", 3);

    override fun getSerializedName(): String = color
    fun getTexture(): ResourceLocation = pazResource("textures/entity/gnome/${color}.png")// 26.1 ClientAsset.ResourceTexture(id).texturePath

    companion object {
        fun getDefault(): GnomeVariant = BLUE
        fun pickRandomVariant(): GnomeVariant = entries.random()

        val CODEC: Codec<GnomeVariant> = StringRepresentable.fromEnum(GnomeVariant::values)
        private val BY_ID: IntFunction<GnomeVariant> = ByIdMap.continuous(GnomeVariant::id, entries.toTypedArray(), ByIdMap.OutOfBoundsStrategy.ZERO);
        val STREAM_CODEC: StreamCodec<ByteBuf, GnomeVariant> = ByteBufCodecs.idMapper<GnomeVariant>(BY_ID, GnomeVariant::id)
    }
}