package me.phantomx.fjetpackreloaded.nms

import kotlinx.coroutines.CancellationException
import me.phantomx.fjetpackreloaded.modules.Module.nmsAPIVersion
import me.phantomx.fjetpackreloaded.modules.Module.plugin
import me.phantomx.fjetpackreloaded.modules.Module.serverVersion
import me.phantomx.fjetpackreloaded.modules.Module.stringEmpty
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.lang.reflect.Method

class ItemMetaData {

    @Throws(Exception::class)
    fun setString(itemStack: ItemStack, key: String, value: String?): ItemStack {
        if (serverVersion > 17) {
            // Minimum server v1.14.x
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                persistentDataContainer.apply {
                    NamespacedKey(plugin, key).let {
                        value?.apply {
                            set(it, PersistentDataType.STRING, this)
                        } ?: remove(it)
                    }
                }
            }
            return itemStack
        }
        Class.forName("org.bukkit.craftbukkit.$nmsAPIVersion.inventory.CraftItemStack").let { craftItem ->
            craftItem.getMethod("asNMSCopy", ItemStack::class.java).apply {
                val cIS: Any = invoke(craftItem, itemStack)
                var nbt = cIS.javaClass.getMethod(if (serverVersion > 17) "s" else "getTag").invoke(cIS)
                if (nbt == null) {
                    nbt = Class.forName(
                        if (serverVersion > 16) "net.minecraft.nbt.NBTTagCompound" else "net.minecraft.server.$nmsAPIVersion.NBTTagCompound"
                    )
                    nbt.javaClass.newInstance()
                }
                nbt.apply {
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
    }

    fun setStringSafe(itemStack: ItemStack, key: String, value: String?): ItemStack {
        try {
            return setString(itemStack, key, value)
        } catch (ex: Exception) {
            if (ex is CancellationException) throw ex
            //ex.printStackTrace();
        }
        return itemStack
    }

    @Throws(Exception::class)
    fun getString(itemStack: ItemStack, key: String): String {
        if (serverVersion > 17) {
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                persistentDataContainer.apply {
                    NamespacedKey(plugin, key).apply {
                        return getOrDefault(this, PersistentDataType.STRING, stringEmpty)
                    }
                }
            }
            return stringEmpty
        }
        Class.forName("org.bukkit.craftbukkit.$nmsAPIVersion.inventory.CraftItemStack").let { craftItem ->
            val method: Method = craftItem.getMethod("asNMSCopy", ItemStack::class.java)
            val cIS: Any = method.invoke(craftItem, itemStack)
            val nbt = cIS.javaClass.getMethod(if (serverVersion > 17) "s" else "getTag").invoke(cIS)
                ?: return stringEmpty
            val r = nbt.javaClass.getMethod(if (serverVersion > 17) "l" else "getString", String::class.java)
                .invoke(nbt, key)
            val rr = r ?: stringEmpty
            return rr as String
        }
    }

    fun getStringSafe(itemStack: ItemStack, key: String): String {
        try {
            return getString(itemStack, key)
        } catch (ex: Exception) {
            if (ex is CancellationException) throw ex
            //ex.printStackTrace();
        }
        return stringEmpty
    }

    fun isNotItemArmor(itemStack: ItemStack): Boolean {
        try {
            val craftItem = Class.forName("org.bukkit.craftbukkit.$nmsAPIVersion.inventory.CraftItemStack")
            val method: Method = craftItem.getMethod("asNMSCopy", ItemStack::class.java)
            val cIS: Any = method.invoke(craftItem, itemStack)
            val itm = cIS.javaClass.getMethod(if (serverVersion > 17) "c" else "getItem").invoke(cIS)
            val isNotArmor = itm.javaClass.name != if (serverVersion > 16) "net.minecraft.world.item.ItemArmor" else
                "net.minecraft.server.$nmsAPIVersion.ItemArmor"
            if (isNotArmor)
                itemStack.type.apply {
                    if (this == Material.LEATHER_HELMET ||
                        this == Material.LEATHER_CHESTPLATE ||
                        this == Material.LEATHER_LEGGINGS ||
                        this == Material.LEATHER_BOOTS
                    )
                        return false
                }
            return isNotArmor
        } catch (ex: Exception) {
            if (ex is CancellationException) throw ex
            //ex.printStackTrace();
        }
        return true
    }
}















