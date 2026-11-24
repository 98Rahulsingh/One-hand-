package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Rect
import androidx.compose.ui.platform.LocalContext
import com.example.data.FloatingStore
import com.example.model.ActiveWindow
import com.example.viewmodel.OneHandViewModel
import kotlin.math.roundToInt

/**
 * 1. QUICK PANEL (Swipe Right from Edge Panel):
 *    Brightness -> Volume -> Quick Settings (same Edge UI) -> Media Seeker -> 5 Media Buttons -> Active Media
 */
@Composable
fun QuickPanelScreen(viewModel: OneHandViewModel, modifier: Modifier = Modifier) {
    val quickSettings by viewModel.quickSettings.collectAsState()
    val media by viewModel.mediaState.collectAsState()
    val panelSettings by viewModel.panelSettings.collectAsState()
    val ctx = LocalContext.current
    val store = remember { FloatingStore(ctx) }
    val savedRect = remember { if (panelSettings.rememberFloatingPosition) store.get("v5_quick_panel") else null }
    var offX by remember { mutableFloatStateOf(savedRect?.left?.toFloat() ?: 0f) }
    var offY by remember { mutableFloatStateOf(savedRect?.top?.toFloat() ?: 0f) }

    Column(
        modifier = modifier
            .offset { IntOffset(offX.roundToInt(), offY.roundToInt()) }
            .background(Color(0xFF0A1418), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0xFF1E4E59), RoundedCornerShape(12.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        V5DraggableHeader(
            title = "QUICK PANEL",
            onDrag = { dx, dy -> offX += dx; offY += dy },
            onBack = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) },
            onClose = { if (panelSettings.rememberFloatingPosition) store.put("v5_quick_panel", Rect(offX.toInt(), offY.toInt(), 0, 0)); viewModel.togglePanel(false) }
        )

        V5EdgeSlider("Bright", media.brightnessPercent, 0f..100f) { viewModel.setBrightness(it) }
        V5EdgeSlider("Volume", media.volumePercent, 0f..100f) { viewModel.setVolume(it) }

        QuickSettingsRow(
            quickSettings = quickSettings,
            showActionNames = panelSettings.showActionNameInList,
            onToggleSetting = { viewModel.toggleQuickSetting(it) },
            onOpenSettings = { viewModel.navigateTo(ActiveWindow.PANEL_SETTINGS) },
            onOpenActionsAdd = { viewModel.openAddWindow(forTiles = true) },
            fiveColumns = true
        )

        V5EdgeSlider(
            label = null,
            value = media.currentSeconds.toFloat(),
            range = 0f..media.totalSeconds.coerceAtLeast(1).toFloat(),
            showSeekTime = true
        ) { viewModel.seekMedia(it.toInt()) }

        V5Media5Buttons(viewModel, media.isPlaying)

        ActiveMediaStreams(
            streams = media.activeStreams,
            mediaAccessOn = panelSettings.mediaControlsEnabled,
            onSelect = { viewModel.switchActiveMedia(it) },
            onEnableAccess = { viewModel.togglePermission("media_controls") },
            onScan = { viewModel.rescanMedia() }
        )
    }
}

/**
 * 2. QUICK TOOLS (Independent Movable Floating Panel):
 *    Brightness -> Volume -> Quick Settings (same Edge UI)
 */
@Composable
fun QuickToolsScreen(viewModel: OneHandViewModel, modifier: Modifier = Modifier) {
    val quickSettings by viewModel.quickSettings.collectAsState()
    val media by viewModel.mediaState.collectAsState()
    val panelSettings by viewModel.panelSettings.collectAsState()
    val ctx = LocalContext.current
    val store = remember { FloatingStore(ctx) }
    val savedRect = remember { if (panelSettings.rememberFloatingPosition) store.get("v5_quick_tools") else null }
    var offX by remember { mutableFloatStateOf(savedRect?.left?.toFloat() ?: 0f) }
    var offY by remember { mutableFloatStateOf(savedRect?.top?.toFloat() ?: 0f) }

    Column(
        modifier = modifier
            .offset { IntOffset(offX.roundToInt(), offY.roundToInt()) }
            .fillMaxWidth()
            .background(Color(0xFF0A1418), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0xFF1E4E59), RoundedCornerShape(12.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        V5DraggableHeader(
            title = "QUICK TOOLS",
            onDrag = { dx, dy -> offX += dx; offY += dy },
            onBack = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) },
            onClose = { if (panelSettings.rememberFloatingPosition) store.put("v5_quick_tools", Rect(offX.toInt(), offY.toInt(), 0, 0)); viewModel.togglePanel(false) }
        )

        V5EdgeSlider("Bright", media.brightnessPercent, 0f..100f) { viewModel.setBrightness(it) }
        V5EdgeSlider("Volume", media.volumePercent, 0f..100f) { viewModel.setVolume(it) }

        QuickSettingsRow(
            quickSettings = quickSettings,
            showActionNames = panelSettings.showActionNameInList,
            onToggleSetting = { viewModel.toggleQuickSetting(it) },
            onOpenSettings = { viewModel.navigateTo(ActiveWindow.PANEL_SETTINGS) },
            onOpenActionsAdd = { viewModel.openAddWindow(forTiles = true) },
            fiveColumns = true
        )
    }
}

/**
 * 3. MEDIA CONTROL (Independent Movable Floating Panel):
 *    Media Seeker -> 5 Buttons -> Speed (1x, 1.5x, 2x, 2.5x, 3x) -> Active Media (same Edge UI)
 */
@Composable
fun MediaControlScreen(viewModel: OneHandViewModel, modifier: Modifier = Modifier) {
    val media by viewModel.mediaState.collectAsState()
    val panelSettings by viewModel.panelSettings.collectAsState()
    val ctx = LocalContext.current
    val store = remember { FloatingStore(ctx) }
    val savedRect = remember { if (panelSettings.rememberFloatingPosition) store.get("v5_media_control") else null }
    var offX by remember { mutableFloatStateOf(savedRect?.left?.toFloat() ?: 0f) }
    var offY by remember { mutableFloatStateOf(savedRect?.top?.toFloat() ?: 0f) }

    Column(
        modifier = modifier
            .offset { IntOffset(offX.roundToInt(), offY.roundToInt()) }
            .fillMaxWidth()
            .background(Color(0xFF0A1418), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0xFF1E4E59), RoundedCornerShape(12.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        V5DraggableHeader(
            title = "MEDIA CONTROL",
            onDrag = { dx, dy -> offX += dx; offY += dy },
            onBack = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) },
            onClose = { if (panelSettings.rememberFloatingPosition) store.put("v5_media_control", Rect(offX.toInt(), offY.toInt(), 0, 0)); viewModel.togglePanel(false) }
        )

        V5EdgeSlider(
            label = "Seek",
            value = media.currentSeconds.toFloat(),
            range = 0f..media.totalSeconds.coerceAtLeast(1).toFloat(),
            showSeekTime = true
        ) { viewModel.seekMedia(it.toInt()) }

        V5Media5Buttons(viewModel, media.isPlaying)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(1.0f, 1.5f, 2.0f, 2.5f, 3.0f).forEach { spd ->
                val active = media.playbackSpeed == spd
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (active) Color(0xFF00BCD4) else Color(0xFF12232A))
                        .clickable { viewModel.setPlaybackSpeed(spd) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (spd % 1f == 0f) "${spd.toInt()}x" else "${spd}x",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        ActiveMediaStreams(
            streams = media.activeStreams,
            mediaAccessOn = panelSettings.mediaControlsEnabled,
            onSelect = { viewModel.switchActiveMedia(it) },
            onEnableAccess = { viewModel.togglePermission("media_controls") },
            onScan = { viewModel.rescanMedia() }
        )
    }
}

@Composable
private fun V5DraggableHeader(
    title: String,
    onDrag: (Float, Float) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF12232A))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "← $title",
            color = Color(0xFFE0F7FA),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onBack)
        )
        Text(
            text = "✕",
            color = Color(0xFF80DEEA),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 6.dp).clickable(onClick = onClose)
        )
    }
}

@Composable
private fun V5EdgeSlider(label: String?, value: Float, range: ClosedFloatingPointRange<Float>, showSeekTime: Boolean = false, onChange: (Float) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var preview by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.fillMaxWidth()) {
        if (showSeekTime && dragging) {
            Text("${preview.toInt() / 60}:${(preview.toInt() % 60).toString().padStart(2, '0')} / ${range.endInclusive.toInt() / 60}:${(range.endInclusive.toInt() % 60).toString().padStart(2, '0')}", color = Color(0xFF80DEEA), fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Row(
        modifier = Modifier.fillMaxWidth().height(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label != null) Text(label, color = Color(0xFF80DEEA), fontSize = 9.sp, modifier = Modifier.width(42.dp))
        Slider(
            value = (if (showSeekTime) preview else value).coerceIn(range.start, range.endInclusive),
            onValueChange = { preview = it; dragging = true; onChange(it) },
            onValueChangeFinished = { dragging = false },
            valueRange = range,
            modifier = Modifier.weight(1f).height(16.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00BCD4),
                inactiveTrackColor = Color(0xFF1E4E59)
            )
        )
        }
    }
}

@Composable
private fun V5Media5Buttons(viewModel: OneHandViewModel, isPlaying: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val btns = listOf(
            "⏮" to { viewModel.previousVideo() },
            "-15s" to { viewModel.skipBackward15() },
            (if (isPlaying) "⏸" else "▶") to { viewModel.togglePlayPause() },
            "+15s" to { viewModel.skipForward15() },
            "⏭" to { viewModel.nextVideoOrChapter() }
        )
        btns.forEach { (label, action) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF12232A))
                    .clickable { action() },
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = Color(0xFFE0F7FA), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
