package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveWindow
import com.example.model.PanelSettings
import com.example.ui.components.FloatingWindowCard
import com.example.ui.components.TealSliderWithBadge
import com.example.viewmodel.OneHandViewModel

@Composable
fun PanelSettingsWindow(
    viewModel: OneHandViewModel,
    modifier: Modifier = Modifier
) {
    val panelSettings by viewModel.panelSettings.collectAsState()
    val permissionDialogInfo by viewModel.permissionDialogInfo.collectAsState()
    val guideType by viewModel.guideType.collectAsState()
    var selectedTab by remember { mutableStateOf("Side Bar") }
    val settingsListState = rememberLazyListState()

    // Dialog for "How to enable?"
    permissionDialogInfo?.let { (title, description) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionGuide() },
            title = {
                Text(
                    text = title,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                SelectionContainer {
                    Text(
                        text = description,
                        color = Color(0xFFE0F7FA),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            dismissButton = {
                guideType?.let { type ->
                    TextButton(onClick = { viewModel.openSystemScreen(type) }) {
                        Text("Open settings", color = Color(0xFF80DEEA))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dismissPermissionGuide() },
                    modifier = Modifier.testTag("dismiss_guide_dialog")
                ) {
                    Text("Got it", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF0B252B),
            shape = RoundedCornerShape(16.dp)
        )
    }

    FloatingWindowCard(
        title = "SETTINGS",
        initialGeometry = panelSettings.settingsGeometry,
        onGeometryChange = { x, y, w, h ->
            viewModel.updateSettingsGeometry(x, y, w, h)
        },
        onClose = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF081C20))
        ) {
            LazyColumn(
                state = settingsListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                item {
                    // Settings Sliders Card matching Image 2 (Clean & Compact!)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B242A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16444E))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Slider 1: Rounded corners
                            TealSliderWithBadge(
                                title = "Rounded corners",
                                subtitle = "Panel corner curvature (0 = Square, 10 = Maximum curve)",
                                value = panelSettings.roundedCorners,
                                range = 0..10,
                                onValueChange = { viewModel.updateRoundedCorners(it) },
                                testTag = "slider_rounded_corners"
                            )

                            // Slider 2: Horizontal offset
                            TealSliderWithBadge(
                                title = "Horizontal offset",
                                subtitle = "Panel inward distance from edge (0 = Edge attached, 10 = Floating)",
                                value = panelSettings.horizontalOffset,
                                range = 0..10,
                                onValueChange = { viewModel.updateHorizontalOffset(it) },
                                testTag = "slider_horizontal_offset"
                            )

                            // Slider 3: Vertical position
                            TealSliderWithBadge(
                                title = "Vertical position",
                                subtitle = "Panel vertical alignment along screen (0 = Top, 10 = Bottom)",
                                value = panelSettings.verticalPosition,
                                range = 0..10,
                                onValueChange = { viewModel.updateVerticalPosition(it) },
                                testTag = "slider_vertical_position"
                            )

                            // Slider 4: Columns
                            TealSliderWithBadge(
                                title = "Columns",
                                subtitle = "App icons per row in side panel",
                                value = panelSettings.columns,
                                range = 1..4,
                                onValueChange = { viewModel.updateColumns(it) },
                                testTag = "slider_columns"
                            )

                            // Slider 5: Rows
                            TealSliderWithBadge(
                                title = "Rows",
                                subtitle = "Visible rows per page in side panel",
                                value = panelSettings.rows,
                                range = 1..10,
                                onValueChange = { viewModel.updateRows(it) },
                                testTag = "slider_rows"
                            )

                            // Slider 6: Background Dimming Opacity (0% to 100%)
                            TealSliderWithBadge(
                                title = "Background dimming opacity",
                                subtitle = "Darkness level when side panel opens (0 = Clear, 100 = Solid Black)",
                                value = panelSettings.backdropOpacityPercent,
                                range = 0..100,
                                onValueChange = { viewModel.updateBackdropOpacity(it) },
                                testTag = "slider_backdrop_opacity"
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Edge Placement Segmented Selector with Master ON/OFF Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Panel Screen Edge Placement",
                                        color = Color(0xFFE0F7FA),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (panelSettings.isEdgeHandleEnabled) "Edge handle is ACTIVE" else "Edge handle is OFF",
                                        color = if (panelSettings.isEdgeHandleEnabled) Color(0xFF00E5FF) else Color(0xFFFF8A80),
                                        fontSize = 10.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (panelSettings.isEdgeHandleEnabled) "ON" else "OFF",
                                        color = if (panelSettings.isEdgeHandleEnabled) Color(0xFF00E5FF) else Color(0xFF80DEEA).copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = panelSettings.isEdgeHandleEnabled,
                                        onCheckedChange = { viewModel.setEdgeHandleEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00E5FF),
                                            checkedTrackColor = Color(0xFF005662),
                                            uncheckedThumbColor = Color(0xFF80DEEA),
                                            uncheckedTrackColor = Color(0xFF1B4048)
                                        ),
                                        modifier = Modifier.testTag("switch_edge_handle_master")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F323A))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Left Edge Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (!panelSettings.isRightEdge) Color(0xFF00E5FF) else Color.Transparent)
                                        .clickable { viewModel.setEdgeSide(false) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "LEFT EDGE",
                                        color = if (!panelSettings.isRightEdge) Color(0xFF00363A) else Color(0xFF80DEEA),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Right Edge Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (panelSettings.isRightEdge) Color(0xFF00E5FF) else Color.Transparent)
                                        .clickable { viewModel.setEdgeSide(true) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "RIGHT EDGE",
                                        color = if (panelSettings.isRightEdge) Color(0xFF00363A) else Color(0xFF80DEEA),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // PERMISSIONS SECTION (Compact & Clean)
                    // ==========================================
                    Text(
                        text = "PERMISSIONS & SERVICES",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B242A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16444E))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // 1: New Dedicated System Controls & Quick Toggles Permission (Requested by User)
                            PermissionTile(
                                title = "System Controls & Quick Toggles",
                                description = "Powers 1-tap hardware toggles (Mobile Net, Wi-Fi, Airplane Mode, Bluetooth), App activity inspector, Switch Previous App, Split Screen, and Floating Windows",
                                isGranted = panelSettings.systemControlPermissionEnabled,
                                icon = Icons.Default.Settings,
                                onToggle = { viewModel.togglePermission("system_control") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide(
                                        "System Controls & Secure Settings",
                                        viewModel.setupGuide()
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 2: Accessibility
                            PermissionTile(
                                title = "Accessibility",
                                description = "Powers widget buttons: lock screen, screenshots, recents, previous app swap, and quick settings",
                                isGranted = panelSettings.accessibilityEnabled,
                                icon = Icons.Default.AccessibilityNew,
                                onToggle = { viewModel.togglePermission("accessibility") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide(
                                        "Accessibility Service",
                                        "Enables system actions like locking screen, pulling notifications and taking screenshots.\n\nSteps:\n1. Open Settings > Accessibility\n2. Turn ON One Hand Control."
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 3: Notifications
                            PermissionTile(
                                title = "Notifications",
                                description = "Delivers search service alerts and stays active",
                                isGranted = panelSettings.notificationsEnabled,
                                icon = Icons.Default.Notifications,
                                onToggle = { viewModel.togglePermission("notifications") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide(
                                        "Notification Access",
                                        "Keeps gesture service running.\n\nSteps:\n1. Settings > Special app access > Notification access\n2. Enable One Hand Control."
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 4: Media controls
                            PermissionTile(
                                title = "Media controls",
                                description = "Shows now-playing music controls in your search overlay",
                                isGranted = panelSettings.mediaControlsEnabled,
                                icon = Icons.Default.MusicNote,
                                onToggle = { viewModel.togglePermission("media_controls") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide(
                                        "Media Controls",
                                        "Allows seek bar and 15s forward/rewind for Spotify, YouTube, etc.\n\nSteps:\n1. Grant Notification & Media Listener access."
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 5: Usage access
                            PermissionTile(
                                title = "Usage access",
                                description = "Surfaces your recently used apps for one-tap search and app activity",
                                isGranted = panelSettings.usageAccessEnabled,
                                icon = Icons.Default.QueryStats,
                                onToggle = { viewModel.togglePermission("usage_access") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide(
                                        "Usage Access",
                                        "Surfaces Recent & Most Used apps.\n\nSteps:\n1. Settings > Usage access\n2. Enable One Hand Control."
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 6: Floating window permission
                            PermissionTile(
                                title = "Floating window permission",
                                description = "Allows edge trigger handle and floating popups to draw over other apps",
                                isGranted = panelSettings.floatingWindowEnabled,
                                icon = Icons.Default.PictureInPicture,
                                onToggle = { viewModel.togglePermission("floating_window") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide(
                                        "Floating Window Overlay",
                                        "Allows edge handle on top of all apps.\n\nSteps:\n1. Settings > Special app access > Display over other apps\n2. Enable One Hand Control."
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 6b: Developer options for floating windows
                            PermissionTile(
                                title = "Enable freeform windows",
                                description = "Developer option. Lets apps open in small floating windows. Restart the phone once after switching on",
                                isGranted = panelSettings.freeformSupportEnabled,
                                icon = Icons.Default.PictureInPicture,
                                onToggle = { viewModel.togglePermission("freeform_support") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide("Enable freeform windows", viewModel.guideFor("freeform_support"))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            PermissionTile(
                                title = "Force activities to be resizable",
                                description = "Developer option. Lets every app shrink into a small window",
                                isGranted = panelSettings.forceResizableEnabled,
                                icon = Icons.Default.PictureInPicture,
                                onToggle = { viewModel.togglePermission("force_resizable") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide("Force activities to be resizable", viewModel.guideFor("force_resizable"))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 6c: Modify system settings
                            PermissionTile(
                                title = "Modify system settings",
                                description = "Needed for Auto-rotate and brightness quick toggles",
                                isGranted = panelSettings.writeSettingsEnabled,
                                icon = Icons.Default.Settings,
                                onToggle = { viewModel.togglePermission("write_settings") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide("Modify system settings", viewModel.guideFor("write_settings"))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 6e: Xiaomi / HyperOS extra permissions
                            PermissionTile(
                                title = "Open new windows in background",
                                description = "Needed so apps can open from the panel while another app is on screen",
                                isGranted = panelSettings.bgStartEnabled,
                                icon = Icons.Default.Settings,
                                onToggle = { viewModel.togglePermission("bg_start") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide("Open new windows in background", viewModel.guideFor("bg_start"))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            PermissionTile(
                                title = "Show on lock screen",
                                description = "Lets the panel work on the lock screen",
                                isGranted = panelSettings.lockScreenEnabled,
                                icon = Icons.Default.Settings,
                                onToggle = { viewModel.togglePermission("lock_screen") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide("Show on lock screen", viewModel.guideFor("lock_screen"))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            PermissionTile(
                                title = "Home screen shortcuts",
                                description = "Lets the app add shortcuts to the home screen",
                                isGranted = panelSettings.shortcutsEnabled,
                                icon = Icons.Default.Settings,
                                onToggle = { viewModel.togglePermission("shortcuts") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide("Home screen shortcuts", viewModel.guideFor("shortcuts"))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            PermissionTile(
                                title = "Autostart",
                                description = "Lets the phone start the panel after a restart or when it was closed",
                                isGranted = panelSettings.autostartEnabled,
                                icon = Icons.Default.Settings,
                                onToggle = { viewModel.togglePermission("autostart") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide("Autostart", viewModel.guideFor("autostart"))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            PermissionTile(
                                title = "Pause app activity if unused: OFF",
                                description = "Stops the phone from switching the app off when you do not open it for a while",
                                isGranted = panelSettings.noPauseEnabled,
                                icon = Icons.Default.Settings,
                                onToggle = { viewModel.togglePermission("no_pause") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide("Pause app activity if unused: OFF", viewModel.guideFor("no_pause"))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(
                                onClick = { viewModel.runDiagnostics() },
                                modifier = Modifier.testTag("run_diagnostics_button")
                            ) {
                                Text(
                                    text = "Run diagnostics (shows why toggles or floating windows fail)",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 12.sp
                                )
                            }

                            // 7: Battery Optimization & Background Keeper (Stopper Prevention)
                            PermissionTile(
                                title = "Battery Optimization & Background Keeper",
                                description = "Prevents system stoppers from killing One Hand Control service, edge handle, and quick toggles",
                                isGranted = panelSettings.batteryOptimizationIgnored,
                                icon = Icons.Default.BatteryChargingFull,
                                onToggle = { viewModel.togglePermission("battery_optimization") },
                                onHowToEnable = {
                                    viewModel.showPermissionGuide(
                                        "Background Keeper & Battery Exemption",
                                        "Prevents Android from killing gesture services and background triggers in low memory states.\n\nSteps:\n1. Open Settings > Apps > Special access\n2. Battery optimization\n3. Select 'All apps' -> Find 'One Hand Control'\n4. Select 'Don't optimize'."
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun PermissionTile(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onToggle: () -> Unit,
    onHowToEnable: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F2E36))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1B4048)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isGranted) Color(0xFF00E5FF) else Color(0xFF80DEEA),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = title,
                        color = Color(0xFFE0F7FA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        color = Color(0xFF80DEEA).copy(alpha = 0.75f),
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Switch(
                checked = isGranted,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00E5FF),
                    checkedTrackColor = Color(0xFF005662),
                    uncheckedThumbColor = Color(0xFF80DEEA),
                    uncheckedTrackColor = Color(0xFF1B4048)
                ),
                modifier = Modifier.testTag("perm_switch_${title.lowercase().replace(" ", "_")}")
            )
        }

        // "How to enable?" guide button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onHowToEnable() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("how_to_enable_${title.lowercase().replace(" ", "_")}")
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "How to enable",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "How to enable?",
                    color = Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

