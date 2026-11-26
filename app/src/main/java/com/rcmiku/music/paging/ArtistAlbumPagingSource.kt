package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.artist.ArtistApi
import com.rcmiku.ncmapi.model.Album

class ArtistAlbumPagingSource(
    private val id: Long,
    private val oldestFirst: Boolean = false
) : PagingSource<Int, Album>() {
    override fun getRefreshKey(state: PagingState<Int, Album>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Album> {
        if (oldestFirst) return loadAllOldestFirst()
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize
            val response = ArtistApi.artistAlbum(id = id, limit = limit, offset = offset)
            if (response.isSuccess) {
                val artistAlbum = response.getOrThrow()
                val data = artistAlbum.hotAlbums
                val nextKey = if (artistAlbum.more) offset + limit else null
                LoadResult.Page(
                    data = data,
                    prevKey = if (offset == 0) null else offset - limit,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Error(Exception("Load data filed"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /** The API only pages newest-first, so oldest-first needs every page before sorting. */
    private suspend fun loadAllOldestFirst(): LoadResult<Int, Album> {
        return try {
            val all = mutableListOf<Album>()
            var offset = 0
            do {
                val page = ArtistApi.artistAlbum(id = id, limit = PAGE_SIZE, offset = offset).getOrThrow()
                all += page.hotAlbums
                offset += PAGE_SIZE
            } while (page.more && page.hotAlbums.isNotEmpty())
            LoadResult.Page(
                data = all.distinctBy { it.id }.sortedBy { it.publishTime },
                prevKey = null,
                nextKey = null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private companion object {
        const val PAGE_SIZE = 100
    }
}
