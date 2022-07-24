package me.phantomx.fjetpackreloaded.data

import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment

@Serializable
data class Config(
    @Comment("The configs version")
    val version: Int = 1,
    @Comment("Enable/Disable Update Notification")
    val updateNotification: Boolean = true,
    @Comment("Set all format configs to yaml")
    val configsYaml: Boolean = true
)