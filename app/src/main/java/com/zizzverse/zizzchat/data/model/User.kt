package com.zizzverse.zizzchat.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePic: String = "",
    val status: String = "Hey there! I am using ZizzChat",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)
