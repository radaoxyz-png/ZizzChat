package com.zizzverse.zizzchat.data.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)
