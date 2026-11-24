package com.example.ui.screens

import com.example.model.MediaStreamItem
import com.example.model.SPECIAL_GROUPS
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.model.ItemType
import com.example.system.AppIcons
import androidx.compose.foundation.Image
import com.example.model.isInGroup
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import com.example.ui.components.FastAlphabetScroller
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveWindow
import com.example.model.AppItem
import com.example.model.QuickSettingItem
import com.example.viewmodel.OneHandViewModel
import kotlin.math.roundToInt

@Composable
fun MainEdgePanel(
    viewModel: OneHandViewModel,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val panelSettings by viewModel.panelSettings.collectAsState()
    val mediaState by viewModel.mediaState.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val masterItems by viewModel.masterItems.collectAsState()
    val quickSettings by viewModel.quickSettings.collectAsState()
    val selectedLetter by viewModel.selectedAlphabetLetter.collectAsState()
    val groupAdded by viewModel.groupAdded.collectAsState()
    val groupRemoved by viewModel.groupRemoved.collectAsState()
    val manualGroups by viewModel.manualGroups.collectAsState()
    val reorder by viewModel.reorder.collectAsState()
    val showAddGroupDialog by viewModel.showAddGroupDialog.collectAsState()

    var newGroupNameInput by remember { mutableStateOf("") }
    var groupMenuTarget by remember { mutableStateOf<String?>(null) }
    var renameGroupTarget by remember { mutableStateOf<String?>(null) }
    var renameGroupName by remember { mutableStateOf("") }

    // Persistent Geometry state (user can resize directly in preview!)
    val screenH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    var currentWidthDp by remember(panelSettings.panelWidthDp) {
        mutableFloatStateOf(panelSettings.panelWidthDp)
    }
    var currentHeightDp by remember(panelSettings.panelHeightPercent, screenH) {
        mutableFloatStateOf((screenH * panelSettings.panelHeightPercent / 100f).coerceIn(220f, screenH.toFloat()))
    }
    var currentYOffset by remember(panelSettings.verticalPosition, panelSettings.verticalOffsetDp) {
        mutableFloatStateOf((panelSettings.verticalPosition - 5) * 45f + panelSettings.verticalOffsetDp)
    }

    // Apps of the selected group (before the A-Z letter filter)
    val groupItems = remember(masterItems, selectedCategory, groupAdded, groupRemoved, manualGroups) {
        val apps = masterItems.filter { it.type == ItemType.APP }
        when (selectedCategory) {
            "Recent" -> apps.filter { it.lastUsed > 0L }.sortedByDescending { it.lastUsed }.take(12)
                .ifEmpty { masterItems.filter { it.isAdded }.take(12) }
            "Most Used" -> apps.filter { it.usageCount > 0 }.sortedByDescending { it.usageCount }.take(30)
                .ifEmpty { masterItems.filter { it.isAdded }.take(12) }
            "All" -> apps
            "Unused" -> apps.filter { it.lastUsed == 0L }
            else -> masterItems.filter { isInGroup(it, selectedCategory, groupAdded, groupRemoved, manualGroups) }
        }
    }
    // Letters that really exist in this group (others are hidden on the A-Z slider)
    val availableLetters = remember(groupItems) {
        groupItems.mapNotNull { it.name.firstOrNull()?.uppercaseChar() }.toSet()
    }
    val categoryFilteredItems = remember(groupItems, selectedLetter) {
        if (selectedLetter != null) {
            groupItems.filter { it.name.startsWith(selectedLetter!!, ignoreCase = true) }
        } else {
            groupItems
        }
    }

    val cornerRadius = (panelSettings.roundedCorners * 3.2f).dp
    val shape = if (panelSettings.isRightEdge) {
        RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius, topEnd = 0.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = cornerRadius, bottomEnd = cornerRadius)
    }

    val horizontalOffsetDp = (panelSettings.horizontalOffset * 3.5f).dp

    // Interactive Dialog to Add a New Custom Group
    if (showAddGroupDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAddGroupDialog() },
            title = {
                Text(
                    text = "Create New Group",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a name for your custom app group:",
                        color = Color(0xFFE0F7FA),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newGroupNameInput,
                        onValueChange = { newGroupNameInput = it },
                        placeholder = { Text("e.g. Work, Gaming, Finance", color = Color(0xFF688C95)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF285660),
                            focusedContainerColor = Color(0xFF0E2A30),
                            unfocusedContainerColor = Color(0xFF0E2A30),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_new_group_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupNameInput.isNotBlank()) {
                            viewModel.createAndSelectGroup(newGroupNameInput)
                            newGroupNameInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier.testTag("btn_confirm_add_group")
                ) {
                    Text("Add Group", color = Color(0xFF00363A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAddGroupDialog() }) {
                    Text("Cancel", color = Color(0xFF80DEEA))
                }
            },
            containerColor = Color(0xFF0B252B),
            shape = RoundedCornerShape(18.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_edge_panel_container")
    ) {
        // Outside Touch Backdrop: Tap outside immediately closes the panel!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (panelSettings.backdropOpacityPercent / 100f).coerceIn(0f, 1f)))
                .clickable { viewModel.togglePanel(false) }
                .testTag("backdrop_touch_dismiss")
        )

        // The Compact One-Hand Side Panel Card
        Card(
            modifier = Modifier
                .align(if (panelSettings.isRightEdge) Alignment.CenterEnd else Alignment.CenterStart)
                .offset {
                    val x = if (panelSettings.isRightEdge) -horizontalOffsetDp.toPx() else horizontalOffsetDp.toPx()
                    IntOffset(x.roundToInt(), currentYOffset.dp.toPx().roundToInt())
                }
                .width(currentWidthDp.dp)
                .height(currentHeightDp.dp)
                .clickable(enabled = false) {} // Prevent click-through
                .shadow(24.dp, shape)
                .clip(shape)
                .border(1.dp, Color(0xFF1E464F).copy(alpha = 0.85f), shape)
                .testTag("main_edge_panel_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF091C20)),
            shape = shape
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // Top header: Quick Panel title + Settings & Close
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "EDGE PANEL",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Mini Media Play/Pause button in header
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.size(26.dp).testTag("header_mini_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(15.dp)
                                )
                            }


                            // Panel Settings
                            IconButton(
                                onClick = { viewModel.navigateTo(ActiveWindow.PANEL_SETTINGS) },
                                modifier = Modifier.size(26.dp).testTag("panel_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Panel Settings",
                                    tint = Color(0xFF80DEEA),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Close Panel
                            IconButton(
                                onClick = { viewModel.togglePanel(false) },
                                modifier = Modifier.size(26.dp).testTag("close_panel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Panel",
                                    tint = Color(0xFF80DEEA),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // ==========================================
                    // MIDDLE SECTION:
                    // ABCD Vertical Scroller on closest edge side
                    // Apps Grid with '+' tile at the end
                    // Groups Selector Row with '+' custom group button
                    // ==========================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // If LEFT edge: Vertical ABCD slider is closest to left thumb
                        if (!panelSettings.isRightEdge) {
                            FastAlphabetScroller(
                                availableLetters = availableLetters,
                                selectedLetter = selectedLetter,
                                onLetterSelected = { viewModel.selectAlphabetLetter(it) },
                                modifier = Modifier.padding(end = 4.dp),
                                isRightAligned = false
                            )
                        }

                        // Center content column: Apps + Groups
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            // LINE 1: APPS Grid with '+' button at the end
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                if (categoryFilteredItems.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = if (selectedLetter != null) "No apps starting with '$selectedLetter'" else "No apps in '$selectedCategory'",
                                                color = Color(0xFF80DEEA).copy(alpha = 0.6f),
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AddAppTile(onClick = { viewModel.openAddWindow(forTiles = false) })
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(panelSettings.columns.coerceIn(2, 4)),
                                        contentPadding = PaddingValues(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(categoryFilteredItems, key = { it.id }) { item ->
                                            CompactAppTile(
                                                item = item,
                                                onClick = { viewModel.launchApp(item) }
                                            )
                                        }

                                        // Last '+' Tile right after apps: to add/remove apps from this group!
                                        item(key = "add_app_tile") {
                                            AddAppTile(onClick = { viewModel.openAddWindow(forTiles = false) })
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // LINE 2: GROUPS Selector Row (with functioning + button for new custom group!)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                categories.forEach { category ->
                                    val isSelected = selectedCategory == category
                                    val isTarget = reorder?.isGroup == true && reorder?.id == category
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isTarget) Color(0xFFFFD54F) else if (isSelected) Color(0xFF00E5FF) else Color(0xFF102E34))
                                            .pointerInput(category) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        if (category !in SPECIAL_GROUPS) {
                                                            groupMenuTarget = category
                                                        }
                                                    },
                                                    onTap = { viewModel.selectCategory(category) }
                                                )
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("group_chip_$category")
                                    ) {
                                        Text(
                                            text = category,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected || isTarget) Color(0xFF061A1D) else Color(0xFFB2EBF2)
                                        )
                                        DropdownMenu(
                                            expanded = groupMenuTarget == category,
                                            onDismissRequest = { groupMenuTarget = null }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Delete group") },
                                                onClick = { groupMenuTarget = null; viewModel.deleteGroup(category) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Rename group") },
                                                onClick = { groupMenuTarget = null; renameGroupTarget = category; renameGroupName = category }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(if (category in manualGroups) "Switch to Auto apps" else "Switch to Manual apps") },
                                                onClick = { groupMenuTarget = null; viewModel.setGroupManual(category, category !in manualGroups) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Reorder") },
                                                onClick = { groupMenuTarget = null; viewModel.startReorder(true, category) }
                                            )
                                        }
                                        if (renameGroupTarget == category) {
                                            AlertDialog(
                                                onDismissRequest = { renameGroupTarget = null },
                                                title = { Text("Rename group") },
                                                text = {
                                                    OutlinedTextField(
                                                        value = renameGroupName,
                                                        onValueChange = { renameGroupName = it },
                                                        singleLine = true,
                                                        label = { Text("Group name") }
                                                    )
                                                },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        viewModel.renameGroup(category, renameGroupName)
                                                        renameGroupTarget = null
                                                    }) { Text("Save") }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { renameGroupTarget = null }) { Text("Cancel") }
                                                }
                                            )
                                        }
                                    }
                                }

                                // '+' Button for adding new manual group!
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF173D45))
                                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), CircleShape)
                                        .clickable { viewModel.openAddWindow(forTiles = false, newGroup = true) }
                                        .testTag("add_custom_group_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Custom Group",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        // If RIGHT edge: Vertical ABCD slider is closest to right thumb!
                        if (panelSettings.isRightEdge) {
                            FastAlphabetScroller(
                                availableLetters = availableLetters,
                                selectedLetter = selectedLetter,
                                onLetterSelected = { viewModel.selectAlphabetLetter(it) },
                                modifier = Modifier.padding(start = 4.dp),
                                isRightAligned = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ==========================================
                    // BOTTOM: Quick Settings Direct Hardware Toggles (with Hide/Unhide toggle)
                    // ==========================================
                    reorder?.let { r ->
                        val moveName = if (r.isGroup) r.id else (quickSettings.find { it.id == r.id }?.name ?: r.id)
                        ReorderBar(
                            title = moveName,
                            onEarlier = { viewModel.moveReorder(-1) },
                            onLater = { viewModel.moveReorder(1) },
                            onDone = { viewModel.stopReorder() }
                        )
                    }
                    

                }

                // Interactive Corner Resize Handle for Main Panel (Adjust size with finger/mouse!)
                Box(
                    modifier = Modifier
                        .align(if (panelSettings.isRightEdge) Alignment.BottomStart else Alignment.BottomEnd)
                        .size(24.dp)
                        .padding(2.dp)
                        .pointerInput(panelSettings.isRightEdge) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                with(density) {
                                    val deltaW = if (panelSettings.isRightEdge) -dragAmount.x.toDp().value else dragAmount.x.toDp().value
                                    val deltaH = dragAmount.y.toDp().value
                                    currentWidthDp = (currentWidthDp + deltaW).coerceIn(220f, 380f)
                                    currentHeightDp = (currentHeightDp + deltaH).coerceIn(400f, 850f)
                                    viewModel.updateMainPanelGeometry(
                                        panelSettings.mainPanelGeometry.xOffset,
                                        currentYOffset,
                                        currentWidthDp,
                                        currentHeightDp
                                    )
                                }
                            }
                        }
                        .testTag("main_panel_resize_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Resize panel",
                        tint = Color(0xFF00E5FF).copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// SUB-COMPONENTS
// =========================================================================

@Composable
fun HorizontalAlphabetBar(
    selectedLetter: Char?,
    onLetterSelected: (Char?) -> Unit,
    modifier: Modifier = Modifier
) {
    val alphabet = remember { ('A'..'Z').toList() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B2126))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "ALL" chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (selectedLetter == null) Color(0xFF00E5FF) else Color.Transparent)
                .clickable { onLetterSelected(null) }
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = "ALL",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedLetter == null) Color(0xFF00363A) else Color(0xFF80DEEA)
            )
        }

        alphabet.forEach { letter ->
            val isSelected = selectedLetter == letter
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF00E5FF) else Color.Transparent)
                    .clickable {
                        if (isSelected) onLetterSelected(null) else onLetterSelected(letter)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF00363A) else Color(0xFF80DEEA)
                )
            }
        }
    }
}

@Composable
fun CompactAppTile(
    item: AppItem,
    showLabel: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(2.dp)
            .testTag("compact_app_${item.id}")
    ) {
        val realIcon = AppIcons.get(item.packageName)
        if (item.type == ItemType.APP && realIcon != null) {
            Image(
                bitmap = realIcon,
                contentDescription = item.name,
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(item.iconColor).copy(alpha = 0.2f))
                    .border(1.dp, Color(item.iconColor).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconVectorForName(item.iconName),
                    contentDescription = item.name,
                    tint = Color(item.iconColor),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        if (showLabel) if (showLabel) Text(
            text = item.name,
            color = Color(0xFFE0F7FA),
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddAppTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(2.dp)
            .testTag("tile_add_app_to_group")
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF10343D))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new app",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "+ Add",
            color = Color(0xFF00E5FF),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CompactMediaAndSliders(
    mediaState: com.example.model.MediaState,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Int) -> Unit,
    onPrev: () -> Unit,
    onRewind15: () -> Unit,
    onPlayPause: () -> Unit,
    onForward15: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D252B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B4953))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            // Brightness Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessMedium,
                    contentDescription = "Brightness",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Slider(
                    value = mediaState.brightnessPercent,
                    onValueChange = onBrightnessChange,
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFD54F),
                        activeTrackColor = Color(0xFFFFD54F),
                        inactiveTrackColor = Color(0xFF19373D)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${mediaState.brightnessPercent.toInt()}%",
                    color = Color(0xFFFFE082),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(26.dp)
                )
            }

            // Volume Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Volume",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Slider(
                    value = mediaState.volumePercent,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0xFF19373D)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${mediaState.volumePercent.toInt()}%",
                    color = Color(0xFF80DEEA),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(26.dp)
                )
            }

            // Track title & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mediaState.trackTitle,
                    color = Color(0xFFE0F7FA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val curMin = mediaState.currentSeconds / 60
                val curSec = mediaState.currentSeconds % 60
                val totMin = mediaState.totalSeconds / 60
                val totSec = mediaState.totalSeconds % 60
                Text(
                    text = String.format("%02d:%02d/%02d:%02d", curMin, curSec, totMin, totSec),
                    color = Color(0xFF00E5FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Seek bar
            Slider(
                value = mediaState.currentSeconds.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..mediaState.totalSeconds.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF),
                    inactiveTrackColor = Color(0xFF17383F)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )

            // 6 Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1: Prev video
                IconButton(onClick = onPrev, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color(0xFF80DEEA), modifier = Modifier.size(18.dp))
                }
                // 2: -15s
                IconButton(onClick = onRewind15, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Replay10, contentDescription = "-15s", tint = Color(0xFF80DEEA), modifier = Modifier.size(18.dp))
                }
                // 3: Play/Pause
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF00363A),
                        modifier = Modifier.size(18.dp)
                    )
                }
                // 4: +15s
                IconButton(onClick = onForward15, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Forward10, contentDescription = "+15s", tint = Color(0xFF80DEEA), modifier = Modifier.size(18.dp))
                }
                // 5: Next chapter/video
                IconButton(onClick = onNext, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next", tint = Color(0xFF80DEEA), modifier = Modifier.size(18.dp))
                }
                // 6: Expand sign
                IconButton(onClick = onExpand, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.OpenInFull, contentDescription = "Expand", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun QuickSettingsRow(
    quickSettings: List<QuickSettingItem>,
    showActionNames: Boolean = true,
    onToggleSetting: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenActionsAdd: () -> Unit,
    reorderId: String? = null,
    onLongPressSetting: (String) -> Unit = {},
    twoColumns: Boolean = false,
    fiveColumns: Boolean = false
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C242A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A464F))) {
        if (fiveColumns) {
            Column(Modifier.fillMaxWidth().padding(5.dp)) {
                LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    items(quickSettings.filter { it.isVisible }) { item ->
                        QuickSettingChip(item, onToggle = { onToggleSetting(item.id) },
                            onLongPress = { onLongPressSetting(item.id) }, isReorderTarget = reorderId == item.id)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("＋", color = Color(0xFF00E5FF), modifier = Modifier.clickable { onOpenActionsAdd() }.padding(horizontal = 10.dp))
                    Text("⚙", color = Color(0xFF00E5FF), modifier = Modifier.clickable { onOpenSettings() }.padding(horizontal = 10.dp))
                }
            }
        } else if (twoColumns) {
            Column(Modifier.fillMaxWidth().padding(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                quickSettings.filter { it.isVisible }.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        pair.forEach { item -> QuickSettingChip(item, onToggle = { onToggleSetting(item.id) },
                            onLongPress = { onLongPressSetting(item.id) }, isReorderTarget = reorderId == item.id) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("＋", color = Color(0xFF00E5FF), modifier = Modifier.clickable { onOpenActionsAdd() }.padding(horizontal = 10.dp))
                    Text("⚙", color = Color(0xFF00E5FF), modifier = Modifier.clickable { onOpenSettings() }.padding(horizontal = 10.dp))
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                quickSettings.filter { it.isVisible }.forEach { item ->
                    QuickSettingChip(item, onToggle = { onToggleSetting(item.id) },
                        onLongPress = { onLongPressSetting(item.id) }, isReorderTarget = reorderId == item.id)
                }
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF133B44))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape)
                    .clickable { onOpenActionsAdd() }.testTag("add_quick_setting_action_button"),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Customize quick settings", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                }
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF133B44))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), CircleShape)
                    .clickable { onOpenSettings() }.testTag("open_panel_settings_gear"),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tune, contentDescription = "Panel settings", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun QuickSettingChip(
    item: QuickSettingItem,
    showLabel: Boolean = true,
    onToggle: () -> Unit,
    onLongPress: () -> Unit = {},
    isReorderTarget: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(item.id) {
                detectTapGestures(onLongPress = { onLongPress() }, onTap = { onToggle() })
            }
            .padding(2.dp)
            .testTag("quick_toggle_${item.id}")
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (item.isEnabled) Color(0xFF00E5FF) else Color(0xFF15333A)
                )
                .border(
                    1.dp,
                    if (isReorderTarget) Color(0xFFFFD54F) else if (item.isEnabled) Color(0xFF00E5FF) else Color(0xFF28555E),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getQuickSettingIcon(item.iconName),
                contentDescription = item.name,
                tint = if (item.isEnabled) Color(0xFF00363A) else Color(0xFF80DEEA),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        if (showLabel) if (showLabel) Text(
            text = item.name,
            color = if (item.isEnabled) Color(0xFF00E5FF) else Color(0xFF80DEEA).copy(alpha = 0.8f),
            fontSize = 8.sp,
            fontWeight = if (item.isEnabled) FontWeight.Bold else FontWeight.Normal
        )
    }
}

fun getIconVectorForName(name: String): ImageVector {
    return when (name) {
        "block" -> Icons.Default.Security
        "security" -> Icons.Default.Security
        "group" -> Icons.Default.SmartToy
        "assistant" -> Icons.Default.SmartToy
        "smart_toy" -> Icons.Default.SmartToy
        "code" -> Icons.Default.Code
        "web" -> Icons.Default.Airplay
        "play_arrow" -> Icons.Default.PlayArrow
        "chat" -> Icons.AutoMirrored.Default.Chat
        "music_note" -> Icons.Default.MusicNote
        "camera_alt" -> Icons.Default.CameraAlt
        "photo_library" -> Icons.Default.PhotoLibrary
        "folder" -> Icons.Default.Folder
        else -> Icons.Default.GridView
    }
}

fun getQuickSettingIcon(name: String): ImageVector {
    return when (name) {
        "history" -> Icons.Default.History
        "cell_tower" -> Icons.Default.CellTower
        "wifi" -> Icons.Default.Wifi
        "bluetooth" -> Icons.Default.Bluetooth
        "airplanemode_active" -> Icons.Default.AirplanemodeActive
        "wifi_tethering" -> Icons.Default.WifiTethering
        "vertical_split" -> Icons.Default.VerticalSplit
        "picture_in_picture" -> Icons.Default.PictureInPicture
        "flashlight_on" -> Icons.Default.FlashlightOn
        "volume_up" -> Icons.Default.VolumeUp
        "screen_rotation" -> Icons.Default.ScreenRotation
        "dark_mode" -> Icons.Default.DarkMode
        "battery_saver" -> Icons.Default.BatteryChargingFull
        "lock" -> Icons.Default.Lock
        "notifications" -> Icons.Default.Notifications
        "home" -> Icons.Default.Home
        "back" -> Icons.AutoMirrored.Filled.ArrowBack
        "power_settings" -> Icons.Default.PowerSettingsNew
        "photo_camera" -> Icons.Default.PhotoCamera
        else -> Icons.Default.Settings
    }
}



@Composable
fun ReorderBar(title: String, onEarlier: () -> Unit, onLater: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF173D45))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Move: $title",
            color = Color(0xFFFFD54F),
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onEarlier) { Text("< Earlier", fontSize = 10.sp, color = Color(0xFF00E5FF)) }
        TextButton(onClick = onLater) { Text("Later >", fontSize = 10.sp, color = Color(0xFF00E5FF)) }
        TextButton(onClick = onDone) { Text("Done", fontSize = 10.sp, color = Color(0xFF80DEEA)) }
    }
}

@Composable
fun ActiveMediaStreams(
    streams: List<MediaStreamItem>,
    mediaAccessOn: Boolean,
    onSelect: (String) -> Unit,
    onEnableAccess: () -> Unit = {},
    onScan: () -> Unit = {}
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp, max = 112.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (streams.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0A2429))
                        .clickable { if (!mediaAccessOn) onEnableAccess() else onScan() }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!mediaAccessOn) "♪ Allow Media Control" else "♪ No active media",
                        color = Color(0xFF80DEEA),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        } else {
            items(streams.size) { i ->
                val s = streams[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (s.isActive) Color(0xFF00BCD4).copy(alpha = 0.25f) else Color(0xFF0A2429))
                        .clickable { onSelect(s.id) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "♪ ${s.appName}: ${s.trackTitle}",
                        color = Color(0xFFE0F7FA),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}