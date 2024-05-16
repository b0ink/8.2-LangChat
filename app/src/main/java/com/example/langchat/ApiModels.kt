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

data class Translation(
    val id: Int,
    val message_id: Int,
    val language: String,
    val message: Message
)

data class Message(
    val id: Int,
    val conversation_id: Int,
    val sender_id: Int,
    var message: String,
    val createdAt: String,
    val updatedAt: String,
    val user: User
)

data class ConversationResponse(
    val id: Int,
    val participants: List<Participant>,
    val lastMessage: Message
)

