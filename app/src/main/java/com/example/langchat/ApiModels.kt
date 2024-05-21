package com.example.langchat

data class User(
    val username: String,
    val preferredLanguage: String?
    //TODO: profile picture url?
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
    val message: String
)

data class Message(
    val id: Int,
    val conversation_id: Int,
    val sender_id: Int,
    var message: String,
    val createdAt: String,
    val updatedAt: String,
    val user: User,
    val translations: List<Translation>
)

data class ConversationResponse(
    val id: Int,
    val participants: List<Participant>,
    val lastMessage: Message,
    val lastUpdatedDisplay: String
)

