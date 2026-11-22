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
    if (!overlayMode) BackHandler(enabled = !panelSettings.oneHandModeEnabled || !panelSettings.oneHandLocked) {
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

    val oneHandScale = when (panelSettings.oneHandSize) {
        3.5f -> 0.56f
        4.5f -> 0.72f
        else -> 0.64f // 4.0" full mobile mini-display
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (panelSettings.oneHandModeEnabled)
                    Modifier.background(Color.Black)
                else Modifier
            )
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
        val oneHandInnerModifier = if (panelSettings.oneHandModeEnabled) {
            Modifier
                .align(if (panelSettings.oneHandCornerRight) Alignment.BottomEnd else Alignment.BottomStart)
                .fillMaxWidth(oneHandScale)
                .fillMaxHeight(oneHandScale)
                .border(1.dp, Color(0xFF1E4E59), RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
        } else {
            Modifier.fillMaxSize()
        }
        Box(
            modifier = oneHandInnerModifier.then(
                if (panelSettings.oneHandModeEnabled) Modifier.pointerInput(panelSettings.oneHandModeEnabled) {
                    var dragDistance = 0f
                    var startedOnLeftHalf = false
                    var actionFired = false
                    detectHorizontalDragGestures(
                        onDragStart = { start ->
                            // Use the mini-display's midpoint, not the physical screen midpoint.
                            startedOnLeftHalf = start.x < size.width / 2f
                            dragDistance = 0f
                            actionFired = false
                        },
                        onHorizontalDrag = { change, amount ->
                            dragDistance += kotlin.math.abs(amount)
                            if (!actionFired && dragDistance >= 72.dp.toPx()) {
                                actionFired = true
                                viewModel.executeGesture(
                                    if (startedOnLeftHalf) "Mini Display Swipe Left" else "Mini Display Swipe Right"
                                )
                            }
                            change.consume()
                        }
                    )
                } else Modifier
            )
        ) {
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

        // One Hand Mode lock is independent from the Edge Panel trigger. It stays
        // inside the mini-display at its bottom-left corner.
        if (!overlayMode && panelSettings.oneHandModeEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(7.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xDD102A31))
                    .border(1.dp, Color(0xFF00BCD4), CircleShape)
                    .clickable {
                        viewModel.updateSettings { cur -> cur.copy(oneHandLocked = !cur.oneHandLocked) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (panelSettings.oneHandLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (panelSettings.oneHandLocked) "Unlock One Hand Mode" else "Lock One Hand Mode",
                    tint = Color(0xFFE0F7FA),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Floating Edge Trigger Handle visible when enabled and panel is retracted or in background
        if (!overlayMode && !panelSettings.oneHandModeEnabled && panelSettings.isEdgeHandleEnabled && (!isPanelOpen || activeWindow == ActiveWindow.PANEL_SETTINGS)) {
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
        AnimatedVisibility(
            visible = (activeWindow == ActiveWindow.PANEL_SETTINGS),
            enter = fadeIn() + slideInHorizontally { -it },
            exit = fadeOut() + slideOutHorizontally { -it }
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
                            text = "Tap or swipe the cyan edge handle on the side, or tap here to open the one-hand panel.",
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
