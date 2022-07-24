package me.phantomx.fjetpackreloaded.listeners.hook

import com.bgsoftware.superiorskyblock.api.events.PluginInitializeEvent
import com.bgsoftware.superiorskyblock.api.island.IslandFlag
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege
import me.phantomx.fjetpackreloaded.extensions.send
import me.phantomx.fjetpackreloaded.fields.HookPlugin.FJR_SS2_FLAG_PRIVILEGE
import me.phantomx.fjetpackreloaded.fields.HookPlugin.fjetpackReloadedSS2Flag
import me.phantomx.fjetpackreloaded.fields.HookPlugin.fjetpackReloadedSS2Privilege
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class SuperiorSkyblock2Listener: Listener {

    init {
        "&aRegistering SuperiorSkyblock2...".send()
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
    }

}