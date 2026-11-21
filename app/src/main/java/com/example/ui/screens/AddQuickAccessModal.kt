package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveWindow
import com.example.model.AppItem
import com.example.model.ItemType
import com.example.model.SPECIAL_GROUPS
import com.example.model.isInGroup
import com.example.system.AppIcons
import com.example.ui.components.FloatingWindowCard
import com.example.viewmodel.OneHandViewModel

private val GESTURE_SLOTS = listOf("Swipe Left", "Swipe Right", "Swipe Up", "Swipe Down")

@Composable
fun AddQuickAccessModal(
    viewModel: OneHandViewModel,
    modifier: Modifier = Modifier
) {
    val masterItems by viewModel.masterItems.collectAsState()
    val searchQuery by viewModel.itemSearchQuery.collectAsState()
    val panelSettings by viewModel.panelSettings.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val targetGroup by viewModel.addTargetGroup.collectAsState()
    val destination by viewModel.addDestination.collectAsState()
    val selectedGesture by viewModel.selectedGesture.collectAsState()
    val tilesTab by viewModel.tilesTab.collectAsState()
    val groupAdded by viewModel.groupAdded.collectAsState()
    val groupRemoved by viewModel.groupRemoved.collectAsState()
    val gestureItems by viewModel.gestureItems.collectAsState()
    val manualGroups by viewModel.manualGroups.collectAsState()
    val quickSettings by viewModel.quickSettings.collectAsState()
    val tileTargets by viewModel.tileTargets.collectAsState()

    var newGroupMode by remember { mutableStateOf(viewModel.consumeNewGroupRequest()) }
    var renameGroupMode by remember { mutableStateOf(false) }
    var renameGroupText by remember { mutableStateOf("") }
    var selectedAddTab by remember(tilesTab) {
        mutableStateOf(if (tilesTab || destination == "QUICK_SETTINGS") "ACTIONS" else "GROUPS")
    }
    var groupItemFilter by remember { mutableStateOf("ALL") }
    var showSearch by remember { mutableStateOf(true) }
    var newGroupName by remember { mutableStateOf("") }
    var newTileName by remember { mutableStateOf("") }

    val groups = remember(categories) { categories.filter { it !in SPECIAL_GROUPS } }

    val uniqueMasterItems = remember(masterItems) {
        masterItems.distinctBy { item ->
            if (item.type == ItemType.APP && item.packageName.isNotBlank()) "app:${item.packageName}"
            else item.id
        }
    }

    val groupCounts = remember(uniqueMasterItems, groups, groupAdded, groupRemoved, manualGroups) {
        groups.associateWith { g ->
            uniqueMasterItems.count { item -> isInGroup(item, g, groupAdded, groupRemoved, manualGroups) }
        }
    }

    val gestureCount = remember(gestureItems, selectedGesture) {
        gestureItems[selectedGesture]?.size ?: 0
    }

    fun isItemSelectedForDest(item: AppItem): Boolean = when (destination) {
        "GESTURE" -> item.id in gestureItems[selectedGesture].orEmpty()
        else -> isInGroup(item, targetGroup, groupAdded, groupRemoved, manualGroups)
    }

    fun toggleItemForDest(item: AppItem) {
        when (destination) {
            "GESTURE" -> viewModel.toggleItemInGesture(item.id, selectedGesture)
            else -> viewModel.toggleItemInGroup(item.id, targetGroup)
        }
    }

    val filteredGroupItems = remember(uniqueMasterItems, groupItemFilter, searchQuery) {
        uniqueMasterItems.filter { item ->
            val matchesType = when (groupItemFilter) {
                "APPS" -> item.type == ItemType.APP
                "ACTIONS" -> item.type == ItemType.ACTION
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true)
            matchesType && matchesQuery
        }
    }

    val filteredActions = remember(uniqueMasterItems, searchQuery) {
        uniqueMasterItems.filter { item ->
            item.type == ItemType.ACTION && (
                searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true)
                )
        }
    }

    val filteredApps = remember(uniqueMasterItems, searchQuery) {
        uniqueMasterItems.filter { item ->
            item.type == ItemType.APP && (
                searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true)
                )
        }
    }

    val filteredTiles = remember(quickSettings, searchQuery) {
        quickSettings.filter {
            searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val tileIdSet = remember(quickSettings) { quickSettings.map { it.id }.toSet() }

    FloatingWindowCard(
        title = "Add Item",
        initialGeometry = panelSettings.quickAccessGeometry,
        onGeometryChange = { x, y, w, h ->
            viewModel.updateQuickAccessGeometry(x, y, w, h)
        },
        onClose = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) },
        headerActions = {
            IconButton(
                onClick = { showSearch = !showSearch },
                modifier = Modifier.size(26.dp).testTag("toggle_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (showSearch || searchQuery.isNotBlank()) Color(0xFF00E5FF) else Color(0xFF80DEEA),
                    modifier = Modifier.size(15.dp)
                )
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (showSearch) {
                CompactInputBar(
                    value = searchQuery,
                    onValueChange = { viewModel.setItemSearchQuery(it) },
                    placeholder = "Search apps, actions, tiles...",
                    leadingIcon = Icons.Default.Search,
                    onClear = if (searchQuery.isNotEmpty()) { { viewModel.setItemSearchQuery("") } } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_access_search_field")
                )
                Spacer(modifier = Modifier.height(5.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("To:", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                FilterPill(
                    label = "Group ($targetGroup)",
                    isSelected = destination == "GROUP",
                    onClick = {
                        viewModel.setAddDestination("GROUP")
                        viewModel.setTilesTab(false)
                    },
                    testTag = "dest_group",
                    modifier = Modifier.weight(1f)
                )
                FilterPill(
                    label = "Gesture ($gestureCount)",
                    isSelected = destination == "GESTURE",
                    onClick = {
                        viewModel.setAddDestination("GESTURE")
                        viewModel.setTilesTab(false)
                        if (selectedAddTab == "GROUPS") selectedAddTab = "ACTIONS"
                    },
                    testTag = "dest_gesture",
                    modifier = Modifier.weight(1f)
                )
                FilterPill(
                    label = "Quick Settings",
                    isSelected = destination == "QUICK_SETTINGS",
                    onClick = {
                        viewModel.setAddDestination("QUICK_SETTINGS")
                        viewModel.setTilesTab(true)
                        selectedAddTab = "ACTIONS"
                    },
                    testTag = "dest_quick_settings",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterPill(
                    label = "GROUPS",
                    isSelected = selectedAddTab == "GROUPS",
                    onClick = {
                        selectedAddTab = "GROUPS"
                        viewModel.setAddDestination("GROUP")
                        viewModel.setTilesTab(false)
                    },
                    testTag = "filter_pill_groups",
                    modifier = Modifier.weight(1f)
                )
                FilterPill(
                    label = "ACTIONS",
                    isSelected = selectedAddTab == "ACTIONS",
                    onClick = {
                        selectedAddTab = "ACTIONS"
                        viewModel.setItemFilterType(ItemType.ACTION)
                    },
                    testTag = "filter_pill_actions",
                    modifier = Modifier.weight(1f)
                )
                FilterPill(
                    label = "APPS",
                    isSelected = selectedAddTab == "APPS",
                    onClick = {
                        selectedAddTab = "APPS"
                        if (destination == "QUICK_SETTINGS") viewModel.setAddDestination("GROUP")
                        viewModel.setTilesTab(false)
                        viewModel.setItemFilterType(ItemType.APP)
                    },
                    testTag = "filter_pill_apps",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (selectedAddTab) {
                "GROUPS" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        groups.forEach { g ->
                            val count = groupCounts[g] ?: 0
                            FilterPill(
                                label = "$g ($count)",
                                isSelected = g == targetGroup,
                                onClick = {
                                    viewModel.setAddTargetGroup(g)
                                    renameGroupMode = false
                                },
                                testTag = "target_group_$g"
                            )
                        }
                        FilterPill(
                            label = "+ New",
                            isSelected = newGroupMode,
                            onClick = {
                                newGroupMode = !newGroupMode
                                renameGroupMode = false
                            },
                            testTag = "new_group_pill"
                        )
                    }

                    if (newGroupMode) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CompactInputBar(
                                value = newGroupName,
                                onValueChange = { newGroupName = it },
                                placeholder = "New group name",
                                modifier = Modifier.weight(1f).testTag("new_group_name_field")
                            )
                            CompactActionChip(
                                label = "Create",
                                color = Color(0xFF00E5FF),
                                onClick = {
                                    if (newGroupName.isNotBlank()) {
                                        viewModel.createGroupFromWindow(newGroupName)
                                        newGroupName = ""
                                        newGroupMode = false
                                    }
                                },
                                modifier = Modifier.testTag("create_group_button")
                            )
                        }
                    }

                    if (groups.contains(targetGroup)) {
                        Spacer(modifier = Modifier.height(5.dp))
                        val isManual = targetGroup in manualGroups
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F2227))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isManual) "Manual" else "Auto",
                                    color = if (isManual) Color(0xFF80DEEA) else Color(0xFF00E5FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Switch(
                                    checked = !isManual,
                                    onCheckedChange = { viewModel.setGroupManual(targetGroup, !it) },
                                    modifier = Modifier.scale(0.7f).height(24.dp).testTag("group_auto_switch")
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CompactActionChip(
                                    label = "Rename",
                                    icon = Icons.Default.Edit,
                                    color = Color(0xFF80DEEA),
                                    onClick = {
                                        renameGroupText = targetGroup
                                        renameGroupMode = !renameGroupMode
                                    },
                                    modifier = Modifier.testTag("rename_group_button")
                                )
                                CompactActionChip(
                                    label = "Delete",
                                    icon = Icons.Default.Delete,
                                    color = Color(0xFFFF8A80),
                                    onClick = {
                                        viewModel.deleteGroup(targetGroup)
                                        renameGroupMode = false
                                    },
                                    modifier = Modifier.testTag("delete_group_button")
                                )
                            }
                        }

                        if (renameGroupMode) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CompactInputBar(
                                    value = renameGroupText,
                                    onValueChange = { renameGroupText = it },
                                    placeholder = "Rename '$targetGroup'",
                                    modifier = Modifier.weight(1f).testTag("rename_group_field")
                                )
                                CompactActionChip(
                                    label = "Save",
                                    color = Color(0xFF00E5FF),
                                    onClick = {
                                        viewModel.renameGroup(targetGroup, renameGroupText)
                                        renameGroupMode = false
                                    },
                                    modifier = Modifier.testTag("save_rename_group_button")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterPill("All", groupItemFilter == "ALL", { groupItemFilter = "ALL" }, "group_filter_all")
                        FilterPill("Apps", groupItemFilter == "APPS", { groupItemFilter = "APPS" }, "group_filter_apps")
                        FilterPill("Actions", groupItemFilter == "ACTIONS", { groupItemFilter = "ACTIONS" }, "group_filter_actions")
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${groupCounts[targetGroup] ?: 0} added",
                            color = Color(0xFF80DEEA),
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(filteredGroupItems, key = { it.id }) { item ->
                            QuickAccessListItem(
                                item = item,
                                isAdded = isInGroup(item, targetGroup, groupAdded, groupRemoved, manualGroups),
                                onToggle = { viewModel.toggleItemInGroup(item.id, targetGroup) }
                            )
                        }
                    }
                }

                "ACTIONS" -> {
                    if (destination == "GESTURE") {
                        GestureTargetBar(
                            selectedGesture = selectedGesture,
                            onSelect = { viewModel.setSelectedGesture(it) }
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                    } else if (destination == "GROUP") {
                        GroupTargetBar(
                            groups = groups,
                            target = targetGroup,
                            onSelect = { viewModel.setAddTargetGroup(it) }
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactInputBar(
                            value = newTileName,
                            onValueChange = { newTileName = it },
                            placeholder = "New custom QS tile...",
                            modifier = Modifier.weight(1f).testTag("new_tile_name_field")
                        )
                        CompactActionChip(
                            label = "+ Tile",
                            color = Color(0xFF00E5FF),
                            onClick = {
                                if (newTileName.isNotBlank()) {
                                    viewModel.addCustomTile(newTileName)
                                    newTileName = ""
                                }
                            },
                            modifier = Modifier.testTag("add_custom_tile_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (filteredTiles.isNotEmpty()) {
                            item(key = "header_qs_tiles") {
                                Text(
                                    text = "QUICK SETTINGS TILES (${filteredTiles.count { it.isVisible }}/${filteredTiles.size} shown)",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 2.dp)
                                )
                            }
                            items(filteredTiles, key = { "tile_${it.id}" }) { tile ->
                                val savedTarget = tileTargets[tile.id]
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F2227))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    ChoiceRow(
                                        icon = getQuickSettingIcon(tile.iconName),
                                        tint = Color(tile.color),
                                        title = tile.name,
                                        subtitle = tile.description,
                                        checked = tile.isVisible,
                                        tag = "quick_tile_item_${tile.id}",
                                        onToggle = { viewModel.toggleTileVisible(tile.id) }
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 38.dp, end = 4.dp, bottom = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (savedTarget != null) {
                                                "Target: ${savedTarget.label.ifBlank { "${savedTarget.x},${savedTarget.y}" }}"
                                            } else {
                                                "No target"
                                            },
                                            color = if (savedTarget != null) Color(0xFF80DEEA) else Color(0xFF6B7280),
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactActionChip(
                                            label = if (savedTarget == null) "Track" else "Re-track",
                                            color = Color(0xFF00E5FF),
                                            onClick = { viewModel.startTrackTile(tile.id, returnToTiles = true) }
                                        )
                                        if (savedTarget != null) {
                                            CompactActionChip(
                                                label = "Reset",
                                                color = Color(0xFFFFCC80),
                                                onClick = { viewModel.clearTileTarget(tile.id) }
                                            )
                                        }
                                        if (tile.id.startsWith("custom_")) {
                                            CompactActionChip(
                                                label = "Delete",
                                                color = Color(0xFFFF8A80),
                                                onClick = { viewModel.removeCustomTile(tile.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (destination != "QUICK_SETTINGS" && filteredActions.isNotEmpty()) {
                            item(key = "header_actions") {
                                val destLabel = if (destination == "GESTURE") selectedGesture else targetGroup
                                Text(
                                    text = "ACTIONS (ADD TO ${destLabel.uppercase()})",
                                    color = Color(0xFF80DEEA),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 2.dp)
                                )
                            }
                            items(filteredActions, key = { "act_${it.id}" }) { item ->
                                val trackId = actionTrackId(item.id)
                                val showInlineTrack = trackId != null && trackId !in tileIdSet
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F2227))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    QuickAccessListItem(
                                        item = item,
                                        isAdded = isItemSelectedForDest(item),
                                        onToggle = { toggleItemForDest(item) }
                                    )
                                    if (showInlineTrack && trackId != null) {
                                        val saved = tileTargets[trackId]
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 38.dp, end = 4.dp, bottom = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (saved == null) "No target" else "Target: ${saved.label.ifBlank { "${saved.x},${saved.y}" }}",
                                                color = if (saved != null) Color(0xFF80DEEA) else Color(0xFF6B7280),
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            CompactActionChip(
                                                label = if (saved == null) "Track" else "Re-track",
                                                color = Color(0xFF00E5FF),
                                                onClick = { viewModel.startTrackAction(item.id) }
                                            )
                                            if (saved != null) {
                                                CompactActionChip(
                                                    label = "Reset",
                                                    color = Color(0xFFFFCC80),
                                                    onClick = { viewModel.clearTileTarget(trackId) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (destination == "GESTURE") {
                        GestureTargetBar(
                            selectedGesture = selectedGesture,
                            onSelect = { viewModel.setSelectedGesture(it) }
                        )
                    } else {
                        GroupTargetBar(
                            groups = groups,
                            target = targetGroup,
                            onSelect = { viewModel.setAddTargetGroup(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(filteredApps, key = { it.id }) { item ->
                            QuickAccessListItem(
                                item = item,
                                isAdded = isItemSelectedForDest(item),
                                onToggle = { toggleItemForDest(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun actionTrackId(itemId: String): String? = when (itemId) {
    "act_wifi" -> "wifi"
    "act_bt" -> "bluetooth"
    "act_data" -> "data"
    "act_hotspot" -> "hotspot"
    "act_airplane" -> "airplane"
    "act_floating" -> "floating"
    "act_lock" -> "lock"
    "act_screenshot" -> "screenshot"
    "act_split" -> "split"
    "act_darkmode" -> "darkmode"
    "act_battery_saver" -> "battery_saver"
    "act_location" -> "location"
    "act_active_apps" -> "active_apps"
    "act_reading_mode" -> "reading_mode"
    else -> null
}

@Composable
private fun GestureTargetBar(
    selectedGesture: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("Gesture:", color = Color(0xFF9CA3AF), fontSize = 10.sp)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            GESTURE_SLOTS.forEach { slot ->
                FilterPill(slot, slot == selectedGesture, { onSelect(slot) }, "target_gesture_$slot")
            }
        }
    }
}

@Composable
private fun GroupTargetBar(
    groups: List<String>,
    target: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Group:", color = Color(0xFF9CA3AF), fontSize = 10.sp)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            groups.forEach { group ->
                FilterPill(group, group == target, { onSelect(group) }, "target_group_$group")
            }
        }
    }
}

@Composable
private fun CompactInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onClear: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141B26))
            .border(1.dp, Color(0xFF242C3D), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                cursorBrush = SolidColor(Color(0xFF00E5FF)),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (onClear != null) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onClear() }
            )
        }
    }
}

@Composable
private fun CompactActionChip(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF1A3A44) else Color(0xFF141B26))
            .border(
                1.dp,
                if (isSelected) Color(0xFF00E5FF) else Color(0xFF263042),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF9CA3AF),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (checked) Color(0xFF122B32) else Color.Transparent)
            .clickable { onToggle() }
            .padding(vertical = 4.dp, horizontal = 6.dp)
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
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(tint.copy(alpha = 0.18f))
                    .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (realIcon != null) {
                    Image(
                        bitmap = realIcon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = Color(0xFF6B7280),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .size(20.dp)
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
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
