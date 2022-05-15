package me.phantomx.fjetpackreloaded.abstracts

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Job
import me.phantomx.fjetpackreloaded.FJetpackReloaded
import me.phantomx.fjetpackreloaded.data.*
import me.phantomx.fjetpackreloaded.nms.ItemMetaData
import java.util.*
import kotlin.coroutines.CoroutineContext

abstract class Plugin {

    val id = 14647

    val mainContext: CoroutineContext = Job()
    lateinit var plugin: FJetpackReloaded
    val stringEmpty = StringBuilder().toString()
    val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    var nmsAPIVersion = "UNKNOWN"
    var serverVersion = 0
    var defaultPrefix = "&e[&bFJetpack&6Reloaded&e]"

    var modifiedConfig: Config = Config()
    var configVersion: Int = 0
    lateinit var messages: Messages
    val jetpacks: MutableMap<String, Jetpack> = HashMap()
    val customFuel: MutableMap<String, CustomFuel> = HashMap()
    val listPlayerUse: WeakHashMap<PlayerFlying, Jetpack> = WeakHashMap()
    val dataPlayer: WeakHashMap<UUID, PlayerFlying> = WeakHashMap()

    lateinit var metaData: ItemMetaData
    val idJetpack: String = FJetpackReloaded::class.java.simpleName
    val idCustomFuel: String = "FJRCustomFuel"
    val permission = idJetpack.lowercase() + "."

    val fuelIdJetpack = "FJRFuel"

    val jetpackFuelPlaceholder = "{#fuel}"
    val jetpackFuelValuePlaceholder = "{#fuel_value}"

    val jetpacksYaml = "configs/Jetpacks.yml"
    val customFuelsYaml = "configs/CustomFuels.yml"
    val messagesYaml = "configs/Messages.yml"


}

