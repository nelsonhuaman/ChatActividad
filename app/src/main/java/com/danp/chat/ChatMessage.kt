package com.danp.chat

data class ChatMessage(
    val id: String = "",
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOwnMessage: Boolean = false
)