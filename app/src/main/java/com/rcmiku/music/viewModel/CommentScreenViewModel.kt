package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.ncmapi.api.comment.CommentApi
import com.rcmiku.ncmapi.model.Comment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** In-memory cache of loaded comment pages; entries expire five minutes after they were first loaded. */
object CommentCache {
    private const val TTL_MS = 5 * 60 * 1000L

    data class Entry(
        val hot: List<Comment>,
        val latest: List<Comment>,
        val total: Int,
        val hasMore: Boolean,
        val savedAt: Long
    )

    private val entries = HashMap<String, Entry>()

    @Synchronized
    fun get(key: String): Entry? {
        val entry = entries[key] ?: return null
        if (System.currentTimeMillis() - entry.savedAt >= TTL_MS) {
            entries.remove(key)
            return null
        }
        return entry
    }

    /** Stores [state]; keeps the original load time so the five minutes run from the first request. */
    @Synchronized
    fun put(key: String, state: CommentUiState) {
        val savedAt = entries[key]?.savedAt ?: System.currentTimeMillis()
        entries[key] = Entry(state.hot, state.latest, state.total, state.hasMore, savedAt)
    }
}

data class CommentUiState(
    val hot: List<Comment> = emptyList(),
    val latest: List<Comment> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val failed: Boolean = false
)

@HiltViewModel
class CommentScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val resourceId = savedStateHandle.get<Long>("resourceId") ?: 0L
    private val isAlbum = savedStateHandle.get<Boolean>("isAlbum") ?: false
    private val cacheKey = (if (isAlbum) "album_" else "song_") + resourceId

    private val _state = MutableStateFlow(CommentUiState())
    val state: StateFlow<CommentUiState> = _state.asStateFlow()

    init {
        val cached = CommentCache.get(cacheKey)
        if (cached != null) {
            _state.value = CommentUiState(
                hot = cached.hot,
                latest = cached.latest,
                total = cached.total,
                hasMore = cached.hasMore
            )
        } else {
            loadMore()
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || !current.hasMore || resourceId == 0L) return
        _state.value = current.copy(loading = true, failed = false)
        viewModelScope.launch {
            val request = if (isAlbum)
                CommentApi.albumComments(resourceId, PAGE_SIZE, current.latest.size)
            else
                CommentApi.songComments(resourceId, PAGE_SIZE, current.latest.size)
            request
                .onSuccess { response ->
                    val next = current.copy(
                        hot = if (current.latest.isEmpty()) response.hotComments else current.hot,
                        latest = current.latest + response.comments,
                        total = response.total,
                        hasMore = response.more && response.comments.isNotEmpty(),
                        loading = false,
                        failed = false
                    )
                    _state.value = next
                    CommentCache.put(cacheKey, next)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, failed = true)
                }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
