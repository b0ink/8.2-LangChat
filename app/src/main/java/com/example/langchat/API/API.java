package com.example.langchat.API;

import com.example.langchat.API.models.ResponsePost;
import com.example.langchat.ConversationResponse;
import com.example.langchat.Message;
import com.example.langchat.NewConversationResponse;
import com.example.langchat.User;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


import java.util.List;
import retrofit2.http.Header;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface API {

    // Login user
    @FormUrlEncoded
    @POST("users/login")
    Call<ResponsePost> loginUser(
            @Field("username") String username,
            @Field("password") String password
    );

    // Create new user
    @FormUrlEncoded
    @POST("users/register")
    Call<ResponsePost> createUser(
            @Field("username") String username,
            @Field("email") String email,
            @Field("confirmEmail") String confirmEmail,
            @Field("password") String password,
            @Field("confirmPassword") String confirmPassword
    );

//    @FormUrlEncoded
//    @POST("conversation/messages")
//    Call<List<Message>> getMessages(
//            @Header("Authorization") String token,
//            @Field("conversation_id") int conversation_id
//    );


    // TODO: format to @GET("conversation/{conversationId}/messages") instead?

    // Get messages for conversation
    @GET("conversation/{conversationId}/messages/{lastMessageId}")
    Call<List<Message>> getMessages(
            @Header("Authorization") String token,
            @Path("conversationId") int conversationId,
            @Path("lastMessageId") int lastMessageId
    );


    // Get users conversations
    @GET("users/conversations")
    Call<List<ConversationResponse>> getUsersConversations(
            @Header("Authorization") String token
    );

    // Send message to converesation
    @FormUrlEncoded
    @POST("conversation/{conversationId}/send-message")
    Call<Message> sendMessage(
            @Header("Authorization") String token,
//            @Field("sender_id") int sender_id, // temporary
            @Path("conversationId") int conversationId,
            @Field("message") String message
    );


    // Translate a message
    @FormUrlEncoded
    @POST("conversation/translate")
    Call<Message> translateMessage(
            @Header("Authorization") String token,
            @Field("messageId") int messageId,
            @Field("usersLanguage") String usersLanguage
    );

    // Get all languages
    @GET("languages")
    Call<List<String>> getAvailableLanguages(
            @Header("Authorization") String token
    );

    // Save preferred language for conversation
    @FormUrlEncoded
    @POST("conversation/save-language")
    Call<Boolean> saveConversationsLanguage(
            @Header("Authorization") String token,
            @Field("conversationId") int conversationId,
            @Field("language") String language
    );

    // Get participants in a conversation
    @GET("conversation/{conversationId}/participants")
    Call<List<User>> getParticipants(
            @Header("Authorization") String token,
            @Path("conversationId") int conversationId
    );

    // Add user to conversation
    @FormUrlEncoded
    @POST("conversation/{conversationId}/add-participant")
    Call<NewConversationResponse> addParticipant(
            @Header("Authorization") String token,
            @Path("conversationId") int conversationId,
            @Field("username") String username
    );

    // Start new conversation with user
    // Returns the conversationId, either an existing conversation id or a new one
    @FormUrlEncoded
    @POST("conversation/new")
    Call<NewConversationResponse> createConversation(
            @Header("Authorization") String token,
            @Field("recipientsUsername") String recipientsUsername
    );

}