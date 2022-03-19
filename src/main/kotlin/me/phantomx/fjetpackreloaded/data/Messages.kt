package me.phantomx.fjetpackreloaded.data

import me.phantomx.fjetpackreloaded.modules.Module.stringEmpty


data class Messages(
    var prefix: String = stringEmpty,
    var turnOn: String = stringEmpty,
    var turnOff: String = stringEmpty,
    var noPerms: String = stringEmpty,
    var detached: String = stringEmpty,
    var outOfFuel: String = stringEmpty,
    var blockedWorlds: String = stringEmpty,
    var noFuel: String = stringEmpty,
    var onEmptyFuelDropped: String = stringEmpty,
    var onEmptyFuelRemoved: String = stringEmpty,
    var onDeathDropped: String = stringEmpty,
    var onDeathRemoved: String = stringEmpty
)