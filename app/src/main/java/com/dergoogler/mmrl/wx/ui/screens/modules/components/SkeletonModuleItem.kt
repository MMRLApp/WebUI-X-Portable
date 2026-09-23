package com.dergoogler.mmrl.wx.ui.screens.modules.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mmrlx.compose.layout.flashlightCard
import dev.mmrlx.compose.ui.Skeleton

@Composable
fun SkeletonModuleItem(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .flashlightCard()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Skeleton(
                    modifier = Modifier.size(36.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Skeleton(
                        modifier = Modifier
                            .height(18.dp)
                            .width(140.dp)
                    )

                    Skeleton(
                        modifier = Modifier
                            .height(12.dp)
                            .width(100.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Skeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Skeleton(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Skeleton(
                    modifier = Modifier
                        .width(72.dp)
                        .height(10.dp)
                )

                Skeleton(
                    modifier = Modifier
                        .width(92.dp)
                        .height(10.dp)
                )
            }
        }
    }
}