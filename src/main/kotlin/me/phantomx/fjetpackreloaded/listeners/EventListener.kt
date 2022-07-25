package me.phantomx.fjetpackreloaded.listeners

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI
import kotlinx.coroutines.*
import me.phantomx.fjetpackreloaded.const.GlobalConst.CUSTOM_FUEL_PREFIX
import me.phantomx.fjetpackreloaded.const.GlobalConst.FJETPACK_PERMISSION_PREFIX
import me.phantomx.fjetpackreloaded.const.GlobalConst.ID_CUSTOM_FUEL
import me.phantomx.fjetpackreloaded.const.GlobalConst.ID_FUEL_JETPACK
import me.phantomx.fjetpackreloaded.const.GlobalConst.ID_JETPACK
import me.phantomx.fjetpackreloaded.const.GlobalConst.JETPACK_FUEL_MESSAGE_PLACEHOLDER
import me.phantomx.fjetpackreloaded.const.GlobalConst.STRING_EMPTY
import me.phantomx.fjetpackreloaded.extensions.*
import me.phantomx.fjetpackreloaded.fields.HookPlugin.GriefPreventionName
import me.phantomx.fjetpackreloaded.fields.HookPlugin.SuperiorSkyblock2Name
import me.phantomx.fjetpackreloaded.fields.HookPlugin.fjetpackReloadedSS2Flag
import me.phantomx.fjetpackreloaded.fields.HookPlugin.fjetpackReloadedSS2Privilege
import me.phantomx.fjetpackreloaded.modules.Module.customFuel
import me.phantomx.fjetpackreloaded.modules.Module.jetpacks
import me.phantomx.fjetpackreloaded.modules.Module.listPlayerUse
import me.phantomx.fjetpackreloaded.modules.Module.mainContext
import me.phantomx.fjetpackreloaded.modules.Module.messages
import me.phantomx.fjetpackreloaded.modules.Module.serverVersion
import me.phantomx.fjetpackreloaded.sealeds.OnDeath
import me.phantomx.fjetpackreloaded.sealeds.OnEmptyFuel
import me.ryanhamshire.GriefPrevention.GriefPrevention
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
    fun onCrouch(e: PlayerToggleSneakEvent) = e.withSafe {
        if (!mainContext.isActive) return
        player.apply player@{
            equipment?.let {
                if (it.armorContents.isEmpty()) return
                val armors = it.armorContents.iterator()
                while (armors.hasNext()) {
                    armors.next()?.let i@{ armor ->
                        if (armor.type == Material.AIR) return@i
                        armor.itemMeta?.let { meta ->
                            if (isSneaking || !meta.hasLore() || !(this as LivingEntity).isOnGround) return@i
                            jetpacks[armor.get(ID_JETPACK)]?.let { jetpack ->
                                if (!hasPermission(jetpack.permission) && !hasPermission("${jetpack.permission}.use")) {
                                    messages.noPerms.send(this)
                                    return@i
                                }

                                // force allow to disable jetpack
                                if (allowFlight && listPlayerUse[asPlayerFlying()] != null) {
                                    turnOff(jetpack)
                                    return@i
                                }

                                // grief prevention
                                if (server.isPluginActive(GriefPreventionName) &&
                                    (jetpack.onlyAllowInsideOwnGriefPreventionClaim || jetpack.onlyAllowInsideAllGriefPreventionClaim)) {
                                    asPlayerFlying().let { pf ->
                                        GriefPrevention.instance.dataStore.getClaimAt(location, true, true, pf.griefClaim)?.let { claim ->
                                            if (jetpack.onlyAllowInsideOwnGriefPreventionClaim && !jetpack.onlyAllowInsideAllGriefPreventionClaim) {
                                                if (claim.getOwnerID() != uniqueId && !isOp && !hasPermission("$FJETPACK_PERMISSION_PREFIX*")) {
                                                    messages.griefPreventionOutsideOwnClaim.send(this)
                                                    return
                                                }
                                            }
                                            pf.griefClaim = claim
                                        } ?: run {
                                            messages.griefPreventionOutsideClaim.send(this)
                                            return
                                        }
                                    }
                                }

                                // check is SuperiorSkyblock2 available
                                if (server.isPluginActive(SuperiorSkyblock2Name)) {
                                    SuperiorSkyblockAPI.getIslandAt(location)?.let {
                                        if (!jetpack.bypassSuperiorSkyblock2Flag)
                                            it.hasSettingsEnabled(fjetpackReloadedSS2Flag).let flag@ {
                                                if (it) return@flag
                                                messages.superiorSkyblock2NoFlag.send(this)
                                                return
                                            }

                                        if (!jetpack.bypassSuperiorSkyblock2Privilege)
                                            if (!it.hasPermission(this, fjetpackReloadedSS2Privilege)) {
                                                messages.superiorSkyblock2NoPermission.send(this)
                                                return
                                            }
                                        // allowed
                                        asPlayerFlying().ss2IslandUUID = it.uniqueId
                                    }
                                }

                                for (world in jetpack.blockedWorlds) {
                                    if (world != getWorld().name) continue
                                    messages.blockedWorlds.send(this)
                                    return@i
                                }

                                var fuel: Long
                                safeRun {
                                    if (!jetpack.fuel.startsWith(CUSTOM_FUEL_PREFIX)) {
                                        val fuelMaterial = Material.valueOf(jetpack.fuel.uppercase())
                                        if (fuelMaterial != Material.AIR) {
                                            val dFuel = jetpack.fuel.replace("_", " ")
                                            fuel = armor.get(ID_FUEL_JETPACK).toLongSafe()
                                            if (fuel < jetpack.fuelCost) {
                                                messages.noFuel.replace(JETPACK_FUEL_MESSAGE_PLACEHOLDER, dFuel).send(this)
                                                return@i
                                            }
                                        }
                                    } else {
                                        fuel = armor.get(ID_FUEL_JETPACK).toLongSafe()
                                        if (fuel < jetpack.fuelCost) {
                                            customFuel[jetpack.fuel.substring(1)]?.let {
                                                messages.noFuel.replace(JETPACK_FUEL_MESSAGE_PLACEHOLDER, displayName).send(this)
                                            } ?: "&cError invalid Custom Fuel".send(this)
                                            return@i
                                        }
                                    }
                                } ?: return@i

                                asPlayerFlying().apply playerFlying@{
                                    allowFlight = true
                                    flySpeed = jetpack.speed.toFloat() / 10.0f

                                    fuelJob = launch {
                                        safeRun {
                                            while (true) {
                                                yield()
                                                var stop =
                                                    equipment?.let stopJob@{ ee ->
                                                        if (ee.armorContents.isEmpty())
                                                            return@stopJob true
                                                        val iterator = ee.armorContents.iterator()
                                                        while (iterator.hasNext()) {
                                                            yield()
                                                            iterator.next()?.let { armor ->
                                                                var lArmor = armor
                                                                if (lArmor.type != Material.AIR)
                                                                    jetpacks[lArmor.get(ID_JETPACK)]?.let jetpack@{ lJetpack ->
                                                                        if (jetpack.id != lJetpack.id) return@jetpack
                                                                        if (!hasPermission(lJetpack.permission) && !hasPermission(
                                                                                "${lJetpack.permission}.use"
                                                                            )
                                                                        ) return@stopJob true
                                                                        if ((this@player as LivingEntity).isOnGround && !isFlying) return@stopJob false
                                                                        for (blocked in lJetpack.blockedWorlds) {
                                                                            if (world.name != blocked) continue
                                                                            messages.blockedWorlds.send(this@player)
                                                                            return@stopJob true
                                                                        }

                                                                        // check fuel
                                                                        fuel =
                                                                            lArmor.get(ID_FUEL_JETPACK).toLongSafe()
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
                                                griefClaim?.withSafe {
                                                    if (!contains(player.location, true, false)) {
                                                        stop = true
                                                        messages.griefPreventionTurnedOffOutsideClaim.send(player)
                                                        turnOff(noMessage = true)
                                                        return@launch
                                                    }
                                                }
                                                ss2IslandUUID?.withSafe {
                                                    SuperiorSkyblockAPI.getIslandAt(player.location)?.let {
                                                        if (it.uniqueId != this) {
                                                            // check new island is allowed or not
                                                            // for flag
                                                            if (!jetpack.bypassSuperiorSkyblock2Flag)
                                                                it.hasSettingsEnabled(fjetpackReloadedSS2Flag).let flag@ {
                                                                    if (it) return@flag
                                                                    messages.superiorSkyblock2NoFlag.send(player)
                                                                    turnOff(noMessage = true)
                                                                    return@launch
                                                                }

                                                            // for permissions
                                                            if (!jetpack.bypassSuperiorSkyblock2Privilege)
                                                                if (!it.hasPermission(player, fjetpackReloadedSS2Privilege)) {
                                                                    messages.superiorSkyblock2NoPermission.send(player)
                                                                    turnOff(noMessage = true)
                                                                    return@launch
                                                                }
                                                        }
                                                        Unit
                                                    } ?: run {
                                                        stop = true
                                                        messages.superiorSkyblock2OutsideIsland.send(player)
                                                        turnOff(noMessage = true)
                                                        return@launch
                                                    }
                                                }
                                                if (stop)
                                                    break
                                                delay(jetpack.burnRate.toLong() * 1000L)
                                            }
                                        } ?: return@launch
                                        turnOff(jetpack)
                                    }

                                    if (jetpack.particleEffect.lowercase() != "none")
                                        safeRun {
                                            if (serverVersion > 8)
                                                Particle.valueOf(jetpack.particleEffect.uppercase().trim())
                                            else
                                                Effect.valueOf(jetpack.particleEffect.uppercase().trim())

                                            particleJob = launch {
                                                withSafe {
                                                    while (true) {
                                                        yield()
                                                        if (!(player as LivingEntity).isOnGround && isFlying && allowFlight && isOnline && listPlayerUse[this@playerFlying] != null)
                                                            world.withSafe {
                                                                if (serverVersion > 8) {
                                                                    val newZ = (0.1 * sin(
                                                                        Math.toRadians(
                                                                            (location.yaw + 270.0f).toDouble()
                                                                        )
                                                                    )).toFloat()
                                                                    jetpack.main {
                                                                        spawnParticle(
                                                                            Particle.valueOf(
                                                                                particleEffect.uppercase().trim()
                                                                            ),
                                                                            location.x + newZ,
                                                                            location.y + 0.8,
                                                                            location.z + newZ,
                                                                            particleAmount,
                                                                            0.0,
                                                                            -0.2,
                                                                            0.0
                                                                        )
                                                                    }
                                                                } else {
                                                                    playEffect(
                                                                        location.add(0.0, 0.8, 0.0),
                                                                        Effect.valueOf(
                                                                            jetpack.particleEffect.uppercase()
                                                                                .trim()
                                                                        ),
                                                                        0
                                                                    )
                                                                }
                                                            }
                                                        delay(jetpack.particleDelay)
                                                    }
                                                }
                                            }
                                        } ?: "&cInvalid Particle Effect!".send(this@player)

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
    fun onDamagedArmorPlayer(e: EntityDamageEvent) = e.withSafe {
        if (!mainContext.isActive) return
        val entity = entity as? Player ?: return
        val eq = entity.equipment ?: return
        val armors = eq.armorContents
        if (armors.isEmpty()) return
        val iterator = armors.iterator()
        while (iterator.hasNext()) {
            iterator.next().withSafe item@{
                if (this == null || type == Material.AIR) return@item
                jetpacks[get(ID_JETPACK)]?.let {
                    if (serverVersion > 16) {
                        itemMeta = itemMeta?.apply {
                            isUnbreakable = it.unbreakable
                        }
                    } else if (it.unbreakable) durability = 0.toShort()
                    eq.armorContents = eq.armorContents.toMutableList().update(this).toTypedArray()
                }
            }
        }
    }

    @EventHandler
    fun onPlayerDied(e: EntityDeathEvent) = e.withSafe {
        if (!mainContext.isActive) return
        if (entity.type != EntityType.PLAYER) return
        val p: Player = entity as Player

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
                            val id = get(ID_JETPACK)
                            if (id.isNotEmpty() && id == jp.id) {

                                when (jp.onDied) {
                                    OnDeath.Drop -> {
                                        p.equipment?.armorContents?.let { eqarmor ->
                                            var dropped = false
                                            val eqArmors = eqarmor.toMutableList()
                                            eqArmors.iterator().apply {
                                                while (hasNext())
                                                    next()?.let { item ->
                                                        if (item.get(ID_JETPACK) == get(ID_JETPACK)) {
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
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) = e.withSafe {
        if (this is InventoryCreativeEvent || !mainContext.isActive) return
        (whoClicked as Player).apply player@{
            cursor?.let { cursorItem ->
                currentItem?.let { slotItem ->
                    if (!slotItem.hasItemMeta() || slotItem.type == Material.AIR || slotItem.amount == 0) return
                    if (click != ClickType.LEFT && click != ClickType.RIGHT && click == ClickType.WINDOW_BORDER_LEFT && click == ClickType.WINDOW_BORDER_RIGHT)
                        return
                    jetpacks[slotItem.get(ID_JETPACK)]?.let { jetpack ->

                        if (slotType == InventoryType.SlotType.ARMOR)
                            listPlayerUse[asPlayerFlying()]?.let {
                                if (jetpack.id == it.id)
                                    turnOff(it)
                                else
                                    return
                            }


                        if (jetpack.fuel.startsWith(CUSTOM_FUEL_PREFIX)) {
                            val key = cursorItem.get(ID_CUSTOM_FUEL).replace(CUSTOM_FUEL_PREFIX, STRING_EMPTY)
                            customFuel[key]?.apply {
                                if (!hasPermission(permission)) {
                                    messages.noPerms.send(this@player)
                                    return
                                }
                            } ?: return
                        } else if (cursorItem.type != Material.valueOf(jetpack.fuel.uppercase().trim()))
                            return

                        if (!hasPermission(jetpack.permission) && !hasPermission("${jetpack.permission}.refuel")) {
                            messages.noPerms.send(this)
                            return
                        }

                        isCancelled = true

                        val addFuelAmount = if (isLeftClick) cursorItem.amount else 1

                        val fuel = slotItem.get(ID_FUEL_JETPACK).toLongSafe()

                        currentItem = slotItem.update((fuel + addFuelAmount).toString(), jetpack)
                        if (isLeftClick)
                            setItemOnCursor(ItemStack(Material.AIR))
                        else {
                            cursorItem.amount = cursorItem.amount - 1
                            setItemOnCursor(cursorItem)
                        }
                    }
                }
            }
        }
    }

}