package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveWindow
import com.example.model.PanelSettings
import com.example.viewmodel.OneHandViewModel

private val GestureItems = listOf(
    "Swipe Up",
    "Swipe Up Hold",
    "Swipe Down",
    "Swipe Down Hold",
    "Swipe Inward",
    "Swipe Inward Hold",
    "Single Tap",
    "Double Tap",
    "Long Press"
)

@Composable
fun PanelSettingsWindow(
    viewModel: OneHandViewModel,
    modifier: Modifier = Modifier
) {
    val s by viewModel.panelSettings.collectAsState()
    val guideType by viewModel.guideType.collectAsState()

    var expanded by remember { mutableStateOf(setOf("Permissions")) }
    fun toggleSec(name: String) {
        expanded = if (name in expanded) expanded - name else expanded + name
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A1418))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(6.dp)
    ) {
        // Slim Title Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF12232A))
                .border(0.5.dp, Color(0xFF1E4E59), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF80DEEA),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) }
                )
                Spacer(Modifier.width(6.dp))
                Text("Settings", color = Color(0xFFE0F7FA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = if (s.oneHandSize == 4.0f) "4.0 Full" else "${s.oneHandSize}",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. RIGHT
            item {
                SlimCollapsibleSection("Right", "Right" in expanded, { toggleSec("Right") }) {
                    SlimSwitch("Right Trigger ON/OFF", s.rightTriggerEnabled) {
                        viewModel.updateSettings { cur -> cur.copy(rightTriggerEnabled = it) }
                    }
                    SlimSlider("Trigger V Pos", s.rightTriggerVerticalPos.toFloat(), 0f..10f, "${s.rightTriggerVerticalPos}") {
                        viewModel.updateSettings { cur -> cur.copy(rightTriggerVerticalPos = it.toInt()) }
                    }
                    SlimSlider("Trigger Width", s.rightTriggerWidthDp, 6f..36f, "${s.rightTriggerWidthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(rightTriggerWidthDp = it) }
                    }
                    SlimSlider("Trigger Length", s.rightTriggerLengthDp, 40f..280f, "${s.rightTriggerLengthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(rightTriggerLengthDp = it) }
                    }
                }
            }

            // 2. LEFT
            item {
                SlimCollapsibleSection("Left", "Left" in expanded, { toggleSec("Left") }) {
                    SlimSwitch("Left Trigger ON/OFF", s.leftTriggerEnabled) {
                        viewModel.updateSettings { cur -> cur.copy(leftTriggerEnabled = it) }
                    }
                    SlimSlider("Trigger V Pos", s.leftTriggerVerticalPos.toFloat(), 0f..10f, "${s.leftTriggerVerticalPos}") {
                        viewModel.updateSettings { cur -> cur.copy(leftTriggerVerticalPos = it.toInt()) }
                    }
                    SlimSlider("Trigger Width", s.leftTriggerWidthDp, 6f..36f, "${s.leftTriggerWidthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(leftTriggerWidthDp = it) }
                    }
                    SlimSlider("Trigger Length", s.leftTriggerLengthDp, 40f..280f, "${s.leftTriggerLengthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(leftTriggerLengthDp = it) }
                    }
                }
            }

            // 3. EDGE POSITION (all separate horizontal sliders)
            item {
                SlimCollapsibleSection("Edge Position", "Edge Position" in expanded, { toggleSec("Edge Position") }) {
                    SlimSlider("Panel V Pos", s.verticalPosition.toFloat(), 0f..10f, "${s.verticalPosition}") {
                        viewModel.updateVerticalPosition(it.toInt())
                    }
                    SlimSlider("Panel Height", s.panelHeightPercent.toFloat(), 30f..95f, "${s.panelHeightPercent}%") {
                        viewModel.updateSettings { cur -> cur.copy(panelHeightPercent = it.toInt()) }
                    }
                    SlimSlider("Panel Width", s.panelWidthDp, 160f..340f, "${s.panelWidthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(panelWidthDp = it) }
                    }
                    SlimSlider("Columns", s.columns.toFloat(), 1f..10f, "${s.columns}") {
                        viewModel.updateColumns(it.toInt())
                    }
                    SlimSlider("Rows", s.rows.toFloat(), 1f..12f, "${s.rows}") {
                        viewModel.updateRows(it.toInt())
                    }
                    SlimSlider("Rounded Corners", s.roundedCorners.toFloat(), 0f..10f, "${s.roundedCorners}") {
                        viewModel.updateRoundedCorners(it.toInt())
                    }
                    SlimSlider("Horiz Offset", s.horizontalOffset.toFloat(), 0f..10f, "${s.horizontalOffset}") {
                        viewModel.updateHorizontalOffset(it.toInt())
                    }
                    SlimSlider("Vert Offset", s.verticalOffsetDp.toFloat(), -60f..60f, "${s.verticalOffsetDp}dp") {
                        viewModel.updateSettings { cur -> cur.copy(verticalOffsetDp = it.toInt()) }
                    }
                }
            }

            // 4. GESTURE CONTROLS
            item {
                SlimCollapsibleSection("Gesture Controls", "Gesture Controls" in expanded, { toggleSec("Gesture Controls") }) {
                    GestureItems.forEach { g ->
                        val bound = s.gestureBindings[g] ?: "None"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0E2329), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(g, color = Color(0xFFE0F7FA), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                Text(if (bound == "None") "Unassigned" else bound, color = if (bound == "None") Color(0xFF64748B) else Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = if (bound == "None") "+ Add Item" else "Change ($bound)",
                                color = Color(0xFF00E5FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { viewModel.startBindGesture(g) }
                            )
                        }
                    }
                }
            }

            // 5. FLOATING WINDOW SIZE
            item {
                SlimCollapsibleSection("Floating Window Size", "Floating Window Size" in expanded, { toggleSec("Floating Window Size") }) {
                    SlimSlider("Floating Length", s.floatingLengthDp, 200f..640f, "${s.floatingLengthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(floatingLengthDp = it) }
                    }
                    SlimSlider("Floating Width", s.floatingWidthDp, 180f..380f, "${s.floatingWidthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(floatingWidthDp = it) }
                    }
                    SlimSwitch("Remember closed position", s.rememberFloatingPosition) {
                        viewModel.updateSettings { cur -> cur.copy(rememberFloatingPosition = it) }
                    }
                }
            }

            // 6. FLOATING CENTRE
            item {
                SlimCollapsibleSection("Floating Centre", "Floating Centre" in expanded, { toggleSec("Floating Centre") }) {
                    SlimSlider("Height Quick", s.quickPanelLengthDp, 220f..560f, "${s.quickPanelLengthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(quickPanelLengthDp = it) }
                    }
                    SlimSlider("Width Quick", s.quickPanelWidthDp, 180f..360f, "${s.quickPanelWidthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(quickPanelWidthDp = it) }
                    }
                    SlimSlider("Length Tool", s.quickToolLengthDp, 140f..380f, "${s.quickToolLengthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(quickToolLengthDp = it) }
                    }
                    SlimSlider("Width Tool", s.quickToolWidthDp, 160f..360f, "${s.quickToolWidthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(quickToolWidthDp = it) }
                    }
                    SlimSlider("Length Media", s.mediaControlLengthDp, 100f..280f, "${s.mediaControlLengthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(mediaControlLengthDp = it) }
                    }
                    SlimSlider("Width Media", s.mediaControlWidthDp, 160f..360f, "${s.mediaControlWidthDp.toInt()}dp") {
                        viewModel.updateSettings { cur -> cur.copy(mediaControlWidthDp = it) }
                    }
                    SlimSwitch("Remember closed position", s.rememberFloatingPosition) {
                        viewModel.updateSettings { cur -> cur.copy(rememberFloatingPosition = it) }
                    }
                    SlimSwitch("Open floating apps bottom-down", s.openFloatingAppsBottomDown) {
                        viewModel.updateSettings { cur -> cur.copy(openFloatingAppsBottomDown = it) }
                    }
                }
            }

            // 7. ONE HAND CONTROL (4.0" Mini Display + OLED Black Battery Saver)
            item {
                SlimCollapsibleSection("One Hand Control", "One Hand Control" in expanded, { toggleSec("One Hand Control") }) {
                    SlimSwitch("Enable One Hand Mode", s.oneHandModeEnabled) {
                        viewModel.updateSettings { cur -> cur.copy(oneHandModeEnabled = it) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mini Display:", color = Color(0xFF80DEEA), fontSize = 10.sp)
                        listOf(3.5f, 4.0f, 4.5f).forEach { sz ->
                            val active = s.oneHandSize == sz
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) Color(0xFF00BCD4) else Color(0xFF102A31))
                                    .clickable { viewModel.updateSettings { cur -> cur.copy(oneHandSize = sz) } }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (sz == 4.0f) "4.0\" (Full)" else "${sz}\"",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Anchor:", color = Color(0xFF80DEEA), fontSize = 10.sp)
                        listOf(false to "Bottom-Left", true to "Bottom-Right").forEach { (isRight, label) ->
                            val active = s.oneHandCornerRight == isRight
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) Color(0xFF00BCD4) else Color(0xFF102A31))
                                    .clickable { viewModel.updateSettings { cur -> cur.copy(oneHandCornerRight = isRight) } }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    SlimSwitch("OLED Pure Black (#000000 Battery Saver)", s.oneHandOledBlack) {
                        viewModel.updateSettings { cur -> cur.copy(oneHandOledBlack = it) }
                    }
                    SlimSwitch("Unrestricted Floating Apps", s.oneHandUnrestrictedFloating) {
                        viewModel.updateSettings { cur -> cur.copy(oneHandUnrestrictedFloating = it) }
                    }
                    SlimSwitch("Sync with Edge Panel", s.syncWithEdgePanel) {
                        viewModel.updateSettings { cur -> cur.copy(syncWithEdgePanel = it) }
                    }
                }
            }

            // 7B. DIAGNOSTICS CHECKER (Quick Mini-Macro)
            item {
                var diagText by remember { mutableStateOf("") }
                SlimCollapsibleSection("Diagnostics Checker", "Diagnostics Checker" in expanded, { toggleSec("Diagnostics Checker") }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00BCD4))
                            .clickable { diagText = viewModel.runMacroDiagnostics() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Run Mini-Macro Check", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (diagText.isNotBlank()) {
                        Text(
                            text = diagText,
                            color = Color(0xFF80DEEA),
                            fontSize = 9.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF09161A), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        )
                    }
                }
            }

            // 8. PERMISSIONS + OTHER
            item {
                SlimCollapsibleSection("Permissions", "Permissions" in expanded, { toggleSec("Permissions") }) {
                    val corePerms = listOf(
                        Triple("accessibility", "Accessibility", s.accessibilityEnabled),
                        Triple("media_controls", "Media Control", s.mediaControlsEnabled),
                        Triple("usage_access", "Usage Access", s.usageAccessEnabled),
                        Triple("floating_window", "Floating Window", s.floatingWindowEnabled),
                        Triple("write_settings", "Modify Settings", s.writeSettingsEnabled),
                        Triple("bg_start", "Background Launch", s.bgStartEnabled),
                        Triple("battery_optimization", "Battery Optimization", s.batteryOptimizationIgnored),
                        Triple("autostart", "Auto Start", s.autostartEnabled),
                        Triple("secure_settings", "Write Secure Settings (ADB/Shizuku)", s.secureSettingsEnabled)
                    )
                    corePerms.forEach { (id, title, granted) ->
                        SlimPermissionRow(title, granted) { viewModel.openSystemScreen(id) }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("Other", color = Color(0xFF80DEEA), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    val otherPerms = listOf(
                        Triple("notifications", "Notifications", s.notificationsEnabled),
                        Triple("freeform", "Freeform Windows", s.freeformEnabled),
                        Triple("lock_screen", "Show on Lock Screen", s.lockScreenEnabled),
                        Triple("shortcuts", "Home Shortcuts", s.shortcutsEnabled),
                        Triple("no_pause", "No Pause if Unused", s.noPauseEnabled)
                    )
                    otherPerms.forEach { (id, title, granted) ->
                        SlimPermissionRow(title, granted) { viewModel.openSystemScreen(id) }
                    }
                }
            }
        }
    }

    guideType?.let { type ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionGuide() },
            title = { Text(type.replace("_", " ").uppercase(), fontSize = 14.sp) },
            text = { Text(viewModel.guideFor(type), fontSize = 11.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.openSystemScreen(type) }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPermissionGuide() }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SlimCollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1D23), RoundedCornerShape(8.dp))
            .border(0.5.dp, Color(0xFF1E4E59), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(5.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color(0xFFE0F7FA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(if (expanded) "Hide ▲" else "Show ▼", color = Color(0xFF00E5FF), fontSize = 9.sp)
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SlimSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color(0xFF80DEEA),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(88.dp)
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f).height(18.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00BCD4),
                inactiveTrackColor = Color(0xFF1E4E59)
            )
        )
        Text(valueText, color = Color(0xFFE0F7FA), fontSize = 9.sp, modifier = Modifier.width(36.dp).padding(start = 4.dp))
    }
}

@Composable
private fun SlimSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFE0F7FA), fontSize = 10.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(18.dp),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00BCD4))
        )
    }
}

@Composable
private fun SlimPermissionRow(title: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF12262E))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color(0xFFE0F7FA), fontSize = 10.sp)
        Text(
            text = if (granted) "Granted" else "Grant →",
            color = if (granted) Color(0xFF34D399) else Color(0xFFFBBF24),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
