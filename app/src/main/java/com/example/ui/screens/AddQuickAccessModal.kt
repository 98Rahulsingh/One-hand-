package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.system.AppIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveWindow
import com.example.model.ItemType
import com.example.model.SPECIAL_GROUPS
import com.example.model.isInGroup
import com.example.ui.components.FloatingWindowCard
import com.example.viewmodel.OneHandViewModel
import kotlinx.coroutines.launch

private val DefaultV5Groups = listOf(
    "Recent", "Most Used", "All", "Image", "Social", "Media", "Productivity",
    "Fresh Install", "Tool", "Video", "Music", "Google", "Work", "Browser",
    "AI", "Communication", "Game"
)

private val DefaultV5Actions = listOf(
    "Edge Panel", "Quick Tool", "Media Control", "Quick Panel", "One Hand Mode",
    "Back", "Home", "Recent", "Switch Previous", "Split Window", "Floating Window",
    "Active App", "Torch", "Dark Mode", "Battery Saver", "Auto Brightness",
    "Reading Mode", "Location Service", "Mobile Data", "Wi-Fi", "Hotspot",
    "Bluetooth", "Earphone Mode"
)

@Composable
fun AddQuickAccessModal(
    viewModel: OneHandViewModel,
    modifier: Modifier = Modifier
) {
    val masterItems by viewModel.masterItems.collectAsState()
    val panelSettings by viewModel.panelSettings.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val manualGroups by viewModel.manualGroups.collectAsState()
    val groupAdded by viewModel.groupAdded.collectAsState()
    val groupRemoved by viewModel.groupRemoved.collectAsState()
    val quickSettings by viewModel.quickSettings.collectAsState()
    val tileTargets by viewModel.tileTargets.collectAsState()
    val pendingGesture by viewModel.pendingGestureToBind.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0=Apps, 1=Action, 2=Groups
    var showGroupList by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var newGroupMode by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var newToggleName by remember { mutableStateOf("") }
    var showNewToggle by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }

    val ctx = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val allGroups = remember(categories) { (DefaultV5Groups + categories).distinct() }
    val appItems = remember(masterItems, selectedGroup, groupAdded, groupRemoved, manualGroups) {
        val baseApps = masterItems.filter { it.type == ItemType.APP || it.type == ItemType.ACTION }.sortedBy { it.name.lowercase() }
        val g = selectedGroup
        if (g == null || g == "All") baseApps
        else baseApps.filter { isInGroup(it, g, groupAdded, groupRemoved, manualGroups) }
    }

    FloatingWindowCard(
        title = "Add Item",
        initialGeometry = panelSettings.quickAccessGeometry,
        onGeometryChange = { x, y, w, h -> viewModel.updateQuickAccessGeometry(x, y, w, h) },
        onClose = { viewModel.navigateTo(ActiveWindow.MAIN_PANEL) },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Single horizontal top bar: Apps | Action | Groups
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Apps", "Action", "Groups").forEachIndexed { idx, label ->
                    val selected = activeTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) Color(0xFF00BCD4) else Color(0xFF12232A))
                            .clickable {
                                if (idx == 2) {
                                    activeTab = 2
                                    showGroupList = !showGroupList
                                } else {
                                    activeTab = idx
                                    showGroupList = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (idx == 2 && selectedGroup != null) "Groups (${selectedGroup!!.take(6)})" else label,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (activeTab == 0) {
                // APPS TAB with ABCD popup slider (no search bar)
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(appItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (item.isAdded) Color(0xFF00BCD4).copy(alpha = 0.2f) else Color(0xFF10252C))
                                    .clickable { viewModel.bindAndFinish(item) }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    val bmp = remember(item.packageName) {
                                        if (item.packageName.isNotBlank()) AppIcons.get(item.packageName) else null
                                    }
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = item.name,
                                            modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = getIconVectorForName(item.iconName),
                                            contentDescription = item.name,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(item.name, color = Color(0xFFE0F7FA), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(if (item.isAdded) "✓" else "+", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    V5AbcdRail(
                        onLetter = { ch ->
                            activeLetter = ch
                            val targetIdx = appItems.indexOfFirst { it.name.startsWith(ch, ignoreCase = true) }
                            if (targetIdx >= 0) scope.launch { listState.scrollToItem(targetIdx) }
                        },
                        onRelease = { activeLetter = null }
                    )
                }
            } else if (activeTab == 1) {
                // ACTION TAB: top Action Name hide/unhide switch + full action list + Create Toggle
                V5SlimSwitchRow("Hide Action Names", !panelSettings.showActionNameInList) { hide ->
                    viewModel.updateSettings { it.copy(showActionNameInList = !hide) }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(quickSettings) { qs ->
                        val actName = qs.name
                        val actionKey = qs.id
                        val savedTarget = tileTargets[actionKey]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (qs.isVisible) Color(0xFF00BCD4).copy(alpha = 0.22f) else Color(0xFF10252C))
                                .clickable {
                                    if (pendingGesture != null) {
                                        viewModel.bindGesture(pendingGesture!!, qs.name)
                                        viewModel.clearPendingGesture()
                                        viewModel.showToast("${pendingGesture} → ${qs.name}")
                                        viewModel.navigateTo(ActiveWindow.PANEL_SETTINGS)
                                    } else {
                                        viewModel.toggleTileVisible(qs.id)
                                    }
                                }
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF00BCD4).copy(alpha = 0.25f))
                                        .clickable { viewModel.startTrackTile(actionKey) }
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (savedTarget != null) "◎" else "Track", color = Color(0xFF00E5FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    imageVector = getQuickSettingIcon(qs.iconName),
                                    contentDescription = actName,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (savedTarget != null && savedTarget.label.isNotBlank()) "$actName (${savedTarget.label})" else actName,
                                    color = Color(0xFFE0F7FA),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (savedTarget != null) {
                                    Text(
                                        text = "Reset",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { viewModel.clearTileTarget(actionKey) }.padding(horizontal = 4.dp)
                                    )
                                }
                                Text(if (qs.isVisible) "✓" else "+", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (false) {
                        val actionItems = masterItems.filter { it.type == ItemType.ACTION }
                        items(actionItems) { item ->
                            val actName = item.name
                        val actionKey = actName.lowercase().replace(" ", "_").replace("-", "")
                        val savedTarget = tileTargets[actionKey]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10252C))
                                .clickable { viewModel.bindAndFinish(item) }
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF00BCD4).copy(alpha = 0.25f))
                                        .clickable { viewModel.startTrackTile(actionKey) }
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (savedTarget != null) "◎" else "Track", color = Color(0xFF00E5FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    imageVector = getIconVectorForName(item.iconName),
                                    contentDescription = actName,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (savedTarget != null && savedTarget.label.isNotBlank()) "${actName} (${savedTarget.label})" else actName,
                                        color = Color(0xFFE0F7FA),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                            }
                            if (savedTarget != null) {
                                Text(
                                    text = "Reset",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { viewModel.clearTileTarget(actionKey) }.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF122B33))
                                .clickable { showNewToggle = !showNewToggle }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("+ Create Toggle", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (showNewToggle) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newToggleName,
                            onValueChange = { newToggleName = it },
                            placeholder = { Text("Toggle name", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        TextButton(onClick = {
                            if (newToggleName.isNotBlank()) {
                                viewModel.showToast("Created: $newToggleName")
                                newToggleName = ""
                                showNewToggle = false
                            }
                        }) { Text("Add", fontSize = 10.sp) }
                    }
                }
            } else {
                // GROUPS TAB: Left Auto/Manual switch, Right vertical groups list + App Name hide switch
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Left side: Auto / Manual detection switch
                    Column(
                        modifier = Modifier
                            .width(92.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF0E1D23), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("App Detection", color = Color(0xFF80DEEA), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        val g = selectedGroup ?: "All"
                        val isManual = g in manualGroups
                        Switch(
                            checked = !isManual,
                            onCheckedChange = { auto -> viewModel.setGroupManual(g, !auto) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00BCD4))
                        )
                        Text(if (isManual) "Manual" else "Auto", color = Color(0xFFE0F7FA), fontSize = 10.sp)
                    }

                    // Right side: App Name Hide/Unhide switch + vertical groups list (when showGroupList) or selected group apps
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        V5SlimSwitchRow("Hide App Names", !panelSettings.showApkNameInGroupList) { hide ->
                            viewModel.updateSettings { it.copy(showApkNameInGroupList = !hide) }
                        }
                        if (showGroupList || selectedGroup == null) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(allGroups) { grp ->
                                    val isSel = selectedGroup == grp
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF00BCD4) else Color(0xFF10252C))
                                            .clickable {
                                                // Single tap selects & closes group list; re-tap deselects
                                                selectedGroup = if (isSel) null else grp
                                                if (!isSel) {
                                                    if (grp !in categories) viewModel.createGroupFromWindow(grp)
                                                    viewModel.selectCategory(grp)
                                                    showGroupList = false
                                                }
                                            }
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = grp,
                                                tint = if (isSel) Color.White else Color(0xFF00E5FF),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(grp, color = Color.White, fontSize = 10.sp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (isSel) Text("✓", color = Color.White, fontSize = 10.sp)
                                            if (grp !in SPECIAL_GROUPS) {
                                                Text(
                                                    text = "🗑",
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.clickable {
                                                        viewModel.deleteGroup(grp)
                                                        if (selectedGroup == grp) selectedGroup = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF122B33))
                                            .clickable { newGroupMode = !newGroupMode }
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("+ Create Group", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            // Selected group apps with ABCD slider
                            Row(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(appItems) { item ->
                                        val inGrp = selectedGroup?.let { isInGroup(item, it, groupAdded, groupRemoved, manualGroups) } == true
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (inGrp) Color(0xFF00BCD4).copy(alpha = 0.2f) else Color(0xFF10252C))
                                                .clickable { selectedGroup?.let { g -> viewModel.toggleItemInGroup(item.id, g) } }
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(item.name, color = Color(0xFFE0F7FA), fontSize = 10.sp, maxLines = 1)
                                            Text(if (inGrp) "✓" else "+", color = Color(0xFF00E5FF), fontSize = 10.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                V5AbcdRail(
                                    onLetter = { ch ->
                                        activeLetter = ch
                                        val idx = appItems.indexOfFirst { it.name.startsWith(ch, ignoreCase = true) }
                                        if (idx >= 0) scope.launch { listState.scrollToItem(idx) }
                                    },
                                    onRelease = { activeLetter = null }
                                )
                            }
                        }
                    }
                }
                if (newGroupMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            placeholder = { Text("New group name", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        TextButton(onClick = {
                            if (newGroupName.isNotBlank()) {
                                viewModel.createGroupFromWindow(newGroupName.trim())
                                selectedGroup = newGroupName.trim()
                                newGroupName = ""
                                newGroupMode = false
                                showGroupList = false
                            }
                        }) { Text("Create", fontSize = 10.sp) }
                    }
                }
            }
        }
        activeLetter?.let { ch ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .background(Color(0xFF00BCD4), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(ch.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        }
    }
}

@Composable
private fun V5SlimSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clickable { onChange(!checked) }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF80DEEA), fontSize = 9.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.height(18.dp),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00BCD4))
        )
    }
}

@Composable
private fun V5AbcdRail(onLetter: (Char) -> Unit, onRelease: () -> Unit) {
    val letters = remember { ('A'..'Z').toList() }
    Column(
        modifier = Modifier
            .width(14.dp)
            .fillMaxHeight()
            .background(Color(0xFF12232A), RoundedCornerShape(7.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = onRelease,
                    onDragCancel = onRelease
                ) { change, _ ->
                    val idx = ((change.position.y / size.height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
                    onLetter(letters[idx])
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        letters.filterIndexed { i, _ -> i % 2 == 0 }.forEach { ch ->
            Text(ch.toString(), color = Color(0xFF80DEEA), fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
    }
}
