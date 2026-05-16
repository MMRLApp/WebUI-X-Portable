package com.dergoogler.mmrl.wx.ui.screens.modules

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dergoogler.mmrl.datastore.model.ModulesMenu
import com.dergoogler.mmrl.ui.component.Loading
import com.dergoogler.mmrl.ui.component.PageIndicator
import com.dergoogler.mmrl.ui.component.text.TextRow
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.model.WorkingMode
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.component.BottomNavigation
import com.dergoogler.mmrl.wx.ui.component.ModuleImporter
import com.dergoogler.mmrl.wx.viewmodel.ModulesViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.icon.IconButton
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.text.rememberInputState
import dev.mmrlx.compose.ui.toolbar.SearchableToolbar
import dev.mmrlx.compose.ui.toolbar.ToolbarDefaults
import dev.mmrlx.compose.ui.toolbar.ToolbarScrollBehavior
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeApi::class)
@Destination<RootGraph>(start = true)
@Composable
fun ModulesScreen(
    viewModel: ModulesViewModel = hiltViewModel(),
) {
    val prefs = LocalUserPreferences.current
    val scrollBehavior = ToolbarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val list by viewModel.local.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isSearch by viewModel.isSearch.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        toolbar = {
            ModuleScreenToolbar(
                isSearch = isSearch,
                query = query,
                onQueryChange = viewModel::search,
                onOpenSearch = viewModel::openSearch,
                onCloseSearch = viewModel::closeSearch,
                setMenu = viewModel::setModulesMenu,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { BottomNavigation() },
        floatingActionButton = {
            if (prefs.workingMode != WorkingMode.MODE_NON_ROOT) return@Scaffold
            ModuleImporter()
        },
    ) {
        when {
            screenState.isFirstLoad -> Loading()
            list.isEmpty() -> PageIndicator(
                icon = if (isSearch) R.drawable.mood_search else R.drawable.mood_cry,
                text = if (isSearch) R.string.search_empty else R.string.modules_empty,
            )
        }

        PullToRefreshBox(
            isRefreshing = screenState.isRefreshing && !screenState.isFirstLoad,
            onRefresh = viewModel::refreshModules,
        ) {
            this@Scaffold.ModulesList(
                list = list,
                state = listState,
            )
        }
    }
}

@Composable
private fun ModuleScreenToolbar(
    isSearch: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    setMenu: (ModulesMenu) -> Unit,
    scrollBehavior: ToolbarScrollBehavior,
) {
    val state = rememberInputState(query)

    LaunchedEffect(Unit) {
        snapshotFlow { state.text.toString() }
            .distinctUntilChanged()
            .collect { text ->
                if (text != query) onQueryChange(text)
            }
    }

    LaunchedEffect(query) {
        if (state.text.toString() != query) {
            state.setTextAndPlaceCursorAtEnd(query)
        }
    }

    SearchableToolbar(
        state = state,
        isSearch = isSearch,
        title = {
            TextRow(
                leadingContent = {
                    Icon(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource(R.drawable.launcher_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surfaceTint,
                    )
                }
            ) {
                Text(stringResource(R.string.app_name))
            }
        },
        scrollBehavior = scrollBehavior,
        onClose = onCloseSearch,
        actions = {
            if (!isSearch) {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                    )
                }
            }
            ModulesMenu(setMenu = setMenu)
        },
    )
}