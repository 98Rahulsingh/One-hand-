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
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.viewmodel.OneHandViewModel

private val V5Bg = Color(0xFF091C20)
private val V5Accent = Color(0xFF00E5FF)

@Composable
fun V5FloatingPanel(viewModel: OneHandViewModel, window: ActiveWindow) {
    val settings by viewModel.panelSettings.collectAsState()
    val quick by viewModel.quickSettings.collectAsState()
    val media by viewModel.mediaState.collectAsState()
    var miniSize by remember { mutableStateOf(4.0f) }
    var rightAnchor by remember { mutableStateOf(true) }
    val unified = window in setOf(ActiveWindow.QUICK_PANEL, ActiveWindow.ONE_HAND_MODE)
    val geometry = when (window) {
        ActiveWindow.MEDIA_CONTROL -> settings.mediaWindowGeometry
        ActiveWindow.QUICK_TOOLS -> settings.quickAccessGeometry
        else -> settings.mainPanelGeometry
    }
    val title = when (window) {
        ActiveWindow.QUICK_PANEL -> "QUICK PANEL"
        ActiveWindow.ONE_HAND_MODE -> "ONE HAND MODE"
        ActiveWindow.MEDIA_CONTROL -> "MEDIA CONTROL"
        ActiveWindow.QUICK_TOOLS -> "QUICK TOOLS"
        else -> "EDGE PANEL"
    }
    Box(Modifier.fillMaxSize().background(if (window == ActiveWindow.ONE_HAND_MODE) Color.Black else Color.Transparent)) {
        Card(
            modifier = Modifier
                .align(if (window == ActiveWindow.ONE_HAND_MODE) if (rightAnchor) Alignment.BottomEnd else Alignment.BottomStart else if (settings.isRightEdge) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(8.dp)
                .width(geometry.widthDp.dp.coerceIn(230.dp, 390.dp))
                .height(geometry.heightDp.dp.coerceIn(300.dp, 760.dp)),
            shape = RoundedCornerShape((settings.roundedCorners * 3 + 8).dp),
            colors = CardDefaults.cardColors(containerColor = V5Bg),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, color = V5Accent, fontSize = 12.sp, letterSpacing = 1.3.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (unified) {
                            TextButton(onClick = { viewModel.navigateTo(if (window == ActiveWindow.QUICK_PANEL) ActiveWindow.ONE_HAND_MODE else ActiveWindow.QUICK_PANEL) }) {
                                Text(if (window == ActiveWindow.QUICK_PANEL) "ONE HAND" else "QUICK", color = V5Accent, fontSize = 10.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.navigateTo(ActiveWindow.PANEL_SETTINGS) }) { Text("⚙", color = Color.White) }
                        IconButton(onClick = { viewModel.togglePanel(false) }) { Icon(Icons.Default.Close, null, tint = Color.White) }
                    }
                }
                when (window) {
                    ActiveWindow.QUICK_PANEL -> {
                        QuickTilesGrid(quick.filter { it.isVisible && it.id !in setOf("torch", "rotate", "mute") }, viewModel, Modifier.weight(1f))
                        Text("BRIGHTNESS", color = Color.LightGray, fontSize = 9.sp)
                        Slider(value = media.brightnessPercent.coerceIn(0f, 100f), onValueChange = viewModel::setBrightness, valueRange = 0f..100f)
                        Text("VOLUME", color = Color.LightGray, fontSize = 9.sp)
                        Slider(value = media.volumePercent.coerceIn(0f, 100f), onValueChange = viewModel::setVolume, valueRange = 0f..100f)
                        Text("${media.trackTitle} · ${media.artist}", color = Color.White, fontSize = 10.sp, maxLines = 1)
                        Slider(value = if (media.totalSeconds > 0) media.currentSeconds.toFloat() / media.totalSeconds else 0f, onValueChange = { viewModel.seekMedia((it * media.totalSeconds).toInt()) })
                        MediaButtons(viewModel)
                        ActiveSessions(viewModel, media.activeStreams, Modifier.weight(1f))
                    }
                    ActiveWindow.MEDIA_CONTROL -> {
                        Text(media.trackTitle, color = Color.White, fontSize = 12.sp, maxLines = 1)
                        Slider(value = if (media.totalSeconds > 0) media.currentSeconds.toFloat() / media.totalSeconds else 0f, onValueChange = { viewModel.seekMedia((it * media.totalSeconds).toInt()) })
                        MediaButtons(viewModel)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(1f,1.5f,2f,2.5f,3f).forEach { speed ->
                                TextButton(onClick = { viewModel.setPlaybackSpeed(speed) }) { Text("${speed}x", color = if (media.playbackSpeed == speed) V5Accent else Color.White, fontSize = 10.sp) }
                            }
                        }
                        ActiveSessions(viewModel, media.activeStreams, Modifier.weight(1f))
                    }
                    ActiveWindow.QUICK_TOOLS -> QuickTilesGrid(quick.filter { it.isVisible && it.id !in setOf("torch", "rotate", "mute") }, viewModel, Modifier.weight(1f))
                    ActiveWindow.ONE_HAND_MODE -> {
                        Text("Mini screen size", color = Color.White, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(3.5f,4f,4.5f).forEach { size -> TextButton(onClick = { miniSize = size }) { Text("${size}\"", color = if (miniSize == size) V5Accent else Color.White) } }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(onClick = { rightAnchor = false }) { Text("LEFT", color = if (!rightAnchor) V5Accent else Color.White) }
                            TextButton(onClick = { rightAnchor = true }) { Text("RIGHT", color = if (rightAnchor) V5Accent else Color.White) }
                        }
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = if (rightAnchor) Alignment.BottomEnd else Alignment.BottomStart) {
                            Box(Modifier.width((miniSize * 65).dp).height((miniSize * 125).dp).background(Color(0xFF102D33), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Text("Mini screen", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                    else -> {
                        Text("Use the Edge Panel for apps and groups.", color = Color.White, fontSize = 12.sp)
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
            Card(Modifier.padding(3.dp).clickable { viewModel.toggleQuickSetting(item.id) }, colors = CardDefaults.cardColors(containerColor = if (item.isEnabled) Color(0xFF16454D) else Color(0xFF142A30))) {
                Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(item.iconName.take(2).uppercase(), color = V5Accent, fontSize = 12.sp)
                    Text(item.name, color = Color.White, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
        item { Card(Modifier.padding(3.dp).clickable { viewModel.openAddWindow(forTiles = true) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF16454D))) { Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Add item", tint = V5Accent) } } }
    }
}

@Composable
private fun MediaButtons(viewModel: OneHandViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = viewModel::previousVideo) { Icon(Icons.Default.SkipPrevious, "Previous chapter", tint = Color.White) }
        TextButton(onClick = viewModel::skipBackward15) { Text("-15s", color = Color.White) }
        IconButton(onClick = viewModel::togglePlayPause) { Icon(Icons.Default.PlayArrow, "Play/Pause", tint = V5Accent) }
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
