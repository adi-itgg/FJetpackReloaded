package me.phantomx.fjetpackreloaded.modules

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import me.phantomx.fjetpackreloaded.abstracts.Minecraft
import me.phantomx.fjetpackreloaded.abstracts.Plugin
import me.phantomx.fjetpackreloaded.data.Config
import me.phantomx.fjetpackreloaded.data.CustomFuel
import me.phantomx.fjetpackreloaded.data.Jetpack
import me.phantomx.fjetpackreloaded.data.Messages
import me.phantomx.fjetpackreloaded.extensions.*
import me.phantomx.fjetpackreloaded.nms.ItemMetaData
import me.phantomx.fjetpackreloaded.sealeds.OnDeath
import me.phantomx.fjetpackreloaded.sealeds.OnEmptyFuel
import org.bukkit.command.CommandSender
import org.json.JSONArray
import java.io.File
import java.util.*


object Module : Plugin() {

    init {
        messages = Messages(defaultPrefix)
        metaData = ItemMetaData()
    }

    /**
     * Load all configs
     */
    suspend fun Minecraft.load(sender: CommandSender): Boolean =
        try {
            reloadConfig()
            modifiedConfig = Config(config["version", 1].toString().toIntSafe(1), config["updateNotification", true].toString().toBoolean())
            if (configVersion == 0)
                getResource(config())?.use {
                    StringBuilder().apply {
                        Scanner(it).use {
                            while (it.hasNext())
                                append(it.nextLine())
                            configVersion = toString().toIntSafe(0)
                        }
                    }
                }

            File(dataFolder, config()).apply {
                if (!exists())
                    saveAllDefaultConfig()
                else if (modifiedConfig.version < configVersion) {
                    val m = "WARNING!: %s has been updated!"
                    "&eWARNING!: Config doesn't support!".send(sender)
                    File(dataFolder, "configs-v${modifiedConfig.version}-backup").let { backup ->
                        if (backup.mkdirs() && exists() && renameTo(File(backup, config())))
                            String.format(m, config()).send(sender)
                        val msg = File(dataFolder, messages())
                        if (msg.exists() && msg.renameTo(File(backup, messages())))
                            String.format(m, messages()).send(sender)
                        val jps = File(dataFolder, jetpacks())
                        if (jps.exists() && jps.renameTo(File(backup, jetpacks())))
                            String.format(m, jetpacks()).send(sender)
                    }
                    saveAllDefaultConfig()
                }
            }

            loadMessages(sender = sender)

            loadJetpacks(sender = sender)

            loadCustomFuels(sender = sender)
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            "Failed To Load Configs!".send(sender)
            disable()
            false
        }

    /**
     * load messages config
     */
    private fun org.bukkit.plugin.Plugin.loadMessages(sender: CommandSender) {
        File(dataFolder, messages()).apply {
            try {
                if (!exists())
                    if (parentFile.mkdirs() || parentFile.exists())
                        saveResource(messages(), false)
                messages = gson.fromJson(
                    File(dataFolder, messages())
                        .bufferedReader().use { it.readText() },
                    Messages::class.java
                )
                "&aMessages config has been loaded.".send(sender)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (renameTo(File(dataFolder, "${messages()}.error")))
                    if (parentFile.mkdirs() || parentFile.exists())
                        saveResource(messages(), true)
                e.printStackTrace()
            }
        }
    }

    /**
     * load Jetpacks config
     */
    private suspend fun org.bukkit.plugin.Plugin.loadJetpacks(sender: CommandSender) {
        File(dataFolder, jetpacks()).apply {
            try {
                if (!exists())
                    if (parentFile.mkdirs() || parentFile.exists())
                        saveResource(jetpacks(), false)
                jetpacks.clear()
                val jsonArray = JSONArray(File(dataFolder, jetpacks()).bufferedReader().use { it.readText() })
                var count = 0
                for (json in jsonArray) try {
                    yield()
                    gson.fromJson(json.toString(), Jetpack::class.java).apply {
                        onNoFuel = when (onEmptyFuel.lowercase()) {
                            "drop" -> OnEmptyFuel.Drop
                            "remove" -> OnEmptyFuel.Remove
                            else -> OnEmptyFuel.Nothing
                        }
                        onDied = when (onDeath.lowercase()) {
                            "drop" -> OnDeath.Drop
                            "remove" -> OnDeath.Remove
                            else -> OnDeath.Nothing
                        }
                        permission = permission.replace("#id", id.lowercase())
                        jetpacks[id] = this
                        "&aLoaded &b$id &ajetpack".send(sender)
                    }
                    count++
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    e.printStackTrace()
                    "Failed To Load Jetpack at index $count".send(sender)
                }
                "&6$count &aJetpacks config has been loaded.".send(sender)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                var count = 1
                var errorLoc = File(dataFolder, "${jetpacks()}.error")
                while (!renameTo(errorLoc)) {
                    if (!renameTo(this)) break
                    count++
                    errorLoc = File(dataFolder, "${jetpacks()}$count.error")
                    delay(100)
                }
                if (parentFile.mkdirs() || parentFile.exists())
                    saveResource(jetpacks(), true)
                "&cInvalid Jetpacks.json!. Automatic restore with default config!".send(sender)
                "&cException: ${e.message}".send(sender)
                delay(500)
                loadJetpacks(sender)
            }
        }
    }

    /**
     * load Custom Fuels config
     */
    private suspend fun org.bukkit.plugin.Plugin.loadCustomFuels(sender: CommandSender) {
        File(dataFolder, customFuels()).apply {
            try {
                if (!exists())
                    if (parentFile.mkdirs() || parentFile.exists())
                        saveResource(customFuels(), false)
                customFuel.clear()
                val jsonArray = JSONArray(File(dataFolder, customFuels()).bufferedReader().use { it.readText() })
                var count = 0
                for (json in jsonArray) try {
                    yield()
                    gson.fromJson(json.toString(), CustomFuel::class.java).apply {
                        this@Module.customFuel[id] = this
                        "&aLoaded &b$id &acustom fuel".send(sender)
                    }
                    count++
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    e.printStackTrace()
                    "Failed To Load Custom Fuel at index $count".send(sender)
                }
                "&6$count &aCustomFuels config has been loaded.".send(sender)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                var count = 1
                var errorLoc = File(dataFolder, "${customFuels()}.error")
                while (!renameTo(errorLoc)) {
                    if (!renameTo(this)) break
                    count++
                    errorLoc = File(dataFolder, "${customFuels()}$count.error")
                    delay(100)
                }
                    if (parentFile.mkdirs() || parentFile.exists())
                        saveResource(customFuels(), true)
                "&cInvalid CustomFuels.json!. Automatic restore with default config!".send(sender)
                "&cException: ${e.message}".send(sender)
                delay(500)
                loadJetpacks(sender)
            }
        }
    }

}