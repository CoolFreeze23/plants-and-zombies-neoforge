package joshxviii.plantz.entity.zombie

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.ByIdMap
import net.minecraft.util.RandomSource
import net.minecraft.util.StringRepresentable
import java.util.function.IntFunction

enum class ImpVariant(val suffix: String, val id: Int) : StringRepresentable {
    IMP("", 0),
    YETI("yeti", 1),
    PIRATE("pirate", 2);

    override fun getSerializedName(): String = suffix

    companion object {
        fun getDefault(): ImpVariant = IMP
        fun pickRandomVariant(): ImpVariant = entries.random()

        fun pickForBiome(isSnowy: Boolean, random: RandomSource): ImpVariant {
            return when {
                isSnowy -> YETI
                else -> IMP
            }
        }

        val CODEC: Codec<ImpVariant> = StringRepresentable.fromEnum(ImpVariant::values)
        private val BY_ID: IntFunction<ImpVariant> = ByIdMap.continuous(ImpVariant::id, entries.toTypedArray(), ByIdMap.OutOfBoundsStrategy.ZERO);
        val STREAM_CODEC: StreamCodec<ByteBuf, ImpVariant> = ByteBufCodecs.idMapper<ImpVariant>(BY_ID, ImpVariant::id)
    }
}