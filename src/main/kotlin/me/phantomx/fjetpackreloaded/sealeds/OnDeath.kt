package me.phantomx.fjetpackreloaded.sealeds

sealed class OnDeath {
    object Drop : OnDeath()
    object Remove : OnDeath()
    object Nothing : OnDeath()
}