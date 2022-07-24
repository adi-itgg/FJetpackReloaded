package me.phantomx.fjetpackreloaded.listeners.hook

import com.bgsoftware.superiorskyblock.api.events.PluginInitializeEvent
import com.bgsoftware.superiorskyblock.api.island.IslandFlag
import me.phantomx.fjetpackreloaded.extensions.send
import me.phantomx.fjetpackreloaded.fields.HookPlugin.fjetpackReloadedFlag
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class SuperiorSkyblock2Listener: Listener {

    private val fjrFlag = "FJETPACK_RELOADED"

    init {
        "&aRegistering SuperiorSkyblock2...".send()
    }

    @Suppress("unused")
    @EventHandler
    fun onPluginInit(e: PluginInitializeEvent) {
        IslandFlag.register(fjrFlag)
        fjetpackReloadedFlag = IslandFlag.getByName(fjrFlag)
        "&aRegistered flag &6$fjrFlag".send()
    }

}