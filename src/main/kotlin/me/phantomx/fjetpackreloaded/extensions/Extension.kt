package me.phantomx.fjetpackreloaded.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import me.phantomx.fjetpackreloaded.const.GlobalConst.CUSTOM_FUEL_PREFIX
import me.phantomx.fjetpackreloaded.const.GlobalConst.FJETPACK_PERMISSION_PREFIX
import me.phantomx.fjetpackreloaded.const.GlobalConst.ID_CUSTOM_FUEL
import me.phantomx.fjetpackreloaded.const.GlobalConst.ID_FUEL_JETPACK
import me.phantomx.fjetpackreloaded.const.GlobalConst.ID_JETPACK
import me.phantomx.fjetpackreloaded.const.GlobalConst.JETPACK_FUEL_PLACEHOLDER
import me.phantomx.fjetpackreloaded.const.GlobalConst.JETPACK_FUEL_VALUE_PLACEHOLDER
import me.phantomx.fjetpackreloaded.const.GlobalConst.STRING_EMPTY
import me.phantomx.fjetpackreloaded.data.CustomFuel
import me.phantomx.fjetpackreloaded.data.Jetpack
import me.phantomx.fjetpackreloaded.data.Messages
import me.phantomx.fjetpackreloaded.data.FJRPlayer
import me.phantomx.fjetpackreloaded.modules.Module.customFuel
import me.phantomx.fjetpackreloaded.modules.Module.dataFJRPlayer
import me.phantomx.fjetpackreloaded.modules.Module.gson
import me.phantomx.fjetpackreloaded.modules.Module.mainContext
import me.phantomx.fjetpackreloaded.modules.Module.messages
import me.phantomx.fjetpackreloaded.modules.Module.metaData
import me.phantomx.fjetpackreloaded.modules.Module.modifiedConfig
import me.phantomx.fjetpackreloaded.modules.Module.plugin
import me.phantomx.fjetpackreloaded.modules.Module.serverVersion
import me.phantomx.fjetpackreloaded.sealeds.OnDeath
import net.mamoe.yamlkt.Yaml
import org.bukkit.*
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.net.URL
import java.util.*
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Run safe [block] and cancallable block coroutines
 * @param exceptionMessage {#et} for stack trace, {#em} for exception message
 */
inline fun <T> T.withSafe(noLogs: Boolean = true, exceptionMessage: String = STRING_EMPTY, block: T.() -> Unit) =
    safeRun(noLogs, exceptionMessage) {
        block()
    } ?: Unit

/**
 * Run safe [block] and cancallable block coroutines
 * @param exceptionMessage {#et} for stack trace, {#em} for exception message
 * @return null if fail, [R] otherwise
 */
inline fun <T, R> T.safeRun(noLogs: Boolean = true, exceptionMessage: String = STRING_EMPTY, block: T.() -> R): R? =
    try {
        block()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (exceptionMessage.isNotEmpty())
            Bukkit.getServer().logger.warning(
                exceptionMessage
                    .replace("{#et}", e.stackTraceToString())
                    .replace("{#em}", e.message ?: STRING_EMPTY)
            )
        if (!noLogs)
            Bukkit.getServer().logger.warning(e.stackTraceToString())
        null
    }

/**
 * @return if failed to parse will return default value or 0
 */
fun String?.toIntSafe(defaultValue: Int = 0): Int = safeRun {
    this?.replace("\\D+".toRegex(), STRING_EMPTY)?.toInt() ?: defaultValue
} ?: defaultValue

/**
 * @return if failed to parse will return zero
 */
fun String?.toLongSafe(): Long = safeRun {
    this?.replace("\\D+".toRegex(), STRING_EMPTY)?.toLong() ?: 0
} ?: 0

/**
 * translate Color Codes & to color
 */
fun String.translateCodes(): String = ChatColor.translateAlternateColorCodes('&', this)

/**
 * send message to target console/player with translated color codes
 * default [target] is console
 */
fun String.send(target: CommandSender = Bukkit.getServer().consoleSender, noPrefix: Boolean = false) {
    if (length < 1) return
    target.sendMessage(
        (when (noPrefix) {
            true -> STRING_EMPTY
            false -> messages.prefix + " "
        } + this).translateAllCodes()
    )
}

/**
 * Translate all color codes (Hex and &)
 */
fun String.translateAllCodes() = translateHex().translateCodes()

/**
 * send message in Coroutine Default Thread to target console/player with translated color codes
 */
suspend fun String.sendDefault(target: CommandSender) = withContext(mainContext) { this@sendDefault.send(target) }

/**
 * run on Main thread without blocking current thread
 */
inline fun <T> T.mainThread(crossinline block: T.() -> Unit): T {
    object : BukkitRunnable() {
        override fun run() {
            block()
        }
    }.runTask(plugin)
    return this
}

/**
 * run sync [block] on main Thread
 */
suspend inline fun <T, R> T.withContextMain(crossinline block: T.() -> R): R {
    return suspendCancellableCoroutine {
        val task = object : BukkitRunnable() {
            override fun run() {
                try {
                    val r = block()
                    if (it.isCancelled) return
                    it.resume(r)
                } catch (e: CancellationException) {
                    it.resumeWithException(e)
                }
            }
        }.runTask(plugin)
        it.invokeOnCancellation {
            task.cancel()
        }
    }
}

/**
 * Translate color codes Hex
 */
fun String.translateHex(): String = safeRun {
    var message = this
    if (serverVersion >= 16) {
        val pattern = Pattern.compile("#[a-fA-F0-9]{6}")
        var matcher = pattern.matcher(message)
        val builder = StringBuilder()
        while (matcher.find()) {
            builder.setLength(0)
            val hexCode = message.substring(matcher.start(), matcher.end())
            val ch = hexCode.replace('#', 'x').toCharArray()
            for (c in ch)
                builder.append("&").append(c)
            message = message.replace(hexCode, builder.toString())
            matcher = pattern.matcher(message)
        }
    }
    message
} ?: this

/**
 * check update plugin, send information to target
 */
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun CommandSender.checkUpdatePlugin(loginEvent: Boolean) {
    if ((!modifiedConfig.updateNotification && loginEvent) || (!hasPermission("${FJETPACK_PERMISSION_PREFIX}update") && !isAdminOrOp())) return
    withContext(IO) {
        safeRun(exceptionMessage = "Can't look for updates: {#em}") {
            URL("https://api.spigotmc.org/legacy/update.php?resource=100816").openStream()
                .use { inputStream ->
                    Scanner(inputStream).use { scanner ->
                        if (scanner.hasNext()) {
                            val sVersion = scanner.next()
                            val spigotVersion = sVersion.toIntSafe(1)
                            val currentVersion = plugin.description.version.toIntSafe(1)

                            yield()
                            when {
                                currentVersion == spigotVersion -> "&aThere is not a new update available. You are using the latest version"
                                currentVersion >= spigotVersion -> "&aThere is not a new update available. You are using the latest dev build version"
                                else -> {
                                    "&bThere is a new update available! v$sVersion".sendDefault(this@checkUpdatePlugin)
                                    "&ehttps://www.spigotmc.org/resources/fjetpackreloaded.100816/"
                                }
                            }.sendDefault(this@checkUpdatePlugin)
                        }
                    }
                }
        }
    }
}

fun config() = "config.yml"

fun messages() = "configs/Messages.json"

fun jetpacks() = "configs/Jetpacks.json"

fun customFuels() = "configs/CustomFuels.json"

/**
 * Save/Update all default configs
 */
fun Plugin.saveAllDefaultConfig() = modifiedConfig.withSafe(false) {
    saveResource(config(), true)

    saveDefaultMessagesConfig()
    saveDefaultJetpacksConfig()
    saveDefaultCustomFuelsConfig()

    reloadConfig()
}

/**
 * for safety mkdir folder configs first
 */
fun Plugin.mkdirConfigsFolder() {
    File(dataFolder, "configs").apply {
        mkdirs()
        mkdir()
    }
}

/**
 * Save default jetpacks config
 */
fun Plugin.saveDefaultJetpacksConfig() {
    mkdirConfigsFolder()

    val configsFolder = dataFolder.absolutePath + File.separator + "configs"
    val defaultJetpacks: List<Jetpack> = listOf(
        Jetpack(
            id = "VIP",
            displayName = "&8&l[&bFJetpack&6&lReloaded&8&l]",
            lore = mutableListOf(
                "&3&m&l----===[&r &8 &b&lINFO &8 &3&m&l]===----",
                "&9Rank: &6&lVIP",
                "",
                "&3&m&l----===[&b &lUSAGE &3&m&l]===----",
                "&9Sneak to toggle on/off",
                "&9Double jump to fly",
                "",
                "&7Fuel: &a{#fuel_value} &b{#fuel}"
            ),
            jetpackItem = "CHAINMAIL_CHESTPLATE",
            unbreakable = true,
            fuel = "${CUSTOM_FUEL_PREFIX}CVIP",
            fuelCostFlySprint = 0,
            burnRate = 5,
            speed = "1.1",
            particleEffect = "CLOUD"
        ),
        Jetpack(blockedWorlds = mutableListOf("world1"))
    )
    File(configsFolder, "Jetpacks." + if (modifiedConfig.configsYaml) "yml" else "json").writeText(
        if (modifiedConfig.configsYaml)
            StringBuilder().run {
                defaultJetpacks.forEach {
                    append(it.toYaml())
                    append("\n")
                }
                toString()
            }
        else
            gson.toJson(defaultJetpacks)
    )
}

/**
 * Save default custom fuels config
 */
fun Plugin.saveDefaultCustomFuelsConfig() {
    mkdirConfigsFolder()

    val configsFolder = dataFolder.absolutePath + File.separator + "configs"
    File(configsFolder, "CustomFuels." + if (modifiedConfig.configsYaml) "yml" else "json").writeText(
        if (modifiedConfig.configsYaml)
            CustomFuel().toYaml()
        else
            gson.toJson(listOf(CustomFuel()))
    )
}

/**
 * Save default messages config
 */
fun Plugin.saveDefaultMessagesConfig() {
    mkdirConfigsFolder()

    val configsFolder = dataFolder.absolutePath + File.separator + "configs"
    File(configsFolder, "Messages." + if (modifiedConfig.configsYaml) "yml" else "json").writeText(
        if (modifiedConfig.configsYaml)
            Yaml.encodeToString(Messages.serializer(), Messages().safeStringsClassYaml()).escapeSafeStringYaml()
        else
            gson.toJson(Messages())
    )
}

/**
 * don't forget call this function after object converted to yaml
 */
fun String.escapeSafeStringYaml(): String = replace("{~\$~}", STRING_EMPTY)

/**
 * check is string safe to convert as yaml string
 */
fun String.safeFieldStringYaml(): String {
    trim().apply {
        if ((startsWith("\"") && endsWith("\"")) ||
            startsWith("'") && endsWith("'")
        )
            return this
    }
    return "{~\$~}$this"
}

/**
 * Convert custom fuel class to yaml config
 */
fun CustomFuel.toYaml(): String = safeRun(false) {
    val clone = copy()
    clone.safeStringsClassYaml()
    val yaml = StringBuilder("${clone.id.trim()}:\n")
    val indent = "  "
    val cf = Yaml.encodeToString(CustomFuel.serializer(), clone).escapeSafeStringYaml()
    cf.split("\n").forEach {
        yaml.append(indent).append(it).append("\n")
    }
    yaml.toString()
} ?: STRING_EMPTY

/**
 * Convert jetpack object to yaml config
 */
fun Jetpack.toYaml(): String = safeRun(false) {
    val clone = copy()
    clone.safeStringsClassYaml()
    val yaml = StringBuilder("${clone.id.trim()}:\n")
    val indent = "  "
    val jp = Yaml.encodeToString(Jetpack.serializer(), clone).escapeSafeStringYaml()
    jp.split("\n").forEach {
        yaml.append(indent).append(it).append("\n")
    }
    yaml.toString()
} ?: STRING_EMPTY

/**
 * Convert player as PlayerFlying
 */
fun Player.asFJRPlayer(): FJRPlayer = let {
    dataFJRPlayer[uniqueId] ?: FJRPlayer(this).run {
        dataFJRPlayer[uniqueId] = this
        this
    }
}

/**
 * turn Off jetpack player
 * require run in Main Thread!
 */
fun Player.turnOff(jetpack: Jetpack? = null, noMessage: Boolean = false) {
    if (!noMessage)
        jetpack?.let {
            if (isDead)
                when (it.onDied) {
                    is OnDeath.Nothing -> if (!(this as LivingEntity).isOnGround || isFlying)
                        messages.detached
                    else
                        messages.turnOff
                    is OnDeath.Drop -> messages.onDeathDropped
                    is OnDeath.Remove -> messages.onDeathRemoved
                }.send(this)
            else
                (if (!(this as LivingEntity).isOnGround || isFlying)
                    messages.detached
                else
                    messages.turnOff).send(this)
        } ?: "&cThis plugin has been unloaded!".send(this)
    asFJRPlayer().stop()
}

/**
 * set item metadata String
 */
fun ItemStack.set(key: String, value: String?) = metaData.setStringSafe(this, key = key, value = value)

/**
 * get item metadata String
 */
fun ItemStack.get(key: String): String = metaData.getStringSafe(this, key = key)

/**
 * Check if the item stack is not armor
 */
fun ItemStack.isNotTypeArmor(): Boolean = metaData.isNotItemArmor(this)

/**
 * update item
 * @return true if succress, false otherwise
 */
fun ItemStack.update(fuel: String, jetpack: Jetpack): ItemStack {
    var fuelItem = jetpack.fuel
    if (fuelItem.startsWith(CUSTOM_FUEL_PREFIX)) {
        fuelItem = fuelItem.substring(1)
        customFuel[fuelItem]?.apply {
            fuelItem = customDisplay.ifEmpty { displayName }.translateAllCodes()
        } ?: return this
    } else
        fuelItem = fuelItem.replace("_", " ")
    if (!hasItemMeta()) return this
    itemMeta = itemMeta?.apply {
        lore = jetpack.lore.toMutableList().apply {
            replaceAll {
                it.replace(JETPACK_FUEL_PLACEHOLDER, fuelItem).replace(JETPACK_FUEL_VALUE_PLACEHOLDER, fuel)
                    .translateAllCodes()
            }
        }
    }
    return set(ID_FUEL_JETPACK, fuel)
}

/**
 * Update armor items
 */
fun MutableList<ItemStack>.update(jetpackItem: ItemStack): MutableList<ItemStack> {
    var index = -1
    var done = false
    val it = iterator()
    while (it.hasNext() && !done) {
        index++
        it.next().withSafe {
            if (get(ID_JETPACK) != jetpackItem.get(ID_JETPACK) || type != jetpackItem.type)
                return@withSafe
            this@update[index] = jetpackItem
            done = true
        }
    }
    return this
}

/**
 * set Jetpack Item from target
 */
@Suppress("Deprecation")
fun CommandSender.setJetpack(item: ItemStack, jetpack: Jetpack, fuelValue: Long): ItemStack {
    var fuel = jetpack.fuel
    if (fuel.startsWith(CUSTOM_FUEL_PREFIX)) {
        customFuel[fuel.substring(1)]?.apply {
            fuel = customDisplay.ifEmpty { displayName }
        } ?: run {
            fuel += " &cInvalid"
            "&cNo Custom Fuel with ID: &l${jetpack.fuel.substring(1)}".send(this)
        }
    } else
        fuel = fuel.replace("_", " ")
    val itemStack = item.run {
        itemMeta = itemMeta?.apply {
            setDisplayName(jetpack.displayName.translateAllCodes())
            lore = jetpack.lore.toMutableList().apply {
                replaceAll {
                    it.replace(JETPACK_FUEL_PLACEHOLDER, fuel).replace(JETPACK_FUEL_VALUE_PLACEHOLDER, fuelValue.toString())
                        .translateAllCodes()
                }
            }

            if (jetpack.flags.isNotEmpty())
                for (flag in jetpack.flags)
                    safeRun {
                        addItemFlags(ItemFlag.valueOf(flag.uppercase().trim()))
                    } ?: "&cInvalid flag $flag".send(this@setJetpack)
            if (serverVersion > 16)
                isUnbreakable = jetpack.unbreakable
        } ?: run {
            "&cInvalid Item!".send(this@setJetpack)
            null
        }
        set(ID_JETPACK, jetpack.id).set(ID_FUEL_JETPACK, fuelValue.toString())
    }

    if (jetpack.enchantments.isNotEmpty())
        for (enchant in jetpack.enchantments)
            safeRun {
                val enchantname = enchant.split(":").toTypedArray()[0]
                val enchantlvl: Int = enchant.split(":").toTypedArray()[1].toIntSafe(1)
                val enchantment =
                    (if (serverVersion > 16)
                        Enchantment.getByKey(NamespacedKey.minecraft(enchantname.lowercase()))
                    else
                        Enchantment.getByName(enchantname.uppercase())) ?: return@safeRun
                itemStack.addUnsafeEnchantment(enchantment, enchantlvl)
            } ?: "&cInvalid enchant $enchant".send(this)

    return itemStack
}

/**
 * give target Jetpack item
 */
fun CommandSender.giveJetpack(p: Player, jetpack: Jetpack, fuelValue: Long) = withSafe {
    var jI: String = jetpack.jetpackItem.uppercase()
    if (jI.contains(":")) jI = jI.split(":").toTypedArray()[0]
    var item = ItemStack(Material.valueOf(jI))
    item.itemMeta?.let { im ->
        if (jetpack.customModelData != -1)
            im.setCustomModelData(jetpack.customModelData)
        if (im is LeatherArmorMeta && jetpack.jetpackItem.contains(":")) {
            val c: List<String> = jetpack.jetpackItem.split(":")[1].split(".")
            im.setColor(Color.fromRGB(c[0].toIntSafe(), c[1].toIntSafe(), c[2].toIntSafe()))
        }
        item.itemMeta = im
        item = setJetpack(item, jetpack, fuelValue)
        p.inventory.addItem(item).run {
            if (isNotEmpty()) {
                forEach { (_, i) ->
                    p.mainThread {
                        world.dropItemNaturally(location, i)
                    }
                }
                "&cInventory is full!, dropped the item".send(p)
            }
        }
        p.updateInventory()
        if (this == p) {
            "&aGive item &6${jetpack.id} &aJetpack with fuel &6&lx${fuelValue}&r &ato your self success!".send(this)
            return
        }
        "&aGive item &6${jetpack.id} &aJetpack with fuel &6&lx${fuelValue}&r &ato player &e${p.displayName}&a success!".send(
            this
        )
        "&aYou have been given a &6${jetpack.id}&a Jetpack with fuel &6&lx${fuelValue}&r &afrom &e${name}".send(p)
    } ?: "&cGive item &6${jetpack.id}&c Jetpack Failed!".send(this)
}


/**
 * get Item In Hand from player
 */
@Suppress("Deprecation")
fun Player.getItemInHandByCheckingServerVersion(): ItemStack? {
    val item = if (serverVersion > 8)
        inventory.itemInMainHand
    else
        itemInHand
    val im = item.itemMeta
    if (item.type == Material.AIR || im == null) {
        "&cYou not holding any item in hand.".send(this)
        return null
    }
    if (item.isNotTypeArmor()) {
        "&cThis item is not armor item!".send(this)
        return null
    }
    return item
}

/**
 * Give player custom fuel item
 */
fun CommandSender.giveCustomFuel(target: Player, customFuel: CustomFuel, amount: Int) {
    val material: Material = try {
        Material.valueOf(customFuel.item.uppercase().trim())
    } catch (ex: IllegalArgumentException) {
        "&cFailed to get Custom Fuel &6${customFuel.id} &cInvalid item material name: &l${customFuel.item}".send(this)
        return
    }
    var item = ItemStack(material)
    if (customFuel.glowing) item = item.set("ench", null)

    item.itemMeta = item.itemMeta?.apply {
        setDisplayName(customFuel.displayName.translateAllCodes())
        val newLore = customFuel.lore.toMutableList()
        newLore.replaceAll { it.translateAllCodes() }
        lore = newLore
        if (customFuel.glowing) {
            addEnchant(Enchantment.LUCK, 1, false)
            addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
    } ?: run {
        "&cFailed to get Custom Fuel &6${customFuel.id}".send(this)
        return
    }

    item = item.set(ID_CUSTOM_FUEL, CUSTOM_FUEL_PREFIX + customFuel.id)
    item.amount = amount

    target.inventory.addItem(item).run {
        if (isNotEmpty()) {
            forEach { (_, i) ->
                target.mainThread {
                    world.dropItemNaturally(location, i)
                }
            }
            "&cInventory is full!, dropped the item".send(target)
        }
    }
    target.updateInventory()
    if (this == target) {
        "&aGive item &6${customFuel.id} &aitem &6x${amount}&r &ato your self success!".send(this)
        return
    }
    "&aGive item &6${customFuel.id} &aitem &6x${amount}&r &ato player &e${target.displayName}&r &asuccess!".send(this)
    "&aYou have been given a &6${customFuel.id} &aitem from &6${name}".send(target)
}

/**
 * Check is plugin enabled or not
 */
fun Server.isPluginActive(plugin: String) = pluginManager.isPluginEnabled(plugin)

/**
 * Check sender is admin or op
 */
fun CommandSender.isAdminOrOp() = hasPermission("$FJETPACK_PERMISSION_PREFIX*") || isOp
