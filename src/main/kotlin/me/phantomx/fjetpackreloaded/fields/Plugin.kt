package me.phantomx.fjetpackreloaded.fields

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.SupervisorJob
import me.phantomx.fjetpackreloaded.FJetpackReloaded
import me.phantomx.fjetpackreloaded.data.*
import me.phantomx.fjetpackreloaded.nms.ItemMetaData
import java.io.File
import java.util.*
import kotlin.coroutines.CoroutineContext

abstract class Plugin {

    val id = 14647

    val mainContext: CoroutineContext = SupervisorJob() + Default

    lateinit var plugin: FJetpackReloaded
    val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    var nmsAPIVersion = "UNKNOWN"
    var serverVersion = 0
    var defaultPrefix = "&e[&bFJetpack&6Reloaded&e]"

    var modifiedConfig: Config = Config()
    var configVersion: Int = 0
    lateinit var messages: Messages
    val jetpacks: MutableMap<String, Jetpack> = HashMap()
    val customFuel: MutableMap<String, CustomFuel> = HashMap()
    val fjrPlayersActive: WeakHashMap<FJRPlayer, Jetpack> = WeakHashMap()
    val dataFJRPlayer: WeakHashMap<UUID, FJRPlayer> = WeakHashMap()

    lateinit var metaData: ItemMetaData

    lateinit var databaseDirectory: File


}

