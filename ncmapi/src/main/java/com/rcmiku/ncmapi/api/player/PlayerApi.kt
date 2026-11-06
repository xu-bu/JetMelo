package com.rcmiku.ncmapi.api.player

import com.rcmiku.ncmapi.api.apiGet
import com.rcmiku.ncmapi.model.LyricResponse
import com.rcmiku.ncmapi.model.SongUrlResponse

object PlayerApi {

    suspend fun songPlayUrlV1(songId: String, songLevel: SongLevel = SongLevel.STANDARD): Result<SongUrlResponse> =
        apiGet(
            "/song/url/v1",
            mapOf("id" to songId.substringBefore('?'), "level" to songLevel.value)
        )

    suspend fun songLyric(musicId: Long): Result<LyricResponse> =
        apiGet("/lyric", mapOf("id" to musicId))
}
