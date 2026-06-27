package com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.theme.MMRLXTheme

@Composable
internal fun TitleItem(
    text: String,
) = Text(
    text = text,
    style = MMRLXTheme.typography.titleSmall,
    modifier = Modifier.padding(start = 18.dp, top = 18.dp)
)