package me.phantomx.fjetpackreloaded.data

import kotlinx.serialization.Serializable
import me.phantomx.fjetpackreloaded.extensions.safeFieldStringYaml
import net.mamoe.yamlkt.Comment

@Serializable
data class Messages(
    @Comment("""
        Leave Empty message will disable the message
        
        Message prefix
        """)
    var prefix: String = "&e&l[&bFJetpack&6Reloaded&e&l]&r",
    @Comment("When jetpack turned on")
    var turnOn: String = "&a&lON",
    @Comment("When jetpack turned off")
    var turnOff: String = "&4&lOFF",
    @Comment("Player don't have premission")
    var noPerms: String = "&cYou don't have permission!",
    @Comment("Jetpack removed from equipment")
    var detached: String = "&cYou take off your Jetpack, making you fall to the ground",
    @Comment("Jetpack out of fuel")
    var outOfFuel: String = "&cYou are out of fuel!",
    @Comment("Player using jetpack in blocked worlds")
    var blockedWorlds: String = "&cYou can't use Jetpack in this World!",
    @Comment("Jetpack don't have enough fuel")
    var noFuel: String = "&cYou don't have fuel to fly!",
    @Comment("When the jetpack runs out of fuel, it will fall to the ground")
    var onEmptyFuelDropped: String = "&cJetpack out of fuel has been dropped!",
    @Comment("When the jetpack runs out of fuel, it will removed from player")
    var onEmptyFuelRemoved: String = "&cJetpack out of fuel has been removed!",
    @Comment("When player died, iit will fall to the ground")
    var onDeathDropped: String = "&cYou died jetpack has been dropped!",
    @Comment("When player died, it will removed from player")
    var onDeathRemoved: String = "&cYou died jetpack has been removed!",
    @Comment("Turn on jetpack, but outside claim!")
    var griefPreventionOutsideClaim: String = "&cYou didn't inside claim!",
    @Comment("Turn off jetpack if player outside claim while flying")
    var griefPreventionTurnedOffOutsideClaim: String = "&cYou outside claim jetpack is turned off",
    @Comment("Turn on jetpack, but outside player own claim")
    var griefPreventionOutsideOwnClaim: String = "&cYou are not in your own claim!",
    @Comment("If player doesn't have permission to fly inside island")
    var superiorSkyblock2NoPermission: String = "&cYou don't have permission to fly in this island!"
) {

    fun safeStringsClassYaml(): Messages {
        prefix = prefix.safeFieldStringYaml()
        turnOn = turnOn.safeFieldStringYaml()
        turnOff = turnOff.safeFieldStringYaml()
        noPerms = noPerms.safeFieldStringYaml()
        detached = detached.safeFieldStringYaml()
        outOfFuel = outOfFuel.safeFieldStringYaml()
        blockedWorlds = blockedWorlds.safeFieldStringYaml()
        noFuel = noFuel.safeFieldStringYaml()
        onEmptyFuelDropped = onEmptyFuelDropped.safeFieldStringYaml()
        onEmptyFuelRemoved = onEmptyFuelRemoved.safeFieldStringYaml()
        onDeathDropped = onDeathDropped.safeFieldStringYaml()
        onDeathRemoved = onDeathRemoved.safeFieldStringYaml()
        griefPreventionOutsideClaim = griefPreventionOutsideClaim.safeFieldStringYaml()
        griefPreventionTurnedOffOutsideClaim = griefPreventionTurnedOffOutsideClaim.safeFieldStringYaml()
        griefPreventionOutsideOwnClaim = griefPreventionOutsideOwnClaim.safeFieldStringYaml()
        superiorSkyblock2NoPermission = superiorSkyblock2NoPermission.safeFieldStringYaml()
        return this
    }
}