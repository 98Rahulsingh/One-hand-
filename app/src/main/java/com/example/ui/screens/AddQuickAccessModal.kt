package com.example.ui.screens

import com.example.ui.components.FastAlphabetScroller
import androidx.compose.material3.Switch
import com.example.system.AppIcons
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveWindow
import com.example.model.AppItem
import com.example.model.ItemType
import com.example.model.isInGroup
import com.example.ui.components.FloatingWindowCard
import com.example.viewmodel.OneHandViewModel

@Composable
fun AddQuickAccessModal(
    viewModel: OneHandViewModel,
    modifier: Modifier = Modifier
) {
    val masterItems by viewModel.masterItems.collectAsState()
    val filterType by viewModel.itemFilterType.collectAsState()
    val panelSettings by viewModel.panelSettings.collectAsState()
    val target by viewModel.addTargetGroup.collectAsState()
    val groupAdded by viewModel.groupAdded.collectAsState()
    val groupRemoved by viewModel.groupRemoved.collectAsState()
    val manualGroups by viewModel.manualGroups.collectAsState()
    val tilesTab by viewModel.tilesTab.collectAsState()
    val quickSettings by viewModel.quickSettings.collectAsState()
    val pendingGesture by viewModel.pendingGestureToBind.collectAsState()
    val selectedLetter by viewModel.selectedAlphabetLetter.collectAsState()

    var selectedAddTab by remember(tilesTab, pendingGesture) { mutableStateOf("ACTIONS") }
    LaunchedEffect(tilesTab, pendingGesture) {
        viewModel.setItemFilterType(ItemType.ACTION)
        selectedAddTab = "ACTIONS"
    }
    var newTileName by remember { mutableStateOf("") }
    var showConnectivityCreator by remember { mutableStateOf(false) }
    var newGroupMode by remember { mutableStateOf(viewModel.consumeNewGroupRequest()) }
    var newGroupName by remember { mutableStateOf("") }
    val tileTargets by viewModel.tileTargets.collectAsState()

    // One canonical row per action/tile. Quick Settings tiles and ACTION master items
    // are merged by the executable tile ID, so the same toggle can never appear twice.
    val actionTileIds = remember {
        mapOf(
            "act_wifi" to "wifi", "act_bt" to "bluetooth", "act_data" to "data",
            "act_hotspot" to "hotspot", "act_airplane" to "airplane", "act_screenshot" to "screenshot", "act_lock" to "lock",
            "act_split" to "split", "act_floating" to "floating", "act_torch" to "torch",
            "act_mute" to "mute", "act_open_edge" to "open_edge", "act_open_quick" to "open_quick",
            "act_open_tools" to "open_tools", "act_open_media" to "open_media"
        )
    }
    fun tileIdFor(item: AppItem): String = actionTileIds[item.id]
        ?: if (item.id.startsWith("act_qs_")) item.id.removePrefix("act_qs_")
        else item.id.removePrefix("act_")

    val unifiedActions = remember(masterItems, quickSettings) {
        val byTileId = linkedMapOf<String, AppItem>()
        masterItems.filter { it.type == ItemType.ACTION }.forEach { item ->
            byTileId[tileIdFor(item)] = item
        }
        quickSettings.filter { !it.id.startsWith("app_") && it.id != "none" }.forEach { tile ->
            if (tile.id !in byTileId) {
                byTileId[tile.id] = AppItem(
                    id = "act_qs_${tile.id}", name = tile.name, packageName = "",
                    type = ItemType.ACTION, categoryName = "Tools", iconName = tile.iconName,
                    iconColor = tile.color, isAdded = false, subtitle = tile.description
                )
            }
        }
        byTileId.values.toList()
    }
    val filteredList = remember(masterItems, unifiedActions, filterType) {
        if (filterType == ItemType.ACTION) {
            val connectivity = setOf("wifi", "bluetooth", "data", "hotspot", "airplane")
            unifiedActions.sortedBy { if (tileIdFor(it) in connectivity || tileIdFor(it).startsWith("custom_")) 0 else 1 }
        } else masterItems.filter { item -> item.type == filterType }
    }
    val availableLetters = remember(filteredList) {
        filteredList.mapNotNull { it.name.firstOrNull()?.uppercaseChar() }.toSet()
    }
    val alphabetFilteredList = remember(filteredList, selectedLetter) {
        if (selectedLetter == null) filteredList else filteredList.filter {
            it.name.startsWith(selectedLetter.toString(), ignoreCase = true)
        }
    }


    FloatingWindowCard(
        title = "Add Item",
        initialGeometry = panelSettings.quickAccessGeometry,
        onGeometryChange = { x, y, w, h ->
            viewModel.updateQuickAccessGeometry(x, y, w, h)
        },
        onClose = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // One shared Apps + Actions catalogue. Group selection is inherited from the Edge Panel.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("ACTIONS", selectedAddTab == "ACTIONS", {
                    selectedAddTab = "ACTIONS"
                    viewModel.setItemFilterType(ItemType.ACTION)
                    viewModel.selectAlphabetLetter(null)
                }, "filter_pill_actions")
                FilterPill("APPS", selectedAddTab == "APPS", {
                    selectedAddTab = "APPS"
                    viewModel.setItemFilterType(ItemType.APP)
                    viewModel.selectAlphabetLetter(null)
                }, "filter_pill_apps")
                if (pendingGesture == null && !tilesTab) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { newGroupMode = !newGroupMode }) {
                        Text("+ Group", color = Color(0xFF80DEEA), fontSize = 11.sp)
                    }
                }
            }


            Spacer(modifier = Modifier.height(8.dp))
            if (pendingGesture != null) {
                Text("Choose an app or action for: $pendingGesture", color = Color(0xFF80DEEA), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (newGroupMode) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = newGroupName, onValueChange = { newGroupName = it },
                        placeholder = { Text("New group name", color = Color(0xFF6B7280), fontSize = 11.sp) },
                        singleLine = true, modifier = Modifier.weight(1f).height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF242C3D), focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    TextButton(onClick = { viewModel.createGroupFromWindow(newGroupName); newGroupName = ""; newGroupMode = false }) {
                        Text("Create", color = Color(0xFF00E5FF), fontSize = 11.sp)
                    }
                }
            }

            if (pendingGesture == null && !tilesTab) {
                Text("Add to group: $target", color = Color(0xFF80DEEA), fontSize = 11.sp)
            } else if (tilesTab) {
                Text("Quick Settings · Apps and Actions", color = Color(0xFF9CA3AF), fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if ((tilesTab && pendingGesture == null) || (showConnectivityCreator && !tilesTab && pendingGesture == null)) {
                    item(key = "connectivity_tile_creator") {
                        Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                            Text(if (tilesTab) "Add Quick Setting tile" else "Add Connectivity toggle", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newTileName, onValueChange = { newTileName = it },
                                    placeholder = { Text("Toggle name", color = Color(0xFF6B7280), fontSize = 12.sp) },
                                    singleLine = true, modifier = Modifier.weight(1f).height(50.dp).testTag("new_tile_name_field"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF242C3D),
                                        focusedContainerColor = Color(0xFF141B26), unfocusedContainerColor = Color(0xFF141B26),
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                    )
                                )
                                TextButton(onClick = {
                                    viewModel.addCustomTile(newTileName)
                                    newTileName = ""
                                    showConnectivityCreator = false
                                }, modifier = Modifier.testTag("add_custom_tile_button")) {
                                    Text("Add", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                if (!tilesTab && pendingGesture == null && filterType == ItemType.ACTION) {
                    item(key = "connectivity_section_header") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("CONNECTIVITY", color = Color(0xFF80DEEA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { showConnectivityCreator = !showConnectivityCreator }) {
                                Text("+", color = Color(0xFF00E5FF), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                items(alphabetFilteredList, key = { it.id }) { item ->
                    val actionTileId = if (item.type == ItemType.ACTION) tileIdFor(item) else null
                    val appTileId = if (item.type == ItemType.APP) "app_${item.packageName}" else null
                    val quickSetting = when {
                        actionTileId != null -> quickSettings.firstOrNull { it.id == actionTileId }
                        appTileId != null -> quickSettings.firstOrNull { it.id == appTileId }
                        else -> null
                    }
                    val added = when {
                        pendingGesture != null -> false
                        tilesTab -> quickSetting?.isVisible == true
                        else -> isInGroup(item, target, groupAdded, groupRemoved, manualGroups)
                    }
                    Column {
                        QuickAccessListItem(item = item, isAdded = added, onToggle = {
                            when {
                                pendingGesture != null && item.type == ItemType.ACTION ->
                                    viewModel.bindActionAndFinish(item, actionTileId ?: item.id)
                                pendingGesture != null -> viewModel.bindAndFinish(item)
                                tilesTab && item.type == ItemType.ACTION ->
                                    viewModel.toggleActionInQuickSettings(actionTileId ?: item.id, item)
                                tilesTab && item.type == ItemType.APP -> viewModel.toggleAppInQuickSettings(item)
                                else -> viewModel.toggleItemInGroup(item.id, target)
                            }
                        })
                        val isConnectivityAction = actionTileId in setOf("wifi", "bluetooth", "data", "hotspot", "airplane") ||
                            (actionTileId?.startsWith("custom_") == true)
                        if (item.type == ItemType.ACTION && actionTileId != null && (tilesTab || isConnectivityAction)) {
                            val saved = tileTargets[actionTileId]
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                if (saved != null) Text("${saved.label.ifBlank { item.name }}  (${saved.x}, ${saved.y})", color = Color(0xFF80DEEA),
                                    fontSize = 10.sp, modifier = Modifier.weight(1f).padding(start = 48.dp))
                                else Spacer(Modifier.weight(1f))
                                TextButton(onClick = { viewModel.startTrackTile(actionTileId) }) {
                                    Text("◎ Track", color = Color(0xFF00E5FF), fontSize = 11.sp)
                                }
                                if (saved != null) TextButton(onClick = { viewModel.clearTileTarget(actionTileId) }) {
                                    Text("Reset", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
                FastAlphabetScroller(
                    availableLetters = availableLetters,
                    selectedLetter = selectedLetter,
                    onLetterSelected = { viewModel.selectAlphabetLetter(it) },
                    modifier = Modifier.padding(start = 4.dp),
                    isRightAligned = true
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) },
                    modifier = Modifier.testTag("close_add_quick_access_button")
                ) {
                    Text(
                        text = "Done",
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF28364C) else Color(0xFF19202D))
            .border(
                1.dp,
                if (isSelected) Color(0xFF4C668C) else Color(0xFF263042),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF9CA3AF),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun QuickAccessListItem(
    item: AppItem,
    isAdded: Boolean,
    onToggle: () -> Unit
) {
    ChoiceRow(
        icon = getIconVectorForName(item.iconName),
        tint = Color(item.iconColor),
        title = item.name,
        subtitle = item.subtitle,
        checked = isAdded,
        tag = "quick_access_item_${item.id}",
        onToggle = onToggle,
        realIcon = if (item.type == ItemType.APP) AppIcons.get(item.packageName) else null
    )
}

@Composable
fun ChoiceRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onToggle: () -> Unit,
    realIcon: ImageBitmap? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(vertical = 5.dp, horizontal = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.2f))
                    .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (realIcon != null) {
                    Image(
                        bitmap = realIcon,
                        contentDescription = title,
                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(7.dp))
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = Color(0xFF6B7280),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) Color(0xFF00E5FF) else Color(0xFF1E2636))
                .border(1.dp, if (checked) Color(0xFF00E5FF) else Color(0xFF333E54), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Added",
                    tint = Color(0xFF00363A),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
