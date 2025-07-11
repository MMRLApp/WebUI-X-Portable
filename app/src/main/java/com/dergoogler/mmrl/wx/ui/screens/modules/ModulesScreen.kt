package com.dergoogler.mmrl.wx.ui.screens.modules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dergoogler.mmrl.datastore.model.ModulesMenu
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.ui.component.Loading
import com.dergoogler.mmrl.ui.component.PageIndicator
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.ui.component.SearchTopBar
import com.dergoogler.mmrl.ui.component.text.TextRow
import com.dergoogler.mmrl.wx.ui.component.ModuleImporter
import com.dergoogler.mmrl.wx.viewmodel.ModulesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeApi::class)
@Composable
fun ModulesScreen(
    viewModel: ModulesViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val list by viewModel.local.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopBar(
                isSearch = viewModel.isSearch,
                query = query,
                onQueryChange = viewModel::search,
                onOpenSearch = viewModel::openSearch,
                onCloseSearch = viewModel::closeSearch,
                setMenu = viewModel::setModulesMenu,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (viewModel.platform != Platform.NonRoot) return@Scaffold

            ModuleImporter()
        },
        contentWindowInsets = WindowInsets.none
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            if (isLoading) {
                Loading()
            }

            if (list.isEmpty() && !isLoading) {
                PageIndicator(
                    icon = if (viewModel.isSearch) R.drawable.mood_search else R.drawable.mood_cry,
                    text = if (viewModel.isSearch) R.string.search_empty else R.string.modules_empty,
                )
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::getLocalAll
            ) {
                ModulesList(
                    list = list,
                    isProviderAlive = viewModel.isProviderAlive,
                    platform = viewModel.platform,
                    state = listState,
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    isSearch: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    setMenu: (ModulesMenu) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var currentQuery by remember { mutableStateOf(query) }
    DisposableEffect(isSearch) {
        onDispose { currentQuery = "" }
    }

    SearchTopBar(
        isSearch = isSearch,
        query = currentQuery,
        onQueryChange = {
            onQueryChange(it)
            currentQuery = it
        },
        onClose = {
            onCloseSearch()
            currentQuery = ""
        },
        title = {
            TextRow(
                leadingContent = {
                    Icon(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource(id = R.drawable.launcher_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surfaceTint
                    )
                }
            ) {
                Text(stringResource(R.string.app_name))
            }
        },
        scrollBehavior = scrollBehavior,
        actions = {
            if (!isSearch) {
                IconButton(
                    onClick = onOpenSearch
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = null
                    )
                }
            }

            ModulesMenu(
                setMenu = setMenu
            )
        }
    )
}
