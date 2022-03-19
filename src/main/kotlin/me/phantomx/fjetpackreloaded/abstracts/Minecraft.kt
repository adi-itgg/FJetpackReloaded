package me.phantomx.fjetpackreloaded.abstracts

import me.phantomx.fjetpackreloaded.commands.FJRCommands

abstract class Minecraft: FJRCommands() {
    fun enable() {
        isEnabled = true
    }
    fun disable() {
        isEnabled = false
    }
}