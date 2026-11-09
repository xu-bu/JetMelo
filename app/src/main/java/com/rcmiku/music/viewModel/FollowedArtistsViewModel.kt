package com.rcmiku.music.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.SearchArtist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job

@HiltViewModel
class FollowedArtistsViewModel @Inject constructor() : ViewModel() {

    private val _artists = MutableStateFlow<List<SearchArtist>>(emptyList())
    val artists: StateFlow<List<SearchArtist>> = _artists.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    init {
        load()
    }

    fun load(): Job {
        return viewModelScope.launch {
            _loading.value = true
            _failed.value = false
            val all = mutableListOf<SearchArtist>()
            var offset = 0
            while (true) {
                val page = AccountApi.artistSublist(offset, 100).getOrNull()
                if (page == null) {
                    _failed.value = all.isEmpty()
                    break
                }
                all += page.data
                _artists.value = all.toList()
                if (!page.hasMore || page.data.isEmpty()) break
                offset += 100
            }
            _loading.value = false
        }
    }
}
