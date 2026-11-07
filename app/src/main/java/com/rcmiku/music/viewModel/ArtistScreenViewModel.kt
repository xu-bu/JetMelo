package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.rcmiku.music.paging.ArtistAlbumPagingSource
import com.rcmiku.music.utils.AlbumFeed
import com.rcmiku.ncmapi.api.artist.ArtistApi
import com.rcmiku.ncmapi.model.ArtistDynamicResponse
import com.rcmiku.ncmapi.model.ArtistHeadInfoResponse
import com.rcmiku.ncmapi.model.ArtistTopSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val artistId = savedStateHandle.get<Long>("artistId")

    private val _artistHeadInfo =
        MutableStateFlow<ArtistHeadInfoResponse?>(null)
    val artistHeadInfo: StateFlow<ArtistHeadInfoResponse?> =
        _artistHeadInfo.asStateFlow()
    private val _artistTopSong =
        MutableStateFlow<ArtistTopSong?>(null)
    val artistTopSong: StateFlow<ArtistTopSong?> =
        _artistTopSong.asStateFlow()

    private val _collectedAlbumIds = MutableStateFlow<Set<Long>>(emptySet())
    val collectedAlbumIds: StateFlow<Set<Long>> = _collectedAlbumIds.asStateFlow()

    private val _artistDynamic = MutableStateFlow<ArtistDynamicResponse?>(null)
    val artistDynamic: StateFlow<ArtistDynamicResponse?> = _artistDynamic.asStateFlow()

    private fun fetchArtistDynamic() {
        viewModelScope.launch {
            artistId?.let { _artistDynamic.value = ArtistApi.artistDynamic(it).getOrNull() }
        }
    }

    fun toggleFollow() {
        val id = artistId ?: return
        val followed = _artistDynamic.value?.followed ?: return
        viewModelScope.launch {
            ArtistApi.artistSub(id, follow = !followed).onSuccess { fetchArtistDynamic() }
        }
    }

    init {
        fetchArtistDynamic()
        viewModelScope.launch {
            AlbumFeed.loadCollectedAlbumIds()?.let { _collectedAlbumIds.value = it }
        }
        viewModelScope.launch {
            artistId?.let {
                _artistHeadInfo.value = ArtistApi.artistHeadInfo(it).getOrNull()
                _artistTopSong.value = ArtistApi.artistTopSong(it).getOrNull()
            }
        }
    }

    val artistAlbumList = artistId?.let { id ->
        Pager(
            config = PagingConfig(
                pageSize = 100,
                prefetchDistance = 50,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { ArtistAlbumPagingSource(id) }
        ).flow.cachedIn(viewModelScope)
    } ?: flowOf()

}