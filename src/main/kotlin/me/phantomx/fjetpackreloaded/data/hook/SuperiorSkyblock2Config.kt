package me.phantomx.fjetpackreloaded.data.hook

import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment

@Serializable
data class SuperiorSkyblock2Config(
    @Comment("Default fjetpackreloaded inside island is allowed?")
    var defaultIsland: Boolean = true
)