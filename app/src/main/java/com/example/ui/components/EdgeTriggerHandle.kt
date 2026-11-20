package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun EdgeTriggerHandle(
    isRightEdge: Boolean,
    verticalOffsetPercent: Float, // 0f to 1f
    handleHeightDp: Float,
    opacity: Float,
    onClick: () -> Unit,
    onDragInward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = if (isRightEdge) {
        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 0.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(26.dp)
            .testTag("edge_trigger_handle")
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset {
                    // Vertical position offset based on setting
                    val yOffset = ((verticalOffsetPercent - 0.5f) * 350).dp.toPx()
                    IntOffset(0, yOffset.roundToInt())
                }
                .width(20.dp)
                .height(handleHeightDp.dp)
                .shadow(6.dp, shape)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = opacity),
                            Color(0xFF00838F).copy(alpha = opacity)
                        )
                    )
                )
                .clickable { onClick() }
                .pointerInput(isRightEdge) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Drag inward: if right edge, drag left (negative X). If left edge, drag right (positive X)
                        if (isRightEdge && dragAmount.x < -8) {
                            onDragInward()
                        } else if (!isRightEdge && dragAmount.x > 8) {
                            onDragInward()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Pill inner grip line
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }
    }
}
