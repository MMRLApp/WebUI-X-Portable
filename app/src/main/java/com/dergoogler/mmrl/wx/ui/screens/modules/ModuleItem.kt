package com.dergoogler.mmrl.wx.ui.screens.modules

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ext.fadingEdge
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.ext.nullply
import com.dergoogler.mmrl.ext.takeTrue
import com.dergoogler.mmrl.platform.PlatformManager
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toFormattedFileSize
import com.dergoogler.mmrl.platform.model.ModId.Companion.toModId
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.dergoogler.mmrl.ui.component.LocalCover
import com.dergoogler.mmrl.ui.component.card.Card
import com.dergoogler.mmrl.ui.component.card.component.Absolute
import com.dergoogler.mmrl.ui.component.text.TextWithIcon
import com.dergoogler.mmrl.ui.component.text.TextWithIconDefaults
import com.dergoogler.mmrl.webui.activity.WXActivity.Companion.launchWebUIX
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.model.module.Module
import com.dergoogler.mmrl.wx.model.module.ModuleState
import com.dergoogler.mmrl.wx.ui.activity.webui.WebUIActivity
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.dergoogler.mmrl.wx.util.toFormattedDateSafely
import com.dergoogler.mmrl.wx.viewmodel.ModulesViewModel
import dev.mmrlx.thread.RootCallable
import dev.mmrlx.thread.ktx.asThread
import dev.mmrlx.thread.ktx.invoke

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
    val menu = userPreferences.modulesMenu
    val context = LocalContext.current

    val canWenUIAccessed =
        PlatformManager.isAlive && (module.hasWebUI) && module.state != ModuleState.Remove

    val clicker: (() -> Unit)? = canWenUIAccessed nullable jump@{
        if (module.hasWebUI) {
            val baseDir = module.paths.adbDir
            context.launchWebUIX<WebUIActivity>(module.id.toModId(baseDir), baseDir)
            return@jump
        }

        Toast.makeText(context, "Unsupported module", Toast.LENGTH_SHORT).show()
    }

//    val config = remember(module) {
//        module.config
//    }

    val bannerByteArray: ByteArray? by produceState(null) {
        val bannerPath = module.banner
        if (bannerPath == null) {
            value = null
            return@produceState
        }

        value = ModulesViewModel.loadModuleBanner(
            mapOf<String, Any>(
                "bannerPath" to bannerPath
            )
        )
    }

    Card(
        onClick = clicker,
        onLongClick = {
//            navigator.navigate(FileExplorerScreenDestination(module))
        }
    ) {
        Absolute(
            alignment = Alignment.Center,
        ) {
            indicator.nullable {
                it()
            }
        }

        Column(
            modifier = Modifier.relative()
        ) {
            bannerByteArray?.let {
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
                    inputStream = it.inputStream(),
                )
            }
            /*  config.cover.nullable(menu.showCover) {
                  val file = SuFile(module.id.moduleDir, it)

                  file.exists { i ->
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
                          inputStream = i.newInputStream(),
                      )
                  }
              }*/

            Row(
                modifier = Modifier.padding(all = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .alpha(alpha = alpha)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TextWithIcon(
                        text = /*config.name ?:*/ module.name!!,
                        //icon = module.hasModConf nullable R.drawable.brand_kotlin,
                        style = TextWithIconDefaults.style.copy(
                            overflow = TextOverflow.Ellipsis,
                            textStyle = MaterialTheme.typography.titleSmall,
                            maxLines = 2
                        )
                    )

                    Text(
                        text = stringResource(
                            id = R.string.author,
                            "module.versionDisplay", module.author!!
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = decoration,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (module.lastUpdated != 0L && menu.showUpdatedTime) {
                        Text(
                            text = stringResource(
                                id = R.string.update_on,
                                module.lastUpdated.toFormattedDateSafely
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = decoration,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Text(
                modifier = Modifier
                    .alpha(alpha = alpha)
                    .padding(horizontal = 16.dp),
                text = /*config.description ?:*/ module.description!!,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = decoration,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.outline
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                userPreferences.developerMode.takeTrue {
                    LabelItem(
                        text = module.id.toString(),
                        upperCase = false
                    )
                }

                LabelItem(
                    text = module.size.toFormattedFileSize(),
                    style = LabelItemDefaults.style.copy(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            HorizontalDivider(
                thickness = 1.5.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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

@Composable
fun LocalCover(
    modifier: Modifier = Modifier,
    imageBitmap: ImageBitmap,
    shape: RoundedCornerShape = DefaultShape,
    aspectRatio: Float = DefaultAspectRatio,
) {
    Image(
        painter = BitmapPainter(imageBitmap),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .aspectRatio(aspectRatio)
                .then(modifier),
    )
}
