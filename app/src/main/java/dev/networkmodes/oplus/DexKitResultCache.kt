package dev.networkmodes.oplus

import android.util.AtomicFile
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import java.io.File
import java.io.FileOutputStream
import java.util.Properties

/** Process-local cache; unavailable storage falls back to memory. */
@OptIn(DexKitExperimentalApi::class)
internal class DexKitResultCache(directory: File, processName: String) : DexKitCacheBridge.Cache {
    private val values = Properties()
    private var dirty = false
    private val file: AtomicFile?

    init {
        directory.mkdirs()
        file = if (directory.isDirectory) {
            val key = Base64.encodeToString(processName.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            AtomicFile(File(directory, "dexkit-cache-$key.properties"))
        } else {
            Log.w(TAG, "DexKit cache directory unavailable; using memory only")
            null
        }
        try {
            file?.openRead()?.use { values.load(it) }
        } catch (_: Exception) {
            values.clear()
        }
    }

    @Synchronized
    override fun getString(key: String, default: String?): String? =
        values.getProperty("s:$key", default)

    @Synchronized
    override fun putString(key: String, value: String) = put("s:$key", value)

    @Synchronized
    override fun getStringList(key: String, default: List<String>?): List<String>? {
        val value = values.getProperty("l:$key") ?: return default
        return try {
            val array = JSONArray(value)
            List(array.length()) { array.getString(it) }
        } catch (_: Exception) {
            default
        }
    }

    @Synchronized
    override fun putStringList(key: String, value: List<String>) = put("l:$key", JSONArray(value).toString())

    @Synchronized
    override fun remove(key: String) {
        val stringRemoved = values.remove("s:$key") != null
        val listRemoved = values.remove("l:$key") != null
        dirty = dirty || stringRemoved || listRemoved
    }

    @Synchronized
    override fun getAllKeys(): Collection<String> = values.stringPropertyNames()
        .filter { it.startsWith("s:") || it.startsWith("l:") }.map { it.substring(2) }.toSet()

    @Synchronized
    override fun clearAll() {
        dirty = dirty || values.isNotEmpty()
        values.clear()
    }

    private fun put(key: String, value: String) {
        val previous = values.setProperty(key, value)
        dirty = dirty || previous != value
    }

    /** Write once after resolution; cache hits never rewrite the file. */
    @Synchronized
    fun flush() {
        val storage = file ?: return
        if (!dirty) return
        var output: FileOutputStream? = null
        try {
            output = storage.startWrite()
            values.store(output, "DexKit query cache")
            storage.finishWrite(output)
            dirty = false
        } catch (failure: Exception) {
            if (output != null) storage.failWrite(output)
            Log.w(TAG, "Unable to persist DexKit query cache", failure)
        }
    }

    private companion object {
        const val TAG = "OplusNetworkModes"
    }
}
