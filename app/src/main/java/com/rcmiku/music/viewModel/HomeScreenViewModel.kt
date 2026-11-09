package com.rcmiku.music.viewModel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.R
import com.rcmiku.music.utils.AlbumFeed
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.music.utils.FeedAlbum
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.album.AlbumApi
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
import kotlinx.coroutines.Job
import java.time.ZoneId
import java.time.ZonedDateTime

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

    fun collectFeedAlbum(album: FeedAlbum) {
        viewModelScope.launch {
            AlbumApi.albumSub(id = album.id, isSub = true)
                .onSuccess {
                    AlbumFeed.remove(context, album.id)
                    Toast.makeText(context, R.string.album_collected, Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(context, R.string.album_collect_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun rescanFeed() {
        AlbumFeed.scanNow(context)
    }

    private var songsJob: Job? = null
    private var songsLoadedAt = 0L

    private fun fetchRecommendSongs() {
        songsJob = viewModelScope.launch {
            val result = RecommendApi.recommendSongs()
            _recommendSongs.value = result
            if (result.isSuccess) songsLoadedAt = System.currentTimeMillis()
        }
    }

    /**
     * Called whenever the app comes to the foreground. The daily list changes at 06:00 (China time),
     * so reload when the loaded copy is older than the latest 06:00 or the last load failed.
     */
    fun refreshIfStale() {
        if (songsJob?.isActive == true) return
        if (songsLoadedAt < latestDailyUpdateMillis()) {
            fetchRecommendSongs()
            fetchFavoriteSongIds()
        }
    }

    private fun latestDailyUpdateMillis(): Long {
        val zone = ZoneId.of("Asia/Shanghai")
        val now = ZonedDateTime.now(zone)
        var boundary = now.toLocalDate().atTime(6, 0).atZone(zone)
        if (boundary.isAfter(now)) boundary = boundary.minusDays(1)
        return boundary.toInstant().toEpochMilli()
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