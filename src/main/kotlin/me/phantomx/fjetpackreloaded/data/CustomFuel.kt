package me.phantomx.fjetpackreloaded.data

data class CustomFuel(
    var id: String,
    var customDisplay: String,
    var displayName: String,
    var lore: List<String>,
    var item: String,
    var permission: String,
    var glowing: Boolean
)
