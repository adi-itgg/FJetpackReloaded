package me.phantomx.fjetpackreloaded.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.phantomx.fjetpackreloaded.annotations.Pure
import me.phantomx.fjetpackreloaded.extensions.safeFieldStringYaml
import net.mamoe.yamlkt.Comment

@Serializable
data class CustomFuel(
    @Pure
    @Transient
    var id: String = "CVIP",
    @Comment("""
        Custom display name the fuel in jetpack placdeholder {#fuel}
        Leave empty, will using DisplayName
        """)
    var customDisplay: String = "&6&lPertamax",
    @Comment("Display Name item")
    var displayName: String = "&b&lC&6&lVIP",
    @Comment("The lore of item")
    var lore: MutableList<String> = mutableListOf(
        "",
        "&eFuel for &l&6VIP!",
        "",
        "&6&lPremium &efuel"
    ),
    @Comment("Item minecraft item id")
    var item: String = "GOLD_INGOT",
    @Comment("Permission to use this fuel")
    var permission: String = "fjetpackreloaded.fuel.#id",
    @Comment("Glow this item")
    var glowing: Boolean = true
) {
    fun safeStringsClassYaml(): CustomFuel {
        customDisplay = customDisplay.safeFieldStringYaml()
        displayName = displayName.safeFieldStringYaml()
        lore = lore.map { it.safeFieldStringYaml() }.toMutableList()
        return this
    }
}
