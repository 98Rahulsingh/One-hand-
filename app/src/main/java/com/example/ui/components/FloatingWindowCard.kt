package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WindowGeometry
import kotlin.math.roundToInt

@Composable
fun FloatingWindowCard(
    title: String,
    initialGeometry: WindowGeometry,
    onGeometryChange: (xOffset: Float, yOffset: Float, widthDp: Float, heightDp: Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    headerActions: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current

    var xOffset by remember(initialGeometry.xOffset) { mutableFloatStateOf(initialGeometry.xOffset) }
    var yOffset by remember(initialGeometry.yOffset) { mutableFloatStateOf(initialGeometry.yOffset) }
    var widthDp by remember(initialGeometry.widthDp) { mutableFloatStateOf(initialGeometry.widthDp) }
    var heightDp by remember(initialGeometry.heightDp) { mutableFloatStateOf(initialGeometry.heightDp) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("floating_window_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Outside Touch Backdrop (Tap outside to immediately dismiss!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onClose() }
                .testTag("floating_backdrop_dismiss")
        )

        // Floating Card
        Card(
            modifier = Modifier
                .offset { IntOffset(xOffset.roundToInt(), yOffset.roundToInt()) }
                .width(widthDp.dp)
                .height(heightDp.dp)
                .clickable(enabled = false) {} // Prevent backdrop click inside card
                .shadow(24.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, Color(0xFF1E464F).copy(alpha = 0.85f), RoundedCornerShape(22.dp))
                .testTag("floating_card_${title.lowercase().replace(" ", "_")}"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF091C20)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Draggable Window Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D252B))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    xOffset += dragAmount.x
                                    yOffset += dragAmount.y
                                    onGeometryChange(xOffset, yOffset, widthDp, heightDp)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag to move",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title.uppercase(),
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            headerActions()

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(28.dp).testTag("floating_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF80DEEA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Card Content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        content()
                    }
                }

                // Bottom-right Interactive Resize Handle (Allows user to adjust size with fingers/mouse directly in preview!)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .padding(3.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                with(density) {
                                    val deltaWDp = dragAmount.x.toDp().value
                                    val deltaHDp = dragAmount.y.toDp().value
                                    widthDp = (widthDp + deltaWDp).coerceIn(220f, 400f)
                                    heightDp = (heightDp + deltaHDp).coerceIn(340f, 850f)
                                    onGeometryChange(xOffset, yOffset, widthDp, heightDp)
                                }
                            }
                        }
                        .testTag("floating_resize_handle"),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Resize window",
                        tint = Color(0xFF00E5FF).copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
