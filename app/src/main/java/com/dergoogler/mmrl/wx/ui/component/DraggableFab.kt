package com.dergoogler.mmrl.wx.ui.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.wx.R
import kotlin.math.roundToInt

@Composable
fun DraggableFab(onClick: () -> Unit) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val fabSizePx = with(density) { 56.dp.toPx() }
    val paddingPx = with(density) { 16.dp.toPx() }

    // Start at bottom-end once we know the container size
    LaunchedEffect(containerSize) {
        if (containerSize != IntSize.Zero) {
            offsetX = containerSize.width - fabSizePx - paddingPx
            offsetY = containerSize.height - fabSizePx - paddingPx
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(containerSize) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x)
                            .coerceIn(paddingPx, containerSize.width - fabSizePx - paddingPx)
                        offsetY = (offsetY + dragAmount.y)
                            .coerceIn(paddingPx, containerSize.height - fabSizePx - paddingPx)
                    }
                }
        ) {
            Icon(painterResource(R.drawable.braces), contentDescription = "Add")
        }
    }
}