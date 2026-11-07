package com.rcmiku.music.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.utils.AlbumFeed
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.music.utils.FeedAlbum
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.recommend.RecommendApi
import com.rcmiku.ncmapi.model.DailySongsResponse
import com.rcmiku.ncmapi.model.PersonalizedPlaylistResponse
import com.rcmiku.ncmapi.model.RecommendPlaylistResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {

    private val _recommendSongs =
        MutableStateFlow<Result<DailySongsResponse>?>(null)
    val recommendSongs: StateFlow<Result<DailySongsResponse>?> =
        _recommendSongs.asStateFlow()
    private val _personalizedPlaylist =
        MutableStateFlow<Result<PersonalizedPlaylistResponse>?>(null)
    val personalizedPlaylist: StateFlow<Result<PersonalizedPlaylistResponse>?> =
        _personalizedPlaylist.asStateFlow()
    private val _recommendPlaylist =
        MutableStateFlow<Result<RecommendPlaylistResponse>?>(null)
    val recommendPlaylist: StateFlow<Result<RecommendPlaylistResponse>?> =
        _recommendPlaylist.asStateFlow()

    val newAlbums: StateFlow<List<FeedAlbum>> = AlbumFeed.feed(context)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val feedScanning: StateFlow<Boolean> = AlbumFeed.scanning
    val feedLastScan: StateFlow<Long> = AlbumFeed.lastScan(context)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    fun rescanFeed() {
        AlbumFeed.scanNow(context)
    }

    private fun fetchRecommendSongs() {
        viewModelScope.launch {
            _recommendSongs.value = RecommendApi.recommendSongs()
        }
    }

    private fun fetchFavoriteSongIds() {
        viewModelScope.launch {
            AccountApi.favoriteSongIds().getOrNull()?.ids?.let {
                FavoriteSongIdsUtil.updateSongIds(context, it)
            }
        }
    }

    private fun fetchRecommendPlaylist() {
        viewModelScope.launch {
            _recommendPlaylist.value = RecommendApi.recommendPlaylist()
        }
    }

    private fun fetchPersonalizedPlaylist() {
        viewModelScope.launch {
            _personalizedPlaylist.value = RecommendApi.personalizedPlaylist()
        }
    }

    init {
        fetchRecommendSongs()
        fetchFavoriteSongIds()
    }

    fun refresh() {
        fetchRecommendSongs()
        fetchFavoriteSongIds()
    }

}