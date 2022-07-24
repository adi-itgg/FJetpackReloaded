package me.phantomx.fjetpackreloaded.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import me.phantomx.fjetpackreloaded.extensions.withSafe
import me.phantomx.fjetpackreloaded.modules.Module.dataPlayer
import me.phantomx.fjetpackreloaded.modules.Module.listPlayerUse
import me.ryanhamshire.GriefPrevention.Claim
import org.bukkit.entity.Player
import java.util.*

data class PlayerFlying(var player: Player) {

    var fuelJob: Job? = null
    var particleJob: Job? = null

    var griefClaim: Claim? = null
    var ss2Island: UUID? = null

    fun stop() {
        if (fuelJob?.isActive == true)
            fuelJob?.cancel(CancellationException("Job Burn Fuel Cancelled"))
        if (particleJob?.isActive == true)
            particleJob?.cancel(CancellationException("Job Particle Effect Cancelled"))
        player.withSafe {
            listPlayerUse.remove(this@PlayerFlying)
            dataPlayer.remove(uniqueId)
            allowFlight = false
            isFlying = false
        }
    }

}