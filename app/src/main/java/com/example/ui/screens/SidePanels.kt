package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.model.ItemType
import com.example.viewmodel.OneHandViewModel
import com.example.system.AppIcons
import com.example.data.FloatingStore
import android.graphics.Rect
import kotlin.math.roundToInt
import java.util.Base64

private val pBg = Color(0xFF0A1418)
private val pCard = Color(0xFF10252C)
private val pAccent = Color(0xFF00E5FF)
private val pText = Color(0xFFE0F7FA)

@Composable
fun QuickAppScreen(viewModel: OneHandViewModel, widthDp: Float, heightDp: Float) {
    val context = LocalContext.current
    val positionStore = remember { FloatingStore(context) }
    val savedPosition = remember { positionStore.get("side_quick_app") }
    var offsetX by remember { mutableIntStateOf(savedPosition?.left ?: 0) }
    var offsetY by remember { mutableIntStateOf(savedPosition?.top ?: 0) }
    val apps by viewModel.masterItems.collectAsState()
    val used = remember(apps) { apps.filter { it.type == ItemType.APP && it.packageName.isNotBlank() }.sortedWith(compareByDescending<AppItem> { it.usageCount }.thenByDescending { it.lastUsed }).distinctBy { it.packageName }.take(6) }
    val recent = remember(apps) { apps.filter { it.type == ItemType.APP && it.packageName.isNotBlank() && it.lastUsed > 0L }.sortedByDescending { it.lastUsed }.distinctBy { it.packageName }.take(6) }
    Column(Modifier.offset { IntOffset(offsetX, offsetY) }.width(widthDp.dp).height(heightDp.dp).clip(RoundedCornerShape(14.dp)).background(pBg).border(1.dp, Color(0xFF1E4E59), RoundedCornerShape(14.dp)).padding(5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth().pointerInput(Unit) { detectDragGestures { change, amount ->
            change.consume(); offsetX += amount.x.roundToInt(); offsetY += amount.y.roundToInt()
            positionStore.put("side_quick_app", Rect(offsetX, offsetY, widthDp.toInt(), heightDp.toInt()))
        } }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("QUICK APP", color = pAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("×", color = pText, modifier = Modifier.clickable { positionStore.put("side_quick_app", Rect(offsetX, offsetY, widthDp.toInt(), heightDp.toInt())); viewModel.dismissFloatingWindow() }.padding(2.dp))
        }
        Row(Modifier.fillMaxWidth()) {
            Text("MOST USED", color = pAccent, fontSize = 8.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("RECENT", color = pAccent, fontSize = 8.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        repeat(6) { i -> Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            QuickAppCell(used.getOrNull(i), Modifier.weight(1f)) { viewModel.launchApp(it) }
            QuickAppCell(recent.getOrNull(i), Modifier.weight(1f)) { viewModel.launchApp(it) }
        } }
    }
}

@Composable
private fun QuickAppCell(item: AppItem?, modifier: Modifier, onClick: (AppItem) -> Unit) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(6.dp)).clickable(enabled = item != null) { item?.let(onClick) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val icon = item?.let { AppIcons.get(it.packageName) }
        if (icon != null) Image(bitmap = icon, contentDescription = item.name, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(7.dp)))
        else Box(Modifier.size(32.dp).background(pCard, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) { Text(item?.name?.take(1)?.uppercase() ?: "·", color = pAccent, fontSize = 14.sp) }
        if (item != null) Text(item.name, color = pText, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun SidePanelScreen(viewModel: OneHandViewModel, widthDp: Float, heightDp: Float) {
    val context = LocalContext.current
    val positionStore = remember { FloatingStore(context) }
    val savedPosition = remember { positionStore.get("side_side_panel") }
    var offsetX by remember { mutableIntStateOf(savedPosition?.left ?: 0) }
    var offsetY by remember { mutableIntStateOf(savedPosition?.top ?: 0) }
    var expanded by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("Action") }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.offset { IntOffset(offsetX, offsetY) }.then(if (expanded) Modifier.fillMaxSize() else Modifier.width(widthDp.dp).height(heightDp.dp))
            .clip(RoundedCornerShape(if (expanded) 0.dp else 14.dp)).background(pBg)
            .border(if (expanded) 0.dp else 1.dp, Color(0xFF1E4E59), RoundedCornerShape(if (expanded) 0.dp else 14.dp)).padding(8.dp)) {
            Row(Modifier.fillMaxWidth().pointerInput(expanded) { detectDragGestures { change, amount ->
                if (!expanded) { change.consume(); offsetX += amount.x.roundToInt(); offsetY += amount.y.roundToInt()
                    positionStore.put("side_side_panel", Rect(offsetX, offsetY, widthDp.toInt(), heightDp.toInt())) }
            } }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SIDE PANEL", color = pAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (expanded) "↙" else "⤢", color = pAccent, modifier = Modifier.clickable { expanded = !expanded }.padding(6.dp))
                    Text("×", color = pText, modifier = Modifier.clickable { positionStore.put("side_side_panel", Rect(offsetX, offsetY, widthDp.toInt(), heightDp.toInt())); viewModel.dismissFloatingWindow() }.padding(6.dp))
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
    val recent = remember(apps) { apps.filter { it.type == ItemType.APP && it.lastUsed > 0L && it.packageName.isNotBlank() }.sortedByDescending { it.lastUsed }.distinctBy { it.packageName }.take(30) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Spacer(Modifier.weight(1f)) // Intentionally empty upper area.
        LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.fillMaxWidth().height(74.dp), userScrollEnabled = false, verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(10) {
                Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(8.dp)).background(pCard).border(1.dp, Color(0xFF28555E), RoundedCornerShape(8.dp)).clickable { viewModel.openAddWindow(forTiles = false) }, contentAlignment = Alignment.Center) {
                    Text("+", color = pAccent, fontSize = 20.sp)
                }
            }
        }
        Text("RECENT APPS", color = pAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().height(58.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            recent.forEach { item ->
                Column(Modifier.width(48.dp).fillMaxHeight().clickable { viewModel.launchApp(item) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val icon = AppIcons.get(item.packageName)
                    if (icon != null) Image(bitmap = icon, contentDescription = item.name, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(7.dp)))
                    else Box(Modifier.size(32.dp).background(pCard, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) { Text(item.name.take(1).uppercase(), color = pAccent) }
                    Text(item.name, color = pText, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
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
    val clips = remember { mutableStateListOf<String>().apply { addAll(loadSideClips(context).distinct()) } }
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
            items(pinned.toList().distinct(), key = { "pin_${it.hashCode()}" }) { value ->
                ClipboardCard(value, true, { copyText(value) }, { selected = value })
            }
            items(clips.filter { it !in pinned && it.contains(query, true) }.distinct(), key = { "clip_${it.hashCode()}" }) { value ->
                ClipboardCard(value, false, { copyText(value) }, { selected = value })
            }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search clipboard") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
    Box(Modifier.fillMaxSize()) {
    selected?.let { value ->
        var draft by remember(value) { mutableStateOf(value) }
        val url = Regex("https?://\\S+", RegexOption.IGNORE_CASE).find(draft)?.value
        val cleanup = draft.contains(Regex("\\s{2,}|\\n"))
        val oneLine = draft.contains('\n') || draft.contains('\r')
        val canUpper = draft.any { it.isLowerCase() }
        val canLower = draft.any { it.isUpperCase() }
        Box(Modifier.fillMaxSize().background(pBg), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF202124)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Clipboard text", color = pText, fontWeight = FontWeight.Bold); Text("×", color = pText, modifier = Modifier.clickable { selected = null }) }
                Text(draft, color = pText, fontSize = 13.sp, maxLines = 5)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save as Note", color = pAccent, modifier = Modifier.clickable { saveAsNote(draft); selected = null }.padding(5.dp))
                    Text("Search", color = pAccent, modifier = Modifier.clickable { query = draft.take(30); selected = null }.padding(5.dp))
                    if (cleanup) Text("Cleanup", color = pAccent, modifier = Modifier.clickable { draft = draft.replace(Regex("\\s+"), " ").trim() }.padding(5.dp))
                    if (oneLine) Text("One Line", color = pAccent, modifier = Modifier.clickable { draft = draft.replace(Regex("[\\r\\n]+"), " ").replace(Regex("\\s+"), " ").trim() }.padding(5.dp))
                    if (canUpper) Text("Uppercase", color = pAccent, modifier = Modifier.clickable { draft = draft.uppercase() }.padding(5.dp))
                    if (canLower) Text("Lowercase", color = pAccent, modifier = Modifier.clickable { draft = draft.lowercase() }.padding(5.dp))
                    if (canUpper) Text("Title Case", color = pAccent, modifier = Modifier.clickable { draft = draft.lowercase().split(Regex("\\s+")).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } }.padding(5.dp))
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (url != null) Text("Open Link", color = pAccent, modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }.padding(4.dp))
                    Text("Copy", color = pAccent, modifier = Modifier.clickable { copyText(draft) }.padding(4.dp))
                    Text("Share", color = pAccent, modifier = Modifier.clickable { runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, draft), "Share")) } }.padding(4.dp))
                    Text(if (value in pinned) "Unpin" else "Pin", color = pAccent, modifier = Modifier.clickable { if (value in pinned) pinned.remove(value) else pinned.add(value); context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putStringSet("pinned_clips", pinned.toSet()).apply() }.padding(4.dp))
                    Text("Delete", color = Color(0xFFFF6B6B), modifier = Modifier.clickable { clips.remove(value); pinned.remove(value); saveSideClips(context, clips); context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putStringSet("pinned_clips", pinned.toSet()).apply(); selected = null }.padding(4.dp))
                    Text("Close", color = pText, modifier = Modifier.clickable { selected = null }.padding(4.dp))
                }
            }
        }
        }
    }
}
@Composable
private fun ClipboardCard(value: String, isPinned: Boolean, onTap: () -> Unit, onLongPress: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Color(0xFF263238))
        .pointerInput(value) { detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() }) }
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (isPinned) "📌  " else "", color = pAccent)
        Text(value, color = pText, fontSize = 12.sp, maxLines = 3, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NotesTab(context: Context) {
    val clipManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val notes = remember { mutableStateListOf<String>().apply { addAll(loadSideNotes(context).distinct()) } }
    val pinned = remember { mutableStateListOf<String>().apply { addAll(context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).getStringSet("pinned_notes", emptySet())?.toList() ?: emptyList()) } }
    var selected by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${notes.size} saved", color = pText.copy(alpha = .75f), fontSize = 10.sp)
            Text("+", color = pAccent, fontSize = 22.sp, modifier = Modifier.clickable { selected = ""; creating = true }.padding(horizontal = 10.dp))
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(pinned.toList().distinct(), key = { "note_pin_${it.hashCode()}" }) { note ->
                NoteCard(note, true, onTap = { clipManager.setPrimaryClip(ClipData.newPlainText("note", note)) }, onLongPress = { selected = note; creating = false }, onSwipeLeft = { if (note in pinned) pinned.remove(note) else pinned.add(note); context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putStringSet("pinned_notes", pinned.toSet()).apply() }, onSwipeRight = { notes.remove(note); pinned.remove(note); saveSideNotes(context, notes); context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putStringSet("pinned_notes", pinned.toSet()).apply() })
            }
            items(notes.filter { it !in pinned }.distinct(), key = { "note_${it.hashCode()}" }) { note ->
                NoteCard(note, false, onTap = { clipManager.setPrimaryClip(ClipData.newPlainText("note", note)) }, onLongPress = { selected = note; creating = false }, onSwipeLeft = { if (note in pinned) pinned.remove(note) else pinned.add(note); context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putStringSet("pinned_notes", pinned.toSet()).apply() }, onSwipeRight = { notes.remove(note); pinned.remove(note); saveSideNotes(context, notes); context.getSharedPreferences("side_panel_data", Context.MODE_PRIVATE).edit().putStringSet("pinned_notes", pinned.toSet()).apply() })
            }
        }
    }
    selected?.let { original ->
        var draft by remember(original) { mutableStateOf(original) }
        Box(Modifier.fillMaxSize().background(Color(0xEE080D10)).clickable { selected = null }, contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxSize().padding(8.dp).background(Color(0xFF111111), RoundedCornerShape(12.dp)).clickable(enabled = false) {}.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
}
@Composable
private fun NoteCard(note: String, pinned: Boolean, onTap: () -> Unit, onLongPress: () -> Unit, onSwipeLeft: () -> Unit, onSwipeRight: () -> Unit) {
    var dragX by remember(note) { mutableFloatStateOf(0f) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(if (pinned) Color(0xFF263238) else Color(0xFF1B1B1B))
        .pointerInput(note) { detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() }) }
        .pointerInput(note) { detectHorizontalDragGestures(onHorizontalDrag = { change, amount -> dragX += amount }, onDragEnd = { if (dragX < -55f) onSwipeLeft() else if (dragX > 55f) onSwipeRight(); dragX = 0f }) }
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
