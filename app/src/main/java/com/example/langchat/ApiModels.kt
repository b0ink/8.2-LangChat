package com.example.langchat

data class User(
    val username: String
)

data class Participant(
    val id: Int,
    val conversation_id: Int,
    val user_id: Int,
    val createdAt: String,
    val updatedAt: String,
    val user: User
)

data class LastMessage(
    val id: Int,
    val conversation_id: Int,
    val sender_id: Int,
    val message: String,
    val createdAt: String,
    val updatedAt: String,
    val user: User
)

data class ConversationResponse(
    val participants: List<Participant>,
    val lastMessage: LastMessage
)

