package me.phantomx.fjetpackreloaded.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.phantomx.fjetpackreloaded.data.Jetpack
import me.phantomx.fjetpackreloaded.extensions.*
import me.phantomx.fjetpackreloaded.modules.Module.customFuel
import me.phantomx.fjetpackreloaded.modules.Module.idJetpack
import me.phantomx.fjetpackreloaded.modules.Module.jetpacks
import me.phantomx.fjetpackreloaded.modules.Module.load
import me.phantomx.fjetpackreloaded.modules.Module.messages
import me.phantomx.fjetpackreloaded.modules.Module.permission
import me.phantomx.fjetpackreloaded.modules.Module.plugin
import me.phantomx.fjetpackreloaded.modules.Module.serverVersion
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.StringUtil
import java.util.*

abstract class FJRCommands : JavaPlugin(), CoroutineScope, TabCompleter {

    private val commandList: List<String> = mutableListOf(
        "help", // 0
        "reload", // 1
        "set", // 2
        "get", // 3
        "give", // 4
        "getfuel", // 5
        "givefuel", // 6
        "checkupdate", // 7
        "setfuel" // 8
    )

    @Suppress("Deprecation")
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean = sender.run {
        if (label != idJetpack && label.lowercase() != "fjr") return false

        var notContainsCmd = true
        if (args.isNotEmpty())
            for (cmd in commandList) {
                if (!cmd.equals(args[0], ignoreCase = true)) continue
                notContainsCmd = false
                break
            }

        if (args.isEmpty() || args[0].equals("help", ignoreCase = true) || notContainsCmd) {
            if (hasPermission("${permission}help")) {
                val stream = getResource("default/help.txt") ?: return true
                stream.use {
                    Scanner(it).use { s ->
                        while (s.hasNextLine())
                            s.nextLine().replace("#{version}", description.version).send(this, true)
                    }
                }
            } else
                messages.noPerms.send(this)
            return true
        }

        if (args[0].equals(commandList[1], ignoreCase = true)) {
            if (hasPermission(permission + commandList[1]))
                launch {
                    if (plugin.load(sender = sender))
                        "&aReload Config Success!".send(target = sender)
                }
            else
                messages.noPerms.send(this)
            return true
        }

        if (args[0].equals(commandList[7], ignoreCase = true)) {
            if (hasPermission(permission + commandList[7]))
                launch {
                    checkUpdatePlugin()
                }
            else
                messages.noPerms.send(this)
            return true
        }

        if (args[0].equals(commandList[2], ignoreCase = true)) {
            if (!hasPermission(permission + commandList[2])) {
                messages.noPerms.send(this)
                return true
            }
            if (args.size == 1) {
                "&8&l- &3/fjr Set [Jetpack] <Fuel>".send(this)
                return true
            }
            if (this is Player) {
                getItemInHandByCheckingServerVersion()?.let {
                    var item = it
                    val jetpack: Jetpack? = jetpacks[args[1]]
                    if (jetpack == null) {
                        "&cJetpack &l${args[1]} &cdidn't exist".send(this)
                        return true
                    }
                    try {
                        item = setJetpack(item, jetpack, if (args.size == 3) args[2].toLong() else 0)
                    } catch (ex: NumberFormatException) {
                        "&cInvalid fuel amount!".send(this)
                        return true
                    }
                    if (serverVersion > 11)
                        inventory.setItemInMainHand(item)
                    else
                        setItemInHand(item)
                    "&aSuccess set item to jetpack ${args[1]} with fuel amount &6&lx${if (args.size == 3) args[2].toLongSafe() else 0}".send(
                        this
                    )
                }
                return true
            }
            "&cThis command can run only in game as player!".send(this)
            return true
        }

        if (args[0].equals(commandList[3], ignoreCase = true) ||
            args[0].equals(commandList[4], ignoreCase = true)
        ) {
            if (!hasPermission(permission + commandList[3]) &&
                !hasPermission(permission + commandList[4])
            ) {
                messages.noPerms.send(this)
                return true
            }
            try {
                if (args.size > 1)
                    Bukkit.getPlayerExact(args[1])?.let {
                        if (args.size > 2)
                            jetpacks[args[2]]?.apply {
                                giveJetpack(it, this, if (args.size == 4) args[3].toLongSafe() else 0)
                                return true
                            }
                    } ?: also {
                        if (this !is Player) {
                            "&cYou can't run this command from Console!".send(this)
                            return true
                        }
                        jetpacks[args[1]]?.apply {
                            giveJetpack(this@run, this, if (args.size == 3) args[2].toLongSafe() else 1)
                            return true
                        }
                    }

            } catch (e: Exception) {
                e.printStackTrace()
            }
            "&bUsage: &3/fjr get (Player) &b${jetpacks.keys}&3 <Fuel>".send(this)
            return true
        }

        if (args[0].equals(commandList[8], ignoreCase = true)) {
            if (!hasPermission(permission + commandList[8])) {
                messages.noPerms.send(this)
                return true
            }
            if (this !is Player) {
                "&cThis command can run only in game as player!".send(this)
                return true
            }
            if (args.size == 1 || args[1].toIntSafe(-1) == -1) {
                "&8&l- &3/fjr SetFuel <Amount>".send(this)
                return true
            }
            getItemInHandByCheckingServerVersion()?.let {
                var item = it
                val fuel = args[1].toLongSafe()
                return jetpacks[item.get(idJetpack)]?.let { jp ->
                    item = setJetpack(item, jp, fuel)
                    if (serverVersion > 11)
                        inventory.setItemInMainHand(item)
                    else
                        setItemInHand(item)
                    "&aSuccess set fuel jetpack to &6$fuel".send(this)
                    true
                } ?: run {
                    "&cThis item is not Jetpack item!".send(this)
                    false
                }
            }
            return true
        }

        if (args[0].equals(commandList[5], ignoreCase = true) || args[0].equals(
                commandList[6],
                ignoreCase = true
            )
        ) {
            if (!hasPermission(permission + commandList[5]) && !hasPermission(permission + commandList[6])) {
                messages.noPerms.send(this)
                return true
            }
            try {
                if (args.size > 1)
                    Bukkit.getPlayerExact(args[1])?.let {
                        if (args.size > 2)
                            customFuel[args[2]]?.apply {
                                giveCustomFuel(it, this, if (args.size == 4) args[3].toIntSafe(1) else 1)
                                return true
                            }
                    } ?: also {
                        if (this !is Player) {
                            "&cYou can't run this command from Console!".send(this)
                            return true
                        }
                        customFuel[args[1]]?.apply {
                            giveCustomFuel(this@run, this, if (args.size == 3) args[2].toIntSafe(1) else 1)
                            return true
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            "&8&l- &3/fjr GetFuel (Player) &b${customFuel.keys} &3<Amount>".send(this)
            return true
        }

        return false
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String>? = sender.run {
        if (!isOp || !hasPermission("${permission}admin") || !hasPermission(permission + args[0]))
            return null

        var completions: MutableList<String> = ArrayList()
        if (args.size == 1)
            StringUtil.copyPartialMatches<MutableList<String>>(
                args[0],
                commandList,
                completions
            )

        if (args[0].equals(commandList[3], ignoreCase = true) || args[0].equals(commandList[4], ignoreCase = true)) {
            if (args.size == 2) {
                val cmds: MutableList<String> = ArrayList()
                for (player in Bukkit.getOnlinePlayers()) {
                    cmds.add(player.name)
                    val displayName = player.displayName
                    if (!displayName.equals(player.name, ignoreCase = true) && displayName.isEmpty())
                        cmds.add(displayName)
                }
                cmds.addAll(jetpacks.keys)
                completions = StringUtil.copyPartialMatches<MutableList<String>>(args[1], cmds, completions)
            }
            if (args.size == 3 && jetpacks[args[1]] == null) {
                completions = StringUtil.copyPartialMatches<MutableList<String>>(args[2], jetpacks.keys, completions)
            }
        }

        if (args[0].equals(commandList[2], ignoreCase = true) && args.size == 2)
            completions = StringUtil.copyPartialMatches<MutableList<String>>(args[1], jetpacks.keys, completions)


        if (args[0].equals(commandList[5], ignoreCase = true) ||
            args[0].equals(commandList[6], ignoreCase = true)
        ) {
            if (args.size == 2) {
                val cmds: MutableList<String> = customFuel.keys.toMutableList()
                for (player in Bukkit.getOnlinePlayers()) {
                    cmds.add(player.name)
                    val displayName = player.displayName
                    if (!displayName.equals(player.name, ignoreCase = true) && displayName.isEmpty())
                        cmds.add(displayName)
                }
                StringUtil.copyPartialMatches<MutableList<String>>(args[1], cmds, completions)
            }
            if (args.size == 3 && customFuel[args[1]] == null)
                StringUtil.copyPartialMatches(args[2], customFuel.keys, completions)
        }

        completions.sort()

        if (args[0].equals(commandList[2], ignoreCase = true) && args.size == 3 ||
            (((args.size == 3 && jetpacks[args[1]] != null || args.size == 4 &&
                    jetpacks[args[2]] != null) && (args[0].equals(commandList[3], ignoreCase = true) ||
                    args[0].equals(commandList[4], ignoreCase = true)) ||
                    (args[0].equals(commandList[5], ignoreCase = true) ||
                            args[0].equals(commandList[6], ignoreCase = true)) &&
                    (args.size == 3 || args.size == 4) && customFuel[args[if (args.size == 4) 2 else 1]] != null))
        ) {
            val defFuel: MutableList<String> = ArrayList()
            var count = 32
            for (i in 1..5)
                defFuel.add(count.also { count *= 2 }.toString())

            StringUtil.copyPartialMatches<MutableList<String>>(args[if (args.size == 4) 3 else 2], defFuel, completions)
        }

        return completions
    }

}