package com.dergoogler.mmrl.wx.ui.screens.settings.appTheme.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ui.component.Logo
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.screens.modules.SkeletonModuleItem
import dev.mmrlx.compose.ui.Surface
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.theme.MMRLXTheme

@Composable
fun ExampleItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .background(MMRLXTheme.colors.background)
                .border(1.dp, MMRLXTheme.colors.border, RoundedCornerShape(15.dp))
                .fillMaxSize(0.5f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Logo(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .size(20.dp),
                    icon = R.drawable.launcher_outline
                )

                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MMRLXTheme.typography.labelLarge
                )
            }

            SkeletonModuleItem(
                modifier = Modifier.graphicsLayer {
                    scaleX = 0.8f
                    scaleY = 0.8f
                }
            )

            Spacer(modifier = Modifier.height(160.dp))

            Surface(
                color = MMRLXTheme.colors.card,
            ) {
                Spacer(
                    modifier = Modifier
                        .height(45.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}