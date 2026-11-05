package dev.networkmodes.oplus

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.ListAdapter
import android.widget.ListView
import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.DexKitCacheBridge.RecyclableBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.matchers.FieldMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher
import java.io.File
import java.security.MessageDigest

/** Resolve and validate the firmware contract without changing UI state. */
@OptIn(DexKitExperimentalApi::class)
internal object DexKitProfileResolver {
    private var cache: DexKitResultCache? = null

    @Synchronized
    fun resolve(info: ApplicationInfo, loader: ClassLoader, processName: String): FirmwareProfile {
        val resultCache = cache ?: initializeCache(info, processName)
        val tag = "oplus-network-modes:v1:${sha256(info.sourceDir)}"
        try {
            return DexKitCacheBridge.create(tag, info.sourceDir).use { resolveProfile(it, loader) }
        } finally {
            resultCache.flush()
        }
    }

    private fun initializeCache(info: ApplicationInfo, processName: String): DexKitResultCache {
        System.loadLibrary("dexkit")
        val base = File(info.deviceProtectedDataDir ?: info.dataDir)
        val result = DexKitResultCache(File(base, "cache/oplus-network-modes"), processName)
        DexKitCacheBridge.init(result)
        cache = result
        return result
    }

    private fun resolveProfile(bridge: RecyclableBridge, loader: ClassLoader): FirmwareProfile {
        val currentDex = bridge.getMethods("current-mode", methodQuery(
            null, "getOplusUserPreferredNetworkFromDb,preferredNetworkMode=", "int", "int",
        )).only("current mode")
        val activityName = currentDex.className
        val activity = Class.forName(activityName, false, loader)
        check(Activity::class.java.isAssignableFrom(activity)) { "Resolved activity has unexpected type" }

        val summaryDex = bridge.getMethods("summary", methodQuery(
            null, "getNetworkTypeString null", "java.lang.String", "int", "android.content.Context",
        )).only("summary")
        val summaryModeDex = bridge.getMethods("summary-mode", methodQuery(
            summaryDex.className, "getPreferredNetworkTypeFromPhone: networkType =",
            "int", "int", "android.content.Context",
        )).only("summary mode")
        val summaryDescriptor = descriptor(summaryDex.className,
            "${summaryDex.name}(ILandroid/content/Context;)Ljava/lang/String;")
        val modeDescriptor = descriptor(summaryModeDex.className,
            "${summaryModeDex.name}(ILandroid/content/Context;)I")
        bridge.getMethods("summary-mode-link", FindMethod.create().matcher(
            MethodMatcher.create().descriptor(summaryDescriptor).addInvoke(modeDescriptor),
        )).only("summary-to-mode relationship")

        val current = currentDex.getMethodInstance(loader)
        val summary = summaryDex.getMethodInstance(loader)
        val summaryMode = summaryModeDex.getMethodInstance(loader)
        val create = descriptor(activityName, "onCreate(Landroid/os/Bundle;)V")
        val resume = descriptor(activityName, "onResume()V")
        val selection = bridge.getMethods("selection", FindMethod.create().matcher(
            MethodMatcher.create().declaredClass(activityName).returnType("int").paramTypes("int")
                .addUsingField(FieldMatcher.create().declaredClass(activityName).type("java.lang.Integer[]")),
        )).only("selection").getMethodInstance(loader)
        val slot = bridge.getFields("slot", FindField.create().matcher(
            FieldMatcher.create().declaredClass(activityName).type("int")
                .addWriteMethod(create).addReadMethod(resume),
        )).only("slot").getFieldInstance(loader)
        val index = bridge.getFields("written-ints", FindField.create().matcher(
            FieldMatcher.create().declaredClass(activityName).type("int").addWriteMethod(create),
        )).map { it.getFieldInstance(loader) }.filter { it != slot }.only("selected index")

        val fields = activity.declaredFields
        val labels = fields.filter { it.type == Array<String>::class.java }.only("labels")
        val modes = fields.filter { it.type == Array<Int>::class.java }.only("modes")
        val list = fields.filter { ListView::class.java.isAssignableFrom(it.type) }.only("list")
        val adapter = fields.filter { isAdapter(it.type, activity) }.only("adapter")
        activity.getDeclaredMethod("onCreate", Bundle::class.java)
        check(summary.declaringClass == summaryMode.declaringClass
            && current.declaringClass == activity && selection.declaringClass == activity
            && summary.returnType == String::class.java && summaryMode.returnType == Int::class.java) {
            "Inconsistent DexKit results"
        }
        return FirmwareProfile(
            name = "DexKit", activityClass = activityName,
            platformAdapterClass = summary.declaringClass.name, adapterClass = adapter.type.name,
            listField = list.name, slotField = slot.name,
            currentModeMethod = current.name, selectedIndexMethod = selection.name,
            summaryMethod = summary.name, summaryModeMethod = summaryMode.name,
            labelsField = labels.name, modesField = modes.name,
            indexField = index.name, adapterField = adapter.name,
        )
    }

    private fun isAdapter(type: Class<*>, activity: Class<*>): Boolean {
        if (!ListAdapter::class.java.isAssignableFrom(type)) return false
        return try {
            type.getDeclaredConstructor(activity, activity, Int::class.java, Int::class.java,
                Array<CharSequence>::class.java)
            true
        } catch (_: NoSuchMethodException) {
            false
        }
    }

    private fun methodQuery(clazz: String?, anchor: String, result: String, vararg args: String): FindMethod {
        val matcher = MethodMatcher.create().returnType(result).paramTypes(*args).usingStrings(anchor)
        if (clazz != null) matcher.declaredClass(clazz)
        return FindMethod.create().searchPackages("com.android.simsettings").matcher(matcher)
    }

    private fun descriptor(clazz: String, method: String) = "L${clazz.replace('.', '/')};->$method"

    private fun <T> List<T>.only(name: String): T {
        check(size == 1) { "Expected one $name, found $size" }
        return first()
    }

    private fun sha256(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        File(path).inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                digest.update(buffer, 0, count)
            }
        }
        return buildString(64) {
            for (byte in digest.digest()) {
                append(Character.forDigit((byte.toInt() and 0xff) ushr 4, 16))
                append(Character.forDigit(byte.toInt() and 0x0f, 16))
            }
        }
    }
}
