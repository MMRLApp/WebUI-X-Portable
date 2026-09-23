package com.dergoogler.mmrl.wx.ui.screens.modules.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ext.nullply
import com.dergoogler.mmrl.ext.toFormattedDateSafely
import com.dergoogler.mmrl.platform.content.State
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toFormattedFileSize
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.ui.component.LocalCover
import com.dergoogler.mmrl.wx.util.toPainter
import com.dergoogler.mmrl.wx.util.versionDisplay
import dev.mmrlx.compose.layout.flashlightCard
import dev.mmrlx.compose.nio.LocalSuFileAlive
import dev.mmrlx.compose.ui.Avatar
import dev.mmrlx.compose.ui.Badge
import dev.mmrlx.compose.ui.BadgeVariant
import dev.mmrlx.compose.ui.HorizontalDivider
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.ext.fadingEdge
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.scaffold.PositionLayout
import dev.mmrlx.compose.ui.text.FormatText
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.nio.inputStream

/**
 * [onClick] is only executed when the WebUI can be accessed
 */
@Composable
internal fun ModuleItem0(
    module: Module,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    alpha: Float = 1f,
    decoration: TextDecoration = TextDecoration.None,
    indicator: @Composable() (BoxScope.() -> Unit)? = null,
    leadingButton: @Composable() (RowScope.() -> Unit)? = null,
    trailingButton: @Composable() (RowScope.() -> Unit)? = null,
) {
    val isAlive = LocalSuFileAlive.current
    val userPreferences = LocalUserPreferences.current
    val menu = userPreferences.modulesMenu

    val canWenUIAccessed = isAlive && (module.hasWebUI) && module.state != State.REMOVE

    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = {
                    if (canWenUIAccessed) {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .fillMaxWidth()
            .flashlightCard()
    ) {
        if (menu.showCover && module.banner != null) {
            module.banner.exists { cover ->
                LocalCover(
                    modifier = Modifier.fadingEdge(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black,
                            ),
                            startY = Float.POSITIVE_INFINITY,
                            endY = 0f
                        ),
                    ),
                    inputStream = cover.inputStream(),
                )
            }
        }

        PositionLayout {
            indicator?.let {
                Box(
                    modifier = Modifier
                        .absolute(alignment = Alignment.Center),
                    content = it
                )
            }

            Column(
                modifier = Modifier
                    .relative()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (menu.showIcon) {
                        Avatar(
                            initials = module.name.take(
                                2
                            ).uppercase(),
                            size = 36.dp,
                            painter = module.icon?.toPainter()
                        )
                    }

                    Column {
                        FormatText(
                            text = buildString {
                                if (module.metaModule) {
                                    append("%y ")
                                }
                                append(module.name)
                            },
                            style = MMRLXTheme.typography.titleSmall
                        ) {
                            if (module.metaModule) {
                                composable {
                                    Badge(
                                        text = "M",
                                        variant = BadgeVariant.Secondary,
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${module.author}, ${module.versionDisplay}",
                            style = MMRLXTheme.typography.labelSmall,
                            color = MMRLXTheme.colors.mutedForeground
                        )
                    }
                }

                Text(
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    text = module.description,
                    style = MMRLXTheme.typography.bodySmall
                )

                val stats = remember(menu) {
                    buildString {
                        if (menu.showSize) {
                            append("%y %s")
                        }
                        if (menu.showSize && menu.showUpdatedTime) {
                            append(" %s ")
                        }
                        if (menu.showUpdatedTime) {
                            append("%y %s")
                        }
                    }
                }

                if (stats.isNotEmpty()) {
                    FormatText(
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        text = stats,
                        style = MMRLXTheme.typography.labelSmall,
                        color = MMRLXTheme.colors.mutedForeground
                    ) {
                        if (menu.showSize) {
                            composable {
                                Icon(
                                    modifier = Modifier.size(fontSize.dp),
                                    painter = painterResource(R.drawable.folder),
                                    tint = MMRLXTheme.colors.mutedForeground
                                )
                            }
                            string(module.size.toFormattedFileSize())
                        }

                        if (menu.showSize && menu.showUpdatedTime) {
                            string("•")
                        }

                        if (menu.showUpdatedTime) {
                            composable {
                                Icon(
                                    modifier = Modifier.size(fontSize.dp),
                                    painter = painterResource(R.drawable.git_branch),
                                    tint = MMRLXTheme.colors.mutedForeground
                                )
                            }
                            string(module.lastUpdated.toFormattedDateSafely(userPreferences.datePattern))
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(top = 8.dp))

                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    leadingButton.nullply {
                        this()
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    trailingButton.nullply {
                        this()
                    }
                }
            }
        }
    }
}