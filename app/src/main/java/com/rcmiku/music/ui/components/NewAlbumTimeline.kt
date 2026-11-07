package com.rcmiku.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rcmiku.music.R
import com.rcmiku.music.utils.FeedAlbum
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_ALBUMS = 20

/** Unheard albums of followed artists, newest first (at most [MAX_ALBUMS]), split into months along a timeline. */
@Composable
fun NewAlbumTimeline(
    albums: List<FeedAlbum>,
    scanning: Boolean,
    onAlbumClick: (FeedAlbum) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only the newest MAX_ALBUMS albums are shown.
    val shown = remember(albums) { albums.take(MAX_ALBUMS) }
    val grouped = remember(shown) {
        val monthFormat = SimpleDateFormat("yyyy年M月", Locale.getDefault())
        shown.groupBy { monthFormat.format(Date(it.publishTime)) }
    }

    LazyColumn(modifier = modifier) {
        item {
            NavigationTitle(title = stringResource(R.string.new_releases))
        }
        if (scanning) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        if (shown.isEmpty()) {
            item {
                Text(
                    text = stringResource(
                        if (scanning) R.string.new_releases_scanning else R.string.new_releases_empty
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        grouped.forEach { (month, monthAlbums) ->
            item(key = "month_$month") { TimelineMonthHeader(month) }
            items(monthAlbums, key = { it.id }) { album ->
                TimelineAlbumRow(album = album, onClick = { onAlbumClick(album) })
            }
        }
    }
}

@Composable
private fun TimelineRail(dotTop: Int, modifier: Modifier = Modifier, showDot: Boolean = true) {
    Box(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        if (showDot) {
            Box(
                Modifier
                    .padding(top = dotTop.dp)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
private fun TimelineMonthHeader(month: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        TimelineRail(dotTop = 10)
        Text(
            text = month,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

@Composable
private fun TimelineAlbumRow(album: FeedAlbum, onClick: () -> Unit) {
    val dayFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
    ) {
        TimelineRail(dotTop = 28, showDot = false)
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = album.picUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artistNames,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dayFormat.format(Date(album.publishTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
