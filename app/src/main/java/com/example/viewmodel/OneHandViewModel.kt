package com.example.viewmodel

import android.os.Build
import android.service.notification.NotificationListenerService
import android.content.ComponentName
import com.example.service.MediaListenerService
import com.example.service.TapTarget
import com.example.service.PanelAccessibilityService
import com.example.service.MiniMacroTrace
import com.example.model.ReorderTarget
import com.example.model.SPECIAL_GROUPS
import java.util.Collections
import com.example.system.MediaRepo
import com.example.system.InstalledApps
import kotlinx.coroutines.Dispatchers
import com.example.model.isInGroup
import com.example.data.LayoutStore
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.data.SettingsStore
import com.example.system.PermissionsManager
import com.example.system.QuickActions
import androidx.lifecycle.viewModelScope
import com.example.model.ActiveWindow
import com.example.model.AppItem
import com.example.model.AudioOutputDevice
import com.example.model.ItemType
import com.example.model.MediaState
import com.example.model.MediaStreamItem
import com.example.model.PanelSettings
import com.example.model.QuickSettingItem
import com.example.model.WindowGeometry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OneHandViewModel(app: Application) : AndroidViewModel(app) {

    // Current active window: starts with MAIN_PANEL as requested ("App open hote hi sidhe side panel open hoga")
    private val _activeWindow = MutableStateFlow(ActiveWindow.MAIN_PANEL)
    val activeWindow: StateFlow<ActiveWindow> = _activeWindow.asStateFlow()

    // Panel visibility: initially open
    private val _isPanelOpen = MutableStateFlow(true)
    val isPanelOpen: StateFlow<Boolean> = _isPanelOpen.asStateFlow()

    // Panel visual settings (Rounded corners, offsets, columns, rows, geometry memory, etc.)
    private val _panelSettings = MutableStateFlow(PanelSettings())
    val panelSettings: StateFlow<PanelSettings> = _panelSettings.asStateFlow()

    // Media state
    private val _mediaState = MutableStateFlow(
        MediaState(
            isPlaying = false, trackTitle = "Nothing playing", artist = "Turn on Media access and start a player",
            totalSeconds = 0, currentSeconds = 0
        )
    )
    val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

    // Category groups list
    private val _availableCategories = MutableStateFlow(
        listOf(
            "Recent",
            "Most Used",
            "Tools",
            "Social",
            "Video",
            "Music",
            "Images",
            "Communication",
            "Productivity",
            "Games"
        )
    )
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Recent")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Dialog state for adding a new group
    private val _showAddGroupDialog = MutableStateFlow(false)
    val showAddGroupDialog: StateFlow<Boolean> = _showAddGroupDialog.asStateFlow()

    // All registered apps / shortcuts / actions master list
    private val _masterItems = MutableStateFlow(createInitialItems())
    val masterItems: StateFlow<List<AppItem>> = _masterItems.asStateFlow()

    // Quick Settings toggles
    private val _quickSettings = MutableStateFlow(createInitialQuickSettings())
    val quickSettings: StateFlow<List<QuickSettingItem>> = _quickSettings.asStateFlow()

    // Window 3: Add Quick Access Search & Tab filters
    private val _itemFilterType = MutableStateFlow(ItemType.APP)
    val itemFilterType: StateFlow<ItemType> = _itemFilterType.asStateFlow()

    private val _itemSearchQuery = MutableStateFlow("")
    val itemSearchQuery: StateFlow<String> = _itemSearchQuery.asStateFlow()

    // Fast Alphabet Scroller current letter
    private val _selectedAlphabetLetter = MutableStateFlow<Char?>(null)
    val selectedAlphabetLetter: StateFlow<Char?> = _selectedAlphabetLetter.asStateFlow()

    // Feedback Toast / Banner message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Permission explanation dialog request
    private val _permissionDialogInfo = MutableStateFlow<Pair<String, String>?>(null)
    val permissionDialogInfo: StateFlow<Pair<String, String>?> = _permissionDialogInfo.asStateFlow()

    private var playbackTimerJob: Job? = null

    private val appContext: Context get() = getApplication<Application>().applicationContext
    private val perms = PermissionsManager(appContext)
    private val actions = QuickActions(appContext, perms)
    private val store = SettingsStore(appContext)
    private val layout = LayoutStore(appContext)
    private val _manualGroups = MutableStateFlow(layout.loadSet("manualGroups") ?: emptySet())
    val manualGroups: StateFlow<Set<String>> = _manualGroups.asStateFlow()
    private val _tileTargets = MutableStateFlow(
        layout.loadMap("tileTargets").mapNotNull { (id, set) ->
            val p = set.firstOrNull()?.split("|", limit = 3)
            val x = p?.getOrNull(0)?.toIntOrNull()
            val y = p?.getOrNull(1)?.toIntOrNull()
            if (p != null && p.size == 3 && x != null && y != null) id to TapTarget(x, y, p[2]) else null
        }.toMap()
    )
    val tileTargets: StateFlow<Map<String, TapTarget>> = _tileTargets.asStateFlow()
    private val _suspended = MutableStateFlow(false)
    val overlaySuspended: StateFlow<Boolean> = _suspended.asStateFlow()
    private val _awaitingPermissionReturn = MutableStateFlow(false)
    fun isAwaitingPermissionReturn(): Boolean = _awaitingPermissionReturn.value
    fun clearAwaitingPermissionReturn() { _awaitingPermissionReturn.value = false }

    fun updateSettings(transform: (PanelSettings) -> PanelSettings) {
        val prev = _panelSettings.value
        _panelSettings.update(transform)
        val next = _panelSettings.value
        if (prev.oneHandModeEnabled != next.oneHandModeEnabled ||
            prev.oneHandSize != next.oneHandSize ||
            prev.oneHandCornerRight != next.oneHandCornerRight) {
            applySystemOneHandedMode(next)
        }
    }

    private fun applySystemOneHandedMode(s: PanelSettings) {
        if (!perms.hasSecureSettings()) return
        try {
            val cr = appContext.contentResolver
            val sizeInt = when (s.oneHandSize) {
                3.5f -> 35
                4.5f -> 45
                else -> 40
            }
            // Xiaomi / Poco MIUI / HyperOS native 3.5" / 4.0" / 4.5" One-Handed Mode
            android.provider.Settings.Global.putInt(cr, "handy_mode_size", sizeInt)
            android.provider.Settings.Global.putInt(cr, "handy_mode_state", if (s.oneHandModeEnabled) (if (s.oneHandCornerRight) 2 else 1) else 0)
            // Android 12+ AOSP One-Handed Mode
            android.provider.Settings.Secure.putInt(cr, "one_handed_mode_enabled", 1)
            android.provider.Settings.Secure.putInt(cr, "one_handed_mode_activated", if (s.oneHandModeEnabled) 1 else 0)
            runCatching {
                appContext.sendBroadcast(android.content.Intent("miui.intent.action.CHANGE_HANDY_MODE_STATE").apply {
                    putExtra("state", if (s.oneHandModeEnabled) (if (s.oneHandCornerRight) 2 else 1) else 0)
                })
            }
        } catch (_: Throwable) {}
    }

    private val _pendingGesture = MutableStateFlow<String?>(null)
    val pendingGestureToBind: StateFlow<String?> = _pendingGesture.asStateFlow()

    fun startBindGesture(gestureName: String) {
        _pendingGesture.value = gestureName
        navigateTo(ActiveWindow.ADD_QUICK_ACCESS)
    }

    fun clearPendingGesture() { _pendingGesture.value = null }

    fun bindAndFinish(item: AppItem) {
        val g = _pendingGesture.value
        if (g != null) {
            bindGesture(g, item.name)
            _pendingGesture.value = null
            showToast("$g → ${item.name}")
            navigateTo(ActiveWindow.PANEL_SETTINGS)
        } else {
            toggleItemAdded(item.id)
        }
    }

    fun toggleAppInQuickSettings(app: AppItem) {
        if (app.packageName.isBlank()) return
        val tileId = "app_${app.packageName}"
        _quickSettings.update { list ->
            val exists = list.any { it.id == tileId }
            if (exists) {
                list.map { if (it.id == tileId) it.copy(isVisible = !it.isVisible) else it }
            } else {
                list + QuickSettingItem(
                    id = tileId,
                    name = app.name,
                    iconName = "apps",
                    isEnabled = true,
                    color = 0xFF00E5FF,
                    description = "Quick app: ${app.name}",
                    isVisible = true
                )
            }
        }
        saveLayout()
        showToast("Updated Quick Settings: ${app.name}")
    }

    fun executeGesture(gestureName: String) {
        val label = _panelSettings.value.gestureBindings[gestureName]
        if (label.isNullOrBlank()) {
            navigateTo(ActiveWindow.MAIN_PANEL)
            togglePanel(true)
            return
        }
        val qsMatch = _quickSettings.value.firstOrNull {
            it.name.equals(label, ignoreCase = true) || it.id.equals(label, ignoreCase = true)
        }
        if (qsMatch != null) {
            toggleQuickSetting(qsMatch.id)
            return
        }
        val match = _masterItems.value.firstOrNull { it.name.equals(label, ignoreCase = true) }
        if (match != null) {
            if (match.type == ItemType.ACTION) {
                val actionId = when (match.id) {
                    "act_bt" -> "bluetooth"
                    else -> match.id.removePrefix("act_")
                }
                toggleQuickSetting(actionId)
            } else {
                launchApp(match)
            }
        } else {
            val actId = label.lowercase().replace(" ", "_").replace("-", "")
            toggleQuickSetting(actId)
        }
    }

    fun bindGesture(gestureName: String, itemLabel: String) {
        _panelSettings.update { s ->
            s.copy(gestureBindings = s.gestureBindings + (gestureName to itemLabel))
        }
    }
    private var lastRevive = 0L
    private val _reorder = MutableStateFlow<ReorderTarget?>(null)
    val reorder: StateFlow<ReorderTarget?> = _reorder.asStateFlow()
    private val installed = InstalledApps(appContext)
    private val mediaRepo = MediaRepo(appContext)

    // True while the app screen is open (the overlay stays hidden then)
    private val _activityVisible = MutableStateFlow(false)
    val activityVisible: StateFlow<Boolean> = _activityVisible.asStateFlow()
    fun setActivityVisible(v: Boolean) { _activityVisible.value = v }

    // Window 4: which group apps are added to, and which tab is open
    private val _groupAdded = MutableStateFlow(layout.loadMap("groupAdded"))
    val groupAdded: StateFlow<Map<String, Set<String>>> = _groupAdded.asStateFlow()
    private val _groupRemoved = MutableStateFlow(layout.loadMap("groupRemoved"))
    val groupRemoved: StateFlow<Map<String, Set<String>>> = _groupRemoved.asStateFlow()
    private val _addTargetGroup = MutableStateFlow("Tools")
    val addTargetGroup: StateFlow<String> = _addTargetGroup.asStateFlow()
    private val _tilesTab = MutableStateFlow(false)
    val tilesTab: StateFlow<Boolean> = _tilesTab.asStateFlow()
    private var newGroupRequested = false
    private var brightnessHintShown = false

    // Which permission the guide dialog is about (lets the dialog offer "Open settings")
    private val _guideType = MutableStateFlow<String?>(null)
    val guideType: StateFlow<String?> = _guideType.asStateFlow()

    init {
        // Remember size, position and options across restarts
        _panelSettings.value = store.load()
        layout.loadList("customTiles")?.forEach { s ->
            val parts = s.split("|", limit = 2)
            if (parts.size == 2) {
                _quickSettings.update {
                    it + QuickSettingItem(
                        id = parts[0], name = parts[1], iconName = "custom", isEnabled = false,
                        color = 0xFF80DEEA, description = "Custom tile", isVisible = true
                    )
                }
            }
        }
        actions.targets = _tileTargets.value
        actions.nameOf = { id -> _quickSettings.value.find { it.id == id }?.name ?: id }
        layout.loadList("categories")?.let { _availableCategories.value = it }
        _availableCategories.update { ensureBuiltIns(it) }
        layout.loadList("tileOrder")?.let { order ->
            _quickSettings.update { l -> l.sortedBy { q -> order.indexOf(q.id).let { i -> if (i < 0) Int.MAX_VALUE else i } } }
        }
        layout.loadSet("visibleTiles")?.let { vis ->
            _quickSettings.update { l -> l.map { q -> q.copy(isVisible = q.id in vis) } }
        }
        viewModelScope.launch { _panelSettings.collect { store.save(it) } }
        perms.ensureHiddenApiRelaxed()
        refreshPermissions()
        refreshQuickStates()
        actions.onTorchChanged = { on -> setQuick("torch", on) }
        actions.onMessage = { msg -> showToast(msg) }
        refreshApps()
        startMediaSync()
    }

    // ---- Real permission state ----
    fun refreshPermissions() {
        refreshUsage()
        _panelSettings.update {
            it.copy(
                accessibilityEnabled = perms.isAccessibilityOn(),
                notificationsEnabled = perms.notificationsAllowed(),
                mediaControlsEnabled = perms.isListenerOn(),
                usageAccessEnabled = perms.hasUsageAccess(),
                floatingWindowEnabled = perms.canDrawOverlay(),
                systemControlPermissionEnabled = perms.hasSecureSettings(),
                batteryOptimizationIgnored = perms.isBatteryExempt(),
                freeformEnabled = perms.isFreeformOn(),
                writeSettingsEnabled = perms.canWriteSettings(),
                bgStartEnabled = perms.hasBgStart(),
                lockScreenEnabled = perms.hasLockScreenOp(),
                shortcutsEnabled = perms.hasShortcutsOp(),
                autostartEnabled = perms.hasAutostart(),
                secureSettingsEnabled = perms.hasSecureSettings(),
                noPauseEnabled = perms.hasNoPause(),
                freeformSupportEnabled = perms.isFreeformSupportOn(),
                forceResizableEnabled = perms.isForceResizableOn()
            )
        }
    }

    private fun isGranted(type: String): Boolean = _panelSettings.value.let {
        when (type) {
            "accessibility" -> it.accessibilityEnabled
            "notifications" -> it.notificationsEnabled
            "media_controls" -> it.mediaControlsEnabled
            "usage_access" -> it.usageAccessEnabled
            "floating_window" -> it.floatingWindowEnabled
            "system_control" -> it.systemControlPermissionEnabled
            "battery_optimization" -> it.batteryOptimizationIgnored
            "freeform" -> it.freeformEnabled
            "write_settings" -> it.writeSettingsEnabled
            "bg_start" -> it.bgStartEnabled
            "lock_screen" -> it.lockScreenEnabled
            "shortcuts" -> it.shortcutsEnabled
            "autostart" -> it.autostartEnabled
            "no_pause" -> it.noPauseEnabled
            "freeform_support" -> it.freeformSupportEnabled
            "force_resizable" -> it.forceResizableEnabled
            else -> false
        }
    }

    fun setupGuide(): String = perms.setupGuide()
    fun guideFor(type: String): String = perms.guideText(type)

    fun openSystemScreen(type: String) {
        _awaitingPermissionReturn.value = true
        perms.systemScreen(type)?.let { appContext.startActivity(it) }
        _guideType.value = null
        _permissionDialogInfo.value = null
    }

    fun refreshQuickStates() {
        val s = actions.currentStates()
        _quickSettings.update { list -> list.map { q -> s[q.id]?.let { q.copy(isEnabled = it) } ?: q } }
        _mediaState.update { it.copy(volumePercent = actions.volumePercent(), brightnessPercent = actions.brightnessPercent()) }
    }

    private fun setQuick(id: String, on: Boolean) {
        _quickSettings.update { list -> list.map { q -> if (q.id == id) q.copy(isEnabled = on) else q } }
    }

    private fun startMediaSync() {
        playbackTimerJob?.cancel()
        playbackTimerJob = viewModelScope.launch {
            while (true) {
                val s = mediaRepo.snapshot()
                val devices = actions.outputDevices()
                _mediaState.update { cur ->
                    cur.copy(
                        isPlaying = s.isPlaying, trackTitle = s.title, artist = s.artist,
                        totalSeconds = s.totalSeconds, currentSeconds = s.currentSeconds,
                        activeStreams = s.streams, outputDevices = devices,
                        selectedDeviceId = devices.firstOrNull { it.isConnected }?.id ?: cur.selectedDeviceId
                    )
                }
                delay(1000)
            }
        }
    }

    // ---- Real apps and usage ----
    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = installed.load()
            _masterItems.update { old -> old.filter { it.type != ItemType.APP } + apps }
        }
    }

    fun refreshUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val u = installed.usageMap()
            val last = installed.lastUsedMap()
            _masterItems.update { list ->
                list.map {
                    if (it.type == ItemType.APP) {
                        it.copy(
                            usageCount = (u[it.packageName]?.first ?: 0L).toInt(),
                            lastUsed = maxOf(last[it.packageName] ?: 0L, it.lastUsed)
                        )
                    } else it
                }
            }
        }
    }

    // Window navigation
    fun navigateTo(window: ActiveWindow) {
        _activeWindow.value = window
        _isPanelOpen.value = true
    }

    fun dismissFloatingWindow() {
        if (_activeWindow.value != ActiveWindow.MAIN_PANEL) {
            _activeWindow.value = ActiveWindow.MAIN_PANEL
        } else {
            _isPanelOpen.value = false
        }
    }

    fun togglePanel(open: Boolean? = null) {
        _isPanelOpen.value = open ?: !_isPanelOpen.value
    }

    // Geometry Memory: Size & Position Persistence
    fun updateMainPanelGeometry(xOffset: Float, yOffset: Float, widthDp: Float, heightDp: Float) {
        _panelSettings.update { cur ->
            cur.copy(
                mainPanelGeometry = WindowGeometry(
                    xOffset = xOffset,
                    yOffset = yOffset,
                    widthDp = widthDp.coerceIn(220f, 380f),
                    heightDp = heightDp.coerceIn(400f, 850f)
                )
            )
        }
    }

    fun updateMediaWindowGeometry(xOffset: Float, yOffset: Float, widthDp: Float, heightDp: Float) {
        _panelSettings.update { cur ->
            cur.copy(
                mediaWindowGeometry = WindowGeometry(
                    xOffset = xOffset,
                    yOffset = yOffset,
                    widthDp = widthDp.coerceIn(260f, 400f),
                    heightDp = heightDp.coerceIn(420f, 850f)
                )
            )
        }
    }

    fun updateQuickAccessGeometry(xOffset: Float, yOffset: Float, widthDp: Float, heightDp: Float) {
        _panelSettings.update { cur ->
            cur.copy(
                quickAccessGeometry = WindowGeometry(
                    xOffset = xOffset,
                    yOffset = yOffset,
                    widthDp = widthDp.coerceIn(260f, 400f),
                    heightDp = heightDp.coerceIn(380f, 750f)
                )
            )
        }
    }

    fun updateSettingsGeometry(xOffset: Float, yOffset: Float, widthDp: Float, heightDp: Float) {
        _panelSettings.update { cur ->
            cur.copy(
                settingsGeometry = WindowGeometry(
                    xOffset = xOffset,
                    yOffset = yOffset,
                    widthDp = widthDp.coerceIn(280f, 420f),
                    heightDp = heightDp.coerceIn(450f, 850f)
                )
            )
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _selectedAlphabetLetter.value = null
    }

    // Group addition functionality
    fun openAddGroupDialog() {
        _showAddGroupDialog.value = true
    }

    fun dismissAddGroupDialog() {
        _showAddGroupDialog.value = false
    }

    fun createAndSelectGroup(groupName: String) {
        val trimmed = groupName.trim()
        if (trimmed.isNotBlank()) {
            if (!_availableCategories.value.contains(trimmed)) {
                _availableCategories.update { it + trimmed }
            }
            _selectedCategory.value = trimmed
            showToast("Group '$trimmed' created and selected!")
        }
        _showAddGroupDialog.value = false
    }

    // Media Controls
    private fun needMedia(ok: Boolean) {
        if (!ok) showToast("Nothing is playing. Turn on Media access and start a player")
    }

    fun togglePlayPause() = needMedia(mediaRepo.playPause())

    fun seekMedia(seconds: Int) {
        val clamped = seconds.coerceIn(0, _mediaState.value.totalSeconds)
        _mediaState.update { it.copy(currentSeconds = clamped) }
        needMedia(mediaRepo.seekTo(clamped))
    }

    fun skipBackward15() = needMedia(mediaRepo.skipBy(-15))
    fun skipForward15() = needMedia(mediaRepo.skipBy(15))
    fun previousVideo() = needMedia(mediaRepo.previous())
    fun nextVideoOrChapter() = needMedia(mediaRepo.next())

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 100f)
        _mediaState.update { it.copy(volumePercent = clamped) }
        actions.setVolumePercent(clamped)
    }

    fun setBrightness(brightness: Float) {
        val clamped = brightness.coerceIn(0f, 100f)
        _mediaState.update { it.copy(brightnessPercent = clamped) }
        if (!actions.setBrightnessPercent(clamped) && !brightnessHintShown) {
            brightnessHintShown = true
            _guideType.value = "write_settings"
            showPermissionGuide(perms.title("write_settings"), perms.guideText("write_settings"))
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _mediaState.update { it.copy(playbackSpeed = speed) }
        showToast("Speed: ${speed}x")
    }

    fun toggleRepeat() {
        showToast("Repeat can only be changed inside the player app")
    }

    fun toggleShuffle() {
        showToast("Shuffle can only be changed inside the player app")
    }

    fun switchActiveMedia(streamId: String) {
        mediaRepo.select(streamId)
        val target = _mediaState.value.activeStreams.find { it.id == streamId }
        showToast("Controlling ${target?.appName ?: "player"}")
    }

    fun selectAudioDevice(deviceId: String) {
        showToast("Android chooses the sound output. Change it from the system volume panel")
    }

    // Item Toggle (Window 3 Add/Remove logic)
    fun toggleItemAdded(itemId: String) {
        _masterItems.update { list ->
            list.map { item ->
                if (item.id == itemId) {
                    val newState = !item.isAdded
                    showToast(if (newState) "Added '${item.name}'" else "Removed '${item.name}'")
                    item.copy(isAdded = newState)
                } else item
            }
        }
    }

    fun toggleActionAdded(item: AppItem) {
        _masterItems.update { list ->
            val existing = list.firstOrNull { it.id == item.id }
            if (existing == null) {
                showToast("Added '${item.name}'")
                list + item.copy(isAdded = true)
            } else {
                val enabled = !existing.isAdded
                showToast(if (enabled) "Added '${item.name}'" else "Removed '${item.name}'")
                list.map { if (it.id == item.id) it.copy(isAdded = enabled) else it }
            }
        }
    }

    fun toggleActionInQuickSettings(tileId: String, item: AppItem) {
        _quickSettings.update { list ->
            if (list.any { it.id == tileId }) {
                list.map { if (it.id == tileId) it.copy(isVisible = !it.isVisible) else it }
            } else {
                list + QuickSettingItem(
                    id = tileId, name = item.name, iconName = item.iconName,
                    isEnabled = false, color = item.iconColor,
                    description = item.subtitle, isVisible = true
                )
            }
        }
        saveLayout()
        showToast("Updated Quick Settings: ${item.name}")
    }

    fun bindActionAndFinish(item: AppItem, quickSettingId: String) {
        val gesture = _pendingGesture.value ?: return
        bindGesture(gesture, quickSettingId)
        _pendingGesture.value = null
        showToast("$gesture → ${item.name}")
        navigateTo(ActiveWindow.PANEL_SETTINGS)
    }

    // ---- Groups, apps and quick tiles (Window 4) ----
    private fun saveLayout() {
        layout.saveList("categories", _availableCategories.value)
        layout.saveMap("groupAdded", _groupAdded.value)
        layout.saveMap("groupRemoved", _groupRemoved.value)
        layout.saveSet("visibleTiles", _quickSettings.value.filter { it.isVisible }.map { it.id }.toSet())
        layout.saveList("tileOrder", _quickSettings.value.map { it.id })
        layout.saveSet("manualGroups", _manualGroups.value)
        layout.saveList("customTiles", _quickSettings.value.filter { it.id.startsWith("custom_") || it.id.startsWith("app_") }.map { it.id + "|" + it.name })
        layout.saveMap("tileTargets", _tileTargets.value.mapValues { setOf("${it.value.x}|${it.value.y}|${it.value.label}") })
    }

    private fun firstEditableGroup(): String =
        _availableCategories.value.firstOrNull { it !in SPECIAL_GROUPS } ?: "Tools"

    fun openAddWindow(forTiles: Boolean, newGroup: Boolean = false) {
        _tilesTab.value = forTiles
        newGroupRequested = newGroup
        val sel = _selectedCategory.value
        _addTargetGroup.value = if (sel in SPECIAL_GROUPS) firstEditableGroup() else sel
        navigateTo(ActiveWindow.ADD_QUICK_ACCESS)
    }

    fun consumeNewGroupRequest(): Boolean {
        val r = newGroupRequested
        newGroupRequested = false
        return r
    }

    fun setAddTargetGroup(group: String) { _addTargetGroup.value = group }
    fun setTilesTab(on: Boolean) { _tilesTab.value = on }

    fun createGroupFromWindow(name: String) {
        val t = name.trim()
        if (t.isBlank()) return
        createAndSelectGroup(t)
        _manualGroups.update { it + t }
        _addTargetGroup.value = t
        saveLayout()
    }

    fun deleteGroup(name: String) {
        if (name in SPECIAL_GROUPS) return
        _availableCategories.update { it - name }
        _groupAdded.update { it - name }
        _groupRemoved.update { it - name }
        _manualGroups.update { it - name }
        if (_selectedCategory.value == name) _selectedCategory.value = "Recent"
        _addTargetGroup.value = firstEditableGroup()
        showToast("Group '$name' deleted")
        saveLayout()
    }

    fun renameGroup(oldName: String, newName: String) {
        val renamed = newName.trim()
        if (oldName in SPECIAL_GROUPS || renamed.isBlank() || renamed == oldName) return
        if (_availableCategories.value.any { it.equals(renamed, ignoreCase = true) }) {
            showToast("A group with that name already exists")
            return
        }
        _availableCategories.update { list -> list.map { if (it == oldName) renamed else it } }
        _groupAdded.update { map ->
            if (oldName !in map) map else map.toMutableMap().also { it[renamed] = it.remove(oldName).orEmpty() }
        }
        _groupRemoved.update { map ->
            if (oldName !in map) map else map.toMutableMap().also { it[renamed] = it.remove(oldName).orEmpty() }
        }
        _manualGroups.update { set -> if (oldName in set) (set - oldName) + renamed else set }
        if (_selectedCategory.value == oldName) _selectedCategory.value = renamed
        if (_addTargetGroup.value == oldName) _addTargetGroup.value = renamed
        showToast("Group renamed to '$renamed'")
        saveLayout()
    }

    fun toggleItemInGroup(itemId: String, group: String) {
        val item = _masterItems.value.find { it.id == itemId } ?: AppItem(
            id = itemId, name = itemId.removePrefix("act_qs_"), packageName = "",
            type = ItemType.ACTION, categoryName = "Tools", iconName = "apps",
            isAdded = false
        )
        val member = isInGroup(item, group, _groupAdded.value, _groupRemoved.value, _manualGroups.value)
        val add = _groupAdded.value[group].orEmpty()
        val rem = _groupRemoved.value[group].orEmpty()
        if (member) {
            _groupAdded.update { it + (group to (add - itemId)) }
            _groupRemoved.update { it + (group to (rem + itemId)) }
            showToast("Removed '${item.name}' from $group")
        } else {
            _groupAdded.update { it + (group to (add + itemId)) }
            _groupRemoved.update { it + (group to (rem - itemId)) }
            showToast("Added '${item.name}' to $group")
        }
        saveLayout()
    }

    fun toggleTileVisible(id: String) {
        _quickSettings.update { l -> l.map { if (it.id == id) it.copy(isVisible = !it.isVisible) else it } }
        saveLayout()
    }

    private fun ensureBuiltIns(list: List<String>): List<String> {
        val out = list.toMutableList()
        for (name in listOf("Unused", "All")) {
            if (name !in out) out.add(if ("Most Used" in out) out.indexOf("Most Used") + 1 else 0, name)
        }
        return out
    }

    fun setGroupManual(group: String, manual: Boolean) {
        _manualGroups.update { if (manual) it + group else it - group }
        showToast(if (manual) "'$group': only apps you pick" else "'$group': apps arrive automatically")
        saveLayout()
    }

    // ---- Long-press reorder of group tabs and quick tiles ----
    fun startReorder(isGroup: Boolean, id: String) { _reorder.value = ReorderTarget(isGroup, id) }
    fun stopReorder() { _reorder.value = null }

    fun moveReorder(delta: Int) {
        val t = _reorder.value ?: return
        if (t.isGroup) {
            _availableCategories.update { list ->
                val i = list.indexOf(t.id)
                val j = i + delta
                if (i < 0 || j !in list.indices) list else list.toMutableList().also { Collections.swap(it, i, j) }
            }
        } else {
            _quickSettings.update { src ->
                val list = src.toMutableList()
                val i = list.indexOfFirst { it.id == t.id }
                if (i < 0) return@update src
                val step = if (delta > 0) 1 else -1
                var j = i + step
                while (j in list.indices && !list[j].isVisible) j += step
                if (j !in list.indices) src else list.also { Collections.swap(it, i, j) }
            }
        }
        saveLayout()
    }

    fun setItemFilterType(type: ItemType) {
        _itemFilterType.value = type
    }

    fun setItemSearchQuery(query: String) {
        _itemSearchQuery.value = query
    }

    fun selectAlphabetLetter(letter: Char?) {
        _selectedAlphabetLetter.value = letter
        if (letter != null) {
            showToast("Filtered: Letter $letter")
        }
    }

    // Quick Settings - Direct hardware toggle in-place (no redirection!)
    private val needsCleanScreen = setOf("split", "prev_app", "lock", "screenshot", "home", "back", "notifications", "power")

    fun swipePanel(toRight: Boolean) {
        if (toRight) {
            // Swipe Right -> One Hand Mode
            updateSettings { it.copy(oneHandModeEnabled = true) }
            navigateTo(ActiveWindow.MAIN_PANEL)
        } else {
            // Swipe Left -> Quick Panel
            navigateTo(ActiveWindow.QUICK_PANEL)
        }
    }

    fun toggleQuickSetting(settingId: String) {
        when (settingId) {
            "open_edge" -> { navigateTo(ActiveWindow.MAIN_PANEL); togglePanel(true); return }
            "open_quick" -> { navigateTo(ActiveWindow.QUICK_PANEL); togglePanel(true); return }
            "open_tools" -> { navigateTo(ActiveWindow.QUICK_TOOLS); togglePanel(true); return }
            "open_media" -> { navigateTo(ActiveWindow.MEDIA_CONTROL); togglePanel(true); return }
            "one_hand" -> { updateSettings { it.copy(oneHandModeEnabled = !it.oneHandModeEnabled) }; return }
            "none" -> return
        }
        if (settingId.startsWith("app_")) {
            val pkg = settingId.removePrefix("app_")
            actions.launch(pkg)
            togglePanel(false)
            return
        }
        val isConnectivityMacro = settingId in setOf("wifi", "bluetooth", "data", "hotspot", "airplane") || settingId in _tileTargets.value
        if (settingId in needsCleanScreen || isConnectivityMacro) {
            // Connectivity macros always close the originating panel before opening Android Quick Settings.
            val macroRunId = if (isConnectivityMacro) actions.beginMacroRun(settingId) else null
            togglePanel(false)
            macroRunId?.let { MiniMacroTrace.record(appContext, it, "ORIGIN_PANEL_CLOSE", if (!_isPanelOpen.value) "PASS" else "FAILED", "Quick Panel close requested; panelOpen=${_isPanelOpen.value} before $settingId") }
            viewModelScope.launch {
                delay(180)
                macroRunId?.let { MiniMacroTrace.record(appContext, it, "ORIGIN_PANEL_CLOSE", "WAIT_COMPLETE", "180ms wait elapsed; invoking action") }
                handleQuickResult(settingId, actions.perform(settingId, macroRunId))
            }
        } else {
            handleQuickResult(settingId, actions.perform(settingId))
        }
    }

    private fun handleQuickResult(settingId: String, res: com.example.system.ActionResult) {
        showToast(res.message)
        if (res.newState != null) {
            setQuick(settingId, res.newState)
        } else {
            refreshQuickStates()
            viewModelScope.launch { delay(1800); refreshQuickStates() }
        }
        res.needs?.let { need ->
            _guideType.value = need
            showPermissionGuide(perms.title(need), perms.guideText(need))
        }
    }

    // ---- Custom tiles and "Track icon" ----
    fun addCustomTile(name: String) {
        val t = name.trim()
        if (t.isBlank()) return
        val item = QuickSettingItem(
            id = "custom_" + System.currentTimeMillis(), name = t, iconName = "custom", isEnabled = false,
            color = 0xFF80DEEA, description = "Custom tile. Tap Track icon to choose what it presses", isVisible = true
        )
        _quickSettings.update { it + item }
        saveLayout()
        showToast("Tile '$t' added. Now tap Track icon")
    }

    fun removeCustomTile(id: String) {
        _quickSettings.update { l -> l.filter { it.id != id } }
        _tileTargets.update { it - id }
        actions.targets = _tileTargets.value
        saveLayout()
    }

    fun clearTileTarget(id: String) {
        _tileTargets.update { it - id }
        actions.targets = _tileTargets.value
        saveLayout()
        showToast("Back to the built-in action")
    }

    /** Opens quick settings; the next icon you press there becomes what this tile presses. */
    fun miniMacroTraceReport(): String = MiniMacroTrace.report(appContext)

    fun runMacroDiagnostics(): String {
        val a11y = if (PanelAccessibilityService.instance != null) "Connected" else "Disconnected"
        val sec = if (perms.hasSecureSettings()) "Granted" else "Not Granted"
        return "Accessibility Service: $a11y\nWRITE_SECURE_SETTINGS: $sec\n" + actions.macroStatusReport()
    }

    fun backButtonTargetDescription(): String? = PanelAccessibilityService.instance?.backButtonTargetDescription()

    fun clearBackButtonTarget() {
        PanelAccessibilityService.instance?.clearBackButtonTarget()
        showToast("Back position cleared")
    }

    fun startTrackBackButton() {
        val svc = PanelAccessibilityService.instance
        if (svc == null || !perms.isAccessibilityOn()) {
            showToast("Turn on Accessibility first")
            return
        }
        showToast("Tap exactly on the Android Back button")
        _suspended.value = true
        svc.startBackButtonTracking { target ->
            _suspended.value = false
            if (target != null) showToast("Back position saved: ${target.x}, ${target.y}")
            else showToast("Back position not captured")
            openAddWindow(forTiles = _tilesTab.value)
        }
    }

    fun startTrackTile(id: String) {
        val returnToTilesTab = _tilesTab.value
        val svc = PanelAccessibilityService.instance
        if (svc == null || !perms.isAccessibilityOn()) {
            showToast("Turn on Accessibility first")
            return
        }
        showToast("Quick settings is opening. Press the icon this tile should press")
        _suspended.value = true
        svc.startTracking { t ->
            _suspended.value = false
            if (t != null) {
                val displayName = t.label.ifBlank {
                    _quickSettings.value.firstOrNull { it.id == id }?.name
                        ?: _masterItems.value.firstOrNull { it.id == "act_$id" || it.id.endsWith(id) }?.name
                        ?: id.replace('_', ' ').replaceFirstChar { it.uppercase() }
                }
                val savedTarget = t.copy(label = displayName)
                _tileTargets.update { it + (id to savedTarget) }
                actions.targets = _tileTargets.value
                saveLayout()
                showToast("Saved target: $displayName (${t.x}, ${t.y})")
            } else {
                showToast("No icon was pressed")
            }
            openAddWindow(forTiles = returnToTilesTab)
        }
    }

    /** Looks for players again. Restarts the notification listener if needed. */
    fun rescanMedia() {
        try {
            NotificationListenerService.requestRebind(ComponentName(appContext, MediaListenerService::class.java))
        } catch (_: Exception) {}
        viewModelScope.launch {
            if (perms.hasSecureSettings() && perms.isListenerOn()) {
                perms.setInPlace("media_controls", false)
                delay(400)
                perms.setInPlace("media_controls", true)
            }
            delay(1200)
            refreshPermissions()
            showToast(
                if (!perms.isListenerOn()) "Media access is OFF. Turn it on first"
                else "Found ${mediaRepo.snapshot().streams.size} player(s)"
            )
        }
    }

    /** Accessibility says ON but the service is not running (phone killed it): switch it off and on again. */
    /** Shows a report of what works on this phone and what the last launch / tile press did. */
    fun runDiagnostics() {
        val sb = StringBuilder()
        sb.appendLine("Phone: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("Accessibility switch: ${perms.isAccessibilityOn()}, service running: ${PanelAccessibilityService.instance != null}")
        sb.appendLine("Secure settings permission: ${perms.hasSecureSettings()}")
        sb.appendLine("Media access: ${perms.isListenerOn()}")
        sb.appendLine("Freeform windows: ${perms.isFreeformSupportOn()}, Force resizable: ${perms.isForceResizableOn()}")
        sb.appendLine("Background pop-up (10021): ${perms.hasBgStart()}, Display over other apps: ${perms.canDrawOverlay()}")
        sb.appendLine("Hidden-call setting (p_apps/pre_p_apps): ${perms.hiddenApiSetting()}  (1 = allowed; takes effect after the app is closed and opened again)")
        sb.appendLine("Hidden-call bypass: ${com.example.OneHandApplication.bypassState}")
        sb.append(actions.diagnostics())
        showPermissionGuide("Diagnostics", sb.toString())
    }

    fun isAccessibilitySwitchOn(): Boolean = perms.isAccessibilityOn()

    fun reviveAccessibilityIfDead() {
        if (PanelAccessibilityService.instance != null) return
        if (!perms.hasSecureSettings() || !perms.isAccessibilityOn()) return
        val now = System.currentTimeMillis()
        if (now - lastRevive < 15_000L) return
        lastRevive = now
        viewModelScope.launch {
            perms.setInPlace("accessibility", false)
            delay(500)
            perms.setInPlace("accessibility", true)
        }
    }

    // Panel visual settings
    fun updateRoundedCorners(value: Int) {
        _panelSettings.update { it.copy(roundedCorners = value.coerceIn(0, 10)) }
    }

    fun updateHorizontalOffset(value: Int) {
        _panelSettings.update { it.copy(horizontalOffset = value.coerceIn(0, 10)) }
    }

    fun updateVerticalPosition(value: Int) {
        _panelSettings.update { it.copy(verticalPosition = value.coerceIn(0, 10)) }
    }

    fun updateColumns(value: Int) {
        _panelSettings.update { it.copy(columns = value.coerceIn(1, 10)) }
    }

    fun updateRows(value: Int) {
        _panelSettings.update { it.copy(rows = value.coerceIn(1, 10)) }
    }

    fun toggleTopMediaSection() {
        _panelSettings.update { it.copy(isTopMediaSectionVisible = !it.isTopMediaSectionVisible) }
        showToast(if (_panelSettings.value.isTopMediaSectionVisible) "Top Media & Sliders Unhidden" else "Top Media & Sliders Hidden")
    }

    fun setEdgeSide(isRight: Boolean) {
        _panelSettings.update { it.copy(isRightEdge = isRight) }
        showToast("Edge set to ${if (isRight) "Right Edge" else "Left Edge"}")
    }

    fun toggleBottomQuickSettings() {
        _panelSettings.update { it.copy(isBottomQuickSettingsVisible = !it.isBottomQuickSettingsVisible) }
        showToast(if (_panelSettings.value.isBottomQuickSettingsVisible) "Bottom Quick Settings Shown" else "Bottom Quick Settings Hidden")
    }

    fun updateBackdropOpacity(percent: Int) {
        _panelSettings.update { it.copy(backdropOpacityPercent = percent.coerceIn(0, 100)) }
    }

    fun setEdgeHandleEnabled(enabled: Boolean) {
        _panelSettings.update { it.copy(isEdgeHandleEnabled = enabled) }
        showToast(if (enabled) "Edge Handle Enabled" else "Edge Handle Disabled")
    }

    fun toggleEdgeSide() {
        _panelSettings.update { it.copy(isRightEdge = !it.isRightEdge) }
        showToast("Edge switched to ${if (_panelSettings.value.isRightEdge) "Right Edge" else "Left Edge"}")
    }

    fun switchToPreviousApp() {
        toggleQuickSetting("prev_app")
    }

    fun togglePermission(type: String) {
        val turnOn = !isGranted(type)
        val done = perms.setInPlace(type, turnOn)
        refreshPermissions()
        if (done) {
            showToast("${perms.title(type)} ${if (turnOn) "ON" else "OFF"}" + (if (type == "freeform" || type == "freeform_support" || type == "force_resizable") ". Restart the phone once" else ""))
        } else {
            // Android needs another route: show the exact steps and an "Open settings" button
            _guideType.value = type
            showPermissionGuide(perms.title(type), perms.guideText(type))
        }
    }

    fun showPermissionGuide(title: String, description: String) {
        _permissionDialogInfo.value = Pair(title, description)
    }

    fun dismissPermissionGuide() {
        _permissionDialogInfo.value = null
        _guideType.value = null
    }

    private fun actionIdFor(itemId: String): String? = mapOf(
        "act_wifi" to "wifi", "act_bt" to "bluetooth", "act_data" to "data", "act_hotspot" to "hotspot",
        "act_screenshot" to "screenshot", "act_lock" to "lock", "act_split" to "split",
        "act_floating" to "floating", "act_torch" to "torch", "act_mute" to "mute",
        "act_open_edge" to "open_edge", "act_open_quick" to "open_quick",
        "act_open_tools" to "open_tools", "act_open_media" to "open_media"
    )[itemId]

    fun launchApp(app: AppItem) {
        if (app.type == ItemType.ACTION) {
            val id = actionIdFor(app.id)
            if (id != null) toggleQuickSetting(id) else showToast("${app.name} is not available")
            return
        }
        val opened = app.packageName.isNotBlank() && actions.launch(app.packageName)
        if (!opened) {
            showToast("${app.name} could not be opened")
            return
        }
        val now = System.currentTimeMillis()
        _masterItems.update { list ->
            list.map { if (it.id == app.id) it.copy(usageCount = it.usageCount + 1, lastUsed = now) else it }
        }
        togglePanel(false)
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            delay(2200)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    private fun createInitialItems(): List<AppItem> = createAllFakeItems().filter { it.type == ItemType.ACTION }

    private fun createAllFakeItems(): List<AppItem> {
        return listOf(
            // Apps matching screenshot
            AppItem("app_adsilence", "Ad-silence", "com.adsilence.app", ItemType.APP, "Tools", "block", 0xFF00BCD4, true, "Ad blocker utility", 12),
            AppItem("app_adguard", "AdGuard", "com.adguard.android", ItemType.APP, "Tools", "security", 0xFF4CAF50, true, "Content blocker & privacy", 28),
            AppItem("app_agentsroom", "AgentsRoom", "com.agentsroom.ai", ItemType.APP, "Productivity", "group", 0xFF9C27B0, true, "AI Team workspace", 18),
            AppItem("app_agnes", "Agnes", "com.agnes.voice", ItemType.APP, "Productivity", "assistant", 0xFFE91E63, true, "Personal voice assistant", 9),
            AppItem("app_moltbot", "AI Assistant for MoltBot Setup", "com.moltbot.setup", ItemType.APP, "Tools", "smart_toy", 0xFFFF9800, true, "Bot automation suite", 5),
            AppItem("app_aistudio", "AI Studio", "com.google.aistudio", ItemType.APP, "Productivity", "code", 0xFFF44336, true, "Developer workspace", 45),
            AppItem("app_chrome", "Chrome", "com.android.chrome", ItemType.APP, "Recent", "web", 0xFF4285F4, true, "Fast web browsing", 60),
            AppItem("app_youtube", "YouTube", "com.google.android.youtube", ItemType.APP, "Video", "play_arrow", 0xFFFF0000, true, "Watch videos & livestreams", 85),
            AppItem("app_whatsapp", "WhatsApp", "com.whatsapp", ItemType.APP, "Communication", "chat", 0xFF25D366, true, "Messaging & calls", 92),
            AppItem("app_spotify", "Spotify", "com.spotify.music", ItemType.APP, "Music", "music_note", 0xFF1DB954, true, "Music & podcasts", 74),
            AppItem("app_camera", "Camera", "com.android.camera", ItemType.APP, "Images", "camera_alt", 0xFFFF5722, true, "Photos & 4K video", 34),
            AppItem("app_gallery", "Gallery", "com.android.gallery3d", ItemType.APP, "Images", "photo_library", 0xFF9C27B0, true, "Photos & albums", 40),
            AppItem("app_maps", "Google Maps", "com.google.android.apps.maps", ItemType.APP, "Tools", "map", 0xFF34A853, true, "Navigation & traffic", 26),
            AppItem("app_calculator", "Calculator", "com.android.calculator2", ItemType.APP, "Tools", "calculate", 0xFF607D8B, true, "Quick math tool", 15),
            AppItem("app_files", "Files", "com.google.android.apps.nbu.files", ItemType.APP, "Tools", "folder", 0xFF2196F3, true, "File manager & cleaner", 22),
            AppItem("app_instagram", "Instagram", "com.instagram.android", ItemType.APP, "Social", "share", 0xFFE1306C, true, "Stories & Reels", 55),
            AppItem("app_messages", "Messages", "com.google.android.apps.messaging", ItemType.APP, "Communication", "message", 0xFF1A73E8, true, "SMS & RCS chat", 38),

            // Shortcuts
            AppItem("sc_direct_msg", "Direct Chat", "com.whatsapp.direct", ItemType.SHORTCUT, "Communication", "send", 0xFF25D366, false, "Instant contact chat"),
            AppItem("sc_new_note", "Take Quick Note", "com.google.android.keep.new", ItemType.SHORTCUT, "Productivity", "edit", 0xFFFFB300, true, "Jot ideas fast"),
            AppItem("sc_qr_scan", "Scan QR Code", "com.google.android.gms.vision", ItemType.SHORTCUT, "Tools", "qr_code", 0xFF00BCD4, true, "Camera QR scanner"),
            AppItem("sc_selfie", "Take Selfie", "com.android.camera.front", ItemType.SHORTCUT, "Images", "face", 0xFFE91E63, false, "Launch front camera"),
            AppItem("sc_incognito", "Incognito Tab", "com.android.chrome.incognito", ItemType.SHORTCUT, "Tools", "visibility_off", 0xFF607D8B, false, "Private browsing"),
            AppItem("sc_navigate", "Navigate Home", "com.google.android.apps.maps.home", ItemType.SHORTCUT, "Tools", "home", 0xFF4CAF50, true, "1-tap GPS route"),

            // Actions
            AppItem("act_open_edge", "Open Edge Panel", "", ItemType.ACTION, "Tools", "apps", 0xFF00E5FF, true, "Switch to Edge Panel"),
            AppItem("act_open_quick", "Open Quick Panel", "", ItemType.ACTION, "Tools", "tune", 0xFF00BCD4, true, "Switch to Quick Panel"),
            AppItem("act_open_tools", "Open Quick Tools", "", ItemType.ACTION, "Tools", "build", 0xFF26C6DA, true, "Independent Quick Tools"),
            AppItem("act_open_media", "Open Media Control", "", ItemType.ACTION, "Tools", "music_note", 0xFF4DD0E1, true, "Independent Media Control"),
            AppItem("act_wifi", "Wi-Fi Toggle", "", ItemType.ACTION, "Tools", "wifi", 0xFF00E5FF, true, "Quick wireless control"),
            AppItem("act_bt", "Bluetooth Toggle", "", ItemType.ACTION, "Tools", "bluetooth", 0xFF2196F3, true, "Pair & connect devices"),
            AppItem("act_data", "Mobile Data", "", ItemType.ACTION, "Tools", "cell_tower", 0xFF00BCD4, true, "Cellular connection"),
            AppItem("act_hotspot", "Personal Hotspot", "", ItemType.ACTION, "Tools", "wifi_tethering", 0xFFFF9800, true, "Share internet connection"),
            AppItem("act_screenshot", "Capture Screenshot", "", ItemType.ACTION, "Tools", "screenshot", 0xFF9C27B0, true, "Save visible screen"),
            AppItem("act_lock", "Lock Screen", "", ItemType.ACTION, "Tools", "lock", 0xFFF44336, true, "Instant sleep display"),
            AppItem("act_split", "Split Screen", "", ItemType.ACTION, "Tools", "vertical_split", 0xFF4CAF50, true, "Multitask two apps"),
            AppItem("act_floating", "Floating App", "", ItemType.ACTION, "Tools", "picture_in_picture", 0xFF00E5FF, true, "Open in window mode"),
            AppItem("act_torch", "Flashlight", "", ItemType.ACTION, "Tools", "flashlight_on", 0xFFFFD600, true, "Toggle LED flash"),
            AppItem("act_mute", "Sound / Mute", "", ItemType.ACTION, "Tools", "volume_up", 0xFFE040FB, true, "Toggle ringer profile")
        )
    }

    private fun createInitialQuickSettings(): List<QuickSettingItem> {
        fun q(id: String, name: String, icon: String, color: Long, desc: String, on: Boolean = false, visible: Boolean = true) =
            QuickSettingItem(id = id, name = name, iconName = icon, isEnabled = on, color = color, description = desc, isVisible = visible)
        return listOf(
            q("open_quick", "Quick Panel", "tune", 0xFF00E5FF, "Open Quick Panel", false, false),
            q("open_tools", "Quick Tool", "build", 0xFF00BCD4, "Open Quick Tools", false, false),
            q("open_media", "Media Control", "music_note", 0xFF26C6DA, "Open Media Control", false, false),
            q("open_edge", "Edge Panel", "apps", 0xFF4DD0E1, "Open Edge Panel", false, false),
            q("one_hand", "One Hand Mode", "smartphone", 0xFF80DEEA, "Toggle One Hand Mode", false, false),
            q("none", "None", "block", 0xFF64748B, "No action", false, false),
            q("location", "Location", "location_on", 0xFF4CAF50, "Location service", false, false),
            q("active_apps", "Active Apps", "layers", 0xFF00E5FF, "Activate apps", false, false),
            q("reading_mode", "Reading", "menu_book", 0xFFFFB74D, "Reading mode", false, false),
            q("prev_app", "Prev App", "history", 0xFF00E5FF, "Switch to previous app", true),
            q("torch", "Torch", "flashlight_on", 0xFFFFD600, "LED torch"),
            q("rotate", "Rotate", "screen_rotation", 0xFF00E5FF, "Auto-rotate"),
            q("darkmode", "Dark", "dark_mode", 0xFF9575CD, "Dark mode"),
            q("battery_saver", "Saver", "battery_saver", 0xFF66BB6A, "Battery saver"),
            q("wifi", "Wi-Fi", "wifi", 0xFF00E5FF, "Wi-Fi"),
            q("bluetooth", "Bluetooth", "bluetooth", 0xFF00E5FF, "Bluetooth"),
            q("data", "Data", "cell_tower", 0xFF00E5FF, "Mobile data"),
            q("airplane", "Airplane", "airplanemode_active", 0xFFFF9800, "Airplane mode"),
            q("hotspot", "Hotspot", "wifi_tethering", 0xFFFF9800, "Hotspot"),
            q("split", "Split", "vertical_split", 0xFF4CAF50, "Split screen"),
            q("floating", "Floating", "picture_in_picture", 0xFF00E5FF, "Floating app windows"),
            q("mute", "Sound", "volume_up", 0xFF26C6DA, "Ring / vibrate", true),
            q("lock", "Lock", "lock", 0xFFFF7043, "Lock screen", visible = false),
            q("screenshot", "Shot", "photo_camera", 0xFF29B6F6, "Take screenshot", visible = false),
            q("notifications", "Notify", "notifications", 0xFF29B6F6, "Pull down notifications", visible = false),
            q("home", "Home", "home", 0xFF29B6F6, "Go to home screen", visible = false),
            q("back", "Back", "back", 0xFF29B6F6, "Back button", visible = false),
            q("power", "Power", "power_settings", 0xFFEF5350, "Power menu", visible = false)
        )
    }
}
