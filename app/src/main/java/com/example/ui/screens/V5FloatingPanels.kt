package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveWindow
import com.example.model.ItemType
import com.example.viewmodel.OneHandViewModel

private val V5Bg = Color(0xFF091C20)
private val V5Accent = Color(0xFF00E5FF)
private val V5Tile = Color(0xFF142A30)

/**
 * Secondary V5 panels. Edge Panel remains rendered by MainEdgePanel; these
 * surfaces deliberately keep Media Control and Quick Tools independent.
 * The three primary surfaces share mainPanelGeometry and the same edge anchor.
 */
@Composable
fun V5FloatingPanel(viewModel: OneHandViewModel, window: ActiveWindow) {
    val settings by viewModel.panelSettings.collectAsState()
    val quick by viewModel.quickSettings.collectAsState()
    val media by viewModel.mediaState.collectAsState()
    val apps by viewModel.masterItems.collectAsState()
    var miniSize by remember { mutableFloatStateOf(4.0f) }
    var rightAnchor by remember { mutableStateOf(true) }
    var dragTotal by remember { mutableStateOf(0f) }

    val primary = window in setOf(ActiveWindow.MAIN_PANEL, ActiveWindow.QUICK_PANEL, ActiveWindow.ONE_HAND_MODE)
    val geometry = when {
        primary -> settings.mainPanelGeometry
        window == ActiveWindow.MEDIA_CONTROL -> settings.mediaWindowGeometry
        else -> settings.quickAccessGeometry
    }
    val title = when (window) {
        ActiveWindow.QUICK_PANEL -> "QUICK PANEL"
        ActiveWindow.ONE_HAND_MODE -> "ONE HAND MODE"
        ActiveWindow.MEDIA_CONTROL -> "MEDIA CONTROL"
        ActiveWindow.QUICK_TOOLS -> "QUICK TOOLS"
        else -> "EDGE PANEL"
    }
    val edgeAlignment = if (settings.isRightEdge) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        Modifier.fillMaxSize().background(if (window == ActiveWindow.ONE_HAND_MODE) Color.Black else Color.Transparent)
    ) {
        Card(
            modifier = Modifier
                .align(if (window == ActiveWindow.ONE_HAND_MODE) if (rightAnchor) Alignment.BottomEnd else Alignment.BottomStart else edgeAlignment)
                .padding(8.dp)
                .width(if (window == ActiveWindow.ONE_HAND_MODE) (miniSize * 65).dp else geometry.widthDp.dp.coerceIn(230.dp, 390.dp))
                .height(if (window == ActiveWindow.ONE_HAND_MODE) (miniSize * 125).dp else geometry.heightDp.dp.coerceIn(300.dp, 760.dp))
                .offset(x = if (window == ActiveWindow.ONE_HAND_MODE) 0.dp else geometry.xOffset.dp, y = if (window == ActiveWindow.ONE_HAND_MODE) 0.dp else geometry.yOffset.dp)
                .pointerInput(window) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount -> dragTotal += amount },
                        onDragEnd = {
                            if (dragTotal > 55f) {
                                viewModel.navigateTo(if (window == ActiveWindow.QUICK_PANEL) ActiveWindow.ONE_HAND_MODE else ActiveWindow.MAIN_PANEL)
                            } else if (dragTotal < -55f) {
                                viewModel.navigateTo(if (window == ActiveWindow.MAIN_PANEL) ActiveWindow.QUICK_PANEL else ActiveWindow.MAIN_PANEL)
                            }
                            dragTotal = 0f
                        }
                    )
                },
            shape = RoundedCornerShape((settings.roundedCorners * 3 + 8).dp),
            colors = CardDefaults.cardColors(containerColor = V5Bg),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, color = V5Accent, fontSize = 11.sp, letterSpacing = 1.2.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (window == ActiveWindow.MAIN_PANEL) {
                            IconButton(onClick = { viewModel.togglePlayPause() }, modifier = Modifier.size(28.dp)) {
                                Icon(if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = V5Accent)
                            }
                        }
                        IconButton(onClick = { viewModel.navigateTo(ActiveWindow.PANEL_SETTINGS) }, modifier = Modifier.size(28.dp)) {
                            Text("⚙", color = Color.White, fontSize = 14.sp)
                        }
                        IconButton(onClick = { viewModel.dismissFloatingWindow() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White)
                        }
                    }
                }
                when (window) {
                    ActiveWindow.QUICK_PANEL -> {
                        QuickTilesGrid(quick.filter { it.isVisible }, viewModel, Modifier.weight(1f))
                        Text("BRIGHTNESS", color = Color.LightGray, fontSize = 9.sp)
                        Slider(value = media.brightnessPercent.coerceIn(0f, 100f), onValueChange = viewModel::setBrightness, valueRange = 0f..100f)
                        Text("VOLUME", color = Color.LightGray, fontSize = 9.sp)
                        Slider(value = media.volumePercent.coerceIn(0f, 100f), onValueChange = viewModel::setVolume, valueRange = 0f..100f)
                        MediaSeek(media.totalSeconds, media.currentSeconds, viewModel)
                        MediaButtons(viewModel, media.isPlaying)
                        ActiveSessions(viewModel, media.activeStreams, Modifier.weight(0.8f))
                    }
                    ActiveWindow.MEDIA_CONTROL -> {
                        MediaSeek(media.totalSeconds, media.currentSeconds, viewModel)
                        MediaButtons(viewModel, media.isPlaying)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(1f, 1.5f, 2f, 2.5f, 3f).forEach { speed ->
                                TextButton(onClick = { viewModel.setPlaybackSpeed(speed) }) {
                                    Text("${speed}x", color = if (media.playbackSpeed == speed) V5Accent else Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                        ActiveSessions(viewModel, media.activeStreams, Modifier.weight(1f))
                    }
                    ActiveWindow.QUICK_TOOLS -> {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            items(quick.filter { it.isVisible }, key = { it.id }) { item ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).background(if (item.isEnabled) Color(0xFF16454D) else V5Tile, RoundedCornerShape(12.dp)).clickable { viewModel.toggleQuickSetting(item.id) }.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.iconName.take(2).uppercase(), color = V5Accent, fontSize = 12.sp, modifier = Modifier.width(34.dp))
                                    Text(item.name, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    Text(if (item.isEnabled) "ON" else "OFF", color = if (item.isEnabled) V5Accent else Color.LightGray, fontSize = 9.sp)
                                }
                            }
                            item {
                                AddRow { viewModel.openAddWindow(forTiles = true) }
                            }
                        }
                    }
                    ActiveWindow.ONE_HAND_MODE -> {
                        Text("Mini screen size", color = Color.White, fontSize = 11.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(3.5f, 4f, 4.5f).forEach { size ->
                                TextButton(onClick = { miniSize = size }) { Text("${size}\"", color = if (miniSize == size) V5Accent else Color.White, fontSize = 10.sp) }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(onClick = { rightAnchor = false }) { Text("LEFT", color = if (!rightAnchor) V5Accent else Color.White) }
                            TextButton(onClick = { rightAnchor = true }) { Text("RIGHT", color = if (rightAnchor) V5Accent else Color.White) }
                        }
                        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp)) {
                            items(apps.filter { it.type == ItemType.APP && it.isAdded }.take(18), key = { it.id }) { app ->
                                Card(Modifier.padding(3.dp).clickable { viewModel.launchApp(app) }, colors = CardDefaults.cardColors(containerColor = V5Tile)) {
                                    Box(Modifier.fillMaxWidth().height(54.dp).padding(4.dp), contentAlignment = Alignment.Center) { Text(app.name, color = Color.White, fontSize = 9.sp, maxLines = 2) }
                                }
                            }
                        }
                    }
                    else -> {
                        Text("Swipe left or right to switch panels.", color = Color.White, fontSize = 11.sp)
                        TextButton(onClick = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) }) { Text("Open Edge Panel", color = V5Accent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTilesGrid(items: List<com.example.model.QuickSettingItem>, viewModel: OneHandViewModel, modifier: Modifier = Modifier) {
    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(2.dp)) {
        items(items, key = { it.id }) { item ->
            Card(Modifier.padding(3.dp).clickable { viewModel.toggleQuickSetting(item.id) }, colors = CardDefaults.cardColors(containerColor = if (item.isEnabled) Color(0xFF16454D) else V5Tile)) {
                Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(item.iconName.take(2).uppercase(), color = V5Accent, fontSize = 12.sp)
                    Text(item.name, color = Color.White, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
        item { AddRow { viewModel.openAddWindow(forTiles = true) } }
    }
}

@Composable
private fun AddRow(onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(3.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color(0xFF16454D))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, "Add item", tint = V5Accent)
            Spacer(Modifier.width(8.dp))
            Text("Add Item", color = V5Accent, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MediaSeek(total: Int, current: Int, viewModel: OneHandViewModel) {
    Slider(value = if (total > 0) current.toFloat() / total else 0f, onValueChange = { viewModel.seekMedia((it * total).toInt()) }, valueRange = 0f..1f)
}

@Composable
private fun MediaButtons(viewModel: OneHandViewModel, isPlaying: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = viewModel::previousVideo) { Icon(Icons.Default.SkipPrevious, "Previous chapter", tint = Color.White) }
        TextButton(onClick = viewModel::skipBackward15) { Text("-15s", color = Color.White) }
        IconButton(onClick = viewModel::togglePlayPause) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = V5Accent) }
        TextButton(onClick = viewModel::skipForward15) { Text("+15s", color = Color.White) }
        IconButton(onClick = viewModel::nextVideoOrChapter) { Icon(Icons.Default.SkipNext, "Next chapter", tint = Color.White) }
    }
}

@Composable
private fun ActiveSessions(viewModel: OneHandViewModel, streams: List<com.example.model.MediaStreamItem>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth()) {
        items(streams, key = { it.id }) { stream ->
            Column(Modifier.fillMaxWidth().clickable { viewModel.switchActiveMedia(stream.id) }.padding(vertical = 7.dp)) {
                Text(stream.trackTitle.ifBlank { stream.appName }, color = if (stream.isActive) V5Accent else Color.White, fontSize = 11.sp, maxLines = 1)
                Text("${stream.appName} · ${stream.artist}", color = Color.LightGray, fontSize = 9.sp, maxLines = 1)
            }
            Divider(color = Color(0xFF24434A))
        }
    }
}
