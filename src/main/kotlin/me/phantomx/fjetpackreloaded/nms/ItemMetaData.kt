package me.phantomx.fjetpackreloaded.nms

import org.bukkit.inventory.ItemStack
import java.lang.reflect.Method

import me.phantomx.fjetpackreloaded.modules.Module.nmsServerVersion
import me.phantomx.fjetpackreloaded.modules.Module.serverVersion
import me.phantomx.fjetpackreloaded.modules.Module.stringEmpty
import org.bukkit.Material

class ItemMetaData {

    fun setString(itemStack: ItemStack, key: String, value: String?): ItemStack {
        try {
            Class.forName("org.bukkit.craftbukkit.$nmsServerVersion.inventory.CraftItemStack").let { craftItem ->
                craftItem.getMethod("asNMSCopy", ItemStack::class.java).apply {
                    val cIS: Any = invoke(craftItem, itemStack)
                    var nbt = cIS.javaClass.getMethod(if (serverVersion > 17) "s" else "getTag").invoke(cIS)
                    if (nbt == null) {
                        nbt = Class.forName(
                            if (serverVersion > 16) "net.minecraft.nbt.NBTTagCompound" else "net.minecraft.server.$nmsServerVersion.NBTTagCompound"
                        )
                        nbt.javaClass.newInstance()
                    }
                    nbt?.apply {
                        if (value == null) javaClass.getMethod(
                            if (serverVersion > 17) "r" else "remove",
                            String::class.java
                        ).invoke(this, key) else javaClass.getMethod(
                            if (serverVersion > 17) "a" else "setString",
                            String::class.java,
                            String::class.java
                        ).invoke(this, key, value)
                        cIS.javaClass.getMethod(if (serverVersion > 17) "c" else "setTag", javaClass).invoke(cIS, this)
                        return craftItem.getMethod("asBukkitCopy", cIS.javaClass).run {
                            invoke(craftItem, cIS) as ItemStack
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            //ex.printStackTrace();
        }
        return itemStack
    }

    fun getString(itemStack: ItemStack, key: String): String {
        try {
            Class.forName("org.bukkit.craftbukkit.$nmsServerVersion.inventory.CraftItemStack").let { craftItem ->
                val method: Method = craftItem.getMethod("asNMSCopy", ItemStack::class.java)
                val cIS: Any = method.invoke(craftItem, itemStack)
                val nbt = cIS.javaClass.getMethod(if (serverVersion > 17) "s" else "getTag").invoke(cIS)
                    ?: return stringEmpty
                val r = nbt.javaClass.getMethod(if (serverVersion > 17) "l" else "getString", String::class.java)
                    .invoke(nbt, key)
                val rr = r ?: stringEmpty
                return rr as String
            }
        } catch (ex: Exception) {
            //ex.printStackTrace();
        }
        return stringEmpty
    }

    fun isNotItemArmor(itemStack: ItemStack): Boolean {
        try {
            val craftItem = Class.forName("org.bukkit.craftbukkit.$nmsServerVersion.inventory.CraftItemStack")
            val method: Method = craftItem.getMethod("asNMSCopy", ItemStack::class.java)
            val cIS: Any = method.invoke(craftItem, itemStack)
            val itm = cIS.javaClass.getMethod(if (serverVersion > 17) "c" else "getItem").invoke(cIS)
            val isNotArmor = itm.javaClass.name != if (serverVersion > 16) "net.minecraft.world.item.ItemArmor" else
                "net.minecraft.server.$nmsServerVersion.ItemArmor"
            if (isNotArmor)
                itemStack.type.apply {
                    if (this == Material.LEATHER_HELMET ||
                        this == Material.LEATHER_CHESTPLATE ||
                        this == Material.LEATHER_LEGGINGS ||
                        this == Material.LEATHER_BOOTS)
                        return false
                }
            return isNotArmor
        } catch (ex: Exception) {
            //ex.printStackTrace();
        }
        return true
    }
}















