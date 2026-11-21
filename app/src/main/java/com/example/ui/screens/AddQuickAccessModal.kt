package com.example.ui.screens

import com.example.model.SPECIAL_GROUPS
import androidx.compose.material3.Switch
import com.example.system.AppIcons
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
fun AddQuickAccessModal(viewModel: OneHandViewModel, modifier: Modifier = Modifier) {
    val masterItems by viewModel.masterItems.collectAsState()
    val filterType by viewModel.itemFilterType.collectAsState()
    val searchQuery by viewModel.itemSearchQuery.collectAsState()
    val panelSettings by viewModel.panelSettings.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val target by viewModel.addTargetGroup.collectAsState()
    val tilesTab by viewModel.tilesTab.collectAsState()
    val groupAdded by viewModel.groupAdded.collectAsState()
    val groupRemoved by viewModel.groupRemoved.collectAsState()
    val manualGroups by viewModel.manualGroups.collectAsState()
    val quickSettings by viewModel.quickSettings.collectAsState()
    val tileTargets by viewModel.tileTargets.collectAsState()
    var page by remember { mutableStateOf("GROUPS") }
    var newGroupMode by remember { mutableStateOf(viewModel.consumeNewGroupRequest()) }
    var newGroupName by remember { mutableStateOf("") }
    var newTileName by remember { mutableStateOf("") }
    var quickDialMode by remember { mutableStateOf(false) }
    val groups = categories.filter { it !in SPECIAL_GROUPS }
    val filteredItems = remember(masterItems, filterType, searchQuery, quickDialMode) {
        masterItems.filter { item ->
            val typeMatch = if (quickDialMode) item.type == ItemType.SHORTCUT &&
                (item.name.contains("dial", true) || item.name.contains("call", true) ||
                 item.id.contains("dial", true) || item.id.contains("call", true))
                else item.type == filterType
            typeMatch && (searchQuery.isBlank() || item.name.contains(searchQuery, true) ||
                item.subtitle.contains(searchQuery, true))
        }
    }
    val filteredTiles = remember(quickSettings, searchQuery) {
        quickSettings.filter { searchQuery.isBlank() || it.name.contains(searchQuery, true) ||
            it.description.contains(searchQuery, true) }
    }
    FloatingWindowCard(
        title = "Add Item", initialGeometry = panelSettings.quickAccessGeometry,
        onGeometryChange = { x, y, w, h -> viewModel.updateQuickAccessGeometry(x, y, w, h) },
        onClose = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) }, modifier = modifier
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0D252B)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterPill("GROUPS", page == "GROUPS", {
                    page = "GROUPS"; quickDialMode = false; viewModel.setTilesTab(false)
                    viewModel.setItemFilterType(ItemType.APP)
                }, "add_item_groups_tab")
                FilterPill("ITEMS", page == "ITEMS", {
                    page = "ITEMS"; quickDialMode = false; viewModel.setTilesTab(false)
                    viewModel.setItemFilterType(ItemType.APP)
                }, "add_item_items_tab")
            }
            Spacer(Modifier.height(8.dp))
            if (tilesTab) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("QUICK SETTINGS TILES", color = Color(0xFF80DEEA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.setTilesTab(false) }) { Text("Back", color = Color(0xFF00E5FF)) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newTileName, onValueChange = { newTileName = it },
                        placeholder = { Text("New tile name", fontSize = 11.sp) }, singleLine = true,
                        modifier = Modifier.weight(1f).height(50.dp).testTag("new_tile_name_field"),
                        shape = RoundedCornerShape(9.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF31515A), focusedContainerColor = Color(0xFF10252A),
                            unfocusedContainerColor = Color(0xFF10252A), focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White))
                    TextButton(onClick = { viewModel.addCustomTile(newTileName); newTileName = "" },
                        modifier = Modifier.testTag("add_custom_tile_button")) { Text("Add", color = Color(0xFF00E5FF)) }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(filteredTiles, key = { it.id }) { tile ->
                        Column {
                            ChoiceRow(getQuickSettingIcon(tile.iconName), Color(tile.color), tile.name, tile.description,
                                tile.isVisible, "quick_tile_item_${tile.id}", { viewModel.toggleTileVisible(tile.id) })
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                val saved = tileTargets[tile.id]
                                Text(if (saved == null) "No target" else "Target: ${saved.label.ifBlank { "saved spot" }}",
                                    color = Color(0xFF80DEEA), fontSize = 9.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.startTrackTile(tile.id) }) { Text("Track", color = Color(0xFF00E5FF)) }
                                if (saved != null) TextButton(onClick = { viewModel.clearTileTarget(tile.id) }) {
                                    Text("Reset", color = Color(0xFFFFCC80))
                                }
                                if (tile.id.startsWith("custom_")) TextButton(onClick = { viewModel.removeCustomTile(tile.id) }) {
                                    Text("Delete", color = Color(0xFFFF8A80))
                                }
                            }
                        }
                    }
                }
            } else if (page == "GROUPS") {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.width(88.dp).fillMaxHeight().clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0D252B)).padding(4.dp)) {
                        Text("GROUPS", color = Color(0xFF80DEEA), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(5.dp))
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(groups, key = { it }) { group ->
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
                                    .background(if (group == target) Color(0xFF00AFC2) else Color(0xFF17343A))
                                    .clickable { viewModel.setAddTargetGroup(group) }
                                    .padding(horizontal = 4.dp, vertical = 9.dp).testTag("target_group_$group"),
                                    contentAlignment = Alignment.Center) {
                                    Text(group, color = if (group == target) Color(0xFF002A30) else Color(0xFFB2EBF2),
                                        fontSize = 10.sp, fontWeight = if (group == target) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 2)
                                }
                            }
                        }
                        TextButton(onClick = { newGroupMode = !newGroupMode }, modifier = Modifier.fillMaxWidth().testTag("new_group_pill")) {
                            Text("+ Group", color = Color(0xFF00E5FF), fontSize = 10.sp)
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(target.ifBlank { "Choose group" }, color = Color.White, fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("Select apps, actions or shortcuts", color = Color(0xFF78949A), fontSize = 10.sp)
                            }
                            if (groups.contains(target)) TextButton(onClick = { viewModel.deleteGroup(target) },
                                modifier = Modifier.testTag("delete_group_button")) {
                                Text("Delete", color = Color(0xFFFF8A80), fontSize = 10.sp)
                            }
                        }
                        if (newGroupMode) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = newGroupName, onValueChange = { newGroupName = it },
                                    placeholder = { Text("Group name", fontSize = 11.sp) }, singleLine = true,
                                    modifier = Modifier.weight(1f).height(50.dp).testTag("new_group_name_field"),
                                    shape = RoundedCornerShape(9.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF31515A), focusedContainerColor = Color(0xFF10252A),
                                        unfocusedContainerColor = Color(0xFF10252A), focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White))
                                TextButton(onClick = {
                                    viewModel.createGroupFromWindow(newGroupName); newGroupName = ""; newGroupMode = false
                                }, modifier = Modifier.testTag("create_group_button")) { Text("Create", color = Color(0xFF00E5FF)) }
                            }
                        }
                        if (groups.contains(target)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(if (target in manualGroups) "Manual group" else "Auto group",
                                        color = Color.White, fontSize = 11.sp)
                                    Text(if (target in manualGroups) "Only selected items" else "Auto-matched + selected",
                                        color = Color(0xFF78949A), fontSize = 9.sp)
                                }
                                Switch(checked = target !in manualGroups,
                                    onCheckedChange = { viewModel.setGroupManual(target, !it) },
                                    modifier = Modifier.testTag("group_auto_switch"))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            FilterPill("Apps", filterType == ItemType.APP, { viewModel.setItemFilterType(ItemType.APP) }, "group_filter_apps")
                            FilterPill("Actions", filterType == ItemType.ACTION, { viewModel.setItemFilterType(ItemType.ACTION) }, "group_filter_actions")
                            FilterPill("Shortcuts", filterType == ItemType.SHORTCUT, { viewModel.setItemFilterType(ItemType.SHORTCUT) }, "group_filter_shortcuts")
                        }
                        OutlinedTextField(value = searchQuery, onValueChange = viewModel::setItemSearchQuery,
                            placeholder = { Text("Search items...", color = Color(0xFF78949A), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color(0xFF80DEEA), modifier = Modifier.size(17.dp)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth().height(47.dp).testTag("quick_access_search_field"),
                            shape = RoundedCornerShape(9.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF31515A), focusedContainerColor = Color(0xFF10252A),
                                unfocusedContainerColor = Color(0xFF10252A), focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White))
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(filteredItems, key = { it.id }) { item ->
                                QuickAccessListItem(item, isInGroup(item, target, groupAdded, groupRemoved, manualGroups)) {
                                    viewModel.toggleItemInGroup(item.id, target)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        FilterPill("Apps", filterType == ItemType.APP, {
                            quickDialMode = false; viewModel.setItemFilterType(ItemType.APP)
                        }, "items_filter_apps")
                        FilterPill("Actions", filterType == ItemType.ACTION, {
                            quickDialMode = false; viewModel.setItemFilterType(ItemType.ACTION)
                        }, "items_filter_actions")
                        FilterPill("Shortcuts", filterType == ItemType.SHORTCUT && !quickDialMode, {
                            quickDialMode = false; viewModel.setItemFilterType(ItemType.SHORTCUT)
                        }, "items_filter_shortcuts")
                        FilterPill("Quick Dial", quickDialMode, {
                            quickDialMode = true; viewModel.setItemFilterType(ItemType.SHORTCUT)
                        }, "items_filter_quick_dial")
                    }
                    OutlinedTextField(value = searchQuery, onValueChange = viewModel::setItemSearchQuery,
                        placeholder = { Text("Search apps, actions and shortcuts...", color = Color(0xFF78949A), fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color(0xFF80DEEA), modifier = Modifier.size(17.dp)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth().height(47.dp).testTag("quick_access_search_field"),
                        shape = RoundedCornerShape(9.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF31515A), focusedContainerColor = Color(0xFF10252A),
                            unfocusedContainerColor = Color(0xFF10252A), focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White))
                    if (filterType == ItemType.ACTION && !quickDialMode) TextButton(onClick = { viewModel.setTilesTab(true) }) {
                        Text("Quick Settings Tiles +", color = Color(0xFF00E5FF), fontSize = 10.sp)
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(filteredItems, key = { it.id }) { item ->
                            QuickAccessListItem(item, item.isAdded) { viewModel.toggleItemAdded(item.id) }
                        }
                        if (quickDialMode && filteredItems.isEmpty()) item {
                            Text("No Quick Dial shortcut found. Add a supported call shortcut first.",
                                color = Color(0xFF78949A), fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) },
                    modifier = Modifier.testTag("close_add_quick_access_button")) {
                    Text("Done", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
