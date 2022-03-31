package me.phantomx.fjetpackreloaded.events

import kotlinx.coroutines.*
import me.phantomx.fjetpackreloaded.extensions.*
import me.phantomx.fjetpackreloaded.modules.Module.customFuel
import me.phantomx.fjetpackreloaded.modules.Module.fuelIdJetpack
import me.phantomx.fjetpackreloaded.modules.Module.idCustomFuel
import me.phantomx.fjetpackreloaded.modules.Module.idJetpack
import me.phantomx.fjetpackreloaded.modules.Module.jetpacks
import me.phantomx.fjetpackreloaded.modules.Module.listPlayerUse
import me.phantomx.fjetpackreloaded.modules.Module.mainContext
import me.phantomx.fjetpackreloaded.modules.Module.messages
import me.phantomx.fjetpackreloaded.modules.Module.serverVersion
import me.phantomx.fjetpackreloaded.sealeds.OnDeath
import me.phantomx.fjetpackreloaded.sealeds.OnEmptyFuel
import org.bukkit.Effect
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import kotlin.coroutines.CoroutineContext
import kotlin.math.sin

@Suppress("unused")
class EventListener : Listener, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = mainContext

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCrouch(e: PlayerToggleSneakEvent) {
        try {
            e.player.apply player@ {
                equipment?.let {
                    if (it.armorContents.isEmpty()) return
                    val armors = it.armorContents.iterator()
                    while (armors.hasNext()) {
                        armors.next()?.let i@ { armor ->
                            if (armor.type == Material.AIR) return@i
                            armor.itemMeta?.let { meta ->
                                if (isSneaking || !meta.hasLore() || !(this as LivingEntity).isOnGround) return@i
                                jetpacks[armor.get(idJetpack)]?.let { jetpack ->
                                    if (!hasPermission(jetpack.permission) && !hasPermission("${jetpack.permission}.use")) {
                                        messages.noPerms.send(this)
                                        return@i
                                    }

                                    if (allowFlight && listPlayerUse[asPlayerFlying()] != null) {
                                        turnOff(jetpack)
                                        return@i
                                    }

                                    for (world in jetpack.worldBlackList) {
                                        if (world != getWorld().name) continue
                                        messages.blockedWorlds.send(this)
                                        return@i
                                    }

                                    var fuel: Long
                                    try {
                                        if (!jetpack.fuel.startsWith("@")) {
                                            val fuelMaterial = Material.valueOf(jetpack.fuel.uppercase())
                                            if (fuelMaterial != Material.AIR) {
                                                val dFuel = jetpack.fuel.replace("_", " ")
                                                fuel = armor.get(fuelIdJetpack).toLongSafe()
                                                if (fuel < jetpack.fuelCost) {
                                                    messages.noFuel.replace("%fuel_item%", dFuel).send(this)
                                                    return@i
                                                }
                                            }
                                        } else {
                                            fuel = armor.get(fuelIdJetpack).toLongSafe()
                                            if (fuel < jetpack.fuelCost) {
                                                customFuel[jetpack.fuel.substring(1)]?.let {
                                                    messages.noFuel.replace("%fuel_item%", displayName).send(this)
                                                } ?: "&cError invalid Custom Fuel".send(this)
                                                return@i
                                            }
                                        }
                                    } catch (e: Exception) {
                                        if (e is CancellationException) throw e
                                        e.printStackTrace()
                                        return@i
                                    }

                                    asPlayerFlying().apply playerFlying@{
                                        allowFlight = true
                                        flySpeed = jetpack.speed.toFloat() / 10.0f

                                        fuelJob = launch {
                                            try {
                                                while (true) {
                                                    yield()
                                                    val stop =
                                                        equipment?.let stopJob@{ ee ->
                                                            if (ee.armorContents.isEmpty())
                                                                return@stopJob true
                                                            val iterator = ee.armorContents.iterator()
                                                            while (iterator.hasNext()) {
                                                                yield()
                                                                iterator.next()?.let { armor ->
                                                                    var lArmor = armor
                                                                    if (lArmor.type != Material.AIR)
                                                                        jetpacks[lArmor.get(idJetpack)]?.let jetpack@{ lJetpack ->
                                                                            if (jetpack.id != lJetpack.id) return@jetpack
                                                                            if (!hasPermission(lJetpack.permission) && !hasPermission(
                                                                                    "${lJetpack.permission}.use"
                                                                                )
                                                                            ) return@stopJob true
                                                                            if ((this@player as LivingEntity).isOnGround && !isFlying) return@stopJob false
                                                                            for (blocked in lJetpack.worldBlackList) {
                                                                                if (world.name != blocked) continue
                                                                                messages.blockedWorlds.send(this@player)
                                                                                return@stopJob true
                                                                            }

                                                                            fuel =
                                                                                lArmor.get(fuelIdJetpack).toLongSafe()
                                                                            if (!lJetpack.canBypassFuel || !hasPermission(
                                                                                    "${lJetpack.permission}.bypass"
                                                                                )
                                                                            ) {
                                                                                if (fuel < lJetpack.fuelCost) {
                                                                                    messages.outOfFuel.send(this@player)
                                                                                    stop()
                                                                                    throw CancellationException(messages.outOfFuel)
                                                                                }
                                                                                fuel -= lJetpack.fuelCost
                                                                            }

                                                                            if ((!lJetpack.canBypassSprintFuel || !hasPermission(
                                                                                    "${lJetpack.permission}.sprintbypass"
                                                                                )) && isSprinting
                                                                            ) {
                                                                                if (fuel < lJetpack.fuelCostFlySprint) {
                                                                                    messages.outOfFuel.send(this@player)
                                                                                    stop()
                                                                                    throw CancellationException(messages.outOfFuel)
                                                                                }
                                                                                fuel -= lJetpack.fuelCostFlySprint
                                                                            }
                                                                            lArmor =
                                                                                lArmor.update(fuel.toString(), lJetpack)
                                                                            equipment?.apply {
                                                                                val liveArmors =
                                                                                    armorContents.toMutableList()
                                                                                if (liveArmors.isEmpty()) return@stopJob true
                                                                                armorContents =
                                                                                    liveArmors.update(lArmor)
                                                                                        .toTypedArray()
                                                                                return@stopJob false
                                                                            } ?: return@stopJob true
                                                                        }
                                                                }
                                                            }
                                                            true
                                                        } ?: true
                                                    if (stop)
                                                        break
                                                    delay(jetpack.burnRate.toLong() * 1000L)
                                                }
                                            } catch (e: Exception) {
                                                if (e is CancellationException) throw e
                                                e.printStackTrace()
                                                return@launch
                                            }
                                            turnOff(jetpack)
                                        }

                                        if (jetpack.particleEffect.lowercase() != "none")
                                            try {
                                                if (serverVersion > 8)
                                                    Particle.valueOf(jetpack.particleEffect.uppercase().trim())
                                                else
                                                    Effect.valueOf(jetpack.particleEffect.uppercase().trim())

                                                particleJob = launch {
                                                    try {
                                                        while (true) {
                                                            yield()
                                                            if (!(player as LivingEntity).isOnGround && isFlying && allowFlight && isOnline && listPlayerUse[this@playerFlying] != null)
                                                                try {
                                                                    if (serverVersion > 8) {
                                                                        val newZ = (0.1 * sin(
                                                                            Math.toRadians(
                                                                                (location.yaw + 270.0f).toDouble()
                                                                            )
                                                                        )).toFloat()
                                                                        main {
                                                                            world.spawnParticle(
                                                                                Particle.valueOf(
                                                                                    jetpack.particleEffect.uppercase()
                                                                                        .trim()
                                                                                ),
                                                                                location.x + newZ,
                                                                                location.y + 0.8,
                                                                                location.z + newZ,
                                                                                jetpack.particleAmount,
                                                                                0.0,
                                                                                -0.2,
                                                                                0.0
                                                                            )
                                                                        }
                                                                    } else {
                                                                        world.playEffect(
                                                                            location.add(0.0, 0.8, 0.0),
                                                                            Effect.valueOf(
                                                                                jetpack.particleEffect.uppercase()
                                                                                    .trim()
                                                                            ),
                                                                            0
                                                                        )
                                                                    }
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            delay(jetpack.particleDelay)
                                                        }
                                                    } catch (e: Exception) {
                                                        if (e is CancellationException) throw e
                                                        e.printStackTrace()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                "&cInvalid Particle Effect!".send(this@player)
                                            }

                                        listPlayerUse[this] = jetpack
                                        messages.turnOn.send(this@player)
                                        return
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerJoin(e: PlayerJoinEvent) {
        launch {
            e.player.checkUpdatePlugin(loginEvent = true)
        }
    }

    @EventHandler
    fun onPlayerDisconnected(e: PlayerQuitEvent) {
        e.player.asPlayerFlying().stop()
    }

    @Suppress("Deprecation")
    @EventHandler
    fun onDamagedArmorPlayer(e: EntityDamageEvent) {
        try {
            val entity = e.entity as? Player ?: return
            val eq = entity.equipment ?: return
            val armors = eq.armorContents
            if (armors.isEmpty()) return
            val iterator = armors.iterator()
            while (iterator.hasNext()) {
                iterator.next().apply {
                    if (this == null || type == Material.AIR) return@apply
                    jetpacks[get(idJetpack)]?.let {
                        if (serverVersion > 16) {
                            itemMeta = itemMeta?.apply {
                                isUnbreakable = it.unbreakable
                            }
                        } else if (it.unbreakable) durability = 0.toShort()
                        eq.armorContents = eq.armorContents.toMutableList().update(this).toTypedArray()
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @EventHandler
    fun onPlayerDied(e: EntityDeathEvent) {
        try {
            if (e.entity.type != EntityType.PLAYER) return
            val p: Player = e.entity as Player

            p.asPlayerFlying().apply {
                listPlayerUse[this]?.let {
                    stop()
                    messages.turnOff.send(p)
                }
            }

            for (jp in jetpacks.values) {
                if (jp.onDied == OnDeath.Nothing && jp.onNoFuel == OnEmptyFuel.Nothing) continue
                p.inventory.let { inventory ->
                    val armors = inventory.armorContents.toMutableList()
                    armors.iterator().let {
                        while (it.hasNext())
                            it.next()?.apply item@{
                                val id = get(idJetpack)
                                if (id.isNotEmpty() && id == jp.id) {

                                    when (jp.onDied) {
                                        OnDeath.Drop -> {
                                            p.equipment?.armorContents?.let { eqarmor ->
                                                var dropped = false
                                                val eqArmors = eqarmor.toMutableList()
                                                eqArmors.iterator().apply {
                                                    while (hasNext())
                                                        next()?.let { item ->
                                                            if (item.get(idJetpack) == get(idJetpack)) {
                                                                dropped = true
                                                                val dropItem = item.clone()
                                                                main {
                                                                    p.world.dropItemNaturally(p.location, dropItem)
                                                                }
                                                                eqArmors.remove(item)
                                                            }
                                                        }
                                                }
                                                if (!dropped) {
                                                    val dropItem = clone()
                                                    main {
                                                        p.world.dropItemNaturally(p.location, dropItem)
                                                    }
                                                }
                                                p.equipment?.armorContents = eqArmors.toTypedArray()
                                            }
                                            messages.onDeathDropped.send(p)
                                        }
                                        OnDeath.Nothing -> {

                                        }
                                        OnDeath.Remove -> {
                                            p.inventory.remove(this@item)
                                            p.updateInventory()
                                            messages.onDeathRemoved.send(p)
                                        }
                                    }

                                    armors.remove(this)
                                }
                            }
                    }
                    inventory.setArmorContents(armors.toTypedArray())
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        (e.whoClicked as Player).apply player@ {
            try {
                if (e is InventoryCreativeEvent) return
                e.cursor?.let { cursorItem ->
                    e.currentItem?.let { slotItem ->
                        if (!slotItem.hasItemMeta() || slotItem.type == Material.AIR || slotItem.amount == 0) return
                        if (e.click != ClickType.LEFT && e.click != ClickType.RIGHT && e.click == ClickType.WINDOW_BORDER_LEFT && e.click == ClickType.WINDOW_BORDER_RIGHT)
                            return
                        jetpacks[slotItem.get(idJetpack)]?.let { jetpack ->

                            if (e.slotType == InventoryType.SlotType.ARMOR)
                                listPlayerUse[asPlayerFlying()]?.let {
                                    if (jetpack.id == it.id)
                                        turnOff(it)
                                    else
                                        return
                                }

                            if (jetpack.fuel.startsWith("@"))
                                customFuel[cursorItem.get(idCustomFuel)]?.apply {
                                    if (!hasPermission(permission)) {
                                        messages.noPerms.send(this@player)
                                        return
                                    }
                                } ?: return
                            else if (cursorItem.type != Material.valueOf(jetpack.fuel.uppercase().trim()))
                                return

                            if (!hasPermission(jetpack.permission) && !hasPermission("${jetpack.permission}.refuel")) {
                                messages.noPerms.send(this)
                                return
                            }

                            e.isCancelled = true

                            val addFuelAmount = if (e.isLeftClick) cursorItem.amount else 1

                            val fuel = slotItem.get(fuelIdJetpack).toLongSafe()

                            e.currentItem = slotItem.update((fuel + addFuelAmount).toString(), jetpack)
                            if (e.isLeftClick)
                                setItemOnCursor(ItemStack(Material.AIR))
                            else {
                                cursorItem.amount = cursorItem.amount - 1
                                setItemOnCursor(cursorItem)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}