package joshxviii.plantz.block

import net.minecraft.util.StringRepresentable

enum class TimeMachineState(val stateName: String) : StringRepresentable {
    INACTIVE("inactive"),
    BATTERY("battery"),
    ACTIVE("active");

    override fun getSerializedName(): String {
        return this.stateName
    }
}