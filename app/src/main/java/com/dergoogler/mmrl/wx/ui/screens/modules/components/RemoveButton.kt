package com.dergoogler.mmrl.wx.ui.screens.modules.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.wx.R
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonSize
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.icon.Icon

@Composable
internal fun RemoveButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        variant = ButtonVariant.Destructive,
        size = ButtonSize.Sm
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.trash),
            contentDescription = null

        )
    }
}