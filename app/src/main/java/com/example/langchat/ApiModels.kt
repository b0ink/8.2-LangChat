package com.example.langchat

data class User(
    val id: Int,
    val username: String,
    val preferredLanguage: String?,
    val isAdmin: Boolean,
    val avatar: String?
)

data class Participant(
    val id: Int,
    val conversation_id: Int,
    val user_id: Int,
    val createdAt: String,
    val updatedAt: String,
    val user: User,
    val isAdmin: Boolean
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
    val translations: List<Translation>?,
    val isTranscribed: Boolean
)

data class ConversationResponse(
    val id: Int,
    val participants: List<Participant>,
    val lastMessage: Message?,
    val lastUpdatedDisplay: String,
    val isGroupChat: Boolean,
)

data class NewConversationResponse(
    val conversationId: Int,
    val message: String
)
