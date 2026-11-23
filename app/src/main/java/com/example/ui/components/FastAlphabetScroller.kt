package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FastAlphabetScroller(
    selectedLetter: Char?,
    onLetterSelected: (Char?) -> Unit,
    modifier: Modifier = Modifier,
    isRightAligned: Boolean = false,
    availableLetters: Set<Char>? = null
) {
    val alphabet = remember(availableLetters) {
        ('A'..'Z').filter { availableLetters == null || it in availableLetters }.ifEmpty { ('A'..'Z').toList() }
    }
    var columnHeightPx by remember { mutableStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var activeScrubLetter by remember { mutableStateOf<Char?>(null) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .testTag("fast_alphabet_scroller")
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D2228).copy(alpha = 0.85f))
                .onGloballyPositioned { coordinates ->
                    columnHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val letterIndex = ((offset.y / columnHeightPx) * alphabet.size)
                                .toInt()
                                .coerceIn(0, alphabet.lastIndex)
                            activeScrubLetter = alphabet[letterIndex]
                            onLetterSelected(alphabet[letterIndex])
                        },
                        onDragEnd = {
                            isDragging = false
                            activeScrubLetter = null
                        },
                        onDragCancel = {
                            isDragging = false
                            activeScrubLetter = null
                        },
                        onVerticalDrag = { change, _ ->
                            val y = change.position.y
                            val letterIndex = ((y / columnHeightPx) * alphabet.size)
                                .toInt()
                                .coerceIn(0, alphabet.lastIndex)
                            val letter = alphabet[letterIndex]
                            activeScrubLetter = letter
                            onLetterSelected(letter)
                        }
                    )
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { letter ->
                val isSelected = (selectedLetter == letter) || (activeScrubLetter == letter)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF00E5FF) else Color.Transparent)
                        .clickable {
                            if (selectedLetter == letter) {
                                onLetterSelected(null)
                            } else {
                                onLetterSelected(letter)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF091B1F) else Color(0xFF80DEEA).copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Floating Magnifying Indicator when scrubbed
        val displayLetter = if (isDragging) activeScrubLetter else null
        AnimatedVisibility(
            visible = displayLetter != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(if (isRightAligned) Alignment.CenterStart else Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = if (isRightAligned) (-44).dp else 44.dp)
                    .size(46.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayLetter?.toString() ?: "",
                    color = Color(0xFF00363A),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
