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
fun AddQuickAccessModal(
    viewModel: OneHandViewModel,
    modifier: Modifier = Modifier
) {
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

    var newGroupMode by remember { mutableStateOf(viewModel.consumeNewGroupRequest()) }
    var newGroupName by remember { mutableStateOf("") }
    var newTileName by remember { mutableStateOf("") }
    val tileTargets by viewModel.tileTargets.collectAsState()
    val groups = categories.filter { it !in SPECIAL_GROUPS }

    val filteredList = remember(masterItems, filterType, searchQuery) {
        masterItems.filter { item ->
            val matchesType = item.type == filterType
            val matchesQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true)
            matchesType && matchesQuery
        }
    }
    val filteredTiles = remember(quickSettings, searchQuery) {
        quickSettings.filter {
            searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
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
            // Tabs: APPS, SHORTCUTS, ACTIONS, QUICK TILES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill("APPS", !tilesTab && filterType == ItemType.APP, {
                    viewModel.setTilesTab(false); viewModel.setItemFilterType(ItemType.APP)
                }, "filter_pill_apps")
                FilterPill("SHORTCUTS", !tilesTab && filterType == ItemType.SHORTCUT, {
                    viewModel.setTilesTab(false); viewModel.setItemFilterType(ItemType.SHORTCUT)
                }, "filter_pill_shortcuts")
                FilterPill("ACTIONS", !tilesTab && filterType == ItemType.ACTION, {
                    viewModel.setTilesTab(false); viewModel.setItemFilterType(ItemType.ACTION)
                }, "filter_pill_actions")
                FilterPill("QUICK TILES", tilesTab, { viewModel.setTilesTab(true) }, "filter_pill_tiles")
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!tilesTab) {
                Text(
                    text = "Add to group",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groups.forEach { g ->
                        FilterPill(g, g == target, { viewModel.setAddTargetGroup(g) }, "target_group_$g")
                    }
                    FilterPill("+ New group", newGroupMode, { newGroupMode = !newGroupMode }, "new_group_pill")
                }
                if (newGroupMode) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            placeholder = { Text("Group name", color = Color(0xFF6B7280), fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("new_group_name_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF242C3D),
                                focusedContainerColor = Color(0xFF141B26),
                                unfocusedContainerColor = Color(0xFF141B26),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        TextButton(
                            onClick = {
                                viewModel.createGroupFromWindow(newGroupName)
                                newGroupName = ""
                                newGroupMode = false
                            },
                            modifier = Modifier.testTag("create_group_button")
                        ) {
                            Text("Create", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (groups.contains(target)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = { viewModel.deleteGroup(target) },
                            modifier = Modifier.testTag("delete_group_button")
                        ) {
                            Text("Delete group '$target'", color = Color(0xFFFF8A80), fontSize = 11.sp)
                        }
                    }
                }
                if (groups.contains(target)) {
                    val isManual = target in manualGroups
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isManual) "Manual group" else "Auto-detect group",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isManual) "Only the apps you tick below" else "Matching apps arrive by themselves",
                                color = Color(0xFF6B7280),
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = !isManual,
                            onCheckedChange = { viewModel.setGroupManual(target, !it) },
                            modifier = Modifier.testTag("group_auto_switch")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Text(
                    text = "Tick the tiles you want in the quick tile row",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setItemSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search apps, shortcuts & actions...",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("quick_access_search_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF242C3D),
                    focusedContainerColor = Color(0xFF141B26),
                    unfocusedContainerColor = Color(0xFF141B26),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tilesTab) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newTileName,
                                    onValueChange = { newTileName = it },
                                    placeholder = { Text("New custom tile name", color = Color(0xFF6B7280), fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(50.dp).testTag("new_tile_name_field"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF242C3D),
                                        focusedContainerColor = Color(0xFF141B26),
                                        unfocusedContainerColor = Color(0xFF141B26),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                TextButton(
                                    onClick = { viewModel.addCustomTile(newTileName); newTileName = "" },
                                    modifier = Modifier.testTag("add_custom_tile_button")
                                ) {
                                    Text("Add", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Make a tile, press Track icon, then press the icon in quick settings that it should tap.",
                                color = Color(0xFF6B7280),
                                fontSize = 10.sp
                            )
                        }
                    }
                    items(filteredTiles, key = { it.id }) { tile ->
                        Column {
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
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val target = tileTargets[tile.id]
                                if (target != null) {
                                    Text(
                                        text = "Presses: " + target.label.ifBlank { "saved spot" },
                                        color = Color(0xFF80DEEA),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f).padding(start = 48.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                TextButton(onClick = { viewModel.startTrackTile(tile.id) }) {
                                    Text("Track icon", color = Color(0xFF00E5FF), fontSize = 11.sp)
                                }
                                if (target != null) {
                                    TextButton(onClick = { viewModel.clearTileTarget(tile.id) }) {
                                        Text("Reset", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                                    }
                                }
                                if (tile.id.startsWith("custom_")) {
                                    TextButton(onClick = { viewModel.removeCustomTile(tile.id) }) {
                                        Text("Delete", color = Color(0xFFFF8A80), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredList, key = { it.id }) { item ->
                        QuickAccessListItem(
                            item = item,
                            isAdded = isInGroup(item, target, groupAdded, groupRemoved, manualGroups),
                            onToggle = { viewModel.toggleItemInGroup(item.id, target) }
                        )
                    }
                }
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
