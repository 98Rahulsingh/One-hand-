package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ItemType
import com.example.viewmodel.OneHandViewModel

private val SideBg = Color(0xFF0A1418)
private val SideAccent = Color(0xFF00E5FF)
private val SideTile = Color(0xFF14252B)

@Composable
fun SidePanelScreen(viewModel: OneHandViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val allItems by viewModel.masterItems.collectAsState()
    var tab by remember { mutableStateOf("Actions") }
    var clipboardText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val prefs = remember { context.getSharedPreferences("side_panel_notes", Context.MODE_PRIVATE) }
    LaunchedEffect(Unit) { note = prefs.getString("note", "") ?: "" }
    Column(modifier.background(SideBg, RoundedCornerShape(14.dp)).padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SIDE PANEL", color = SideAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("×", color = Color.White, modifier = Modifier.clickable { viewModel.togglePanel(false) }.padding(horizontal = 8.dp), fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                "Actions" -> {
                    val actions = allItems.filter { it.type == ItemType.ACTION && it.isAdded }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(actions, key = { it.id }) { item ->
                            Row(Modifier.fillMaxWidth().background(SideTile, RoundedCornerShape(8.dp)).clickable { viewModel.launchApp(item) }.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, color = Color.White, fontSize = 12.sp)
                            }
                        }
                        if (actions.isEmpty()) item { Text("No actions added yet", color = Color.LightGray, fontSize = 12.sp) }
                    }
                }
                "Clipboard" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardText = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                    }) { Text("Read clipboard") }
                    Text(clipboardText.ifBlank { "Clipboard content will appear here" }, color = Color.White, fontSize = 12.sp)
                    if (clipboardText.isNotBlank()) TextButton(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Side Panel", clipboardText))
                    }) { Text("Copy again") }
                }
                "Notes" -> Column {
                    OutlinedTextField(value = note, onValueChange = { note = it; prefs.edit().putString("note", it).apply() }, modifier = Modifier.fillMaxSize(), placeholder = { Text("Write a note…") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SideAccent, unfocusedBorderColor = Color.Gray))
                }
                "Files" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }
                        runCatching { context.startActivity(intent) }
                    }) { Text("Browse files") }
                    Text("Choose a file from your device", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().background(Color(0xFF102027), RoundedCornerShape(10.dp)).padding(3.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Actions", "Clipboard", "Notes", "Files").forEach { label ->
                Column(Modifier.weight(1f).clickable { tab = label }.background(if (tab == label) Color(0xFF16414A) else Color.Transparent, RoundedCornerShape(8.dp)).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = if (tab == label) SideAccent else Color.White, fontSize = 9.sp, fontWeight = if (tab == label) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
