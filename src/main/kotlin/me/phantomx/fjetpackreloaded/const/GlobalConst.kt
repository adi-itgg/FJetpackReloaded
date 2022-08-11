package me.phantomx.fjetpackreloaded.const

import me.phantomx.fjetpackreloaded.FJetpackReloaded

object GlobalConst {

    const val STRING_EMPTY = ""

    const val CUSTOM_FUEL_PREFIX = "#"
    const val ID_CUSTOM_FUEL: String = "FJRCustomFuel"
    const val ID_FUEL_JETPACK = "FJRFuel"

    // Placeholders
    const val JETPACK_FUEL_PLACEHOLDER = "{#fuel}"
    const val JETPACK_FUEL_VALUE_PLACEHOLDER = "{#fuel_value}"
    const val JETPACK_FUEL_MESSAGE_PLACEHOLDER = "%fuel_item%"

    const val CONFIG_JETPACKS_LOCATION = "configs/Jetpacks.yml"
    const val CONFIG_CUSTOM_FUELS_LOCATION = "configs/CustomFuels.yml"
    const val CONFIG_MESSAGES_LOCATION = "configs/Messages.yml"

    val ID_JETPACK: String = FJetpackReloaded::class.java.simpleName
    val FJETPACK_PERMISSION_PREFIX = ID_JETPACK.lowercase() + "."
}