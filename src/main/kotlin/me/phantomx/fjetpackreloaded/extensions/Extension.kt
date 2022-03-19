package me.phantomx.fjetpackreloaded.extensions

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import me.phantomx.fjetpackreloaded.data.CustomFuel
import me.phantomx.fjetpackreloaded.data.Jetpack
import me.phantomx.fjetpackreloaded.data.PlayerFlying
import me.phantomx.fjetpackreloaded.modules.Module
import me.phantomx.fjetpackreloaded.modules.Module.customFuel
import me.phantomx.fjetpackreloaded.modules.Module.dataPlayer
import me.phantomx.fjetpackreloaded.modules.Module.fuelIdJetpack
import me.phantomx.fjetpackreloaded.modules.Module.idCustomFuel
import me.phantomx.fjetpackreloaded.modules.Module.idJetpack
import me.phantomx.fjetpackreloaded.modules.Module.jetpackFuelPlaceholder
import me.phantomx.fjetpackreloaded.modules.Module.jetpackFuelValuePlaceholder
import me.phantomx.fjetpackreloaded.modules.Module.mainContext
import me.phantomx.fjetpackreloaded.modules.Module.messages
import me.phantomx.fjetpackreloaded.modules.Module.metaData
import me.phantomx.fjetpackreloaded.modules.Module.permission
import me.phantomx.fjetpackreloaded.modules.Module.plugin
import me.phantomx.fjetpackreloaded.modules.Module.serverVersion
import me.phantomx.fjetpackreloaded.modules.Module.stringEmpty
import me.phantomx.fjetpackreloaded.sealeds.OnDeath
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.plugin.Plugin
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.regex.Pattern

/**
 * @return if failed to parse will return default value
 */
fun String?.toIntSafe(defaultValue: Int): Int = try {
    this?.replace("\\D+".toRegex(), stringEmpty)?.toInt() ?: defaultValue
} catch (e: NumberFormatException) {
    defaultValue
}

/**
 * @return if failed to parse will return zero
 */
fun String?.toIntSafe(): Int = toIntSafe(0)

/**
 * @return if failed to parse will return zero
 */
fun String?.toLongSafe(): Long = try {
    this?.replace("\\D+".toRegex(), stringEmpty)?.toLong() ?: 0
} catch (e: Exception) {
    if (e is CancellationException) throw e
    0
}

/**
 * translate Color Codes & to color
 */
fun String.translateCodes(): String = ChatColor.translateAlternateColorCodes('&', this)

/**
 * send message to target console/player with translated color codes
 */
fun String.send(target: CommandSender, noPrefix: Boolean) {
    if (length < 1) return
    target.sendMessage(
        (when (noPrefix) {
            true -> stringEmpty
            false -> messages.prefix + " "
        } + this).translateAllCodes()
    )
}

/**
 * Translate all color codes (Hex and &)
 */
fun String.translateAllCodes() = translateHex().translateCodes()

/**
 * send message to target console/player with translated color codes
 */
fun String.send(target: CommandSender) = send(target, false)


/**
 * send message in Coroutine Default Thread to target console/player with translated color codes
 */
suspend fun String.sendDefault(target: CommandSender) = withContext(mainContext) { this@sendDefault.send(target) }

/**
 * run on main Thread
 */
inline fun <T> T.main(crossinline block: T.(T) -> Unit): T {
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin) {
        block(this)
    }
    return this
}

/**
 * Translate color codes Hex
 */
fun String.translateHex(): String = try {
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
} catch (e: Exception) {
    if (e is CancellationException) throw e
    e.printStackTrace()
    this
}

/**
 * check update plugin, send information to target
 */
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun CommandSender.checkUpdatePlugin() {
    if (!hasPermission("${permission}update")) return
    withContext(IO) {
        try {
            URL("https://api.spigotmc.org/legacy/update.php?resource=78318").openStream()
                .use { inputStream ->
                    Scanner(inputStream).use { scanner ->
                        if (scanner.hasNext()) {
                            val sVersion = scanner.next()
                            val spigotVersion = sVersion.toIntSafe(1)
                            val currentVersion = plugin.description.version.toIntSafe(1)

                            yield()
                            when {
                                currentVersion == spigotVersion -> "${Module.defaultPrefix} &aThere is not a new update available. You are using the latest version"
                                currentVersion >= spigotVersion -> "${Module.defaultPrefix} &aThere is not a new update available. You are using the latest dev build version"
                                else -> {
                                    "&bThere is a new update available! v$sVersion".sendDefault(this@checkUpdatePlugin)
                                    "&ehttps://www.spigotmc.org/resources/fjetpack.78318/"
                                }
                            }.sendDefault(this@checkUpdatePlugin)
                        }
                    }
                }
        } catch (e: IOException) {
            yield()
            server.logger.info("Cannot look for updates: ${e.message}")
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
fun Plugin.saveAllDefaultConfig() {
    saveResource(config(), true)
    saveResource(messages(), true)
    saveResource(jetpacks(), true)
    reloadConfig()
}

/**
 * Convert player as PlayerFlying
 */
fun Player.asPlayerFlying(): PlayerFlying = let {
    dataPlayer[uniqueId] ?: PlayerFlying(this).run {
        dataPlayer[uniqueId] = this
        this
    }
}

/**
 * turn Off jetpack player
 * require run in Main Thread!
 */
fun Player.turnOff(jetpack: Jetpack?) {
    jetpack?.let {
        if (isDead)
            when (it.onDied) {
                OnDeath.Nothing -> if (!(this as LivingEntity).isOnGround || isFlying)
                    messages.detached
                else
                    messages.turnOff
                OnDeath.Drop -> messages.onDeathDropped
                OnDeath.Remove -> messages.onDeathRemoved
            }.send(this)
        else
            (if (!(this as LivingEntity).isOnGround || isFlying)
                messages.detached
            else
                messages.turnOff).send(this)
    } ?: "&cThis plugin has been unloaded!".send(this)
    asPlayerFlying().stop()
}

/**
 * turn Off jetpack player
 * require run in Main Thread!
 */
fun Player.turnOff() = turnOff(null)

/**
 * set item metadata String
 */
fun ItemStack.set(key: String, value: String?) = metaData.setString(this, key = key, value = value)

/**
 * get item metadata String
 */
fun ItemStack.get(key: String): String = metaData.getString(this, key = key)

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
    if (fuelItem.startsWith("@")) {
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
                it.replace(jetpackFuelPlaceholder, fuelItem).replace(jetpackFuelValuePlaceholder, fuel)
                    .translateAllCodes()
            }
        }
    }
    return set(fuelIdJetpack, fuel)
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
        it.next().apply {
            try {
                if (get(idJetpack) != jetpackItem.get(idJetpack) || type != jetpackItem.type)
                    return@apply
                this@update[index] = jetpackItem
                done = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
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
    if (fuel.startsWith("@")) {
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
                    it.replace(jetpackFuelPlaceholder, fuel).replace(jetpackFuelValuePlaceholder, fuelValue.toString())
                        .translateAllCodes()
                }
            }

            if (jetpack.flags.isNotEmpty())
                for (flag in jetpack.flags)
                    try {
                        addItemFlags(ItemFlag.valueOf(flag.uppercase().trim()))
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        "&cInvalid flag $flag".send(this@setJetpack)
                    }
            if (serverVersion > 16)
                isUnbreakable = jetpack.unbreakable
        } ?: run {
            "&cInvalid Item!".send(this@setJetpack)
            null
        }
        set(idJetpack, jetpack.id).set(fuelIdJetpack, fuelValue.toString())
    }

    if (jetpack.enchantments.isNotEmpty())
        for (enchant in jetpack.enchantments)
            try {
                val enchantname = enchant.split(":").toTypedArray()[0]
                val enchantlvl: Int = enchant.split(":").toTypedArray()[1].toIntSafe(1)
                val enchantment =
                    (if (serverVersion > 16)
                        Enchantment.getByKey(NamespacedKey.minecraft(enchantname.lowercase()))
                    else
                        Enchantment.getByName(enchantname.uppercase())) ?: continue
                itemStack.addUnsafeEnchantment(enchantment, enchantlvl)
            } catch (ignored: Exception) {
                "&cInvalid enchant $enchant".send(this)
            }
    return itemStack
}

/**
 * give target Jetpack item
 */
fun CommandSender.giveJetpack(p: Player, jetpack: Jetpack, fuelValue: Long) {
    try {
        var jI: String = jetpack.jetpackItem.uppercase()
        if (jI.contains(":")) jI = jI.split(":").toTypedArray()[0]
        var item = ItemStack(Material.valueOf(jI))
        item.itemMeta?.let { im ->
            if (im is LeatherArmorMeta && jetpack.jetpackItem.contains(":")) {
                val c: List<String> = jetpack.jetpackItem.split(":")[1].split(".")
                im.setColor(Color.fromRGB(c[0].toIntSafe(), c[1].toIntSafe(), c[2].toIntSafe()))
            }
            item.itemMeta = im
            item = setJetpack(item, jetpack, fuelValue)
            p.inventory.addItem(item)
            p.updateInventory()
            if (this == p) {
                "&aGive item &6${jetpack.id} &aJetpack with fuel &6&lx${fuelValue}&r &ato your self success!".send(this)
                return
            }
            "&aGive item &6${jetpack.id} &aJetpack with fuel &6&lx${fuelValue}&r &ato player &e${p.displayName}&a success!".send(this)
            "&aYou have been given a &6${jetpack.id}&a Jetpack with fuel &6&lx${fuelValue}&r &afrom &e${name}".send(p)
        } ?: "&cGive item &6${jetpack.id}&c Jetpack Failed!".send(this)
    } catch (e: Exception) {
        e.printStackTrace()
    }
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

    item = item.set(idCustomFuel, "@${customFuel.id}")
    item.amount = amount

    target.inventory.addItem(item)
    target.updateInventory()
    if (this == target) {
        "&aGive item &6${customFuel.id} &aitem &6x${amount}&r &ato your self success!".send(this)
        return
    }
    "&aGive item &6${customFuel.id} &aitem &6x${amount}&r &ato player &e${target.displayName}&r &asuccess!".send(this)
    "&aYou have been given a &6${customFuel.id} &aitem from &6${name}".send(target)
}