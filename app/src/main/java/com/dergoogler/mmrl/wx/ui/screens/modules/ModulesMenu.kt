package com.dergoogler.mmrl.wx.ui.screens.modules

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.datastore.model.ModulesMenu
import com.dergoogler.mmrl.datastore.model.Option
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Segment
import dev.mmrlx.compose.ui.button.SegmentedButtons
import dev.mmrlx.compose.ui.chip.FilterChip
import dev.mmrlx.compose.ui.chip.FilterChipDefaults
import dev.mmrlx.compose.ui.dialog.rememberModalBottomSheet
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.icon.IconButton

@Composable
fun ModulesMenu(
    setMenu: (ModulesMenu) -> Unit,
) {
    val userPreferences = LocalUserPreferences.current
    val sheet = rememberModalBottomSheet()

    IconButton(
        onClick = { sheet.open() }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.filter_outlined),
            contentDescription = null
        )

        sheet {
            MenuBottomSheetContent(
                menu = userPreferences.modulesMenu,
                setMenu = setMenu
            )
        }
    }
}

@Composable
private fun ColumnScope.MenuBottomSheetContent(
    menu: ModulesMenu,
    setMenu: (ModulesMenu) -> Unit,
) {
    val options = listOf(
        Option.Name to R.string.menu_sort_option_name,
        Option.UpdatedTime to R.string.menu_sort_option_updated,
        Option.Size to R.string.menu_sort_option_size,
    )

    Text(
        text = stringResource(id = R.string.menu_advanced_menu),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )

    Column(
        modifier = Modifier.padding(all = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.menu_sort_mode),
            style = MaterialTheme.typography.titleSmall
        )

        SegmentedButtons {
            options.forEach { (option, label) ->
                Segment(
                    selected = option == menu.option,
                    onClick = { setMenu(menu.copy(option = option)) },
                    icon = null
                ) {
                    Text(text = stringResource(id = label))
                }
            }
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight(align = Alignment.Top),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuChip(
                selected = menu.descending,
                onClick = { setMenu(menu.copy(descending = !menu.descending)) },
                label = { Text(text = stringResource(id = R.string.menu_descending)) }
            )

            MenuChip(
                selected = menu.pinEnabled,
                onClick = { setMenu(menu.copy(pinEnabled = !menu.pinEnabled)) },
                label = { Text(text = stringResource(id = R.string.menu_pin_enabled)) }
            )

            MenuChip(
                selected = menu.showUpdatedTime,
                onClick = { setMenu(menu.copy(showUpdatedTime = !menu.showUpdatedTime)) },
                label = { Text(text = stringResource(id = R.string.menu_show_updated)) }
            )
        }
    }
}

@Composable
fun MenuChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.height(FilterChipDefaults.Height),
        enabled = enabled,
        leadingIcon = {
            if (!selected) {
                Point(size = 8.dp)
            }
        },
        trailingIcon = {
            if (selected) {
                Icon(
                    painter = painterResource(dev.mmrlx.ui.R.drawable.done),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        },
        shape = CircleShape,
    )
}

@Composable
private fun Point(
    size: Dp,
    color: Color = LocalContentColor.current,
) = Canvas(
    modifier = Modifier.size(size),
) {
    drawCircle(
        color = color,
        radius = this.size.width / 2,
        center = this.center,
    )
}
