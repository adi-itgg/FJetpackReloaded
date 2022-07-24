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
    val configsYaml: Boolean = true,
    @Comment("""
        add new Flag FJETPACK_RELOADED in SuperiorSkyblock2 plugin
    """)
    val addFlagSuperiorSkyblock2: Boolean = false
)