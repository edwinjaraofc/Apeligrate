package com.apeligrate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apeligrate.ui.theme.DarkBackground

@Composable
fun SentinelBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        GridPattern()
        content()
    }
}

@Composable
fun GridPattern() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSpacing = 40.dp.toPx()
        val gridColor = Color.White.copy(alpha = 0.05f)
        val strokeWidth = 1.dp.toPx()

        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth,
            )
            x += gridSpacing
        }

        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth,
            )
            y += gridSpacing
        }
    }
}
