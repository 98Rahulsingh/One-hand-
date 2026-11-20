package com.example.model

enum class ItemType {
    APP,
    SHORTCUT,
    ACTION
}

data class WindowGeometry(
    val xOffset: Float = 0f,
    val yOffset: Float = 0f,
    val widthDp: Float = 280f,
    val heightDp: Float = 560f
)

data class AppItem(
    val id: String,
    val name: String,
    val packageName: String = "",
    val type: ItemType = ItemType.APP,
    val categoryName: String = "Recent",
    val iconName: String = "apps",
    val iconColor: Long = 0xFF00BCD4,
    val isAdded: Boolean = true,
    val subtitle: String = "",
    val usageCount: Int = 0,
    val lastUsed: Long = 0L
)

data class QuickSettingItem(
    val id: String,
    val name: String,
    val iconName: String,
    val isEnabled: Boolean,
    val color: Long = 0xFF00E5FF,
    val description: String = "",
    val isVisible: Boolean = true
)

data class MediaStreamItem(
    val id: String,
    val appName: String,
    val trackTitle: String,
    val artist: String,
    val durationText: String,
    val isActive: Boolean,
    val iconName: String,
    val progress: Float = 0.5f,
    val isPlaying: Boolean = false
)

data class AudioOutputDevice(
    val id: String,
    val name: String,
    val typeName: String,
    val isConnected: Boolean,
    val iconName: String
)

data class MediaState(
    val isPlaying: Boolean = true,
    val trackTitle: String = "Cyber Chillwave • Lo-Fi Midnight",
    val artist: String = "Aesthetic Beats Radio",
    val totalSeconds: Int = 254, // 4:14
    val currentSeconds: Int = 98, // 1:38
    val volumePercent: Float = 65f,
    val brightnessPercent: Float = 70f,
    val playbackSpeed: Float = 1.0f,
    val isRepeat: Boolean = false,
    val isShuffle: Boolean = false,
    val activeStreams: List<MediaStreamItem> = emptyList(),
    val outputDevices: List<AudioOutputDevice> = emptyList(),
    val selectedDeviceId: String = "dev_phone"
)

data class PanelSettings(
    val roundedCorners: Int = 5,      // 0 = Square, 10 = Maximum curve
    val horizontalOffset: Int = 0,    // 0 = Edge attached, 10 = Floating
    val verticalPosition: Int = 5,    // 0 = Top, 10 = Bottom
    val columns: Int = 3,             // 1 to 10
    val rows: Int = 6,                // 1 to 10
    val isRightEdge: Boolean = true,  // Left or Right edge trigger
    val handleOpacity: Float = 0.85f,
    val handleHeightDp: Float = 76f,
    val autoDetectCategories: Boolean = true,
    // Window geometries for persistent position and size
    val mainPanelGeometry: WindowGeometry = WindowGeometry(xOffset = 0f, yOffset = 0f, widthDp = 275f, heightDp = 620f),
    val mediaWindowGeometry: WindowGeometry = WindowGeometry(xOffset = 0f, yOffset = 0f, widthDp = 330f, heightDp = 580f),
    val quickAccessGeometry: WindowGeometry = WindowGeometry(xOffset = 0f, yOffset = 0f, widthDp = 320f, heightDp = 520f),
    val settingsGeometry: WindowGeometry = WindowGeometry(xOffset = 0f, yOffset = 0f, widthDp = 340f, heightDp = 590f),
    // Permissions toggles
    val accessibilityEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val mediaControlsEnabled: Boolean = false,
    val usageAccessEnabled: Boolean = false,
    val floatingWindowEnabled: Boolean = false,
    val systemControlPermissionEnabled: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val freeformEnabled: Boolean = false,
    val writeSettingsEnabled: Boolean = false,
    val bgStartEnabled: Boolean = false,
    val lockScreenEnabled: Boolean = false,
    val shortcutsEnabled: Boolean = false,
    val autostartEnabled: Boolean = false,
    val noPauseEnabled: Boolean = false,
    val freeformSupportEnabled: Boolean = false,
    val forceResizableEnabled: Boolean = false,
    // Hide / Unhide Top Media & Sliders section
    val isTopMediaSectionVisible: Boolean = true,
    // Hide / Unhide Bottom Quick Settings section
    val isBottomQuickSettingsVisible: Boolean = true,
    // Background dim opacity (0% = completely transparent, 100% = pitch black)
    val backdropOpacityPercent: Int = 45,
    // Edge handle master ON / OFF switch
    val isEdgeHandleEnabled: Boolean = true,
    // Compact UI & Extended Settings
    val rightTriggerVerticalPos: Int = 5,
    val rightTriggerWidthDp: Float = 14f,
    val rightTriggerLengthDp: Float = 76f,
    val leftTriggerVerticalPos: Int = 5,
    val leftTriggerWidthDp: Float = 14f,
    val leftTriggerLengthDp: Float = 76f,
    val panelHeightPercent: Int = 65,
    val panelWidthDp: Float = 240f,
    val verticalOffsetDp: Int = 0,
    val floatingLengthDp: Float = 380f,
    val floatingWidthDp: Float = 270f,
    val quickToolLengthDp: Float = 220f,
    val quickToolWidthDp: Float = 250f,
    val mediaControlLengthDp: Float = 160f,
    val mediaControlWidthDp: Float = 250f,
    val rememberFloatingPosition: Boolean = true,
    val openFloatingAppsBottomDown: Boolean = true,
    val oneHandModeEnabled: Boolean = false,
    val oneHandSize: Float = 4.0f,
    val oneHandCornerRight: Boolean = true,
    val oneHandOledBlack: Boolean = true,
    val oneHandUnrestrictedFloating: Boolean = true,
    val syncWithEdgePanel: Boolean = true,
    val showApkNameInGroupList: Boolean = true,
    val showActionNameInList: Boolean = true,
    val showActiveMediaInQuickPanel: Boolean = true,
    val showActiveMediaInMediaControl: Boolean = true,
    val gestureBindings: Map<String, String> = emptyMap()
)

enum class ActiveWindow {
    MAIN_PANEL,        // Unified 1: Edge Panel
    QUICK_PANEL,       // Unified 2: Quick Panel (Swipe Left from Edge Panel)
    QUICK_TOOLS,       // Independent 1: Quick Tools Floating Panel
    MEDIA_CONTROL,     // Independent 2: Media Control Floating Panel
    ADD_QUICK_ACCESS,  // Unified Add Item & Group/Action Selector
    PANEL_SETTINGS     // Collapsible Compact Settings & Permissions
}


/** A group shows an app if it is a default member (when added) or was added by hand, and was not removed by hand. */
fun isInGroup(
    item: AppItem,
    group: String,
    added: Map<String, Set<String>>,
    removed: Map<String, Set<String>>,
    manual: Set<String> = emptySet()
): Boolean {
    if (item.id in (removed[group] ?: emptySet())) return false
    val byHand = item.id in (added[group] ?: emptySet())
    if (group in manual) return byHand          // manual group: only apps you picked
    return byHand || (item.isAdded && item.categoryName.equals(group, ignoreCase = true))   // auto group
}


/** Groups that fill themselves and cannot be edited by hand. */
val SPECIAL_GROUPS = setOf("Recent", "Most Used", "All", "Unused")

/** What the long-press reorder bar is moving: a group tab or a quick tile. */
data class ReorderTarget(val isGroup: Boolean, val id: String)
