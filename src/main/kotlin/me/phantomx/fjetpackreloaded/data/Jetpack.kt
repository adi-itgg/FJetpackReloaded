package me.phantomx.fjetpackreloaded.data

import me.phantomx.fjetpackreloaded.sealeds.OnDeath
import me.phantomx.fjetpackreloaded.sealeds.OnEmptyFuel

data class Jetpack(
    var id: String,
    var displayName: String,
    var lore: MutableList<String>,
    var permission: String,
    var canBypassFuel: Boolean,
    var canBypassSprintFuel: Boolean,
    var jetpackItem: String,
    var unbreakable: Boolean,
    var onEmptyFuel: String,
    var onDeath: String,
    var fuel: String,
    var fuelCost: Int,
    var fuelCostFlySprint: Int,
    var burnRate: Int,
    var speed: String,
    var particleEffect: String,
    var particleAmount: Int,
    var particleDelay: Long,
    var flags: List<String>,
    var enchantments: List<String>,
    var worldBlackList: List<String>
) {
    lateinit var onNoFuel: OnEmptyFuel
    lateinit var onDied: OnDeath
}
