package com.rcmiku.music.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rcmiku.music.R
import com.rcmiku.music.ui.components.CommentItem
import com.rcmiku.music.ui.components.CommentSectionTitle
import com.rcmiku.music.viewModel.CommentScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    navController: NavHostController,
    viewModel: CommentScreenViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.comments) +
                                if (state.total > 0) " (${state.total})" else ""
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            if (state.hot.isNotEmpty()) {
                item { CommentSectionTitle(stringResource(R.string.hot_comments)) }
                items(state.hot, key = { "hot_${it.commentId}" }) { CommentItem(it) }
            }

            if (state.latest.isNotEmpty()) {
                item { CommentSectionTitle(stringResource(R.string.latest_comments)) }
                items(state.latest, key = { "new_${it.commentId}" }) { CommentItem(it) }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        state.failed -> TextButton(onClick = { viewModel.loadMore() }) {
                            Text(stringResource(R.string.load_failed_retry))
                        }

                        state.loading -> CircularProgressIndicator(Modifier.size(24.dp))

                        state.hasMore -> LaunchedEffect(state.latest.size) { viewModel.loadMore() }

                        state.latest.isEmpty() && state.hot.isEmpty() -> Text(
                            stringResource(R.string.no_comments),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
