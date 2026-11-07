package com.rcmiku.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rcmiku.music.R
import com.rcmiku.ncmapi.api.comment.CommentApi
import com.rcmiku.ncmapi.model.Comment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PAGE_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    songId: Long?,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit,
) {
    if (!openBottomSheet || songId == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var hot by remember(songId) { mutableStateOf<List<Comment>>(emptyList()) }
    var latest by remember(songId) { mutableStateOf<List<Comment>>(emptyList()) }
    var total by remember(songId) { mutableStateOf(0) }
    var hasMore by remember(songId) { mutableStateOf(true) }
    var loading by remember(songId) { mutableStateOf(false) }
    var failed by remember(songId) { mutableStateOf(false) }

    fun loadMore() {
        if (loading || !hasMore) return
        loading = true
        failed = false
        scope.launch {
            CommentApi.songComments(songId, PAGE_SIZE, latest.size)
                .onSuccess { response ->
                    if (latest.isEmpty()) hot = response.hotComments
                    latest = latest + response.comments
                    total = response.total
                    hasMore = response.more && response.comments.isNotEmpty()
                }
                .onFailure { failed = true }
            loading = false
        }
    }

    LaunchedEffect(songId) { loadMore() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier.navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.comments) + if (total > 0) " ($total)" else "",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (hot.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.hot_comments)) }
                items(hot, key = { "hot_${it.commentId}" }) { CommentItem(it) }
            }

            if (latest.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.latest_comments)) }
                items(latest, key = { "new_${it.commentId}" }) { CommentItem(it) }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        failed -> TextButton(onClick = { loadMore() }) {
                            Text(stringResource(R.string.load_failed_retry))
                        }

                        loading -> CircularProgressIndicator(Modifier.size(24.dp))

                        hasMore -> LaunchedEffect(latest.size) { loadMore() }

                        latest.isEmpty() && hot.isEmpty() -> Text(
                            stringResource(R.string.no_comments),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun CommentItem(comment: Comment) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = comment.user.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.user.nickname,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = formatCommentTime(comment.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                comment.beReplied.firstOrNull()?.let { replied ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "@${replied.user.nickname}: ${replied.content.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(8.dp)
                    )
                }
            }
            if (comment.likedCount > 0) {
                Text(
                    text = comment.likedCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

private fun formatCommentTime(time: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
