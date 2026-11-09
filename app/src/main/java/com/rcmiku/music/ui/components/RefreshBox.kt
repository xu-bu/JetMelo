package com.rcmiku.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.launch

/**
 * Pull-to-refresh container. [onRefresh] runs while the spinner is shown and should return when
 * the new data has been loaded (for a ViewModel function returning a Job: `{ vm.refresh().join() }`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshBox(
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier, content = content)
        return
    }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            if (!refreshing) {
                scope.launch {
                    refreshing = true
                    try {
                        onRefresh()
                    } finally {
                        refreshing = false
                    }
                }
            }
        },
        modifier = modifier,
        content = content
    )
}

/**
 * Pull-to-refresh container for a paged list: refreshes [items] and keeps the spinner until the
 * refresh load has finished. [onExtraRefresh] can reload other data of the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagingRefreshBox(
    items: LazyPagingItems<*>,
    modifier: Modifier = Modifier,
    onExtraRefresh: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    var pulled by remember { mutableStateOf(false) }
    val loading = items.loadState.refresh is LoadState.Loading
    LaunchedEffect(loading) {
        if (!loading) pulled = false
    }
    PullToRefreshBox(
        isRefreshing = pulled,
        onRefresh = {
            pulled = true
            onExtraRefresh()
            items.refresh()
        },
        modifier = modifier,
        content = content
    )
}
