package me.phantomx.fjetpackreloaded

import kotlinx.coroutines.*
import me.phantomx.fjetpackreloaded.commands.FJRCommands
import me.phantomx.fjetpackreloaded.events.EventListener
import me.phantomx.fjetpackreloaded.extensions.*
import me.phantomx.fjetpackreloaded.modules.Module.customFuel
import me.phantomx.fjetpackreloaded.modules.Module.id
import me.phantomx.fjetpackreloaded.modules.Module.idJetpack
import me.phantomx.fjetpackreloaded.modules.Module.jetpacks
import me.phantomx.fjetpackreloaded.modules.Module.listPlayerUse
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
                            setString(ItemStack(Material.CHAINMAIL_CHESTPLATE), idJetpack, nmsAPIVersion),
                            idJetpack
                        ) != nmsAPIVersion
                    )
                        throw UnsupportedClassVersionError("Unsupported server api version!")
                }
            "&6Detected Server: &a${Bukkit.getName()} v$serverVersion - API $nmsAPIVersion"
        } catch (e: Exception) {
            e.printStackTrace()
            "&cNot tested server version ${server.version} disabling plugin...".send(console, true)
            "&cUnknown Server: &a${Bukkit.getName()} ${Bukkit.getBukkitVersion()}".send(console, true)
            "&cThis plugin will not work because this server has unknown version!"
        }.send(console, true)
        if (serverVersion == 0) {
            isEnabled = false
            return
        }

        launch {
            if (load(sender = console))
                "&a&lAll configs has been loaded".send(console)
            else {
                main {
                    isEnabled = false
                }
                return@launch
            }
            console.checkUpdatePlugin(loginEvent = false)
            main {
                server.pluginManager.registerEvents(EventListener(), plugin)
            }
        }
        Metrics(this, id)
    }

    override fun onDisable() {
        if (mainContext.isActive) mainContext.cancel(CancellationException("Plugin is disabled"))
        val players = listPlayerUse.entries.iterator()
        while (players.hasNext()) {
            val player = players.next()
            player.key.player.turnOff()
            listPlayerUse.remove(player.key)
        }
        jetpacks.clear()
        customFuel.clear()
        if (mainContext.isActive)
            runBlocking {
                (mainContext as Job).cancelAndJoin()
            }
    }

}