package me.phantomx.fjetpackreloaded.data.hook

import java.util.*
import kotlin.collections.HashMap

data class SuperiorSkyblock2Player(
    /**
     * is Player UUID!
     */
    val uuid: UUID,
    var playersState: MutableMap<UUID, Boolean> = HashMap()
    )