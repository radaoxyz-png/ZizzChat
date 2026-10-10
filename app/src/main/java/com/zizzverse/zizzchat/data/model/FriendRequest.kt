package com.zizzverse.zizzchat.data.model

data class FriendRequest(
    val requestId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val timestamp: Long = 0L
)
