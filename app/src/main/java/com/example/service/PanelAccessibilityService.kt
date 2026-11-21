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
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
    private var handleView: View? = null
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
        if (t == AccessibilityEvent.TYPE_WINDOWS_CHANGED || t == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            mainHandler.removeCallbacks(recordRunnable)
            mainHandler.postDelayed(recordRunnable, 500L)
        }
    }

    /** Remembers where each floating (small) app window is, so it opens in the same place next time. */
    private fun recordFloatingBounds() {
        try {
            val dm = resources.displayMetrics
            for (w in windows) {
                if (w.type != AccessibilityWindowInfo.TYPE_APPLICATION) continue
                val pkg = w.root?.packageName?.toString() ?: continue
                if (pkg == packageName) continue
                val r = Rect()
                w.getBoundsInScreen(r)
                // a floating window is smaller than the screen in BOTH directions (split-screen halves are not)
                val floating = r.width() < dm.widthPixels * 0.96f && r.height() < dm.heightPixels * 0.9f
                if (floating && r.width() > dp(120f) && r.height() > dp(120f)) floatingStore.put(pkg, r)
            }
        } catch (_: Throwable) {}
    }

    private var trackView: View? = null

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
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 40L))
            .build()
        val sent = dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { done(true) }
            override fun onCancelled(gestureDescription: GestureDescription?) { done(false) }
        }, null)
        if (!sent) done(false)
    }

    private fun closeShade() {
        if (Build.VERSION.SDK_INT >= 31) performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        else performGlobalAction(GLOBAL_ACTION_BACK)
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
        hidePanel(); hideHandle(); hideTrackLayer()
        owner?.stop(); owner = null
    }

    private fun render(s: OverlayState) {
        val overlayAllowed = !s.activityVisible && !s.suspended
        val panelNeeded = s.panelOpen || s.window != ActiveWindow.MAIN_PANEL
        if (overlayAllowed && panelNeeded) showPanel() else hidePanel()
        if (overlayAllowed && !panelNeeded && s.settings.isEdgeHandleEnabled) showHandle(s.settings) else hideHandle()
    }

    private fun showHandle(st: PanelSettings) {
        val dm = resources.displayMetrics
        val lp = (handleView?.layoutParams as? WindowManager.LayoutParams) ?: WindowManager.LayoutParams(
            dp(14f), dp(st.handleHeightDp), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.height = dp(st.handleHeightDp)
        lp.gravity = (if (st.isRightEdge) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
        lp.y = ((st.verticalPosition / 10f - 0.5f) * dm.heightPixels).toInt()

        val existing = handleView
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
        var fired = false
        v.setOnTouchListener { _, ev ->
            val inward = if (vm.panelSettings.value.isRightEdge) downX - ev.rawX else ev.rawX - downX
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = ev.rawX; fired = false }
                MotionEvent.ACTION_MOVE -> if (!fired && inward > dp(20f)) { fired = true; openPanel() }
                MotionEvent.ACTION_UP -> if (!fired && abs(ev.rawX - downX) < dp(6f)) openPanel()
            }
            true
        }
        wm.addView(v, lp)
        handleView = v
    }

    private fun openPanel() {
        vm.navigateTo(ActiveWindow.MAIN_PANEL)
        vm.togglePanel(true)
    }

    private fun hideHandle() {
        handleView?.let { runCatching { wm.removeView(it) } }
        handleView = null
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
            if (Build.VERSION.SDK_INT >= 26 && Settings.canDrawOverlays(this)) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
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
    fun tapQuickTile(labels: List<String>, target: TapTarget? = null, done: (Boolean) -> Unit) {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        if (target != null) {
            // Wait for the shade animation before using the saved screen point.
            // If the first coordinate tap is rejected, keep the target while
            // searching accessible tile labels so the final coordinate fallback
            // is still available (the old code accidentally discarded it).
            mainHandler.postDelayed({
                tapAt(target.x, target.y) { ok ->
                    if (ok || labels.isEmpty()) {
                        mainHandler.postDelayed({ closeShade(); done(ok) }, 90L)
                    } else {
                        tryTap(labels, target, 0, done)
                    }
                }
            }, 650L)
        } else {
            tryTap(labels, null, 0, done)
        }
    }

    private fun tryTap(labels: List<String>, target: TapTarget?, attempt: Int, done: (Boolean) -> Unit) {
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
                mainHandler.postDelayed({ closeShade(); done(true) }, 90L)
            } else if (attempt < 12) {
                tryTap(labels, target, attempt + 1, done)
            } else if (target != null) {
                tapAt(target.x, target.y) { ok -> mainHandler.postDelayed({ closeShade(); done(ok) }, 90L) }
            } else {
                closeShade()
                done(false)
            }
        }, if (attempt == 0) 300L else 120L)
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
