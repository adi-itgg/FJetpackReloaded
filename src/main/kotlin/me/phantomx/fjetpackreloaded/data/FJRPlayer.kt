package me.phantomx.fjetpackreloaded.data

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI
import com.bgsoftware.superiorskyblock.api.island.Island
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import me.phantomx.fjetpackreloaded.extensions.*
import me.phantomx.fjetpackreloaded.fields.HookPlugin
import me.phantomx.fjetpackreloaded.modules.Module.messages
import me.phantomx.fjetpackreloaded.modules.Module.dataFJRPlayer
import me.phantomx.fjetpackreloaded.modules.Module.fjrPlayersActive
import me.ryanhamshire.GriefPrevention.Claim
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.atomic.AtomicReference

data class FJRPlayer(val player: Player) {

    val isActive: Boolean
    get() = fuelJob?.isActive == true
    val isParticleActive: Boolean
    get() = particleJob?.isActive == true

    var fuelJob: Job? = null
    var particleJob: Job? = null

    var griefClaim: Claim? = null
    var ss2IslandUUID: UUID? = null
    var jetpack: AtomicReference<Jetpack> = AtomicReference()

    /**
     * Require run on main Thread
     */
    fun stop() {
        if (isActive)
            fuelJob?.cancel(CancellationException("Job Burn Fuel Cancelled"))
        if (isParticleActive)
            particleJob?.cancel(CancellationException("Job Particle Effect Cancelled"))
        griefClaim = null
        ss2IslandUUID = null
        player.withSafe {
            fjrPlayersActive.remove(this@FJRPlayer)
            dataFJRPlayer.remove(uniqueId)
            allowFlight = false
            isFlying = false
        }
    }

    /**
     * Require run on Main thread
     */
    fun safeUnloadPluginXSS2() {
        ss2IslandUUID?.withSafe {
            val island = SuperiorSkyblockAPI.getIslandByUUID(this) ?: return@withSafe
            safeTeleportSS2Player(island)
        }
        stop()
    }

    fun checkSuperiorSkyblock2EventsChanged(event: Boolean) {
        ss2IslandUUID?.withSafe {
            val jetpack = jetpack.get() ?: return
            SuperiorSkyblockAPI.getIslandAt(player.location)?.let {
                if (it.uniqueId == this && !event) return@let
                // check inside new island is allowed or not
                if (!checkJetpackHasPermission(jetpack, it) && !player.isAdminOrOp()) {
                    safeTeleportSS2Player(it)
                    it.mainThread {
                        player.turnOff(noMessage = true)
                    }
                }

            }
        }
    }

    fun checkJetpackHasPermission(jetpack: Jetpack, it: Island, message: Boolean = true): Boolean {
        // check player inside island is allowed or not
        // for flag
        if (!jetpack.bypassSuperiorSkyblock2Flag)
            it.hasSettingsEnabled(HookPlugin.fjetpackReloadedSS2Flag).apply {
                if (!this) {
                    if (message)
                        messages.superiorSkyblock2NoFlag.send(player)
                    return false
                }
            }

        // for permissions
        if (!jetpack.bypassSuperiorSkyblock2Privilege) {
            if (!it.hasPermission(player, HookPlugin.fjetpackReloadedSS2Privilege)) {
                if (message)
                    messages.superiorSkyblock2NoPermission.send(player)
                return false
            }
        }
        return true
    }

    private fun safeTeleportSS2Player(it: Island) {
        val envNormal = World.Environment.NORMAL
        val loc = it.visitorsLocation ?: it.getIslandHome(envNormal) ?: it.getCenter(envNormal) ?: run {
            var location: Location? = null
            for (l in it.teleportLocations) {
                if (!l.value.isWorldLoaded) continue
                location = l.value
                break
            }
            location
        } ?: run {
            "&cFailed to teleport player".send()
            null
        }
        if (loc != null)
            player.mainThread { teleport(loc) }
    }

}