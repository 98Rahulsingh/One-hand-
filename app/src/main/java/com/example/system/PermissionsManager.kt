package com.example.system

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.service.MediaListenerService
import com.example.service.PanelAccessibilityService

/** Reads the REAL state of every permission and switches the ones Android allows in place. */
class PermissionsManager(private val ctx: Context) {
    private val cr get() = ctx.contentResolver
    private val a11y = ComponentName(ctx, PanelAccessibilityService::class.java)
    private val listener = ComponentName(ctx, MediaListenerService::class.java)
    private val listenersKey = "enabled_notification_listeners"

    // ---- real state ----
    fun hasSecureSettings() =
        ctx.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

    private fun enabledList(key: String): List<String> =
        (Settings.Secure.getString(cr, key) ?: "").split(':').filter { it.isNotBlank() }

    fun isAccessibilityOn() = enabledList(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        .any { ComponentName.unflattenFromString(it) == a11y }

    fun isListenerOn() = enabledList(listenersKey).any { ComponentName.unflattenFromString(it) == listener }

    fun notificationsAllowed() = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun hasUsageAccess(): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Xiaomi/HyperOS extra permissions (numbered app-ops). Unknown on other phones, so treated as allowed. */
    private fun miuiOp(code: Int): Boolean = try {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val m = AppOpsManager::class.java.getMethod(
            "checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java
        )
        (m.invoke(ops, code, Process.myUid(), ctx.packageName) as Int) == AppOpsManager.MODE_ALLOWED
    } catch (_: Throwable) {
        true
    }

    fun hasBgStart() = miuiOp(10021)          // Open new windows while running in the background
    fun hasLockScreenOp() = miuiOp(10020)     // Show on lock screen
    fun hasShortcutsOp() = miuiOp(10017)      // Home screen shortcuts
    fun hasAutostart() = miuiOp(10008)        // Autostart (Xiaomi)

    /** "Pause app activity if unused" must be OFF. */
    fun hasNoPause(): Boolean = try {
        if (Build.VERSION.SDK_INT >= 30) ctx.packageManager.isAutoRevokeWhitelisted else true
    } catch (_: Throwable) {
        true
    }

    fun canDrawOverlay() = Settings.canDrawOverlays(ctx)
    fun canWriteSettings() = Settings.System.canWrite(ctx)
    fun isBatteryExempt() =
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)

    fun isFreeformSupportOn() = Settings.Global.getInt(cr, "enable_freeform_support", 0) == 1
    fun isForceResizableOn() = Settings.Global.getInt(cr, "force_resizable_activities", 0) == 1

    /** Lets this old-target app call the window APIs that floating windows need (applies to apps built for Android 9 or older only). */
    private fun relaxHiddenApi(on: Boolean) {
        if (!on) return
        try {
            if (Settings.Global.getInt(cr, "hidden_api_policy_p_apps", -1) != 1) Settings.Global.putInt(cr, "hidden_api_policy_p_apps", 1)
            if (Settings.Global.getInt(cr, "hidden_api_policy_pre_p_apps", -1) != 1) Settings.Global.putInt(cr, "hidden_api_policy_pre_p_apps", 1)
        } catch (_: SecurityException) {}
    }

    /** Called when the app starts. The phone applies this setting the next time the app process starts. */
    fun ensureHiddenApiRelaxed() {
        if (hasSecureSettings()) relaxHiddenApi(true)
    }

    fun hiddenApiSetting(): String =
        Settings.Global.getInt(cr, "hidden_api_policy_p_apps", -1).toString() + "/" +
            Settings.Global.getInt(cr, "hidden_api_policy_pre_p_apps", -1).toString()

    fun isFreeformOn() =
        Settings.Global.getInt(cr, "enable_freeform_support", 0) == 1 &&
            Settings.Global.getInt(cr, "force_resizable_activities", 0) == 1

    // ---- in-place switching (needs WRITE_SECURE_SETTINGS, granted once) ----
    private fun setComponent(key: String, c: ComponentName, on: Boolean) {
        val list = enabledList(key).filterNot { ComponentName.unflattenFromString(it) == c }.toMutableList()
        if (on) list.add(c.flattenToString())
        Settings.Secure.putString(cr, key, list.joinToString(":"))
        if (key == Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) {
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, if (list.isEmpty()) 0 else 1)
        }
    }

    /** Returns true if the switch was flipped right here, false if Android needs another route. */
    fun setInPlace(type: String, on: Boolean): Boolean {
        if (!hasSecureSettings()) return false
        return try {
            when (type) {
                "accessibility" -> setComponent(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, a11y, on)
                "media_controls" -> setComponent(listenersKey, listener, on)
                "freeform_support" -> {
                    val v = if (on) 1 else 0
                    Settings.Global.putInt(cr, "enable_freeform_support", v)
                    Settings.Global.putInt(cr, "freeform_window_management", v)
                    relaxHiddenApi(on)
                }
                "force_resizable" -> {
                    Settings.Global.putInt(cr, "force_resizable_activities", if (on) 1 else 0)
                    relaxHiddenApi(on)
                }
                "freeform" -> {
                    relaxHiddenApi(on)
                    val v = if (on) 1 else 0
                    Settings.Global.putInt(cr, "enable_freeform_support", v)
                    Settings.Global.putInt(cr, "force_resizable_activities", v)
                    Settings.Global.putInt(cr, "freeform_window_management", v)
                }
                else -> return false
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }

    // ---- fallback screens and guide text ----
    fun systemScreen(type: String): Intent? {
        val uri = Uri.parse("package:${ctx.packageName}")
        val i = when (type) {
            "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            "media_controls" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            "usage_access" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            "floating_window" -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
            "write_settings" -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, uri)
            "battery_optimization" -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, uri)
            "notifications" -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            "freeform", "freeform_support", "force_resizable" -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            "bg_start", "lock_screen", "shortcuts" -> miuiEditor() ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            "autostart" -> pick(Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
                ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            "no_pause" -> pick(Intent("android.intent.action.AUTO_REVOKE_PERMISSIONS").setData(uri))
                ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            else -> return null
        }
        return i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun pick(i: Intent): Intent? = if (i.resolveActivity(ctx.packageManager) != null) i else null

    private fun miuiEditor(): Intent? {
        val i = Intent("miui.intent.action.APP_PERM_EDITOR")
            .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            .putExtra("extra_pkgname", ctx.packageName)
        return if (i.resolveActivity(ctx.packageManager) != null) i else null
    }

    fun setupCommands(): String {
        val p = ctx.packageName
        return "adb shell pm grant $p android.permission.WRITE_SECURE_SETTINGS\n" +
            "adb shell pm grant $p android.permission.POST_NOTIFICATIONS\n" +
            "adb shell appops set $p GET_USAGE_STATS allow\n" +
            "adb shell appops set $p SYSTEM_ALERT_WINDOW allow\n" +
            "adb shell appops set $p WRITE_SETTINGS allow\n" +
            "adb shell settings put global enable_freeform_support 1\n" +
            "adb shell settings put global force_resizable_activities 1\n" +
            "adb shell appops set $p 10021 allow\n" +
            "adb shell appops set $p 10020 allow\n" +
            "adb shell appops set $p 10017 allow\n" +
            "adb shell appops set $p 10008 allow\n" +
            "adb shell appops set $p AUTO_REVOKE_PERMISSIONS_IF_UNUSED ignore\n" +
            "adb shell dumpsys deviceidle whitelist +$p"
    }

    fun setupGuide(): String =
        "One-time setup. Connect the phone to a PC (USB debugging or Wireless debugging) and run:\n\n" +
            setupCommands() +
            "\n\n(POST_NOTIFICATIONS is Android 13+ only and the 10021/10020/10017 lines are for Xiaomi phones; an error on other phones is fine.)\n\n" +
            "After this, Accessibility, Media access and Floating apps switch right here with no redirect, " +
            "and every other permission shows its real status."

    fun title(type: String) = when (type) {
        "accessibility" -> "Accessibility"
        "media_controls" -> "Media controls"
        "notifications" -> "Notifications"
        "usage_access" -> "Usage access"
        "floating_window" -> "Floating window permission"
        "freeform" -> "Floating apps (freeform windows)"
        "write_settings" -> "Modify system settings"
        "bg_start" -> "Open new windows in background"
        "lock_screen" -> "Show on lock screen"
        "shortcuts" -> "Home screen shortcuts"
        "autostart" -> "Autostart"
        "freeform_support" -> "Enable freeform windows"
        "force_resizable" -> "Force activities to be resizable"
        "no_pause" -> "Pause app activity if unused: OFF"
        "battery_optimization" -> "Battery optimization"
        else -> "System controls"
    }

    fun guideText(type: String): String =
        "Android does not allow this switch to be flipped from inside the app until the one-time setup is done.\n\n" + setupGuide()
}
