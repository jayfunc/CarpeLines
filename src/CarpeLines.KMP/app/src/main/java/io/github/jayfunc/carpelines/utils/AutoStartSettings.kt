package io.github.jayfunc.carpelines.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Whether this device runs a Xiaomi ROM. MIUI/HyperOS gate the system's rebind of a notification
 * listener behind the app's auto-start switch, so granted notification access alone is not enough
 * after the app was force-stopped.
 */
internal fun isMiuiFamilyDevice(
    manufacturer: String = Build.MANUFACTURER,
    brand: String = Build.BRAND,
): Boolean {
    val manufacturerName = manufacturer.trim().lowercase()
    val brandName = brand.trim().lowercase()
    return manufacturerName == "xiaomi" ||
        brandName == "xiaomi" ||
        brandName == "redmi" ||
        brandName == "poco"
}

/** Opens the MIUI/HyperOS auto-start screen, falling back to the app's system details page. */
internal fun openAutoStartSettings(context: Context) {
    val autoStartIntent = Intent().apply {
        component = ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (runCatching { context.startActivity(autoStartIntent) }.isSuccess) return

    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
