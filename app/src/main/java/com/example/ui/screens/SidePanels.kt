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
    val recent = remember(apps) { apps.filter { it.type == ItemType.APP && it.lastUsed > 0L }.sortedByDescending { it.lastUsed }.take(4) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(2) { row ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { col ->
                    if (row == 0) {
                        val item = recent.getOrNull(col)
                        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(pCard)
                            .clickable(enabled = item != null) { item?.let { viewModel.launchApp(it) } }.padding(3.dp), contentAlignment = Alignment.Center) {
                            Text(item?.name ?: "Recent", color = pText, fontSize = 9.sp, maxLines = 2)
                        }
                    } else Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(pCard)
                        .clickable { viewModel.openAddWindow(forTiles = false) }, contentAlignment = Alignment.Center) {
                        Text("+", color = pAccent, fontSize = 24.sp)
                    }
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

@Composable
private fun ClipboardTab(context: Context) {
    val clipManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val clips = remember { mutableStateListOf<String>() }
    var selected by remember { mutableIntStateOf(-1) }
    var pinned by remember { mutableStateOf(setOf<Int>()) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val current = clipManager.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        if (current.isNotBlank()) clips.add(current)
    }
    fun delete(i: Int) { if (i in clips.indices) { clips.removeAt(i); selected = -1 } }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(clips.filter { it.contains(search, true) }) { i, clip ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(pCard)
                    .pointerInput(i) { var delta=0f; detectHorizontalDragGestures(onDragEnd={ if(delta>40) pinned=pinned+i else if(delta< -40) delete(i); delta=0f }) { _,d -> delta+=d } }
                    .clickable { selected=i; clipManager.setPrimaryClip(ClipData.newPlainText("clipboard",clip)); Toast.makeText(context,"Copied to clipboard",Toast.LENGTH_SHORT).show() }
                    .padding(7.dp), verticalAlignment=Alignment.CenterVertically) {
                    Text(if(i in pinned) "📌 " else "", color=pAccent, fontSize=9.sp)
                    Text(clip, color=pText, fontSize=10.sp, maxLines=2, modifier=Modifier.weight(1f))
                    Text("⋯", color=pAccent, modifier=Modifier.clickable { expanded=clip })
                }
            }
        }
        Text("Tools", color=pAccent, fontSize=10.sp, fontWeight=FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            listOf("Search","Clean Up One Line","Uppercase","Lowercase","Title Case").forEach { tool ->
                Text(tool,color=pText,fontSize=9.sp,modifier=Modifier.clip(RoundedCornerShape(6.dp)).background(pCard).clickable {
                    val value=clips.getOrNull(selected) ?: ""
                    when(tool) {
                        "Search" -> search=if(search.isEmpty()) value.take(12) else ""
                        "Clean Up One Line" -> if(selected in clips.indices) clips[selected]=value.replace(Regex("\\s+")," ").trim()
                        "Uppercase" -> if(selected in clips.indices) clips[selected]=value.uppercase()
                        "Lowercase" -> if(selected in clips.indices) clips[selected]=value.lowercase()
                        "Title Case" -> if(selected in clips.indices) clips[selected]=value.split(" ").joinToString(" "){it.replaceFirstChar{c->c.uppercase()}}
                    }
                }.padding(7.dp))
            }
        }
        OutlinedTextField(value=search,onValueChange={search=it},label={Text("Search clipboard")},modifier=Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf("Copy","Share","Pin","Delete Note").forEach { action ->
                Text(action,color=pAccent,fontSize=9.sp,modifier=Modifier.clickable {
                    val value=clips.getOrNull(selected).orEmpty()
                    when(action) {
                        "Copy" -> if(value.isNotBlank()) clipManager.setPrimaryClip(ClipData.newPlainText("clipboard",value))
                        "Share" -> if(value.isNotBlank()) context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,value),"Share"))
                        "Pin" -> if(selected>=0) pinned=pinned+selected
                        "Delete Note" -> delete(selected)
                    }
                }.padding(vertical=8.dp))
            }
        }
    }
    expanded?.let { text -> AlertDialog(onDismissRequest={expanded=null},title={Text("Clipboard text")},text={Text(text)},
        confirmButton={TextButton(onClick={expanded=null}){Text("Close")}}) }
}
@Composable
private fun NotesTab(context: Context) {
    val clipManager=remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val notes=remember { mutableStateListOf<String>().apply { addAll(loadSideNotes(context)) } }
    var selected by remember { mutableIntStateOf(-1) }
    var pinned by remember { mutableStateOf(setOf<Int>()) }
    var edit by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            itemsIndexed(notes) { i,note ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(pCard)
                    .pointerInput(i) { var delta=0f; detectHorizontalDragGestures(onDragEnd={if(delta>40)pinned=pinned+i else if(delta< -40){if(i in notes.indices)notes.removeAt(i);saveSideNotes(context,notes);selected=-1};delta=0f}){_,d->delta+=d} }
                    .clickable { selected=i;clipManager.setPrimaryClip(ClipData.newPlainText("note",note));Toast.makeText(context,"Note copied to clipboard",Toast.LENGTH_SHORT).show() }
                    .padding(7.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(i in pinned)"📌 " else "",color=pAccent,fontSize=9.sp)
                    Text(note,color=pText,fontSize=10.sp,maxLines=2,modifier=Modifier.weight(1f))
                    Text("⋯",color=pAccent,modifier=Modifier.clickable{edit=note})
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf("Edit","Copy","Share","Pin","Delete","+").forEach { action ->
                Text(action,color=pAccent,fontSize=9.sp,modifier=Modifier.clickable {
                    val value=notes.getOrNull(selected).orEmpty()
                    when(action) {
                        "Edit" -> edit=value
                        "Copy" -> if(value.isNotBlank())clipManager.setPrimaryClip(ClipData.newPlainText("note",value))
                        "Share" -> if(value.isNotBlank())context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,value),"Share"))
                        "Pin" -> if(selected>=0)pinned=pinned+selected
                        "Delete" -> if(selected in notes.indices){notes.removeAt(selected);saveSideNotes(context,notes);selected=-1}
                        "+" -> edit=""
                    }
                }.padding(vertical=8.dp))
            }
        }
    }
    edit?.let { initial ->
        var draft by remember(initial){mutableStateOf(initial)}
        AlertDialog(onDismissRequest={edit=null},title={Text(if(initial.isBlank())"New note" else "Edit note")},
            text={OutlinedTextField(value=draft,onValueChange={draft=it},modifier=Modifier.fillMaxWidth(),minLines=3)},
            confirmButton={TextButton(onClick={
                if(initial.isBlank())notes.add(draft) else {val i=notes.indexOf(initial);if(i>=0)notes[i]=draft}
                saveSideNotes(context,notes);edit=null
            }){Text("Save")}},
            dismissButton={TextButton(onClick={edit=null}){Text("Cancel")}})
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
