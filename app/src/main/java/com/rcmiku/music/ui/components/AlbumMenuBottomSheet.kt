package com.rcmiku.music.ui.components

import android.content.Intent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rcmiku.music.R
import com.rcmiku.music.ui.icons.Artist
import com.rcmiku.music.ui.icons.LibraryAdd
import com.rcmiku.music.utils.FeedAlbum

/** Menu for an album of the "latest releases" feed: view artist, collect, share. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumMenuBottomSheet(
    album: FeedAlbum?,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit,
    onViewArtist: (FeedAlbum) -> Unit,
    onCollect: (FeedAlbum) -> Unit,
) {
    if (!openBottomSheet || album == null) return

    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(64.dp)
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = album.picUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = album.name,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = album.artistNames,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))

            AlbumMenuRow(
                icon = Artist,
                text = stringResource(R.string.view_artist),
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp
                ),
                onClick = {
                    onDismiss()
                    onViewArtist(album)
                }
            )
            AlbumMenuRow(
                icon = LibraryAdd,
                text = stringResource(R.string.library_add),
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    onDismiss()
                    onCollect(album)
                }
            )
            AlbumMenuRow(
                icon = Icons.Outlined.Share,
                text = stringResource(R.string.share),
                shape = RoundedCornerShape(
                    topStart = 8.dp, topEnd = 8.dp, bottomStart = 16.dp, bottomEnd = 16.dp
                ),
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://music.163.com/#/album?id=${album.id}")
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, context.getString(R.string.share_link))
                    )
                    onDismiss()
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AlbumMenuRow(
    icon: ImageVector,
    text: String,
    shape: CornerBasedShape,
    onClick: () -> Unit
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}
