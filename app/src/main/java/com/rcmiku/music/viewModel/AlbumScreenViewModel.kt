package com.rcmiku.music.viewModel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.utils.AlbumFeed
import com.rcmiku.ncmapi.api.album.AlbumApi
import com.rcmiku.ncmapi.api.artist.ArtistApi
import com.rcmiku.ncmapi.model.Album
import com.rcmiku.ncmapi.model.AlbumDetailResponse
import com.rcmiku.ncmapi.model.AlbumInfoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val albumId = savedStateHandle.get<Long>("albumId")

    private val _albumDetail =
        MutableStateFlow<Result<AlbumDetailResponse>?>(null)
    val albumDetail: StateFlow<Result<AlbumDetailResponse>?> =
        _albumDetail.asStateFlow()
    private val _albumInfo =
        MutableStateFlow<AlbumInfoResponse?>(null)
    val albumInfo: StateFlow<AlbumInfoResponse?> =
        _albumInfo.asStateFlow()

    init {
        viewModelScope.launch {
            albumId?.let {
                _albumDetail.value = AlbumApi.albumDetail(albumId)
                fetchAlbumInfo()
            }
        }
    }

    private fun fetchAlbumInfo() {
        viewModelScope.launch {
            albumId?.let {
                _albumInfo.value = AlbumApi.albumInfo(albumId).getOrNull()
            }
        }
    }

    fun albumSub(isSub: Boolean) {
        viewModelScope.launch {
            albumId?.let {
                AlbumApi.albumSub(id = albumId, isSub = isSub).onSuccess {
                    fetchAlbumInfo()
                }
            }
        }
    }

    // ----- "听到此处": collect this album and un-collect the artist's older albums (no prompts) -----

    data class ListenedHereState(val working: Boolean = false)

    private val _listenedHere = MutableStateFlow(ListenedHereState())
    val listenedHere: StateFlow<ListenedHereState> = _listenedHere.asStateFlow()

    /** Only albums with exactly one artist offer this action. */
    fun listenedHereArtistId(album: Album): Long? =
        album.artists.ifEmpty { listOf(album.artist) }.singleOrNull()?.id

    fun listenedHere(album: Album) {
        val artistId = listenedHereArtistId(album) ?: return
        if (_listenedHere.value.working) return
        _listenedHere.value = ListenedHereState(working = true)
        viewModelScope.launch {
            try {
                val collected = AlbumFeed.loadCollectedAlbumIds() ?: return@launch
                val artistAlbums = loadAllAlbumsOf(artistId) ?: return@launch
                // Collect this album first: once it is collected the older ones no longer matter to the feed.
                if (AlbumApi.albumSub(id = album.id, isSub = true).getOrNull()?.code != 200) return@launch
                artistAlbums
                    .filter { it.publishTime < album.publishTime && it.id in collected }
                    .forEach { older ->
                        AlbumApi.albumSub(id = older.id, isSub = false)
                        delay(150)
                    }
                AlbumFeed.removeArtistUpTo(context, artistId, album.publishTime)
                fetchAlbumInfo()
            } finally {
                _listenedHere.value = ListenedHereState()
            }
        }
    }

    /** Every album of the artist, or null if a request failed. */
    private suspend fun loadAllAlbumsOf(artistId: Long): List<Album>? {
        val all = mutableListOf<Album>()
        var offset = 0
        while (offset < 2000) {
            val page = ArtistApi.artistAlbum(artistId, 50, offset).getOrNull() ?: return null
            all += page.hotAlbums
            if (!page.more || page.hotAlbums.isEmpty()) return all
            offset += 50
            delay(100)
        }
        return all
    }
}
