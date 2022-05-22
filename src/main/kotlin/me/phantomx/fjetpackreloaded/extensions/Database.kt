package me.phantomx.fjetpackreloaded.extensions

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import me.phantomx.fjetpackreloaded.modules.Module.databaseDirectory
import me.phantomx.fjetpackreloaded.modules.Module.gson
import me.phantomx.fjetpackreloaded.modules.Module.stringEmpty
import java.io.*

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun getDatabase(database: String) = withContext(IO) {
    File(databaseDirectory, "$database.dat").safeRun {
        if (!databaseDirectory.exists() || !exists())
            stringEmpty
        else
            BufferedReader(FileReader(this)).use {
                it.readText()
            }
    } ?: stringEmpty
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun<T : Any> saveDatabase(database: String, data: T) = withContext(IO) {
    File(databaseDirectory, "$database.dat").withSafe {
        withSafe {
            parentFile.mkdirs()
            parentFile.mkdir()
        }
        BufferedWriter(FileWriter(this)).use {
            it.write(gson.toJson(data))
            it.flush()
        }
    }
}