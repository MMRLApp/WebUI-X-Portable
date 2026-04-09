package com.dergoogler.mmrl.wx.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dergoogler.mmrl.ext.LoadData
import com.dergoogler.mmrl.ui.component.Loading
import com.dergoogler.mmrl.ui.component.PageIndicator
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.model.license.UiLicense
import com.dergoogler.mmrl.wx.ui.component.BottomNavigation
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.viewmodel.LicenseViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.Badge
import dev.mmrlx.compose.ui.BadgeVariant
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.ext.with
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.list.List
import dev.mmrlx.compose.ui.list.ListScope
import dev.mmrlx.compose.ui.list.component.RawItem
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.Supporting
import dev.mmrlx.compose.ui.list.component.item.Title
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.scaffold.ScaffoldScope
import dev.mmrlx.compose.ui.text.FormatText
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults
import dev.mmrlx.compose.ui.toolbar.ToolbarScrollBehavior

@Destination<RootGraph>()
@Composable
fun LicensesScreen(
    viewModel: LicenseViewModel = hiltViewModel(),
) {
    val scrollBehavior = ToolbarDefaults.pinnedScrollBehavior()
    val navController = LocalNavController.current

    Scaffold(
        toolbar = {
            TopBar(
                navController = navController,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            // TODO: blur is somehow not applied
            BottomNavigation()
        }
    ) {
        when (val data = viewModel.data) {
            LoadData.Pending, LoadData.Loading -> Loading(
                modifier = Modifier.padding(contentPadding)
            )

            is LoadData.Success<List<UiLicense>> -> LicensesContent(
                list = data.value,
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            )

            is LoadData.Failure -> PageIndicator(
                icon = R.drawable.bug,
                text = data.error.message
                    ?: stringResource(com.dergoogler.mmrl.ui.R.string.unknown_error),
                modifier = Modifier.padding(contentPadding)
            )
        }
    }
}

@Composable
private fun ScaffoldScope.LicensesContent(
    list: List<UiLicense>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    List {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .with(this@LicensesContent) {
                    it.scaffoldHazeSource("licenses")
                }
                .then(modifier),
            contentPadding = PaddingValues(
                top = this@LicensesContent.scaffoldTopPadding + 8.dp,
                start = 8.dp,
                end = 8.dp,
                bottom = this@LicensesContent.scaffoldBottomPadding + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(list) {
                this@List.LicenseItem(it)
            }
        }
    }
}

@Composable
private fun ListScope.LicenseItem(
    license: UiLicense,
) {
    val context = LocalContext.current

    RawItem(
        modifier = Modifier
            .let {
                if (license.hasUrl) {
                    it.onClick {
                        context.startActivity(
                            Intent.parseUri(license.url, Intent.URI_INTENT_SCHEME)
                        )
                    }
                } else Modifier
            }
            .contentPadding()
    ) {
        Title {
            if (license.hasUrl) {
                FormatText(license.name + " %c") {
                    composable {
                        Icon(
                            // Don't ever do that :clown:
                            modifier = Modifier.size(fontSize.dp),
                            painter = painterResource(R.drawable.external_link)
                        )
                    }
                }
                return@Title
            }

            Text(license.name)
        }
        Description(license.dependency)

        Supporting {
            Badge(license.version, variant = BadgeVariant.Default)

            license.spdxLicenses.forEach {
                Badge(it.name, variant = BadgeVariant.Outline)
            }

            license.unknownLicenses.forEach {
                Badge(it.name, variant = BadgeVariant.Warning)
            }
        }
    }
}

@Composable
private fun TopBar(
    navController: NavController,
    scrollBehavior: ToolbarScrollBehavior,
) {
    NavigateUpToolbar(
        title = stringResource(R.string.license_title),
        scrollBehavior = scrollBehavior,
        navController = navController,
    )
}