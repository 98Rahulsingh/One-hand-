package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveWindow
import com.example.model.MediaStreamItem
import com.example.model.QuickSettingItem
import com.example.viewmodel.OneHandViewModel

@Composable
fun QuickPanelScreen(viewModel: OneHandViewModel, modifier: Modifier = Modifier) {
    val quickSettings by viewModel.quickSettings.collectAsState()
    val media by viewModel.mediaState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1418), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0xFF1E4E59), RoundedCornerShape(12.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        V5SlimHeader("QUICK PANEL", onBack = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) }) {
            viewModel.togglePanel(false)
        }

        V5QuickTilesGrid(
            items = quickSettings.filter { it.isVisible },
            onToggle = { viewModel.toggleQuickSetting(it) },
            onAdd = { viewModel.navigateTo(ActiveWindow.ADD_QUICK_ACCESS) }
        )

        V5SlimSlider("Bright", media.brightnessPercent, 0f..100f) { viewModel.setBrightness(it) }
        V5SlimSlider("Volume", media.volumePercent, 0f..100f) { viewModel.setVolume(it) }
        V5SlimSlider(
            label = "Seek",
            value = media.currentSeconds.toFloat(),
            range = 0f..media.totalSeconds.coerceAtLeast(1).toFloat()
        ) { viewModel.seekMedia(it.toInt()) }

        V5MediaActionButtons(viewModel, media.isPlaying)
        V5ActiveMediaCleanList(media.activeStreams) { viewModel.switchActiveMedia(it) }
    }
}

@Composable
fun QuickToolsScreen(viewModel: OneHandViewModel, modifier: Modifier = Modifier) {
    val quickSettings by viewModel.quickSettings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1418), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0xFF1E4E59), RoundedCornerShape(12.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        V5SlimHeader("QUICK TOOLS", onBack = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) }) {
            viewModel.togglePanel(false)
        }
        V5QuickTilesGrid(
            items = quickSettings.filter { it.isVisible },
            onToggle = { viewModel.toggleQuickSetting(it) },
            onAdd = { viewModel.navigateTo(ActiveWindow.ADD_QUICK_ACCESS) }
        )
    }
}

@Composable
fun MediaControlScreen(viewModel: OneHandViewModel, modifier: Modifier = Modifier) {
    val media by viewModel.mediaState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1418), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0xFF1E4E59), RoundedCornerShape(12.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        V5SlimHeader("MEDIA CONTROL", onBack = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) }) {
            viewModel.togglePanel(false)
        }

        V5SlimSlider(
            label = "Seek",
            value = media.currentSeconds.toFloat(),
            range = 0f..media.totalSeconds.coerceAtLeast(1).toFloat()
        ) { viewModel.seekMedia(it.toInt()) }

        V5MediaActionButtons(viewModel, media.isPlaying)

        // Speed row: 1x, 1.5x, 2x, 2.5x, 3x
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

        V5ActiveMediaCleanList(media.activeStreams) { viewModel.switchActiveMedia(it) }
    }
}

@Composable
private fun V5SlimHeader(title: String, onBack: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF12232A))
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
            modifier = Modifier
                .padding(end = 6.dp)
                .clickable(onClick = onClose)
        )
    }
}

@Composable
private fun V5QuickTilesGrid(
    items: List<QuickSettingItem>,
    onToggle: (String) -> Unit,
    onAdd: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items(items) { item ->
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (item.isEnabled) Color(0xFF00BCD4).copy(alpha = 0.25f) else Color(0xFF10252C))
                    .border(
                        0.5.dp,
                        if (item.isEnabled) Color(0xFF00E5FF) else Color(0xFF1E4E59),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onToggle(item.id) }
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name.take(8),
                    color = Color(0xFFE0F7FA),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        item {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF122B33))
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun V5SlimSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF80DEEA), fontSize = 9.sp, modifier = Modifier.width(42.dp))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
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

@Composable
private fun V5MediaActionButtons(viewModel: OneHandViewModel, isPlaying: Boolean) {
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

@Composable
private fun V5ActiveMediaCleanList(streams: List<MediaStreamItem>, onSelect: (String) -> Unit) {
    // No header text, no per-item Scan or Play/Pause icons
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 96.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(streams) { s ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (s.isActive) Color(0xFF00BCD4).copy(alpha = 0.22f) else Color(0xFF10252C))
                    .clickable { onSelect(s.id) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${s.appName} · ${s.trackTitle}",
                    color = Color(0xFFE0F7FA),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
