package me.phantomx.fjetpackreloaded

import kotlinx.coroutines.*
import me.phantomx.fjetpackreloaded.commands.FJRCommands
import me.phantomx.fjetpackreloaded.const.GlobalConst.ID_JETPACK
import me.phantomx.fjetpackreloaded.listeners.EventListener
import me.phantomx.fjetpackreloaded.listeners.hook.SuperiorSkyblock2Listener
import me.phantomx.fjetpackreloaded.extensions.*
import me.phantomx.fjetpackreloaded.modules.Module.customFuel
import me.phantomx.fjetpackreloaded.modules.Module.dataFJRPlayer
import me.phantomx.fjetpackreloaded.modules.Module.id
import me.phantomx.fjetpackreloaded.modules.Module.jetpacks
import me.phantomx.fjetpackreloaded.modules.Module.fjrPlayersActive
import me.phantomx.fjetpackreloaded.modules.Module.load
import me.phantomx.fjetpackreloaded.modules.Module.mainContext
import me.phantomx.fjetpackreloaded.modules.Module.metaData
import me.phantomx.fjetpackreloaded.modules.Module.nmsAPIVersion
import me.phantomx.fjetpackreloaded.modules.Module.plugin
import me.phantomx.fjetpackreloaded.modules.Module.serverVersion
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.coroutines.CoroutineContext


class FJetpackReloaded : FJRCommands() {

    override val coroutineContext: CoroutineContext
        get() = mainContext

    override fun onEnable() {
        plugin = this
        val console = server.consoleSender
        try {
            serverVersion = Bukkit.getVersion().lowercase().split("mc:")[1].split(".")[1].toIntSafe()
            if (serverVersion == 0) throw IncompatibleClassChangeError("Unknown Server Version!")
            nmsAPIVersion = server.javaClass.getPackage().name.split(".").toTypedArray()[3]
            if (serverVersion > 17)
                metaData.apply {
                    if (getString(
                            setString(ItemStack(Material.CHAINMAIL_CHESTPLATE), ID_JETPACK, nmsAPIVersion),
                            ID_JETPACK
                        ) != nmsAPIVersion
                    )
                        throw UnsupportedClassVersionError("Unsupported server api version!")
                }
            "&6Detected Server: &a${Bukkit.getName()} v$serverVersion - API $nmsAPIVersion"
        } catch (e: Exception) {
            e.printStackTrace()
            "&cNot tested server version ${server.version} disabling plugin...".send(console)
            "&cUnknown Server: &a${Bukkit.getName()} ${Bukkit.getBukkitVersion()}".send(console)
            "&cThis plugin will not work because this server has unknown version!"
        }.send(console)
        if (serverVersion == 0) {
            isEnabled = false
            return
        }

        server.pluginManager.registerEvents(SuperiorSkyblock2Listener(), this)

        launch {
            if (load(sender = console))
                "&a&lAll configs has been loaded".send()
            else {
                mainThread {
                    isEnabled = false
                }
                return@launch
            }
            server.mainThread {
                pluginManager.registerEvents(EventListener(), plugin)
            }
            console.checkUpdatePlugin(loginEvent = false)
        }
        Metrics(this, id)
    }

    override fun onDisable() {
        "&cCancelling all jobs running".send()
        if (mainContext.isActive) mainContext.cancel(CancellationException("Plugin is disabled"))
        fjrPlayersActive.entries.iterator().withSafe {
            while (hasNext()) next().key.safeUnloadPluginXSS2()
        }
        jetpacks.clear()
        customFuel.clear()
        dataFJRPlayer.clear()
        mainContext.withSafe {
            cancelChildren()
        }
        if (mainContext.isActive)
            runBlocking {
                (mainContext as Job).cancelAndJoin()
            }
        "&cCancalled all jobs and plugin has been unloaded".send()
    }

}