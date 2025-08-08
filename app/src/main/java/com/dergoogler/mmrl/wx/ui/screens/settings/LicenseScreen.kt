package com.dergoogler.mmrl.wx.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dergoogler.mmrl.ext.LoadData
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.ext.plus
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.Loading
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.component.PageIndicator
import com.dergoogler.mmrl.ui.component.card.Card
import com.dergoogler.mmrl.ui.component.listItem.dsl.List
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.Item
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Description
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Labels
import com.dergoogler.mmrl.ui.component.listItem.dsl.component.item.Title
import com.dergoogler.mmrl.ui.providable.LocalNavController
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.model.license.UiLicense
import com.dergoogler.mmrl.wx.viewmodel.LicenseViewModel

@Composable
fun LicensesScreen(
    viewModel: LicenseViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val navController = LocalNavController.current

    Scaffold(
        topBar = {
            TopBar(
                navController = navController,
                scrollBehavior = scrollBehavior
            )
        }
    ) { contentPadding ->
        when (val data = viewModel.data) {
            LoadData.Pending, LoadData.Loading -> Loading(
                modifier = Modifier.padding(contentPadding)
            )

            is LoadData.Success<List<UiLicense>> -> LicensesContent(
                list = data.value,
                contentPadding = contentPadding,
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
private fun LicensesContent(
    list: List<UiLicense>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(all = 20.dp) + contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(list) {
            LicenseItem(it)
        }
    }
}

@Composable
private fun LicenseItem(
    license: UiLicense,
) {
    val context = LocalContext.current

    Card(
        onClick = license.hasUrl nullable {
            context.startActivity(
                Intent.parseUri(license.url, Intent.URI_INTENT_SCHEME)
            )
        }
    ) {
        List(
            modifier = Modifier.relative()
        ) {
            Item {
                Title(license.name)
                Description(license.dependency)

                Labels {
                    LabelItem(
                        text = license.version
                    )

                    license.spdxLicenses.forEach {
                        LabelItem(
                            text = it.name
                        )
                    }

                    license.unknownLicenses.forEach {
                        LabelItem(
                            text = it.name.ifEmpty { it.url }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    NavigateUpTopBar(
        title = stringResource(R.string.license_title),
        scrollBehavior = scrollBehavior,
        navController = navController,
    )
}