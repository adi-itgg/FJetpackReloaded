package me.phantomx.fjetpackreloaded.sealeds

sealed class OnEmptyFuel {
    object Drop : OnEmptyFuel()
    object Remove : OnEmptyFuel()
    object Nothing : OnEmptyFuel()
}