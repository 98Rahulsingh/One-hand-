package com.example.service

import com.example.data.FloatingStore
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.TextView
import android.widget.FrameLayout
import android.graphics.Rect
import android.graphics.Path
import android.graphics.Color
import android.accessibilityservice.GestureDescription
import android.os.Build
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.OneHandApplication
import com.example.model.ActiveWindow
import com.example.model.PanelSettings
import com.example.ui.OneHandApp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun start() {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}

/** A spot (and label) in quick settings that a tile should press. */
/** Persistent, bounded diagnostic log for the Connectivity mini-macro. */
object MiniMacroTrace {
    private const val PREFS = "mini_macro_trace"
    private const val LOG = "events"
    private const val CURRENT = "current_run"

    fun begin(context: Context, action: String, target: TapTarget?): String {
        val id = "MM-${System.currentTimeMillis()}"
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(CURRENT, id).apply()
        record(context, id, "START", "INFO", "Action=$action; target=${target?.label ?: "not set"}; XY=${target?.let { "${it.x},${it.y}" } ?: "none"}")
        return id
    }

    fun record(context: Context, runId: String?, stage: String, status: String, detail: String) {
        if (runId.isNullOrBlank()) return
        runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val old = JSONArray(prefs.getString(LOG, "[]") ?: "[]")
            val next = JSONArray()
            for (i in (old.length() - 119).coerceAtLeast(0) until old.length()) next.put(old.getJSONObject(i))
            next.put(JSONObject().apply {
                put("run", runId); put("time", System.currentTimeMillis()); put("stage", stage)
                put("status", status); put("detail", detail.take(240))
            })
            prefs.edit().putString(LOG, next.toString()).apply()
        }
    }

    fun report(context: Context): String = runCatching {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(LOG, "[]") ?: "[]")
        if (arr.length() == 0) {
            "No mini-macro trace yet. Run a Connectivity toggle first."
        } else {
            buildString {
                for (i in (arr.length() - 30).coerceAtLeast(0) until arr.length()) {
                    val e = arr.getJSONObject(i)
                    val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(e.optLong("time")))
                    append(time).append(" | ").append(e.optString("run")).append(" | ")
                        .append(e.optString("stage")).append(" | ").append(e.optString("status"))
                        .append(" | ").append(e.optString("detail")).append('\n')
                }
            }
        }
    }.getOrElse { "Could not read mini-macro trace: ${it.message}" }
}

data class TapTarget(val x: Int, val y: Int, val label: String)

private data class OverlayState(
    val activityVisible: Boolean,
    val panelOpen: Boolean,
    val window: ActiveWindow,
    val settings: PanelSettings,
    val suspended: Boolean
)

/**
 * Shows the edge handle and the side panel on top of every other app,
 * and runs system actions (recents, lock, split screen...).
 */
class PanelAccessibilityService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private var owner: OverlayOwner? = null
    private var scope: CoroutineScope? = null
    private var rightHandleView: View? = null
    private var leftHandleView: View? = null
    private var bottomLeftGestureView: View? = null
    private var bottomRightGestureView: View? = null
    private var panelView: ComposeView? = null

    private val vm get() = (application as OneHandApplication).viewModel
    private fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (owner != null) return
        owner = OverlayOwner().also { it.start() }
        // Panel must not pop up by itself when the service starts in the background
        if (!vm.activityVisible.value) vm.togglePanel(false)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main).also { sc ->
            sc.launch {
                combine(vm.activityVisible, vm.isPanelOpen, vm.activeWindow, vm.panelSettings, vm.overlaySuspended) { v, o, w, s, x ->
                    OverlayState(v, o, w, s, x)
                }.collect { render(it) }
            }
        }
    }

    private val recordRunnable = Runnable { recordFloatingBounds() }
    private val floatingStore by lazy { FloatingStore(this) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val t = event?.eventType ?: return
        if (t == AccessibilityEvent.TYPE_WINDOWS_CHANGED || t == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || t == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            // Capture immediately while the floating app still exists. A delayed
            // read after the close event misses the window because Android has removed it.
            mainHandler.removeCallbacks(recordRunnable)
            mainHandler.post(recordRunnable)
        }
    }

    /**
     * Continuously keeps the last valid bounds while a floating app is visible.
     * When it closes, the most recent saved rectangle remains available for relaunch.
     */
    private fun recordFloatingBounds() {
        var foundFloating = false
        try {
            val dm = resources.displayMetrics
            for (w in windows) {
                if (w.type != AccessibilityWindowInfo.TYPE_APPLICATION) continue
                val pkg = w.root?.packageName?.toString() ?: continue
                if (pkg == packageName) continue
                val r = Rect()
                w.getBoundsInScreen(r)
                val floating = r.width() < dm.widthPixels * 0.96f && r.height() < dm.heightPixels * 0.9f
                if (floating && r.width() > dp(120f) && r.height() > dp(120f)) {
                    floatingStore.put(pkg, r)
                    foundFloating = true
                }
            }
        } catch (_: Throwable) {}
        // Poll only while at least one floating app is present; stop automatically on close.
        if (foundFloating) {
            mainHandler.removeCallbacks(recordRunnable)
            mainHandler.postDelayed(recordRunnable, 250L)
        }
    }

    private var trackView: View? = null

    /** Lets the user mark the exact Android Back button location; saved for Mini-Macro use. */
    fun startBackButtonTracking(done: (TapTarget?) -> Unit) {
        // Back calibration is independent of Quick Settings. Never inspect or
        // control System UI here: on some OEM builds even the dismiss-shade
        // global action can reopen/expand the Quick Settings shade.
        MiniMacroTrace.record(this, "BACK-CAL-${System.currentTimeMillis()}", "BACK_TRACK_START", "START", "Starting Back coordinate capture only; no Quick Settings action")
        showBackTrackLayer(done)
    }

    private fun showBackTrackLayer(done: (TapTarget?) -> Unit) {
        val hint = TextView(this).apply {
            text = "Tap exactly where your Android BACK button is.\nThis only records its position."
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(dp(24f), dp(24f), dp(24f), dp(24f))
        }
        val layer = FrameLayout(this).apply {
            setBackgroundColor(0x99000000.toInt())
            addView(hint, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        var finished = false
        fun finish(target: TapTarget?) {
            if (finished) return
            finished = true
            hideTrackLayer()
            if (target != null) {
                getSharedPreferences("mini_macro_back_target", Context.MODE_PRIVATE).edit()
                    .putInt("x", target.x).putInt("y", target.y).putBoolean("saved", true).apply()
                MiniMacroTrace.record(this, "BACK-CAL-${System.currentTimeMillis()}", "BACK_TARGET_CAPTURE", "SAVED", "Back button coordinate=${target.x},${target.y}")
            }
            done(target)
        }
        layer.setOnTouchListener { _, ev ->
            if (ev.actionMasked == MotionEvent.ACTION_UP) {
                finish(TapTarget(ev.rawX.toInt(), ev.rawY.toInt(), "Android Back"))
            }
            true
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        runCatching { wm.addView(layer, lp); trackView = layer }
            .onFailure { finish(null) }
        mainHandler.postDelayed({ finish(null) }, 20_000L)
    }

    fun backButtonTargetDescription(): String? {
        val prefs = getSharedPreferences("mini_macro_back_target", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("saved", false)) return null
        return "X: ${prefs.getInt("x", -1)}   Y: ${prefs.getInt("y", -1)}"
    }

    fun clearBackButtonTarget() {
        getSharedPreferences("mini_macro_back_target", Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** Opens quick settings and covers it with a see-through layer; the next press becomes the saved spot. */
    fun startTracking(done: (TapTarget?) -> Unit) {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        mainHandler.postDelayed({ showTrackLayer(done) }, 800L)
    }

    private fun showTrackLayer(done: (TapTarget?) -> Unit) {
        val hint = TextView(this).apply {
            text = "Press the quick settings icon this tile should press"
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16f), dp(70f), dp(16f), 0)
        }
        val layer = FrameLayout(this).apply {
            setBackgroundColor(0x33000000)
            addView(hint)
        }
        var finished = false
        fun finish(t: TapTarget?) {
            if (finished) return
            finished = true
            hideTrackLayer()
            closeShade()
            done(t)
        }
        layer.setOnTouchListener { _, ev ->
            if (ev.actionMasked == MotionEvent.ACTION_UP) {
                val x = ev.rawX.toInt()
                val y = ev.rawY.toInt()
                finish(TapTarget(x, y, labelAt(x, y) ?: ""))
            }
            true
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(layer, lp)
        trackView = layer
        mainHandler.postDelayed({ finish(null) }, 25_000L)
    }

    private fun hideTrackLayer() {
        trackView?.let { runCatching { wm.removeView(it) } }
        trackView = null
    }

    /** Label of the quick settings item under a screen point, if the system shares one. */
    private fun labelAt(x: Int, y: Int): String? {
        val r = Rect()
        fun walk(n: AccessibilityNodeInfo?, depth: Int): String? {
            if (n == null || depth > 12) return null
            n.getBoundsInScreen(r)
            if (!r.contains(x, y)) return null
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)?.let { return it }
            val own = (n.text?.toString() ?: n.contentDescription?.toString())
                ?.split(',', ':', '\n')?.firstOrNull()?.trim()
            return if (!own.isNullOrBlank()) own.take(40) else null
        }
        return windows.asSequence()
            .mapNotNull { it.root }
            .filter { it.packageName?.toString() != packageName }
            .mapNotNull { walk(it, 0) }
            .firstOrNull()
    }

    private fun tapAt(x: Int, y: Int, done: (Boolean) -> Unit) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()); lineTo(x + 1f, y + 1f) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()
        val sent = dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { done(true) }
            override fun onCancelled(gestureDescription: GestureDescription?) { done(false) }
        }, null)
        if (!sent) done(false)
    }

    fun tapSavedBackTarget(runId: String? = null, done: (Boolean) -> Unit) {
        val prefs = getSharedPreferences("mini_macro_back_target", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("saved", false)) {
            MiniMacroTrace.record(this, runId, "LAST_ONE", "FAILED", "No Back coordinate saved; use Add Item > Tracker first")
            done(false)
            return
        }
        val x = prefs.getInt("x", -1)
        val y = prefs.getInt("y", -1)
        if (x < 0 || y < 0) {
            MiniMacroTrace.record(this, runId, "LAST_ONE", "FAILED", "Saved Back coordinate is invalid")
            done(false)
            return
        }
        MiniMacroTrace.record(this, runId, "LAST_ONE", "START", "Tap saved Back coordinate ($x,$y)")
        tapAt(x, y) { ok ->
            MiniMacroTrace.record(this, runId, "LAST_ONE", if (ok) "GESTURE_SENT" else "FAILED", "Saved Back tap dispatched=$ok; screen transition must be checked")
            done(ok)
        }
    }

    /** Tap the Android navigation-bar Back control directly after the Quick Settings tile action. */
    private fun tapNavigationBackButton(runId: String?, done: (Boolean) -> Unit) {
        val savedBack = getSharedPreferences("mini_macro_back_target", Context.MODE_PRIVATE)
        if (savedBack.getBoolean("saved", false)) {
            val x = savedBack.getInt("x", -1)
            val y = savedBack.getInt("y", -1)
            if (x >= 0 && y >= 0) {
                MiniMacroTrace.record(this, runId, "BACK_TARGET_TAP", "START", "Using user-marked Back position ($x,$y)")
                tapAt(x, y) { ok ->
                    MiniMacroTrace.record(this, runId, "BACK_TARGET_TAP", if (ok) "GESTURE_SENT" else "FAILED", "User-marked Back coordinate gesture dispatched=$ok")
                    done(ok)
                }
                return
            }
        }
        fun findBack(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (n == null) return null
            val label = "${n.text ?: ""} ${n.contentDescription ?: ""}".trim().lowercase()
            if (label == "back" || label == "back button" || label == "navigate back") {
                if (n.isClickable) return n
                var p = n.parent
                while (p != null) {
                    if (p.isClickable) return p
                    p = p.parent
                }
            }
            for (i in 0 until n.childCount) findBack(n.getChild(i))?.let { return it }
            return null
        }

        val navNode = runCatching {
            windows.asSequence().mapNotNull { it.root }.mapNotNull { findBack(it) }.firstOrNull()
        }.getOrNull()
        if (navNode != null && runCatching { navNode.performAction(AccessibilityNodeInfo.ACTION_CLICK) }.getOrDefault(false)) {
            MiniMacroTrace.record(this, runId, "NAV_BAR_BACK_TAP", "REQUESTED", "Clicked the accessible Android navigation Back button")
            done(true)
            return
        }

        // Fallback for System UI builds that do not expose navigation-bar nodes.
        // Standard Android 3-button navigation places Back at the left sixth of the bottom bar.
        val point = runCatching {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            val barHeight = if (resourceId != 0) resources.getDimensionPixelSize(resourceId) else (24 * resources.displayMetrics.density).toInt()
            Pair((metrics.widthPixels / 6).coerceAtLeast(1), (metrics.heightPixels - barHeight / 2).coerceAtLeast(1))
        }.getOrNull()
        if (point == null) {
            MiniMacroTrace.record(this, runId, "NAV_BAR_BACK_TAP", "FAILED", "Could not determine Android navigation-bar Back coordinates")
            done(false)
            return
        }
        MiniMacroTrace.record(this, runId, "NAV_BAR_BACK_TAP", "START", "Direct bottom navigation Back tap at (${point.first}, ${point.second})")
        tapAt(point.first, point.second) { ok ->
            MiniMacroTrace.record(this, runId, "NAV_BAR_BACK_TAP", if (ok) "GESTURE_SENT" else "FAILED", "Navigation Back tap gesture dispatched=$ok; dispatch does not confirm screen transition")
            done(ok)
        }
    }

    /** Return from Quick Settings by tapping the Android bottom navigation Back button. */
    private fun closeShade(runId: String? = null, done: ((Boolean) -> Unit)? = null) {
        fun shadeVisible(): Boolean = runCatching {
            windows.any { it.root?.packageName?.toString()?.contains("systemui", ignoreCase = true) == true }
        }.getOrDefault(false)

        mainHandler.postDelayed({
            tapNavigationBackButton(runId) { firstTapSent ->
                mainHandler.postDelayed({
                    if (shadeVisible()) {
                        MiniMacroTrace.record(this, runId, "NAV_BAR_BACK_CHECK", "STILL_VISIBLE", "System UI remains visible; retrying Back once")
                        mainHandler.postDelayed({
                            tapNavigationBackButton(runId) { retrySent ->
                                mainHandler.postDelayed({
                                    val closed = !shadeVisible()
                                    MiniMacroTrace.record(this, runId, "NAV_BAR_BACK_CHECK", if (closed) "RETURNED" else "FAILED", "After one retry: closed=$closed; first=$firstTapSent; retry=$retrySent")
                                    done?.invoke(closed)
                                }, 300L)
                            }
                        }, 180L)
                    } else {
                        MiniMacroTrace.record(this, runId, "NAV_BAR_BACK_CHECK", if (firstTapSent) "RETURNED" else "CHECK_ONLY", "System UI closed after first Back attempt")
                        done?.invoke(true)
                    }
                }, 350L)
            }
        }, 750L)
    }

    private fun labelOf(n: AccessibilityNodeInfo?, depth: Int): String? {
        if (n == null || depth > 3) return null
        val own = (n.text?.toString() ?: n.contentDescription?.toString())
            ?.split(',', ':', '\n')?.firstOrNull()?.trim()
        if (!own.isNullOrBlank()) return own.take(40)
        for (i in 0 until n.childCount) labelOf(n.getChild(i), depth + 1)?.let { return it }
        return null
    }
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        instance = null
        scope?.cancel(); scope = null
        hidePanel(); hideHandle(); hideBottomGestureZones(); hideTrackLayer()
        owner?.stop(); owner = null
    }

    private fun render(s: OverlayState) {
        val overlayAllowed = !s.activityVisible && !s.suspended
        // Panel visibility is controlled by panelOpen; activeWindow can remain on a
        // previously used tab after close and must not suppress the edge trigger.
        val panelNeeded = s.panelOpen
        if (overlayAllowed && panelNeeded) showPanel() else hidePanel()
        // Keep the edge trigger available after every panel is retracted.
        // It must not depend on the previous active window (e.g. Add Item/Quick Panel).
        val handlesAllowed = overlayAllowed && !panelNeeded && s.settings.isEdgeHandleEnabled
        if (overlayAllowed && !panelNeeded) {
            showBottomGestureZone(isLeft = true)
            showBottomGestureZone(isLeft = false)
        } else {
            hideBottomGestureZones()
        }
        if (handlesAllowed) {
            if (s.settings.rightTriggerEnabled) showSideHandle(isRight = true, st = s.settings) else hideSideHandle(true)
            if (s.settings.leftTriggerEnabled) showSideHandle(isRight = false, st = s.settings) else hideSideHandle(false)
        } else {
            hideHandle()
        }
    }

    private fun showSideHandle(isRight: Boolean, st: PanelSettings) {
        val dm = resources.displayMetrics
        val existing = if (isRight) rightHandleView else leftHandleView
        val wDp = if (isRight) st.rightTriggerWidthDp else st.leftTriggerWidthDp
        val hDp = if (isRight) st.rightTriggerLengthDp else st.leftTriggerLengthDp
        val vPos = if (isRight) st.rightTriggerVerticalPos else st.leftTriggerVerticalPos

        val lp = (existing?.layoutParams as? WindowManager.LayoutParams) ?: WindowManager.LayoutParams(
            dp(wDp), dp(hDp), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.width = dp(wDp)
        lp.height = dp(hDp)
        lp.gravity = (if (isRight) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
        lp.y = ((vPos / 10f - 0.5f) * dm.heightPixels).toInt()

        if (existing != null) {
            existing.alpha = st.handleOpacity.coerceIn(0.15f, 1f)
            wm.updateViewLayout(existing, lp)
            return
        }
        val v = View(this).apply {
            background = GradientDrawable().apply { setColor(0xFF00E5FF.toInt()); cornerRadius = dp(6f).toFloat() }
            alpha = st.handleOpacity.coerceIn(0.15f, 1f)
        }
        var downX = 0f
        var downY = 0f
        var downTime = 0L
        var lastTapTime = 0L
        var fired = false
        v.setOnTouchListener { _, ev ->
            val inward = if (isRight) downX - ev.rawX else ev.rawX - downX
            val dy = ev.rawY - downY
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX; downY = ev.rawY
                    downTime = System.currentTimeMillis()
                    fired = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val held = System.currentTimeMillis() - downTime > 380L
                    if (!fired && inward > dp(24f)) {
                        fired = true
                        if (isRight) vm.executeGesture(if (held) "Right Pull Inward Hold" else "Right Pull Inward")
                        else if (held && st.gestureBindings.containsKey("Swipe Inward Hold")) vm.executeGesture("Swipe Inward Hold")
                        else if (st.gestureBindings.containsKey("Swipe Inward")) vm.executeGesture("Swipe Inward")
                        else openPanel()
                    } else if (!fired && dy < -dp(36f)) {
                        fired = true
                        vm.executeGesture(if (held) "Swipe Up Hold" else "Swipe Up")
                    } else if (!fired && dy > dp(36f)) {
                        fired = true
                        vm.executeGesture(if (held) "Swipe Down Hold" else "Swipe Down")
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!fired && abs(ev.rawX - downX) < dp(8f) && abs(ev.rawY - downY) < dp(8f)) {
                        val now = System.currentTimeMillis()
                        if (now - downTime > 450L && st.gestureBindings.containsKey("Long Press")) {
                            vm.executeGesture("Long Press")
                        } else if (now - lastTapTime < 280L && st.gestureBindings.containsKey("Double Tap")) {
                            lastTapTime = 0L
                            vm.executeGesture("Double Tap")
                        } else {
                            lastTapTime = now
                            if (st.gestureBindings.containsKey("Single Tap")) vm.executeGesture("Single Tap")
                            else openPanel()
                        }
                    }
                }
            }
            true
        }
        wm.addView(v, lp)
        if (isRight) rightHandleView = v else leftHandleView = v
    }


    private fun showBottomGestureZone(isLeft: Boolean) {
        val current = if (isLeft) bottomLeftGestureView else bottomRightGestureView
        if (current != null) return

        // Divide the physical display width into three equal columns:
        // left column remains free, the left gesture occupies the middle,
        // and the right gesture occupies the right column.
        val dm = resources.displayMetrics
        val third = dm.widthPixels / 3
        val zoneWidth = if (isLeft) third else dm.widthPixels - (third * 2)
        val zoneX = if (isLeft) third else third * 2
        val zoneHeight = dp(48f)
        val zoneY = dm.heightPixels - zoneHeight

        val zone = View(this).apply { setBackgroundColor(Color.TRANSPARENT) }
        val lp = WindowManager.LayoutParams(
            zoneWidth, zoneHeight, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = zoneX
            y = zoneY
        }

        var downX = 0f
        var downY = 0f
        var downTime = 0L
        var swipeDetected = false
        var horizontalDetected = false
        var movedBeyondTapSlop = false
        var actionFired = false
        var released = false
        var holdRunnable: Runnable? = null
        val shortName = if (isLeft) "Bottom Left Swipe Up" else "Bottom Right Swipe Up"
        val holdName = if (isLeft) "Bottom Left Swipe Up Hold" else "Bottom Right Swipe Up Hold"
        val horizontalName = if (isLeft) "Mini Display Swipe Left" else "Mini Display Swipe Right"

        zone.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    holdRunnable?.let { mainHandler.removeCallbacks(it) }
                    downX = ev.rawX
                    downY = ev.rawY
                    downTime = System.currentTimeMillis()
                    swipeDetected = false
                    horizontalDetected = false
                    movedBeyondTapSlop = false
                    actionFired = false
                    released = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val horizontalDistance = abs(ev.rawX - downX)
                    if (horizontalDistance > dp(12f) || abs(ev.rawY - downY) > dp(12f)) {
                        movedBeyondTapSlop = true
                    }
                    if (!horizontalDetected && horizontalDistance > dp(24f)) {
                        horizontalDetected = true
                        actionFired = true
                        // Middle third runs the old left-side action; right third runs the old right-side action.
                        vm.executeGesture(horizontalName)
                    } else if (!horizontalDetected && !swipeDetected && downY - ev.rawY > dp(24f)) {
                        swipeDetected = true
                        val waitMs = (450L - (System.currentTimeMillis() - downTime)).coerceAtLeast(0L)
                        holdRunnable = Runnable {
                            if (swipeDetected && !released && !actionFired) {
                                actionFired = true
                                vm.executeGesture(holdName)
                            }
                        }
                        mainHandler.postDelayed(holdRunnable!!, waitMs)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    released = true
                    holdRunnable?.let { mainHandler.removeCallbacks(it) }
                    if (swipeDetected && !actionFired && ev.actionMasked == MotionEvent.ACTION_UP) {
                        actionFired = true
                        vm.executeGesture(shortName)
                    } else if (!actionFired && !swipeDetected && !horizontalDetected &&
                        !movedBeyondTapSlop && ev.actionMasked == MotionEvent.ACTION_UP &&
                        System.currentTimeMillis() - downTime <= 450L) {
                        // The overlay owns the original DOWN event. Replay a plain tap so
                        // controls underneath the bottom gesture strip remain usable.
                        tapAt(ev.rawX.toInt(), ev.rawY.toInt()) { }
                    }
                }
            }
            true
        }
        runCatching { wm.addView(zone, lp) }.onSuccess {
            if (isLeft) bottomLeftGestureView = zone else bottomRightGestureView = zone
        }
    }

    private fun hideBottomGestureZones() {
        bottomLeftGestureView?.let { runCatching { wm.removeView(it) } }
        bottomRightGestureView?.let { runCatching { wm.removeView(it) } }
        bottomLeftGestureView = null
        bottomRightGestureView = null
    }

    private fun hideSideHandle(isRight: Boolean) {
        val view = if (isRight) rightHandleView else leftHandleView
        view?.let { runCatching { wm.removeView(it) } }
        if (isRight) rightHandleView = null else leftHandleView = null
    }

    private fun openPanel() {
        vm.navigateTo(ActiveWindow.MAIN_PANEL)
        vm.togglePanel(true)
    }

    private fun hideHandle() {
        hideSideHandle(true)
        hideSideHandle(false)
    }

    private fun showPanel() {
        if (panelView != null) return
        val o = owner ?: return
        val cv = ComposeView(this).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setViewTreeViewModelStoreOwner(o)
            setContent {
                MyApplicationTheme(darkTheme = true) {
                    OneHandApp(viewModel = vm, overlayMode = true)
                }
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        wm.addView(cv, lp)
        panelView = cv
    }

    private fun hidePanel() {
        panelView?.let { runCatching { wm.removeView(it) } }
        panelView = null
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    /** 1. open quick settings  2. press the tile  3. close it. Done as fast as the phone's animation allows. */
    fun tapQuickTile(labels: List<String>, target: TapTarget? = null, runId: String? = null, done: (Boolean) -> Unit) {
        val opened = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        MiniMacroTrace.record(this, runId, "QUICK_SETTINGS_OPEN", if (opened) "REQUESTED" else "FAILED", "Global Quick Settings action accepted=$opened")
        // Always locate and click the live System UI tile first. A tracked point can become
        // stale when Quick Settings changes layout, expands, or shifts between sessions.
        // Keep the saved point only as a last-resort fallback after Accessibility lookup.
        mainHandler.postDelayed({
            val systemUiWindows = runCatching {
                windows.filter { it.root?.packageName?.toString()?.contains("systemui", true) == true }
            }.getOrDefault(emptyList())
            val visible = systemUiWindows.isNotEmpty()
            val bounds = Rect()
            systemUiWindows.firstOrNull()?.getBoundsInScreen(bounds)
            MiniMacroTrace.record(this, runId, "QUICK_SETTINGS_VISIBLE", if (visible) "PASS" else "WARNING",
                "System UI window visible=$visible; waited 450ms; display=${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}; window=$bounds")
            MiniMacroTrace.record(this, runId, "ACCESSIBILITY_LABEL_TAP", "START",
                "Searching live tile labels first: ${labels.joinToString()}; saved coordinate fallback=${target?.let { "${it.x},${it.y}" } ?: "none"}")
            tryTap(labels, target, 0, runId, done)
        }, 450L)
    }

    private fun tryTap(labels: List<String>, target: TapTarget?, attempt: Int, runId: String?, done: (Boolean) -> Unit) {
        mainHandler.postDelayed({
            val node = if (labels.isEmpty()) null else windows.asSequence()
                .mapNotNull { it.root }
                .filter { it.packageName?.toString() != packageName }
                .mapNotNull { findTile(it, labels) }
                .firstOrNull()
            var clicked = false
            if (node != null) {
                var c: AccessibilityNodeInfo? = node
                var hops = 0
                while (c != null && hops < 6 && !clicked) {
                    if (c.isClickable) clicked = c.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    c = c.parent
                    hops++
                }
            }
            if (clicked) {
                MiniMacroTrace.record(this, runId, "ACCESSIBILITY_LABEL_TAP", "PASS", "Found and clicked tile after ${attempt + 1} lookup attempt(s)")
                closeShade(runId) { closed -> done(closed) }
            } else if (attempt < 5) {
                if (attempt == 4) MiniMacroTrace.record(this, runId, "ACCESSIBILITY_LABEL_TAP", "WARNING", "Tile label not found after 6 fast attempts; trying saved coordinate")
                tryTap(labels, target, attempt + 1, runId, done)
            } else if (target != null) {
                MiniMacroTrace.record(this, runId, "COORDINATE_TAP", "START", "Label lookup failed; coordinate fallback (${target.x}, ${target.y})")
                tapAt(target.x, target.y) { ok ->
                    MiniMacroTrace.record(this, runId, "COORDINATE_TAP", if (ok) "PASS" else "FAILED", "Fallback gesture completed=$ok")
                    if (ok) closeShade(runId) { closed -> done(closed) } else done(false)
                }
            } else {
                MiniMacroTrace.record(this, runId, "TILE_LOOKUP", "FAILED", "No clickable tile found for labels: ${labels.joinToString()}")
                closeShade(runId) { done(false) }
            }
        }, if (attempt == 0) 140L else 80L)
    }

    private fun findTile(n: AccessibilityNodeInfo?, labels: List<String>): AccessibilityNodeInfo? {
        if (n == null) return null
        val t = "${n.text ?: ""} ${n.contentDescription ?: ""}"
        if (labels.any { t.contains(it, ignoreCase = true) }) return n
        for (i in 0 until n.childCount) findTile(n.getChild(i), labels)?.let { return it }
        return null
    }

    companion object {
        @Volatile var instance: PanelAccessibilityService? = null
        fun global(action: Int): Boolean = instance?.performGlobalAction(action) ?: false
    }
}
