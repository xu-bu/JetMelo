package com.rcmiku.ncmapi.api.comment

import com.rcmiku.ncmapi.api.apiClient
import com.rcmiku.ncmapi.api.requireSuccess
import com.rcmiku.ncmapi.model.CommentResponse
import com.rcmiku.ncmapi.utils.json
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

object CommentApi {

    /**
     * Song comments through the public, unencrypted GET endpoint.
     * `hotComments` are only returned on the first page (offset = 0).
     */
    suspend fun songComments(songId: Long, limit: Int = 20, offset: Int = 0): Result<CommentResponse> =
        resourceComments("R_SO_4_$songId", limit, offset)

    /** Album comments; same endpoint, album thread prefix. */
    suspend fun albumComments(albumId: Long, limit: Int = 20, offset: Int = 0): Result<CommentResponse> =
        resourceComments("R_AL_3_$albumId", limit, offset)

    private suspend fun resourceComments(threadId: String, limit: Int, offset: Int): Result<CommentResponse> =
        runCatching {
            val response = apiClient.get("https://music.163.com/api/v1/resource/comments/$threadId") {
                parameter("limit", limit)
                parameter("offset", offset)
            }
            val body = response.bodyAsText()
            response.requireSuccess(body)
            json.decodeFromString<CommentResponse>(body)
        }
}
