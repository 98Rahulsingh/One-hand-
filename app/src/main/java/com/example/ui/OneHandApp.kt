package com.example.ui

import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveWindow
import com.example.ui.components.EdgeTriggerHandle
import com.example.ui.screens.AddQuickAccessModal
import com.example.ui.screens.MainEdgePanel
import com.example.ui.screens.PanelSettingsWindow
import com.example.ui.screens.QuickPanelScreen
import com.example.ui.screens.QuickToolsScreen
import com.example.ui.screens.MediaControlScreen
import com.example.viewmodel.OneHandViewModel

@Composable
fun OneHandApp(
    viewModel: OneHandViewModel,
    modifier: Modifier = Modifier,
    overlayMode: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    val activeWindow by viewModel.activeWindow.collectAsState()
    val isPanelOpen by viewModel.isPanelOpen.collectAsState()
    val panelSettings by viewModel.panelSettings.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    // Proper Back Navigation
    if (!overlayMode) BackHandler {
        when {
            activeWindow != ActiveWindow.MAIN_PANEL -> {
                viewModel.navigateTo(ActiveWindow.MAIN_PANEL)
            }
            isPanelOpen -> {
                viewModel.togglePanel(false)
            }
            else -> {
                // Let system exit
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (overlayMode) Modifier else Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF041114),
                                Color(0xFF071F24),
                                Color(0xFF0A2930)
                            )
                        )
                    )
                    .statusBarsPadding()
            )
            .testTag("one_hand_app_root")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        // Simulated system home screen canvas underneath
        if (overlayMode) {
            // Tap outside the windows closes the panel and shows the app underneath again
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        viewModel.navigateTo(ActiveWindow.MAIN_PANEL)
                        viewModel.togglePanel(false)
                        onDismiss?.invoke()
                    }
            )
        } else {
            HomeScreenBackdrop(
                isPanelOpen = isPanelOpen,
                onOpenPanel = { viewModel.togglePanel(true) },
                onOpenSettings = { viewModel.navigateTo(ActiveWindow.PANEL_SETTINGS) }
            )
        }

        // Floating Edge Trigger Handle visible when enabled and panel is retracted or in background
        if (!overlayMode && panelSettings.isEdgeHandleEnabled && (!isPanelOpen || activeWindow == ActiveWindow.PANEL_SETTINGS)) {
            EdgeTriggerHandle(
                isRightEdge = panelSettings.isRightEdge,
                verticalOffsetPercent = panelSettings.verticalPosition / 10f,
                handleHeightDp = panelSettings.handleHeightDp,
                opacity = panelSettings.handleOpacity,
                onClick = {
                    viewModel.togglePanel(true)
                    viewModel.navigateTo(ActiveWindow.MAIN_PANEL)
                },
                onDragInward = {
                    viewModel.togglePanel(true)
                    viewModel.navigateTo(ActiveWindow.MAIN_PANEL)
                },
                modifier = Modifier.align(
                    if (panelSettings.isRightEdge) Alignment.CenterEnd else Alignment.CenterStart
                )
            )
        }

        // Window 1: Main One Hand Edge Side Panel (Opens by default on launch!)
        AnimatedVisibility(
            visible = (activeWindow == ActiveWindow.MAIN_PANEL && isPanelOpen),
            enter = slideInHorizontally(
                initialOffsetX = { if (panelSettings.isRightEdge) it else -it }
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { if (panelSettings.isRightEdge) it else -it }
            ) + fadeOut()
        ) {
            MainEdgePanel(viewModel = viewModel)
        }

        AnimatedVisibility(
            visible = (activeWindow == ActiveWindow.QUICK_PANEL && isPanelOpen),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(if (panelSettings.isRightEdge) Alignment.CenterEnd else Alignment.CenterStart).padding(6.dp)
        ) {
            QuickPanelScreen(viewModel = viewModel, modifier = Modifier.width(panelSettings.quickPanelWidthDp.dp).height(panelSettings.quickPanelLengthDp.dp))
        }

        AnimatedVisibility(
            visible = (activeWindow == ActiveWindow.QUICK_TOOLS && isPanelOpen),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center).padding(6.dp)
        ) {
            QuickToolsScreen(viewModel = viewModel, modifier = Modifier.width(panelSettings.quickToolWidthDp.dp).height(panelSettings.quickToolLengthDp.dp))
        }

        AnimatedVisibility(
            visible = (activeWindow == ActiveWindow.MEDIA_CONTROL && isPanelOpen),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center).padding(6.dp)
        ) {
            MediaControlScreen(viewModel = viewModel, modifier = Modifier.width(panelSettings.mediaControlWidthDp.dp).height(panelSettings.mediaControlLengthDp.dp))
        }

        // Window 2: Add Quick Access Modal (Matching Image 1)
        AnimatedVisibility(
            visible = (activeWindow == ActiveWindow.ADD_QUICK_ACCESS),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AddQuickAccessModal(viewModel = viewModel)
        }

        // Window 4: Panel Settings & Permissions (Matching Image 2)
        val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
        val settingsPanelHeightDp = (screenHeightDp * panelSettings.panelHeightPercent / 100f)
            .coerceIn(220f, screenHeightDp.toFloat())
        val settingsHorizontalOffset = (panelSettings.horizontalOffset * 3.5f).dp
        val settingsVerticalOffset = ((panelSettings.verticalPosition - 5) * 45 + panelSettings.verticalOffsetDp).dp
        val settingsCorner = (panelSettings.roundedCorners * 3.2f).dp
        val settingsShape = if (panelSettings.isRightEdge) {
            RoundedCornerShape(topStart = settingsCorner, bottomStart = settingsCorner, topEnd = 0.dp, bottomEnd = 0.dp)
        } else {
            RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = settingsCorner, bottomEnd = settingsCorner)
        }
        AnimatedVisibility(
            visible = (activeWindow == ActiveWindow.PANEL_SETTINGS),
            enter = fadeIn() + slideInHorizontally { if (panelSettings.isRightEdge) it else -it },
            exit = fadeOut() + slideOutHorizontally { if (panelSettings.isRightEdge) it else -it },
            modifier = Modifier
                .align(if (panelSettings.isRightEdge) Alignment.CenterEnd else Alignment.CenterStart)
                .offset(
                    x = if (panelSettings.isRightEdge) -settingsHorizontalOffset else settingsHorizontalOffset,
                    y = settingsVerticalOffset
                )
                .width(panelSettings.panelWidthDp.dp)
                .height(settingsPanelHeightDp.dp)
                .clip(settingsShape)
        ) {
            PanelSettingsWindow(viewModel = viewModel)
        }

        // Toast Feedback Banner Overlay
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            toastMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F3B43).copy(alpha = 0.95f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .shadow(8.dp)
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFFE0F7FA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        }
    }
}

@Composable
fun HomeScreenBackdrop(
    isPanelOpen: Boolean,
    onOpenPanel: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Clock & Date
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Text(
                text = "04:20",
                fontSize = 58.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFFE0F7FA).copy(alpha = 0.9f)
            )
            Text(
                text = "Monday, November 9",
                fontSize = 15.sp,
                color = Color(0xFF80DEEA).copy(alpha = 0.8f)
            )
        }

        // Helper instruction card when panel is retracted
        if (!isPanelOpen) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPanel() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B252B).copy(alpha = 0.85f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Swipe handle",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edge Panel Ready",
                            color = Color(0xFFE0F7FA),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap or swipe the cyan edge handle on the side, or tap here to open the Edge Panel.",
                            color = Color(0xFF80DEEA).copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Bottom dock row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DockItem(name = "Open Panel", onClick = onOpenPanel)
            DockItem(name = "Settings", onClick = onOpenSettings)
        }
    }
}

@Composable
fun DockItem(name: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E2F36).copy(alpha = 0.7f))
            .border(1.dp, Color(0xFF1E4E59), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = name,
            color = Color(0xFF80DEEA),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
