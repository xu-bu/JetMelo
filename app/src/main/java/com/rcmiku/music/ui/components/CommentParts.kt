package com.rcmiku.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rcmiku.ncmapi.model.Comment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** <= 999 shows the exact number, up to 9999 shows "999+", 10k and above shows "1w+". */
fun formatCommentCount(count: Int): String = when {
    count <= 999 -> count.toString()
    count < 10_000 -> "999+"
    else -> "1w+"
}

@Composable
fun CommentSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun CommentItem(comment: Comment) {
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
