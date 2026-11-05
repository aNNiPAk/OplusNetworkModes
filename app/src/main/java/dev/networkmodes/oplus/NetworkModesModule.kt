package dev.networkmodes.oplus

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.ListAdapter
import android.widget.ListView
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.concurrent.thread

class NetworkModesModule : XposedModule() {
    private var resolutionStarted = false
    private lateinit var processName: String

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
        log(Log.INFO, TAG, "Modern API 102 module loaded in $processName")
    }

    @Synchronized
    override fun onPackageReady(param: PackageReadyParam) {
        if (resolutionStarted || param.packageName != PHONE_PACKAGE) return
        resolutionStarted = true
        thread(name = "OplusNetworkModes-resolver", isDaemon = true) {
            val loader = param.classLoader
            val profile = try {
                val resolved = DexKitProfileResolver.resolve(param.applicationInfo, loader, processName)
                log(Log.INFO, TAG, "Resolved firmware with DexKit")
                resolved
            } catch (failure: Throwable) {
                log(Log.WARN, TAG, "DexKit resolution failed; trying known profiles", failure)
                try {
                    findProfile(Class.forName(ACTIVITY_CLASS, false, loader))
                } catch (fallbackFailure: Throwable) {
                    log(Log.ERROR, TAG, "No compatible firmware profile", fallbackFailure)
                    return@thread
                }
            }
            installListHook(loader, profile)
            installSummaryHook(loader, profile)
        }
    }

    private fun installListHook(loader: ClassLoader, profile: FirmwareProfile) {
        try {
            val activity = Class.forName(profile.activityClass, false, loader)
            val bindings = ListBindings(activity, profile)
            hook(activity.getDeclaredMethod("onCreate", Bundle::class.java))
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed()
                    val target = chain.thisObject
                    if (target is Activity) patchList(target, bindings)
                    result
                }
            log(Log.INFO, TAG, "Hook installed for ${profile.activityClass}")
        } catch (failure: Throwable) {
            log(Log.ERROR, TAG, "Unable to install preferred-network hook", failure)
        }
    }

    private fun installSummaryHook(loader: ClassLoader, profile: FirmwareProfile) {
        try {
            val adapter = Class.forName(profile.platformAdapterClass, false, loader)
            val summary = adapter.getDeclaredMethod(profile.summaryMethod, Int::class.java, Context::class.java)
            val getMode = findMethod(adapter, profile.summaryModeMethod, Int::class.java, Context::class.java)
            hook(summary).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { chain ->
                val result = chain.proceed()
                if (chain.args.size < 2) return@intercept result
                try {
                    val target = chain.thisObject ?: return@intercept result
                    val slotId = chain.getArg(0) as Int
                    val context = chain.getArg(1) as Context? ?: return@intercept result
                    val mode = getMode.invoke(target, slotId, context) as Int
                    NetworkMode.labelForValue(mode, localeFor(context)) ?: result
                } catch (failure: Throwable) {
                    Log.e(TAG, "Unable to patch network summary", failure)
                    result
                }
            }
            log(Log.INFO, TAG, "Summary hook installed for ${profile.name}")
        } catch (failure: Throwable) {
            log(Log.ERROR, TAG, "Unable to install network-summary hook", failure)
        }
    }

    private fun patchList(activity: Activity, bindings: ListBindings) {
        try {
            val labels = NetworkMode.labels(localeFor(activity))
            val modes = NetworkMode.networkTypes()
            val resources = activity.resources
            val rowLayout = resources.getIdentifier("network_mode_item_options", "layout", PHONE_PACKAGE)
            val textId = resources.getIdentifier("text", "id", PHONE_PACKAGE)
            if (rowLayout == 0 || textId == 0) {
                throw Resources.NotFoundException("Network-mode row resources not found")
            }
            val adapter = bindings.adapter.newInstance(activity, activity, rowLayout, textId, labels)
            val list = bindings.list.get(activity)
            check(list is ListView && adapter is ListAdapter) { "Unexpected network-mode list or adapter" }
            val slotId = bindings.slot.getInt(activity)
            val currentMode = bindings.currentMode.invoke(activity, slotId) as Int
            val fields = bindings.uiFields
            val previous = fields.map { it.get(activity) }
            val previousAdapter = list.adapter
            val previousChecked = list.checkedItemPosition
            val selectedIndex: Int
            try {
                fields[0].set(activity, labels)
                fields[1].set(activity, modes)
                // Preserve the OEM's radio-family normalization and selection behavior.
                selectedIndex = bindings.selectedIndex.invoke(activity, currentMode) as Int
                fields[2].set(activity, selectedIndex)
                fields[3].set(activity, adapter)
                list.adapter = adapter
                list.clearChoices()
                if (selectedIndex in labels.indices) list.setItemChecked(selectedIndex, true)
            } catch (failure: Throwable) {
                for (i in fields.indices) {
                    try {
                        fields[i].set(activity, previous[i])
                    } catch (rollbackFailure: Throwable) {
                        failure.addSuppressed(rollbackFailure)
                    }
                }
                try {
                    list.adapter = previousAdapter
                    list.clearChoices()
                    if (previousChecked >= 0) list.setItemChecked(previousChecked, true)
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
                throw failure
            }
            Log.i(TAG, "List patched; currentMode=$currentMode, selectedIndex=$selectedIndex")
        } catch (failure: Throwable) {
            Log.e(TAG, "Unable to patch preferred-network list", failure)
        }
    }

    private fun findProfile(activity: Class<*>): FirmwareProfile {
        for (profile in PROFILES) {
            try {
                val adapter = Class.forName(profile.adapterClass, false, activity.classLoader)
                if (!ListView::class.java.isAssignableFrom(activity.getDeclaredField(profile.listField).type)
                    || activity.getDeclaredField(profile.slotField).type != Int::class.java
                    || activity.getDeclaredField(profile.labelsField).type != Array<String>::class.java
                    || activity.getDeclaredField(profile.modesField).type != Array<Int>::class.java
                    || activity.getDeclaredField(profile.indexField).type != Int::class.java
                    || activity.getDeclaredField(profile.adapterField).type != adapter
                    || activity.getDeclaredMethod(profile.currentModeMethod, Int::class.java).returnType != Int::class.java
                    || activity.getDeclaredMethod(profile.selectedIndexMethod, Int::class.java).returnType != Int::class.java
                ) continue
                adapter.getDeclaredConstructor(activity, activity, Int::class.java, Int::class.java,
                    Array<CharSequence>::class.java)
                return profile
            } catch (_: ReflectiveOperationException) {
                // Try the next known contract before changing the stock UI.
            }
        }
        throw NoSuchMethodException("Unsupported Oplus preferred-network screen")
    }

    /** Resolve reflection once, before installing the hook. */
    private class ListBindings(activity: Class<*>, profile: FirmwareProfile) {
        val adapter = Class.forName(profile.adapterClass, false, activity.classLoader)
            .getDeclaredConstructor(activity, activity, Int::class.java, Int::class.java,
                Array<CharSequence>::class.java).apply { isAccessible = true }
        val list = findField(activity, profile.listField)
        val slot = findField(activity, profile.slotField)
        val uiFields = arrayOf(
            findField(activity, profile.labelsField), findField(activity, profile.modesField),
            findField(activity, profile.indexField), findField(activity, profile.adapterField),
        )
        val currentMode = findMethod(activity, profile.currentModeMethod, Int::class.java)
        val selectedIndex = findMethod(activity, profile.selectedIndexMethod, Int::class.java)
    }

    private companion object {
        const val TAG = "OplusNetworkModes"
        const val PHONE_PACKAGE = "com.android.phone"
        const val ACTIVITY_CLASS = "com.android.simsettings.network.OplusExportPreferredNetworkSettings"
        const val PLATFORM_ADAPTER_CLASS = "com.android.simsettings.platformadaptor.d"
        val PROFILES = listOf(
            FirmwareProfile("OxygenOS 17", ACTIVITY_CLASS, PLATFORM_ADAPTER_CLASS,
                "u5.b", "f", "c", "h", "g", "u", "x"),
            FirmwareProfile("OxygenOS 16", ACTIVITY_CLASS, PLATFORM_ADAPTER_CLASS,
                "g6.c", "g", "d", "w", "v", "z", "D"),
        )

        fun findField(type: Class<*>, name: String): Field =
            type.getDeclaredField(name).apply { isAccessible = true }

        fun findMethod(type: Class<*>, name: String, vararg parameters: Class<*>): Method =
            type.getDeclaredMethod(name, *parameters).apply { isAccessible = true }

        fun localeFor(context: Context) = context.resources.configuration.locales[0]
    }
}
