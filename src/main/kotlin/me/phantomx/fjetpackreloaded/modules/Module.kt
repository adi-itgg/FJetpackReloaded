package me.phantomx.fjetpackreloaded.modules

import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import me.phantomx.fjetpackreloaded.FJetpackReloaded
import me.phantomx.fjetpackreloaded.const.GlobalConst.CONFIG_CUSTOM_FUELS_LOCATION
import me.phantomx.fjetpackreloaded.const.GlobalConst.CONFIG_JETPACKS_LOCATION
import me.phantomx.fjetpackreloaded.const.GlobalConst.CONFIG_MESSAGES_LOCATION
import me.phantomx.fjetpackreloaded.const.GlobalConst.STRING_EMPTY
import me.phantomx.fjetpackreloaded.data.Config
import me.phantomx.fjetpackreloaded.data.CustomFuel
import me.phantomx.fjetpackreloaded.data.Jetpack
import me.phantomx.fjetpackreloaded.data.Messages
import me.phantomx.fjetpackreloaded.data.hook.SuperiorSkyblock2Config
import me.phantomx.fjetpackreloaded.data.hook.SuperiorSkyblock2Player
import me.phantomx.fjetpackreloaded.extensions.*
import me.phantomx.fjetpackreloaded.fields.HookPlugin.SuperiorSkyblock2ConfigFile
import me.phantomx.fjetpackreloaded.fields.HookPlugin.SuperiorSkyblock2Name
import me.phantomx.fjetpackreloaded.fields.HookPlugin.superiorPlayersData
import me.phantomx.fjetpackreloaded.fields.HookPlugin.superiorSkyblock2ConfigLoaded
import me.phantomx.fjetpackreloaded.fields.Plugin
import me.phantomx.fjetpackreloaded.nms.ItemMetaData
import me.phantomx.fjetpackreloaded.sealeds.OnDeath
import me.phantomx.fjetpackreloaded.sealeds.OnEmptyFuel
import net.mamoe.yamlkt.Yaml
import org.bukkit.command.CommandSender
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
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
    suspend fun FJetpackReloaded.load(sender: CommandSender): Boolean {
        databaseDirectory = File(plugin.dataFolder, "database")

        val r = sender.safeRun(false) {
            reloadConfig()
            modifiedConfig = Config(
                config["version", 1].toString().toIntSafe(1),
                config["updateNotification", true].toString().toBoolean(),
                config["configsYaml", true].toString().toBoolean()
            )
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
                    sender.withSafe {
                        val m = "WARNING!: %s has been updated!"
                        "&eWARNING!: Config doesn't support!".send(this)
                        File(dataFolder, "configs-v${modifiedConfig.version}-backup").let { backup ->
                            if (backup.mkdirs() && exists() && renameTo(File(backup, config())))
                                String.format(m, config()).send(this)
                            val msg = File(dataFolder, messages())
                            if (msg.exists() && msg.renameTo(File(backup, messages())))
                                String.format(m, messages()).send(this)
                            val jps = File(dataFolder, jetpacks())
                            if (jps.exists() && jps.renameTo(File(backup, jetpacks())))
                                String.format(m, jetpacks()).send(this)
                        }
                    }
                    saveAllDefaultConfig()
                }
            }

            loadMessages(this)

            loadJetpacks(this)

            loadCustomFuels(this)
            true
        } ?: sender.run {
            "&cLoad configs failed!".send(this)
            false
        }
        if (server.isPluginActive(SuperiorSkyblock2Name))
            getDatabase(SuperiorSkyblock2Name).let {
                sender.safeRun(false) {
                    var ss2Config = SuperiorSkyblock2ConfigFile
                    if (!modifiedConfig.configsYaml) ss2Config = ss2Config.replace(".yml", ".json")
                    File(dataFolder, ss2Config).apply {
                        if (!exists() || length() < 1) {
                            withSafe {
                                parentFile.mkdirs()
                                parentFile.mkdir()
                            }
                            writeText(
                                if (modifiedConfig.configsYaml)
                                    Yaml.encodeToString(SuperiorSkyblock2Config.serializer(), SuperiorSkyblock2Config())
                                else
                                    gson.toJson(SuperiorSkyblock2Config())
                            )
                        }
                        superiorSkyblock2ConfigLoaded = if (modifiedConfig.configsYaml)
                            Yaml.decodeFromString(SuperiorSkyblock2Config.serializer(), readText())
                        else
                            gson.fromJson(readText(), SuperiorSkyblock2Config::class.java)
                    }
                    "&aLoaded config &b$SuperiorSkyblock2Name".send(this)

                    val data = gson.fromJson(it, Array<SuperiorSkyblock2Player>::class.java)?.toMutableList() ?: mutableListOf()
                    superiorPlayersData.clear()
                    data.forEach {
                        superiorPlayersData[it.uuid] = it
                    }

                    "&aLoaded database &b${SuperiorSkyblock2Name}".send(this)
                    "&aHooked to &b$SuperiorSkyblock2Name".send(this)
                } ?: "&cFailed to load config &b$SuperiorSkyblock2Name".send(sender)
            }
        return r
    }

    /**
     * load messages config
     */
    private suspend fun org.bukkit.plugin.Plugin.loadMessages(sender: CommandSender) {
        val messagesFile = if (modifiedConfig.configsYaml) CONFIG_MESSAGES_LOCATION else messages()
        File(dataFolder, messagesFile).apply {
            sender.safeRun(false) {
                if (!exists() || !parentFile.exists())
                    saveDefaultMessagesConfig()


                val conf = bufferedReader().use { it.readText() }
                messages = if (modifiedConfig.configsYaml)
                    Yaml.decodeFromString(Messages.serializer(), conf)
                else
                    gson.fromJson(conf, Messages::class.java)

                "&aMessages config has been loaded.".send(this)
            } ?: sender.run {
                handleInvalidConfig(this@apply, messagesFile)
                saveDefaultMessagesConfig()
                "&cInvalid Messages config!. Automatic restore with default config!".send(this)
                delay(500)
                loadMessages(this)
            }
        }
    }

    /**
     * load Jetpacks config
     */
    private suspend fun org.bukkit.plugin.Plugin.loadJetpacks(sender: CommandSender) {
        val jpFile = if (modifiedConfig.configsYaml) CONFIG_JETPACKS_LOCATION else jetpacks()
        File(dataFolder, jpFile).apply file@{
            sender.safeRun(false) {
                if (!exists() || !parentFile.exists())
                    saveDefaultJetpacksConfig()

                jetpacks.clear()

                if (modifiedConfig.configsYaml) {
                    YamlConfiguration.loadConfiguration(this@file).let { yml ->
                        yml.getConfigurationSection(STRING_EMPTY)?.getKeys(false)?.forEach { jpid ->
                            val jp = Jetpack(id = jpid)


                            yml.getConfigurationSection(jpid)?.getString("displayName")?.let {
                                jp.displayName = it
                            }

                            yml.getConfigurationSection(jpid)?.getStringList("lore")?.let {
                                jp.lore = it
                            }
                            yml.getConfigurationSection(jpid)?.getString("permission")?.let {
                                jp.permission = it.replace("#id", jpid)
                            }
                            yml.getConfigurationSection(jpid)?.getBoolean("canBypassFuel")?.let {
                                jp.canBypassFuel = it
                            }
                            yml.getConfigurationSection(jpid)?.getBoolean("canBypassSprintFuel")?.let {
                                jp.canBypassSprintFuel = it
                            }
                            yml.getConfigurationSection(jpid)?.getString("jetpackItem")?.let {
                                jp.jetpackItem = it
                            }
                            yml.getConfigurationSection(jpid)?.getBoolean("unbreakable")?.let {
                                jp.unbreakable = it
                            }
                            yml.getConfigurationSection(jpid)?.getString("onEmptyFuel")?.let {
                                jp.onEmptyFuel = it
                            }
                            yml.getConfigurationSection(jpid)?.getString("onDeath")?.let {
                                jp.onDeath = it
                            }
                            yml.getConfigurationSection(jpid)?.getBoolean("onlyAllowInsideOwnGriefPreventionClaim")?.let {
                                jp.onlyAllowInsideOwnGriefPreventionClaim = it
                            }
                            yml.getConfigurationSection(jpid)?.getBoolean("onlyAllowInsideAllGriefPreventionClaim")?.let {
                                jp.onlyAllowInsideAllGriefPreventionClaim = it
                            }
                            yml.getConfigurationSection(jpid)?.getInt("customModelData")?.let {
                                jp.customModelData = it
                            }
                            yml.getConfigurationSection(jpid)?.getString("fuel")?.let {
                                jp.fuel = it
                            }
                            yml.getConfigurationSection(jpid)?.getInt("fuelCost")?.let {
                                jp.fuelCost = it
                            }
                            yml.getConfigurationSection(jpid)?.getInt("fuelCostFlySprint")?.let {
                                jp.fuelCostFlySprint = it
                            }
                            yml.getConfigurationSection(jpid)?.getInt("burnRate")?.let {
                                jp.burnRate = it
                            }
                            yml.getConfigurationSection(jpid)?.getString("speed")?.let {
                                jp.speed = it
                            }
                            yml.getConfigurationSection(jpid)?.getString("particleEffect")?.let {
                                jp.particleEffect = it
                            }
                            yml.getConfigurationSection(jpid)?.getInt("particleAmount")?.let {
                                jp.particleAmount = it
                            }
                            yml.getConfigurationSection(jpid)?.getLong("particleDelay")?.let {
                                jp.particleDelay = it
                            }
                            yml.getConfigurationSection(jpid)?.getStringList("flags")?.let {
                                jp.flags = it
                            }
                            yml.getConfigurationSection(jpid)?.getStringList("enchantments")?.let {
                                jp.enchantments = it
                            }
                            yml.getConfigurationSection(jpid)?.getStringList("blockedWorlds")?.let {
                                jp.blockedWorlds = it
                            }

                            jetpacks[jp.id] = jp.initJetpackSealed()
                            "&aLoaded Jetpack: &6&b${jp.id}".send(this)
                        }
                    }
                    "&6${jetpacks.size} &aJetpacks config has been loaded.".send(this)
                    return
                }

                val jsonArray = JSONArray(bufferedReader().use { it.readText() })
                var count = 0
                for (json in jsonArray)
                    safeRun {
                        yield()
                        gson.fromJson(json.toString(), Jetpack::class.java).apply {
                            initJetpackSealed()
                            permission = permission.replace("#id", id).lowercase()
                            jetpacks[id] = this
                        }
                        "&aLoaded Jetpack: &6&b$id".send(this)
                        count++
                    } ?: "Failed To Load Jetpack at index $count".send(this)

                "&6$count &aJetpacks config has been loaded.".send(this)
            } ?: sender.run {
                handleInvalidConfig(this@file, jpFile)
                saveDefaultJetpacksConfig()
                "&cInvalid Jetpacks config!. Automatic restore with default config!".send(this)
                delay(500)
                loadJetpacks(this)
            }
        }
    }

    /**
     * load Custom Fuels config
     */
    private suspend fun org.bukkit.plugin.Plugin.loadCustomFuels(sender: CommandSender) {
        val customFuelFile = if (modifiedConfig.configsYaml) CONFIG_CUSTOM_FUELS_LOCATION else customFuels()
        File(dataFolder, customFuelFile).apply file@{
            sender.safeRun(false) {
                if (!exists() || !parentFile.exists())
                    saveDefaultCustomFuelsConfig()

                customFuel.clear()

                if (modifiedConfig.configsYaml) {
                    YamlConfiguration.loadConfiguration(this@file).let { yml ->
                        yml.getConfigurationSection(STRING_EMPTY)?.getKeys(false)?.forEach { cfid ->
                            val cf = CustomFuel(id = cfid)

                            yml.getConfigurationSection(cfid)?.apply {
                                getString("customDisplay")?.let {
                                    cf.customDisplay = it
                                }
                                getString("displayName")?.let {
                                    cf.displayName = it
                                }
                                cf.lore = getStringList("lore")
                                getString("item")?.let {
                                    cf.item = it
                                }
                                getString("permission")?.let {
                                    cf.permission = it.replace("#id", cfid)
                                }
                                cf.glowing = getBoolean("glowing")
                            }

                            customFuel[cf.id] = cf
                            "&aLoaded CustomFuel: &6${cf.id}".send(this)
                        } ?: throw InvalidConfigurationException()
                    }
                    "&6${customFuel.size} &aCustomFuels config has been loaded.".send(this)
                    return
                }

                val jsonArray = JSONArray(bufferedReader().use { it.readText() })
                var count = 0
                for (json in jsonArray)
                    safeRun {
                        yield()
                        gson.fromJson(json.toString(), CustomFuel::class.java).apply {
                            permission = permission.replace("#id", id).lowercase()
                            this@Module.customFuel[id] = this
                        }
                        "&aLoaded Custom Fuel: &6$id".send(this)
                        count++
                    } ?: "Failed To Load Custom Fuel at index $count".send(this)
                "&6$count &aCustomFuels config has been loaded.".send(this)
            } ?: sender.run {
                handleInvalidConfig(this@file, customFuelFile)
                saveDefaultCustomFuelsConfig()
                "&cInvalid CustomFuels config!. Automatic restore with default config!".send(this)
                delay(500)
                loadCustomFuels(this)
            }
        }
    }

    /**
     * If config is invalid will rename with suffix .error
     */
    private suspend fun org.bukkit.plugin.Plugin.handleInvalidConfig(file: File, path: String) =
        file.withSafe {
            var count = 1
            var errorLoc = File(dataFolder, "${path}.error")
            while (!renameTo(errorLoc)) {
                if (!renameTo(this)) break
                count++
                errorLoc = File(dataFolder, "${path}$count.error")
                delay(100)
            }
        }


    private fun Jetpack.initJetpackSealed(): Jetpack {
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
        return this
    }

}