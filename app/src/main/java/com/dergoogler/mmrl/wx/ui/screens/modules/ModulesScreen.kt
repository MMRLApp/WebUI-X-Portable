package com.dergoogler.mmrl.wx.ui.screens.modules

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.ui.component.Loading
import com.dergoogler.mmrl.ui.component.PageIndicator
import com.dergoogler.mmrl.ui.component.SearchTopBar
import com.dergoogler.mmrl.ui.component.text.TextRow
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.ui.component.BottomNavigation
import com.dergoogler.mmrl.wx.ui.component.ModuleImporter
import com.dergoogler.mmrl.wx.viewmodel.ModulesViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.toolbar.Toolbar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeApi::class)
@Destination<RootGraph>(start = true)
@Composable
fun ModulesScreen(
    viewModel: ModulesViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val list by viewModel.modules.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        toolbar = {

            Toolbar(
                title = {
                    Text("WebUI X")
                },
            )
//
//            TopBar(
//                isSearch = viewModel.isSearch,
//                query = query,
//                onQueryChange = viewModel::search,
//                onOpenSearch = viewModel::openSearch,
//                onCloseSearch = viewModel::closeSearch,
//                setMenu = viewModel::setModulesMenu,
//                scrollBehavior = scrollBehavior
//            )
        },
        bottomBar = {
            BottomNavigation()
        },
        floatingActionButton = {
            if (viewModel.platform != Platform.NonRoot) return@Scaffold

            ModuleImporter()
        },
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

//        PullToRefreshBox(
//            isRefreshing = state.isLoading,
//            onRefresh = viewModel::getLocalAll
//        ) {
            this@Scaffold.ModulesList(
                list = list,
                platform = viewModel.platform,
                state = listState,
            )
//        }
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
