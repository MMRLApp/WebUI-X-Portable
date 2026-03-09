package com.dergoogler.mmrl.wx.ui.component.devtools

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ui.token.applyTonalElevation


val ColorScheme.tonalSurface: Color
    @Composable get() {
        val absoluteElevation = LocalAbsoluteTonalElevation.current + 1.dp
        val color: Color = surface
        return applyTonalElevation(
            color,
            absoluteElevation,
        )
    }

