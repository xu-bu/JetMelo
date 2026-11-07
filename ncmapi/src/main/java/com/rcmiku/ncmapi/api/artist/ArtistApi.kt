package com.rcmiku.ncmapi.api.artist

import com.rcmiku.ncmapi.api.apiGet
import com.rcmiku.ncmapi.model.*

object ArtistApi {
    suspend fun artistHeadInfo(artistId: Long): Result<ArtistHeadInfoResponse> =
        apiGet("/artist/detail", mapOf("id" to artistId))

    suspend fun artistTopSong(artistId: Long): Result<ArtistTopSong> =
        apiGet("/artist/top/song", mapOf("id" to artistId))

    suspend fun artistAlbum(id: Long, limit: Int, offset: Int): Result<ArtistAlbumResponse> =
        apiGet("/artist/album", mapOf("id" to id, "limit" to limit, "offset" to offset))

    suspend fun artistDynamic(artistId: Long): Result<ArtistDynamicResponse> =
        apiGet("/artist/detail/dynamic", mapOf("id" to artistId))

    suspend fun artistSub(artistId: Long, follow: Boolean): Result<ApiCodeResponse> =
        apiGet<ApiCodeResponse>(
            "/artist/sub",
            mapOf("id" to artistId, "t" to if (follow) 1 else 0)
        ).mapCatching {
            if (it.code == 200) it else throw IllegalStateException(
                it.message?.takeIf(String::isNotBlank) ?: it.msg?.takeIf(String::isNotBlank)
                    ?: "服务端返回业务码 ${it.code}"
            )
        }
}
