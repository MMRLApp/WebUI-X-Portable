package com.dergoogler.mmrl.wx.ui.screens.modules.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.wx.R
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonGroup
import dev.mmrlx.compose.ui.button.ButtonGroupDefaults
import dev.mmrlx.compose.ui.button.ButtonSize
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.icon.Icon

@Composable
internal fun OptionButtonGroup(
    enabled: Boolean,
    onConfigClick: () -> Unit,
    onShortcutClick: () -> Unit,
) {
    ButtonGroup {
        Button(
            onClick = onConfigClick,
            enabled = enabled,
            variant = ButtonVariant.Outline,
            size = ButtonSize.Sm,
            shape = ButtonGroupDefaults.shape(0, 2)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.settings),
                contentDescription = null
            )
        }

        Button(
            onClick = onShortcutClick,
            enabled = enabled,
            variant = ButtonVariant.Outline,
            size = ButtonSize.Sm,
            shape = ButtonGroupDefaults.shape(1, 2)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.link),
                contentDescription = null
            )
        }
    }
}