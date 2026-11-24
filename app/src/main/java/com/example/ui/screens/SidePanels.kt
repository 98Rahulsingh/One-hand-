package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.model.ItemType
import com.example.viewmodel.OneHandViewModel
import java.util.Base64

private val pBg = Color(0xFF0A1418)
private val pCard = Color(0xFF10252C)
private val pAccent = Color(0xFF00E5FF)
private val pText = Color(0xFFE0F7FA)

@Composable
fun QuickAppScreen(viewModel: OneHandViewModel, widthDp: Float, heightDp: Float) {
    val apps by viewModel.masterItems.collectAsState()
    val used = remember(apps) { apps.filter { it.type == ItemType.APP }.sortedWith(compareByDescending<AppItem> { it.usageCount }.thenByDescending { it.lastUsed }).take(6) }
    val recent = remember(apps) { apps.filter { it.type == ItemType.APP && it.lastUsed > 0L }.sortedByDescending { it.lastUsed }.take(6) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.width(widthDp.dp).height(heightDp.dp).clip(RoundedCornerShape(14.dp)).background(pBg)
            .border(1.dp, Color(0xFF1E4E59), RoundedCornerShape(14.dp)).padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("QUICK APP", color = pAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("×", color = pText, modifier = Modifier.clickable { viewModel.dismissFloatingWindow() }.padding(4.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("MOST USED", color = pAccent, fontSize = 9.sp, modifier = Modifier.weight(1f))
                Text("RECENT", color = pAccent, fontSize = 9.sp, modifier = Modifier.weight(1f))
            }
            repeat(6) { i ->
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickAppCell(used.getOrNull(i), Modifier.weight(1f)) { viewModel.launchApp(it) }
                    QuickAppCell(recent.getOrNull(i), Modifier.weight(1f)) { viewModel.launchApp(it) }
                }
            }
        }
    }
}
@Composable
private fun QuickAppCell(item: AppItem?, modifier: Modifier, onClick: (AppItem) -> Unit) {
    Box(modifier.fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(pCard)
        .clickable(enabled = item != null) { item?.let(onClick) }.padding(4.dp), contentAlignment = Alignment.Center) {
        Text(item?.name ?: "—", color = if (item == null) Color(0xFF607D86) else pText, fontSize = 10.sp, maxLines = 2)
    }
}

@Composable
fun SidePanelScreen(viewModel: OneHandViewModel, widthDp: Float, heightDp: Float) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("Action") }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.then(if (expanded) Modifier.fillMaxSize() else Modifier.width(widthDp.dp).height(heightDp.dp))
            .clip(RoundedCornerShape(if (expanded) 0.dp else 14.dp)).background(pBg)
            .border(if (expanded) 0.dp else 1.dp, Color(0xFF1E4E59), RoundedCornerShape(if (expanded) 0.dp else 14.dp)).padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SIDE PANEL", color = pAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (expanded) "↙" else "⤢", color = pAccent, modifier = Modifier.clickable { expanded = !expanded }.padding(6.dp))
                    Text("×", color = pText, modifier = Modifier.clickable { viewModel.dismissFloatingWindow() }.padding(6.dp))
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    "Action" -> SideActionTab(viewModel)
                    "Clipboard" -> ClipboardTab(context)
                    "Notes" -> NotesTab(context)
                    "Files" -> FilesTab(context)
                }
            }
            Row(Modifier.fillMaxWidth().background(pCard, RoundedCornerShape(8.dp)), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Action", "Clipboard", "Notes", "Files").forEach { name ->
                    Text(name, color = if (tab == name) pAccent else pText, fontSize = 10.sp,
                        modifier = Modifier.weight(1f).clickable { tab = name }.padding(vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}
@Composable
private fun SideActionTab(viewModel: OneHandViewModel) {
    val apps by viewModel.masterItems.collectAsState()
    val actions = remember(apps) { apps.filter { it.type == ItemType.ACTION && it.isAdded } }
    val recent = remember(apps) { apps.filter { it.type == ItemType.APP && it.lastUsed > 0L }.sortedByDescending { it.lastUsed }.take(5) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("ACTIONS", color = pAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items(actions, key = { it.id }) { item ->
                Column(Modifier.clip(RoundedCornerShape(8.dp)).background(pCard).clickable { viewModel.launchApp(item) }
                    .padding(vertical = 9.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text(item.iconName.take(2).uppercase(), color = pAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(item.name, color = pText, fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Text("RECENT APPS", color = pAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().height(62.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            recent.forEach { item ->
                Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(pCard)
                    .clickable { viewModel.launchApp(item) }.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text(item.name.take(2).uppercase(), color = pAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(item.name, color = pText, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text("+ Add item", color = pAccent, fontSize = 10.sp, modifier = Modifier.align(Alignment.End)
            .clickable { viewModel.openAddWindow(forTiles = false) }.padding(5.dp))
    }
}

private fun saveSideNotes(context: Context, notes: List<String>) {
    val raw = notes.joinToString("|") { Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)) }
    context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putString("notes", raw).apply()
}
private fun loadSideNotes(context: Context): List<String> = context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE)
    .getString("notes", "").orEmpty().split("|").filter { it.isNotBlank() }
    .mapNotNull { runCatching { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }.getOrNull() }

private fun loadSideClips(context: Context): List<String> = context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE)
    .getString("clips", "").orEmpty().split("|").filter { it.isNotBlank() }
    .mapNotNull { runCatching { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }.getOrNull() }
private fun saveSideClips(context: Context, clips: List<String>) {
    val raw = clips.distinct().take(100).joinToString("|") { Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)) }
    context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putString("clips", raw).apply()
}
private fun loadPinnedClips(context: Context): Set<String> = context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE)
    .getStringSet("pinned_clips", emptySet())?.toSet() ?: emptySet()

@Composable
private fun ClipboardTab(context: Context) {
    val clipManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val clips = remember { mutableStateListOf<String>().apply { addAll(loadSideClips(context)) } }
    val pinned = remember { mutableStateListOf<String>().apply { addAll(loadPinnedClips(context)) } }
    var selected by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val current = clipManager.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        if (current.isNotBlank() && current !in clips) { clips.add(0, current); saveSideClips(context, clips) }
    }
    fun copyText(value: String) { clipManager.setPrimaryClip(ClipData.newPlainText("clipboard", value)); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }
    fun saveAsNote(value: String) { val notes = loadSideNotes(context).toMutableList(); if (value !in notes) notes.add(0, value); saveSideNotes(context, notes); Toast.makeText(context, "Saved as note", Toast.LENGTH_SHORT).show() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${clips.size} saved · ${pinned.size} pinned", color = pText.copy(alpha = .7f), fontSize = 10.sp)
            Text("Clear", color = pAccent, fontSize = 10.sp, modifier = Modifier.clickable { clips.clear(); saveSideClips(context, clips) }.padding(4.dp))
        }
        if (pinned.isNotEmpty()) Text("PINNED", color = pAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(pinned.toList(), key = { "pin_$it" }) { value ->
                ClipboardCard(value, true, { selected = value }, { selected = value })
            }
            items(clips.filter { it !in pinned && it.contains(query, true) }, key = { it }) { value ->
                ClipboardCard(value, false, { selected = value }, { selected = value })
            }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search clipboard") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
    selected?.let { value ->
        var draft by remember(value) { mutableStateOf(value) }
        val url = Regex("https?://\\S+", RegexOption.IGNORE_CASE).find(value)?.value
        val hasCleanup = value.contains(Regex("\\s{2,}|\\n"))
        val hasUpper = value.any { it.isUpperCase() }
        val hasLower = value.any { it.isLowerCase() }
        AlertDialog(onDismissRequest = { selected = null }, containerColor = Color(0xFF202124),
            title = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Clipboard text", color = pText); Text("×", color = pText, modifier = Modifier.clickable { selected = null }) } },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(draft, color = pText, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp)); Text("TOOLS", color = pAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Save as note", color = pAccent, modifier = Modifier.clickable { saveAsNote(draft) }.padding(6.dp))
                        Text("Search", color = pAccent, modifier = Modifier.clickable { query = draft.take(30); selected = null }.padding(6.dp))
                        if (url != null) Text("Open link", color = pAccent, modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }.padding(6.dp))
                        if (hasCleanup) Text("Clean up", color = pAccent, modifier = Modifier.clickable { draft = draft.replace(Regex("\\s+"), " ").trim() }.padding(6.dp))
                        if (hasCleanup) Text("One line", color = pAccent, modifier = Modifier.clickable { draft = draft.replace(Regex("[\\r\\n]+"), " ").replace(Regex("\\s+"), " ").trim() }.padding(6.dp))
                        if (hasLower) Text("UPPERCASE", color = pAccent, modifier = Modifier.clickable { draft = draft.uppercase() }.padding(6.dp))
                        if (hasUpper) Text("lowercase", color = pAccent, modifier = Modifier.clickable { draft = draft.lowercase() }.padding(6.dp))
                        if (hasLower) Text("Title case", color = pAccent, modifier = Modifier.clickable { draft = draft.lowercase().split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } } }.padding(6.dp))
                    }
                }
            },
            confirmButton = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = { copyText(draft) }) { Text("Copy") }
                TextButton(onClick = { runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, draft), "Share")) } }) { Text("Share") }
                TextButton(onClick = { if (value in pinned) pinned.remove(value) else pinned.add(value); context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putStringSet("pinned_clips", pinned.toSet()).apply() }) { Text(if (value in pinned) "Unpin" else "Pin") }
                TextButton(onClick = { clips.remove(value); pinned.remove(value); saveSideClips(context, clips); context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putStringSet("pinned_clips", pinned.toSet()).apply(); selected = null }) { Text("Delete") }
                TextButton(onClick = { selected = null }) { Text("Done") }
            } }
        )
    }
}
@Composable
private fun ClipboardCard(value: String, isPinned: Boolean, onTap: () -> Unit, onLongPress: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Color(0xFF263238))
        .pointerInput(value) { androidx.compose.foundation.gestures.detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() }) }
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (isPinned) "📌  " else "", color = pAccent)
        Text(value, color = pText, fontSize = 12.sp, maxLines = 3, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NotesTab(context: Context) {
    val clipManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val notes = remember { mutableStateListOf<String>().apply { addAll(loadSideNotes(context)) } }
    val pinned = remember { mutableStateListOf<String>().apply { addAll(context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).getStringSet("pinned_notes", emptySet())?.toList() ?: emptyList()) } }
    var selected by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${notes.size} saved", color = pText.copy(alpha = .75f), fontSize = 10.sp)
            Text("+", color = pAccent, fontSize = 22.sp, modifier = Modifier.clickable { selected = ""; creating = true }.padding(horizontal = 10.dp))
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(pinned.toList(), key = { "p_$it" }) { note ->
                NoteCard(note, true) { selected = note; creating = false }
            }
            items(notes.filter { it !in pinned }, key = { it }) { note ->
                NoteCard(note, false) { selected = note; creating = false }
            }
        }
    }
    selected?.let { original ->
        var draft by remember(original) { mutableStateOf(original) }
        Dialog(onDismissRequest = { selected = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(Color(0xFF111111)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (creating) "New note" else "Edit note", color = pText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("×", color = pText, fontSize = 24.sp, modifier = Modifier.clickable { selected = null }.padding(6.dp))
                }
                OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f).fillMaxWidth(),
                    placeholder = { Text("Write a note…") }, minLines = 8)
                Row(Modifier.fillMaxWidth().background(Color(0xFF202124), RoundedCornerShape(12.dp)).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Text("Copy", color = pAccent, modifier = Modifier.clickable { clipManager.setPrimaryClip(ClipData.newPlainText("note", draft)) }.padding(5.dp))
                    Text("Share", color = pAccent, modifier = Modifier.clickable { if (draft.isNotBlank()) runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, draft), "Share")) } }.padding(5.dp))
                    Text(if (original in pinned) "Unpin" else "Pin", color = pAccent, modifier = Modifier.clickable {
                        if (original in pinned) pinned.remove(original) else if (original.isNotBlank()) pinned.add(original)
                        context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putStringSet("pinned_notes", pinned.toSet()).apply()
                    }.padding(5.dp))
                    Text("Delete", color = Color(0xFFFF6B6B), modifier = Modifier.clickable {
                        if (!creating) notes.remove(original)
                        saveSideNotes(context, notes); selected = null
                    }.padding(5.dp))
                    Text("Done", color = pAccent, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        if (draft.isNotBlank()) {
                            if (creating) notes.add(0, draft) else { val i = notes.indexOf(original); if (i >= 0) notes[i] = draft else notes.add(0, draft) }
                            if (original in pinned) { pinned.remove(original); pinned.add(draft); context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putStringSet("pinned_notes", pinned.toSet()).apply() }
                        }
                        saveSideNotes(context, notes); selected = null
                    }.padding(5.dp))
                }
            }
        }
    }
}
@Composable
private fun NoteCard(note: String, pinned: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(if (pinned) Color(0xFF263238) else Color(0xFF1B1B1B))
        .clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (pinned) "📌  " else "", color = pAccent)
        Text(note, color = pText, fontSize = 12.sp, maxLines = 4, modifier = Modifier.weight(1f))
    }
}
@Composable
private fun FilesTab(context: Context) {
    var label by remember { mutableStateOf("FV File Explorer") }
    Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Text(label,color=pText,fontSize=12.sp)
        Button(onClick={
            val pm=context.packageManager
            val app=runCatching{pm.getInstalledApplications(PackageManager.GET_META_DATA).firstOrNull{pm.getApplicationLabel(it).toString().equals("FV File Explorer",true)}}.getOrNull()
            val intent=app?.let{pm.getLaunchIntentForPackage(it.packageName)}
            if(intent!=null)context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) else label="FV File Explorer not installed"
        }){Text("Open FV File Explorer")}
    }
}
