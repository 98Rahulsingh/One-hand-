package com.example.system

import com.example.data.FloatingStore
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import com.example.service.TapTarget
import com.example.model.AudioOutputDevice
import android.media.AudioDeviceInfo
import kotlin.math.roundToInt
import android.app.ActivityOptions
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.accessibilityservice.AccessibilityService
import com.example.service.PanelAccessibilityService
import com.example.service.MiniMacroTrace

data class ActionResult(val message: String, val newState: Boolean? = null, val needs: String? = null)

/** Real quick-setting actions. Anything Android blocks reports it instead of pretending. */
class QuickActions(private val ctx: Context, private val perms: PermissionsManager) {
    private val cr get() = ctx.contentResolver
    private val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var torchOn = false
    var onTorchChanged: ((Boolean) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null
    /** tile id -> label of the quick-settings icon the user picked with "Track icon" */
    var targets: Map<String, TapTarget> = emptyMap()
    var nameOf: (String) -> String = { it }
    private var hotspotOn = false
    private var freeformWarned = false
    private var lastLaunch = "none yet"
    private var lastTile = "none yet"
    private var activeMacroRunId: String? = null

    init {
        try {
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    torchOn = enabled
                    onTorchChanged?.invoke(enabled)
                }
            }, Handler(Looper.getMainLooper()))
        } catch (_: Exception) {}
    }

    private fun torchId(): String? = cm.cameraIdList.firstOrNull {
        cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    @Suppress("DEPRECATION")
    fun currentStates(): Map<String, Boolean> {
        val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return mapOf(
            "torch" to torchOn,
            "rotate" to (Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) == 1),
            "darkmode" to (Settings.Secure.getInt(cr, "ui_night_mode", 1) == 2),
            "battery_saver" to (Settings.Global.getInt(cr, "low_power", 0) == 1),
            "wifi" to wifi.isWifiEnabled,
            "bluetooth" to (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true),
            "airplane" to (Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) == 1),
            "mute" to (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL),
            "floating" to perms.isFreeformOn(),
            "data" to (Settings.Global.getInt(cr, "mobile_data", 1) == 1),
            "hotspot" to hotspotOn
        )
    }

    private val audioMgr get() = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun volumePercent(): Float {
        val max = audioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioMgr.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / max
    }

    fun setVolumePercent(p: Float) {
        try {
            val max = audioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioMgr.setStreamVolume(AudioManager.STREAM_MUSIC, (p / 100f * max).roundToInt().coerceIn(0, max), 0)
        } catch (_: Exception) {}
    }

    fun brightnessPercent(): Float = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128) * 100f / 255f

    /** Returns false when "Modify system settings" is not allowed yet. */
    fun setBrightnessPercent(p: Float): Boolean {
        if (!perms.canWriteSettings()) return false
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, (p / 100f * 255f).roundToInt().coerceIn(1, 255))
        return true
    }

    fun outputDevices(): List<AudioOutputDevice> {
        val kinds = setOf(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_HDMI
        )
        val devs = audioMgr.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { it.type in kinds }
        val inUse = devs.firstOrNull { it.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } ?: devs.firstOrNull()
        return devs.map {
            val builtin = it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            val bt = it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            AudioOutputDevice(
                it.id.toString(),
                if (builtin) "Phone Speaker" else it.productName.toString(),
                if (bt) "Bluetooth" else if (builtin) "Internal Audio" else "Connected",
                it.id == inUse?.id,
                if (it.type == AudioDeviceInfo.TYPE_HDMI) "tv" else if (builtin) "speaker" else "headphones"
            )
        }
    }

    fun beginMacroRun(id: String): String {
        val runId = MiniMacroTrace.begin(ctx, nameOf(id), targets[id])
        activeMacroRunId = runId
        MiniMacroTrace.record(ctx, runId, "ACCESSIBILITY_CHECK", if (perms.isAccessibilityOn() && PanelAccessibilityService.instance != null) "PASS" else "FAILED", "Accessibility enabled=${perms.isAccessibilityOn()}, service running=${PanelAccessibilityService.instance != null}")
        return runId
    }

    fun perform(id: String, existingRunId: String? = null): ActionResult {
        val connectivity = id in setOf("wifi", "bluetooth", "data", "hotspot", "airplane") || targets.containsKey(id)
        activeMacroRunId = if (connectivity) existingRunId ?: beginMacroRun(id) else null
        val result = performInner(id)
        lastTile = "$id: ${result.message}"
        activeMacroRunId?.let { MiniMacroTrace.record(ctx, it, "ACTION_DISPATCH", if (result.needs == "accessibility") "FAILED" else "INFO", result.message) }
        return result
    }

    private fun performInner(id: String): ActionResult = try {
        val tapped = targets[id]
        if (tapped != null) viaShade(nameOf(id), listOfNotNull(tapped.label.takeIf { it.isNotBlank() }), tapped) else when (id) {
            "torch" -> torch()
            "rotate" -> rotate()
            "darkmode" -> secureFlip("Dark mode", "ui_night_mode", off = 1, on = 2, global = false)
            "battery_saver" -> secureFlip("Battery saver", "low_power", off = 0, on = 1, global = true)
            "mute" -> mute()
            "wifi" -> wifi()
            "bluetooth" -> bluetooth()
            "airplane" -> airplane()
            "data" -> mobileData()
            "hotspot" -> viaShade("Hotspot", listOf("Hotspot", "Tethering", "Portable")) { hotspotOn = !hotspotOn }
            "split" -> splitScreen()
            "prev_app" -> previousApp()
            "floating" -> floating()
            "lock" -> if (Build.VERSION.SDK_INT >= 28) global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, "Lock screen") else ActionResult("Needs Android 9 or newer")
            "screenshot" -> if (Build.VERSION.SDK_INT >= 28) global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, "Screenshot") else ActionResult("Needs Android 9 or newer")
            "notifications" -> global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "Notifications")
            "home" -> global(AccessibilityService.GLOBAL_ACTION_HOME, "Home")
            "last_one" -> {
                val svc = PanelAccessibilityService.instance ?: return ActionResult("Last One needs Accessibility", null, "accessibility")
                var dispatched = false
                svc.tapSavedBackTarget(activeMacroRunId) { ok ->
                    dispatched = ok
                    if (!ok) onMessage?.invoke("Last One: set its Back position in Add Item > Tracker first")
                }
                ActionResult(if (dispatched) "Last One: Back tap sent" else "Last One: Back tap starting", null)
            }
            "back" -> global(AccessibilityService.GLOBAL_ACTION_BACK, "Back")
            "power" -> global(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG, "Power menu")
            else -> ActionResult("Not supported yet")
        }
    } catch (e: SecurityException) {
        ActionResult("Android blocked this: ${e.message}")
    } catch (e: Exception) {
        ActionResult("Could not do that: ${e.message}")
    }

    private fun torch(): ActionResult {
        val id = torchId() ?: return ActionResult("No flashlight on this phone")
        val want = !torchOn
        cm.setTorchMode(id, want)
        torchOn = want
        return ActionResult("Flashlight ${if (want) "ON" else "OFF"}", want)
    }

    private fun rotate(): ActionResult {
        if (!perms.canWriteSettings()) return ActionResult("Allow 'Modify system settings' first", null, "write_settings")
        val on = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, if (on) 0 else 1)
        return ActionResult("Auto-rotate ${if (on) "OFF" else "ON"}", !on)
    }

    private fun secureFlip(name: String, key: String, off: Int, on: Int, global: Boolean): ActionResult {
        if (!perms.hasSecureSettings()) return ActionResult("$name needs the one-time setup", null, "system_control")
        val isOn = if (global) Settings.Global.getInt(cr, key, off) == on else Settings.Secure.getInt(cr, key, off) == on
        val target = if (isOn) off else on
        if (global) Settings.Global.putInt(cr, key, target) else Settings.Secure.putInt(cr, key, target)
        return ActionResult("$name ${if (isOn) "OFF" else "ON"}", !isOn)
    }

    private fun mute(): ActionResult {
        val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val normal = audio.ringerMode == AudioManager.RINGER_MODE_NORMAL
        audio.ringerMode = if (normal) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_NORMAL
        return ActionResult(if (normal) "Vibrate mode" else "Sound ON", !normal)
    }

    /** Opens the quick settings shade, taps the tile for [name] and closes the shade again. */
    private fun viaShade(
        name: String,
        labels: List<String>,
        target: TapTarget? = null,
        onOk: () -> Unit = {}
    ): ActionResult {
        val svc = PanelAccessibilityService.instance
        if (svc == null || !perms.isAccessibilityOn()) {
            activeMacroRunId?.let { MiniMacroTrace.record(ctx, it, "ACCESSIBILITY_CHECK", "FAILED", "Service missing or Accessibility switch OFF") }
            return ActionResult("$name needs Accessibility", null, "accessibility")
        }
        val runId = activeMacroRunId
        svc.tapQuickTile(labels, target, runId) { ok ->
            lastTile = "$name: " + (if (ok) "icon pressed" else "icon NOT found or press failed")
            MiniMacroTrace.record(ctx, runId, "MACRO_RESULT", if (ok) "PASS" else "FAILED", lastTile)
            if (ok) onOk() else onMessage?.invoke("Could not press the $name icon. Open Quick tiles, tap Track icon and press it once")
        }
        return ActionResult("$name: switching...", null)
    }

    @Suppress("DEPRECATION")
    private fun wifi(): ActionResult {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val want = !wm.isWifiEnabled
        if (wm.setWifiEnabled(want)) return ActionResult("Wi-Fi ${if (want) "ON" else "OFF"}", want)
        return viaShade("Wi-Fi", listOf("Wi-Fi", "WLAN", "Wifi"))
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun bluetooth(): ActionResult {
        val bt = BluetoothAdapter.getDefaultAdapter() ?: return ActionResult("No Bluetooth on this phone")
        val want = !bt.isEnabled
        val ok = try { if (want) bt.enable() else bt.disable() } catch (_: SecurityException) { false }
        if (ok) return ActionResult("Bluetooth ${if (want) "ON" else "OFF"}", want)
        return viaShade("Bluetooth", listOf("Bluetooth"))
    }

    private fun mobileData(): ActionResult = viaShade("Mobile data", listOf("Mobile data", "Cellular", "Mobile Data"))

    private fun airplane(): ActionResult =
        viaShade("Airplane mode", listOf("Airplane", "Aeroplane", "Flight mode", "Flight"))

    /** Wi-Fi, Bluetooth, airplane, data and hotspot cannot be switched by normal apps on Android 10+. */
    private fun panelFallback(id: String): ActionResult {
        val action = when (id) {
            "wifi", "data", "airplane" ->
                if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIRELESS_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            else -> Settings.ACTION_WIRELESS_SETTINGS
        }
        ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return ActionResult("Opened the system panel")
    }

    private fun global(action: Int, name: String): ActionResult {
        if (!perms.isAccessibilityOn() || PanelAccessibilityService.instance == null)
            return ActionResult("$name needs Accessibility", null, "accessibility")
        return ActionResult(if (PanelAccessibilityService.global(action)) name else "$name failed")
    }

    private fun homePackages(): Set<String> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return ctx.packageManager.queryIntentActivities(i, 0).map { it.activityInfo.packageName }.toSet()
    }

    /** The app you used before the one on screen, from the usage history. */
    @Suppress("DEPRECATION")
    private fun previousPackage(): String? {
        if (!perms.hasUsageAccess()) return null
        return try {
            val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val events = usm.queryEvents(end - 30 * 60_000L, end)
            val ev = UsageEvents.Event()
            val order = LinkedHashMap<String, Long>()
            while (events.hasNextEvent()) {
                events.getNextEvent(ev)
                if (ev.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    order.remove(ev.packageName)
                    order[ev.packageName] = ev.timeStamp
                }
            }
            val home = homePackages()
            val recent = order.keys.reversed().filter { it != ctx.packageName && it != "com.android.systemui" }
            val current = recent.firstOrNull()
            val rest = if (current != null && current !in home) recent.drop(1) else recent
            rest.firstOrNull { it !in home }
        } catch (_: Exception) {
            null
        }
    }

    private fun previousApp(): ActionResult {
        val pkg = previousPackage()
        val intent = pkg?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
        if (pkg != null && intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            val starter: Context = PanelAccessibilityService.instance ?: ctx
            try {
                starter.startActivity(intent)
                return ActionResult("Back to previous app")
            } catch (_: Exception) {}
        }
        // No usage history: use the phone's own double-press on Recents
        if (!perms.isAccessibilityOn() || PanelAccessibilityService.instance == null)
            return ActionResult("Previous app needs Accessibility", null, "accessibility")
        PanelAccessibilityService.global(AccessibilityService.GLOBAL_ACTION_RECENTS)
        Handler(Looper.getMainLooper()).postDelayed({
            PanelAccessibilityService.global(AccessibilityService.GLOBAL_ACTION_RECENTS)
        }, 250)
        return ActionResult("Switching to previous app (Recents)")
    }

    /** Requests an app launch in Android's split-screen secondary window. OEM policy may reject it. */
    fun launchSplitSecondary(pkg: String): Boolean {
        if (pkg.isBlank()) return false
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val options = ActivityOptions.makeBasic()
        val modeSet = try {
            // 4 = WINDOWING_MODE_SPLIT_SCREEN_SECONDARY
            ActivityOptions::class.java
                .getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                .invoke(options, 4)
            true
        } catch (_: Throwable) {
            false
        }
        if (!modeSet) {
            lastLaunch = "$pkg: split-screen launch mode unavailable"
            return false
        }
        return try {
            val starter: Context = PanelAccessibilityService.instance ?: ctx
            starter.startActivity(intent, options.toBundle())
            lastLaunch = "$pkg: requested split-screen secondary"
            true
        } catch (e: Exception) {
            lastLaunch = "$pkg: split launch failed ${e.javaClass.simpleName}: ${e.message}"
            onMessage?.invoke("Split screen could not open this app: " + (e.message ?: e.javaClass.simpleName))
            false
        }
    }

    private fun splitScreen(): ActionResult {
        if (!perms.isAccessibilityOn() || PanelAccessibilityService.instance == null)
            return ActionResult("Split screen needs Accessibility", null, "accessibility")
        val sent = PanelAccessibilityService.global(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
        return ActionResult(
            if (sent) "Choose the second app from Recents"
            else "The phone refused the split-screen action"
        )
    }

    private fun floating(): ActionResult {
        if (!perms.hasSecureSettings()) return ActionResult("Floating apps need the one-time setup", null, "freeform")
        if (perms.isFreeformSupportOn() && perms.isForceResizableOn()) {
            return ActionResult("Floating windows are already ON", true)
        }
        perms.setInPlace("freeform_support", true)
        perms.setInPlace("force_resizable", true)
        return ActionResult("Floating windows turned ON. Restart the phone once", true)
    }

    /** A readable report of which routes this phone allows. */
    fun macroStatusReport(): String {
        val tStr = if (targets.isEmpty()) "None" else targets.entries.joinToString(", ") { "${it.key}:(${it.value.x},${it.value.y})" }
        return "Targets: $tStr\nLast Tile: $lastTile\nLast App: $lastLaunch"
    }

    fun diagnostics(): String {
        val sb = StringBuilder()
        val mode = try {
            ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
            "available"
        } catch (e: Throwable) {
            "BLOCKED (" + e.javaClass.simpleName + ")"
        }
        sb.appendLine("Freeform launch call: $mode")
        val visible = try {
            ActivityOptions::class.java.declaredMethods.map { it.name }
                .filter { it.contains("aunch", true) || it.contains("indowing", true) || it.contains("reeform", true) }
                .distinct().sorted()
        } catch (e: Throwable) {
            emptyList()
        }
        sb.appendLine("Window-option calls the app can see: " + (if (visible.isEmpty()) "none" else visible.joinToString()))
        val miui = try {
            val c = Class.forName("android.util.MiuiMultiWindowUtils")
            val all = c.declaredMethods.map { it.name }.distinct().sorted()
            val key = all.filter {
                it.contains("ree", true) || it.contains("mall", true) || it.contains("loat", true) ||
                    it.contains("ption", true) || it.contains("tart", true)
            }
            "class found, " + all.size + " calls. Likely: " + (if (key.isEmpty()) all.take(15) else key.take(20)).joinToString()
        } catch (e: Throwable) {
            "not available (" + e.javaClass.simpleName + ")"
        }
        sb.appendLine("Xiaomi floating helper: $miui")
        sb.appendLine("Last app launch: $lastLaunch")
        sb.appendLine("Last tile press: $lastTile")
        return sb.toString()
    }

    /** Xiaomi / HyperOS has its own floating-window helper. Returns launch options for it, or null if it is not there. */
    private fun miuiFreeformBundle(pkg: String): Bundle? {
        val maker = Build.MANUFACTURER.lowercase()
        if (!(maker.contains("xiaomi") || maker.contains("redmi") || maker.contains("poco"))) return null
        return try {
            val cls = Class.forName("android.util.MiuiMultiWindowUtils")
            val m = cls.methods.firstOrNull {
                it.name == "getActivityOptions" && it.parameterTypes.size >= 3 &&
                    it.parameterTypes[0] == Context::class.java && it.parameterTypes[1] == String::class.java
            } ?: return null
            val args = m.parameterTypes.mapIndexed { i, t ->
                when {
                    i == 0 -> ctx
                    i == 1 -> pkg
                    t == Boolean::class.javaPrimitiveType -> i == 2
                    else -> null
                }
            }.toTypedArray()
            when (val r = m.invoke(null, *args)) {
                is ActivityOptions -> r.toBundle()
                is Bundle -> r
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * First launch: place the new floating app at the bottom edge, just above
     * Android navigation controls. Re-launch: restore the last observed closed
     * position for this package. Keep configured dimensions stable; never reuse
     * an OEM's transient/shrunken window size.
     */
    private fun launchRect(pkg: String): Rect {
        val dm = ctx.resources.displayMetrics
        val settings = com.example.data.SettingsStore(ctx).load()
        val maxW = (dm.widthPixels - 24).coerceAtLeast(220)
        val maxH = (dm.heightPixels - 24).coerceAtLeast(240)
        val w = (settings.floatingWidthDp * dm.density).toInt().coerceIn(220, maxW)
        val h = (settings.floatingLengthDp * dm.density).toInt().coerceIn(240, maxH)
        val remembered = FloatingStore(ctx).get(pkg)
        if (remembered != null) {
            val left = remembered.left.coerceIn(0, (dm.widthPixels - w).coerceAtLeast(0))
            val top = remembered.top.coerceIn(0, (dm.heightPixels - h).coerceAtLeast(0))
            return Rect(left, top, left + w, top + h)
        }
        // Brand-new app: align to the lower-right, matching the user's reference
        // placement. Keep clear of the right edge and Android navigation area.
        val rightMargin = (24f * dm.density).toInt()
        val bottomMargin = (36f * dm.density).toInt()
        val left = (dm.widthPixels - w - rightMargin).coerceAtLeast(0)
        val top = (dm.heightPixels - h - bottomMargin).coerceAtLeast(0)
        return Rect(left, top, left + w, top + h)
    }

    /** Opens an installed app in a small floating window (if the phone has freeform windows turned on). */
    fun launch(pkg: String): Boolean {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            lastLaunch = "$pkg: no launch intent"
            return false
        }
        if (!(perms.isFreeformSupportOn() && perms.isForceResizableOn()) && !freeformWarned) {
            freeformWarned = true
            onMessage?.invoke("Floating windows need 'Enable freeform windows' and 'Force activities to be resizable' ON in Permissions, then one phone restart")
        }
        // A new task every time, otherwise an app that is already open just comes to the front and ignores the window options
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        // Use the public ActivityOptions builder directly. The Xiaomi helper returns a
        // Bundle, but ActivityOptions.fromBundle is not available in this compile SDK.
        // Stable launch bounds are applied on every launch so the window cannot shrink.
        val route = "standard bottom bounds"
        val opts = ActivityOptions.makeBasic()
        opts.setLaunchBounds(launchRect(pkg))
        val modeCall = try { // 5 = WINDOWING_MODE_FREEFORM
            ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType).invoke(opts, 5)
            "ok"
        } catch (e: Throwable) {
            "failed (" + e.javaClass.simpleName + ")"
        }
        val bundle: Bundle = opts.toBundle()
        // Starting from the accessibility service is allowed even when this app is in the background
        val fromService = PanelAccessibilityService.instance != null
        val starter: Context = PanelAccessibilityService.instance ?: ctx
        return try {
            starter.startActivity(intent, bundle)
            lastLaunch = "$pkg: sent via $route (windowing mode call: $modeCall), started from " +
                (if (fromService) "accessibility service" else "app")
            true
        } catch (e: Exception) {
            lastLaunch = "$pkg: FAILED " + e.javaClass.simpleName + ": " + e.message
            onMessage?.invoke("Could not open the app: " + e.message)
            false
        }
    }
}
