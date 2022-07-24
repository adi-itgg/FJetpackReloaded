package me.phantomx.fjetpackreloaded.commands.hook

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.phantomx.fjetpackreloaded.const.GlobalConst.FJETPACK_PERMISSION_PREFIX
import me.phantomx.fjetpackreloaded.const.GlobalConst.ID_JETPACK
import me.phantomx.fjetpackreloaded.data.hook.SuperiorSkyblock2Player
import me.phantomx.fjetpackreloaded.extensions.isPluginActive
import me.phantomx.fjetpackreloaded.extensions.safeRun
import me.phantomx.fjetpackreloaded.extensions.saveDatabase
import me.phantomx.fjetpackreloaded.extensions.send
import me.phantomx.fjetpackreloaded.fields.HookPlugin.SuperiorSkyblock2Name
import me.phantomx.fjetpackreloaded.fields.HookPlugin.SuperiorSkyblock2Permission
import me.phantomx.fjetpackreloaded.fields.HookPlugin.superiorPlayersData
import me.phantomx.fjetpackreloaded.modules.Module.mainContext
import me.phantomx.fjetpackreloaded.modules.Module.messages
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class SS2FJRCommands(private val plugin: JavaPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean =
        sender.safeRun {
            if (!label.equals(ID_JETPACK, true) && label.lowercase() != "ss2fjr" || !server.isPluginActive(SuperiorSkyblock2Name)
            ) return false

            if (args.isEmpty() || args[0].equals("help", true)) {
                if (hasPermission("${FJETPACK_PERMISSION_PREFIX}help")) {
                    val stream = plugin.getResource("default/hook/$SuperiorSkyblock2Name.txt") ?: return true
                    stream.use {
                        Scanner(it).use { s ->
                            while (s.hasNextLine())
                                s.nextLine().send(this, true)
                        }
                    }
                } else
                    messages.noPerms.send(this)
                return true
            }

            if (senderDontHavePermissions()) {
                messages.noPerms.send(this)
                return false
            }

            var requestTarget: Player? = null
            var isAllowed = false
            var targetPlayer: Player? = null

            when (args.size) {
                2 -> if (sender is Player) {
                    requestTarget = sender
                    targetPlayer = Bukkit.getPlayerExact(args[0])?.also { isAllowed = args[1].equals("allow", true) }
                        ?: Bukkit.getPlayer(args[1])?.also { isAllowed = args[0].equals("allow", true) }
                } else {
                    "&cYou can't run this command from console!".send(this)
                    return true
                }
                3 -> {
                    requestTarget =
                        SuperiorSkyblockAPI.getIsland(args[0])?.owner?.asPlayer() ?: Bukkit.getPlayerExact(args[0])
                    targetPlayer = Bukkit.getPlayerExact(args[1])?.also { isAllowed = args[2].equals("allow", true) }
                        ?: Bukkit.getPlayer(args[2])?.also { isAllowed = args[1].equals("allow", true) }
                }

            }

            requestTarget?.let { request ->
                SuperiorSkyblockAPI.getPlayer(request)?.island?.let { island ->
                    targetPlayer?.let {
                        if (args.size == 3 && !hasPermission("$SuperiorSkyblock2Permission*") && !hasPermission("$SuperiorSkyblock2Permission${if (isAllowed) "allow" else "deny"}.other")) {
                            if (sender is Player && sender.uniqueId != request.uniqueId) {
                                messages.noPerms.send(this)
                                return true
                            }
                        }

                        val playerData = superiorPlayersData[request.uniqueId] ?: SuperiorSkyblock2Player(
                            request.uniqueId
                        )
                        playerData.playersState[it.uniqueId] = isAllowed
                        "${if (isAllowed) "&aAllowed" else "&cDenied"} &6${it.displayName} &bfly using jetpack in island &6${request.displayName} (&e${island.name}&6)".send(
                            this
                        )
                        superiorPlayersData[request.uniqueId] = playerData
                        CoroutineScope(mainContext).launch {
                            val saveData: MutableList<SuperiorSkyblock2Player> = mutableListOf()
                            superiorPlayersData.forEach { (_, u) ->
                                saveData.add(u)
                            }
                            saveDatabase(SuperiorSkyblock2Name, saveData)
                        }
                        return true
                    } ?: "&cPlayer doesn't exist".send(this)
                } ?: "&cYou don't have island!".send(this)
                return true
            } ?: "&cIsland/Player doesn't exist!".send(this)
            false
        } ?: false

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String> = sender.safeRun {
        val completer: MutableList<String> = mutableListOf()
        if (senderDontHavePermissions())
            return completer

        // FJR <Island|Player> [Allow|Deny] (Player) - Allow/Deny player fly
        //  -         0              1         2
        when (args.size) {
            1 -> {
                if (sender is Player) {
                    completer.add("Allow")
                    completer.add("Deny")
                    completer.addAll(getPlayers())
                    if (hasPermission(SuperiorSkyblock2Permission + "allow.other") ||
                        hasPermission(SuperiorSkyblock2Permission + "deny.other"))
                        completer.addAll(getIslands())
                } else {
                    completer.addAll(getIslands())
                    completer.addAll(getPlayers())
                }
            }
            2 -> {
                if (Bukkit.getPlayerExact(args[0]) != null ||
                    SuperiorSkyblockAPI.getIsland(args[0]) != null
                ) {
                    completer.add("Allow")
                    completer.add("Deny")
                    if (hasPermission(SuperiorSkyblock2Permission + "allow.other") ||
                        hasPermission(SuperiorSkyblock2Permission + "deny.other")
                    )
                        completer.addAll(getPlayers())
                } else
                    completer.addAll(getPlayers())
            }
            3 -> {
                if (args[0].equals("allow", true) || args[0].equals("deny", true))
                    return completer

                if (Bukkit.getPlayerExact(args[1]) != null) {
                    completer.add("Allow")
                    completer.add("Deny")
                } else
                    completer.addAll(getPlayers())
            }
        }

        completer
    } ?: mutableListOf()

    private fun getPlayers(): MutableList<String> {
        val players: MutableList<String> = mutableListOf()
        Bukkit.getOnlinePlayers().forEach {
            players.add(it.name)
        }
        players.sort()
        return players
    }

    private fun getIslands(): MutableList<String> {
        val islands: MutableList<String> = mutableListOf()
        SuperiorSkyblockAPI.getGrid().islands.forEach {
            islands.add(it.name)
        }
        islands.sort()
        return islands
    }

    private fun CommandSender.senderDontHavePermissions(): Boolean = !hasPermission("${SuperiorSkyblock2Permission}*") &&
            !hasPermission("${SuperiorSkyblock2Permission}allow") &&
            !hasPermission("${SuperiorSkyblock2Permission}deny") &&
            !hasPermission("${SuperiorSkyblock2Permission}allow.other") &&
            !hasPermission("${SuperiorSkyblock2Permission}deny.other")

}