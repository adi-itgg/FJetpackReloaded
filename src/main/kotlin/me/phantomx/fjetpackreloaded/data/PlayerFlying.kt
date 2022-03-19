package me.phantomx.fjetpackreloaded.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import me.phantomx.fjetpackreloaded.modules.Module.dataPlayer
import me.phantomx.fjetpackreloaded.modules.Module.listPlayerUse
import org.bukkit.entity.Player

data class PlayerFlying(var player: Player) {
    var fuelJob: Job? = null
    var particleJob: Job? = null

    fun stop() {
        stop(null)
    }

    fun stop(message: String?) {
        fuelJob?.apply {
            if (isActive) cancel(CancellationException(message ?: "Job Burn Fuel Cancelled"))
        }
        particleJob?.apply {
            if (isActive) cancel(CancellationException(message ?: "Job Particle Effect Cancelled"))
        }
        try {
            listPlayerUse.remove(this)
            dataPlayer.remove(player.uniqueId)
            player.allowFlight = false
            player.isFlying = false
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }
    }

}