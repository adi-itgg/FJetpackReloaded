package me.phantomx.fjetpackreloaded.fields

import com.bgsoftware.superiorskyblock.api.island.IslandFlag
import me.phantomx.fjetpackreloaded.data.hook.SuperiorSkyblock2Config
import me.phantomx.fjetpackreloaded.data.hook.SuperiorSkyblock2Player
import java.util.*
import kotlin.collections.HashMap

object HookPlugin {

    const val GriefPreventionName = "GriefPrevention"

    const val SuperiorSkyblock2Name = "SuperiorSkyblock2"
    const val SuperiorSkyblock2Permission = "ss2fjr."
    const val SuperiorSkyblock2ConfigFile = "configs/hook/SuperiorSkyblock2/config.yml"

    var superiorPlayersData: MutableMap<UUID, SuperiorSkyblock2Player> = HashMap()
    var superiorSkyblock2ConfigLoaded: SuperiorSkyblock2Config = SuperiorSkyblock2Config()

    lateinit var fjetpackReloadedFlag: IslandFlag

}