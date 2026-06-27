package com.dergoogler.mmrl.wx.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.wx.util.badgeDebugBackground
import com.dergoogler.mmrl.wx.util.badgeDebugForeground
import com.dergoogler.mmrl.wx.util.badgeErrorBackground
import com.dergoogler.mmrl.wx.util.badgeErrorForeground
import com.dergoogler.mmrl.wx.util.badgeInfoBackground
import com.dergoogler.mmrl.wx.util.badgeInfoForeground
import com.dergoogler.mmrl.wx.util.badgeWarnBackground
import com.dergoogler.mmrl.wx.util.badgeWarnForeground
import dev.mmrlx.compose.ui.ProvideContentColor
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.theme.MMRLXTheme

@Composable
fun Alert(
    title: String,
    message: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(16.dp)
    ) {
        ProvideContentColor(contentColor) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun InfoAlert(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) = Alert(
    title,
    message,
    MMRLXTheme.colors.badgeInfoBackground,
    MMRLXTheme.colors.badgeInfoForeground,
    modifier,
)

@Composable
fun WarningAlert(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) = Alert(
    title,
    message,
    MMRLXTheme.colors.badgeWarnBackground,
    MMRLXTheme.colors.badgeWarnForeground,
    modifier,
)

@Composable
fun DebugAlert(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) = Alert(
    title,
    message,
    MMRLXTheme.colors.badgeDebugBackground,
    MMRLXTheme.colors.badgeDebugForeground,
    modifier,
)

@Composable
fun ErrorAlert(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) = Alert(
    title,
    message,
    MMRLXTheme.colors.badgeErrorBackground,
    MMRLXTheme.colors.badgeErrorForeground,
    modifier,
)