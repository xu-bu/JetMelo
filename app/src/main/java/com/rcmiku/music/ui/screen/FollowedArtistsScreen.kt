package com.rcmiku.music.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rcmiku.music.R
import com.rcmiku.music.ui.components.ArtistListItem
import com.rcmiku.music.ui.navigation.ArtistNav
import com.rcmiku.music.viewModel.FollowedArtistsViewModel
import com.rcmiku.music.ui.components.RefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedArtistsScreen(
    navController: NavHostController,
    viewModel: FollowedArtistsViewModel = hiltViewModel()
) {
    val artists by viewModel.artists.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val failed by viewModel.failed.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.followed_artists)) },
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            RefreshBox(
                onRefresh = { viewModel.load().join() },
                modifier = Modifier.fillMaxSize()
            ) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(artists, key = { it.id }) { artist ->
                    ArtistListItem(
                        artist = artist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(ArtistNav(artistId = artist.id)) }
                    )
                }
            }
}
            when {
                failed -> TextButton(
                    onClick = { viewModel.load() },
                    modifier = Modifier.align(Alignment.Center)
                ) { Text(stringResource(R.string.load_failed_retry)) }

                loading && artists.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                !loading && artists.isEmpty() -> Text(
                    text = stringResource(R.string.no_followed_artists),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
