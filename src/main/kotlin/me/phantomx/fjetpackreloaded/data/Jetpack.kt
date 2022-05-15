package me.phantomx.fjetpackreloaded.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.phantomx.fjetpackreloaded.annotations.Pure
import me.phantomx.fjetpackreloaded.sealeds.OnDeath
import me.phantomx.fjetpackreloaded.sealeds.OnEmptyFuel
import net.mamoe.yamlkt.Comment

@Serializable
data class Jetpack(
    @Pure
    @Transient
    @Comment("The jetpack id")
    var id: String= "Member",
    @Comment("Display the item name")
    var displayName: String = "&8&l[&bFJetpack&6&lReloaded&8&l]",
    @Comment("Lore item")
    var lore: MutableList<String> = mutableListOf(
        "&3&m&l----===[&r &8 &b&lINFO &8 &3&m&l]===----",
        "&9Rank: &b&lMember",
        "",
        "&3&m&l----===[&b &lUSAGE &3&m&l]===----",
        "&9Sneak to toggle on/off",
        "&9Double jump to fly",
        "",
        "&7Fuel: &a{#fuel_value} &b{#fuel}"
    ),
    @Comment("""
        Permission to use this jetpack
        use #id will replace the jetpack id
        example: fjetpackreloaded.#id -> fjetpackreloaded.Member
        """)
    var permission: String = "fjetpackreloaded.#id",
    @Comment("Allow player to bypass fuel cost")
    var canBypassFuel: Boolean = false,
    @Comment("Allow player to bypass fuel sprint cost")
    var canBypassSprintFuel: Boolean = false,
    @Comment("""
        The minecraft item id (Supports all armor items)
        Example for colored leather armor:
        Red armor -> LEATHER_CHESTPLATE:255.0.0
        The format is ITEM:R.G.B
        """)
    var jetpackItem: String = "LEATHER_CHESTPLATE:255.0.0",
    @Comment("Set jetpack to unbreakable")
    var unbreakable: Boolean = false,
    @Comment("""
        Event when player out of fuel!
        'Remove' - Remove Jetpack on Fuel is empty
        'Drop' - Drop Jetpack on Fuel is empty
        'None' - Nothing to do
    """)
    var onEmptyFuel: String = "None",
    @Comment("""
        Event when player died
        'Remove' - Remove Jetpack on Player died
        'Drop' - Drop Jetpack on Player died
        'None' - Nothing to do
    """)
    var onDeath: String = "None",
    @Comment("""
        Only allow jetpack to fly inside player own Grief Prevention claim
        this is not work if onlyAllowInsideAllGriefPreventionClaim is true
        """)
    var onlyAllowInsideOwnGriefPreventionClaim: Boolean = false,
    @Comment("Only allow jetpack to fly inside all Grief Prevention claim")
    var onlyAllowInsideAllGriefPreventionClaim: Boolean = false,
    @Comment("Set Custom Model Data, -1 to disable")
    var customModelData: Int = -1,
    @Comment("""
        The Fuel Item Material ID
        For CustomFuels.yml using ID with prefix @
        Example: @CVIP
    """)
    var fuel: String = "Coal",
    @Comment("Fuel cost amount")
    var fuelCost: Int = 1,
    @Comment("Sprint Fuel cost amount")
    var fuelCostFlySprint: Int = 1,
    @Comment("Burn rate in seconds")
    var burnRate: Int = 3,
    @Comment("""
        Set jetpack fly speed in float
        Maximum speed value is -10 to 10!
        """)
    var speed: String = "1.0",
    @Comment("""
        If you don't want to use particle effect set to 'none'
        If particle doesn't support or error, this plugin will automatically replace particle effect to CLOUD!
    """)
    var particleEffect: String = "None",
    @Comment("Amount of particle")
    var particleAmount: Int = 0,
    @Comment("""
        Particle delay in miliseconds
        I don't recommend to set delay below 100ms!
        """)
    var particleDelay: Long = 100,
    @Comment("If you don't want to use flags set to 'none'")
    var flags: MutableList<String> = mutableListOf(
        "HIDE_ATTRIBUTES",
        "HIDE_ENCHANTS"
    ),
    @Comment("""
        If you don't want to use enchantment set to 'none'
        Enchant under version 1.17 server, use old method enchantments. Like "DURABILITY:3"
        unbreaking:3 for newer server version 1.17+
    """)
    var enchantments: MutableList<String> = mutableListOf(
        "unbreaking:3",
        "DURABILITY:3"
    ),
    @Comment("Blocked worlds")
    var blockedWorlds: MutableList<String> = mutableListOf()
) {
    @Transient
    lateinit var onNoFuel: OnEmptyFuel
    @Transient
    lateinit var onDied: OnDeath
}
