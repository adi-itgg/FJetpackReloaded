package me.phantomx.fjetpackreloaded.listeners.hook

import com.bgsoftware.superiorskyblock.api.events.IslandChangeRolePrivilegeEvent
import com.bgsoftware.superiorskyblock.api.events.IslandDisableFlagEvent
import com.bgsoftware.superiorskyblock.api.events.PluginInitializeEvent
import com.bgsoftware.superiorskyblock.api.island.IslandFlag
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.phantomx.fjetpackreloaded.extensions.asFJRPlayer
import me.phantomx.fjetpackreloaded.extensions.send
import me.phantomx.fjetpackreloaded.extensions.withSafe
import me.phantomx.fjetpackreloaded.fields.HookPlugin.FJR_SS2_FLAG_PRIVILEGE
import me.phantomx.fjetpackreloaded.fields.HookPlugin.SuperiorSkyblock2Name
import me.phantomx.fjetpackreloaded.fields.HookPlugin.fjetpackReloadedSS2Flag
import me.phantomx.fjetpackreloaded.fields.HookPlugin.fjetpackReloadedSS2Privilege
import me.phantomx.fjetpackreloaded.modules.Module.mainContext
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import kotlin.coroutines.CoroutineContext

class SuperiorSkyblock2Listener: Listener, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = mainContext

    var isRegistered = false

    init {
        "&aRegistering SuperiorSkyblock2...".send()
        launch {
            delay(5_000)
            if (!Bukkit.getServer().pluginManager.isPluginEnabled(SuperiorSkyblock2Name) || isRegistered) return@launch
            "&cFailed to register flag and privilege for &b$SuperiorSkyblock2Name".send()
            "&cIf you want to hook to &b$SuperiorSkyblock2Name&c don't use plugman to load this plugin!".send()
            "&cYou have to restart the server to make sure &b$SuperiorSkyblock2Name&c plugin is hooked".send()
        }
    }

    @Suppress("unused")
    @EventHandler
    fun onPluginInit(e: PluginInitializeEvent) {
        IslandFlag.register(FJR_SS2_FLAG_PRIVILEGE)
        fjetpackReloadedSS2Flag = IslandFlag.getByName(FJR_SS2_FLAG_PRIVILEGE)
        "&aRegistered flag &6$FJR_SS2_FLAG_PRIVILEGE".send()

        IslandPrivilege.register(FJR_SS2_FLAG_PRIVILEGE)
        fjetpackReloadedSS2Privilege = IslandPrivilege.getByName(FJR_SS2_FLAG_PRIVILEGE)
        "&aRegistered privilege &6$FJR_SS2_FLAG_PRIVILEGE".send()
        isRegistered = true
    }

    @Suppress("unused")
    @EventHandler
    fun onIslandChangeRolePrivilegeEvent(e: IslandChangeRolePrivilegeEvent) {
        onEvents(e.island.allPlayersInside)
    }

    @Suppress("unused")
    @EventHandler
    fun onIslandDisableFlagEvent(e: IslandDisableFlagEvent) {
        onEvents(e.island.allPlayersInside)
    }

    private fun onEvents(p: List<SuperiorPlayer>) {
        launch {
            for (sp in p)
                sp.withSafe {
                    asPlayer()?.asFJRPlayer()?.checkSuperiorSkyblock2EventsChanged(true)
                }
        }
    }

}