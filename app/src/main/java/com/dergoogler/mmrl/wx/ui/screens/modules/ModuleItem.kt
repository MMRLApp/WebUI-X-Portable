package com.dergoogler.mmrl.wx.ui.screens.modules

import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ext.nullply
import com.dergoogler.mmrl.ext.toFormattedDateSafely
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.content.State
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toFormattedFileSize
import com.dergoogler.mmrl.platform.model.ModId.Companion.toModId
import com.dergoogler.mmrl.webui.activity.WXActivity.Companion.launchWebUIX
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.model.WebUIEngine
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.ui.component.LocalCover
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.dergoogler.mmrl.wx.util.versionDisplay
import dev.mmrlx.compose.layout.flashlightCard
import dev.mmrlx.compose.ui.Avatar
import dev.mmrlx.compose.ui.HorizontalDivider
import dev.mmrlx.compose.ui.Skeleton
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.ext.fadingEdge
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.text.FormatText
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.compose.utilities.io.toPainter
import dev.mmrlx.nio.inputStream
import dev.mmrlx.thread.RootCallable
import dev.mmrlx.thread.ktx.asThread

@Composable
fun <T> RootCallable<T>.produceState(
    initialValue: T,
) = produceState(initialValue) {
    value = asThread()
}

@Composable
fun <T> produceRootCallableState(
    initialValue: T,
    block: RootCallable<T>,
) = block.produceState(initialValue)

@Composable
fun ModuleItem(
    module: Module,
    alpha: Float = 1f,
    decoration: TextDecoration = TextDecoration.None,
    indicator: @Composable() (() -> Unit?)? = null,
    leadingButton: @Composable() (RowScope.() -> Unit)? = null,
    trailingButton: @Composable() (RowScope.() -> Unit)? = null,
) {
    val navigator = LocalDestinationsNavigator.current
    val userPreferences = LocalUserPreferences.current
    // TODO: add menu settings back
    val menu = userPreferences.modulesMenu
    val context = LocalContext.current

    val canWenUIAccessed =
        PlatformManager.isAlive && (module.hasWebUI) && module.state != State.REMOVE
//
//    val config = remember(module) {
//        module.config
//    }

    val toastStr = stringResource(R.string.unsupported_engine)

    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = {
                    if (canWenUIAccessed) {
                        val baseDir = module.adbPath.baseDir

                        if (userPreferences.webuiEngine == WebUIEngine.MX) {
                            val intent = Intent(
                                context,
                                com.dergoogler.mmrl.wx.ui.webui.WebUIActivity::class.java
                            )
                                .apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                    putExtra("MODULE_ID", module.id)
                                }

                            context.startActivity(intent)
                            return@combinedClickable
                        }

                        // TODO: deprecate WX engine, devtools are crashing currently!
                        if (userPreferences.webuiEngine == WebUIEngine.WX) {
                            context.launchWebUIX<com.dergoogler.mmrl.wx.ui.activity.webui.WebUIActivity>(
                                module.id.toModId(baseDir),
                                baseDir
                            )
                            return@combinedClickable
                        }
                    }

                    Toast.makeText(
                        context, toastStr, Toast.LENGTH_SHORT
                    ).show()
                }
            )
            .fillMaxWidth()
            .flashlightCard()
    ) {
        module.banner?.let {
            it.exists { cover ->
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

        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Avatar(
                    initials = module.name.take(
                        2
                    ).uppercase(),
                    size = 36.dp,
                    painter = module.icon?.toPainter()
                )

                Column {
                    Text(
                        text = module.name,
                        style = MMRLXTheme.typography.titleSmall
                    )

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

            FormatText(
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                text = "%y %s • %y %s",
                style = MMRLXTheme.typography.labelSmall,
                color = MMRLXTheme.colors.mutedForeground
            ) {
                composable {
                    Icon(
                        modifier = Modifier.size(fontSize.dp),
                        painter = painterResource(R.drawable.folder),
                        tint = MMRLXTheme.colors.mutedForeground
                    )
                }
                string(module.size.toFormattedFileSize())
                composable {
                    Icon(
                        modifier = Modifier.size(fontSize.dp),
                        painter = painterResource(R.drawable.git_branch),
                        tint = MMRLXTheme.colors.mutedForeground
                    )
                }
                string(module.lastUpdated.toFormattedDateSafely(userPreferences.datePattern))
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

@Composable
fun StateIndicator(
    @DrawableRes icon: Int,
    color: Color = MaterialTheme.colorScheme.outline,
) = Image(
    modifier = Modifier.requiredSize(150.dp),
    painter = painterResource(id = icon),
    contentDescription = null,
    alpha = 0.1f,
    colorFilter = ColorFilter.tint(color)
)

private const val DefaultAspectRatio = 2.048f
private val DefaultShape: RoundedCornerShape = RoundedCornerShape(0.dp)
