package com.rcmiku.ncmapi.model

import kotlinx.serialization.Serializable

@Serializable
data class CommentUser(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String = ""
)

@Serializable
data class CommentReplied(
    val user: CommentUser = CommentUser(),
    val content: String? = null
)

@Serializable
data class Comment(
    val commentId: Long = 0,
    val content: String = "",
    val time: Long = 0,
    val likedCount: Int = 0,
    val user: CommentUser = CommentUser(),
    val beReplied: List<CommentReplied> = emptyList()
)

@Serializable
data class CommentResponse(
    val total: Int = 0,
    val more: Boolean = false,
    val hotComments: List<Comment> = emptyList(),
    val comments: List<Comment> = emptyList()
)
