package com.example.langchat.API;

import com.example.langchat.API.models.ResponsePost;
import com.example.langchat.ConversationResponse;
import com.example.langchat.Message;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


import java.util.List;

public interface API {
    @FormUrlEncoded
    @POST("users/conversations")
    Call<List<ConversationResponse>> getUsersConversations(
            @Field("user_id") int user_id
    );

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

    // Create new user
    @FormUrlEncoded
    @POST("conversation/messages")
    Call<List<Message>> getMessages(
            @Field("conversation_id") int conversation_id
    );

    // Create new user
    @FormUrlEncoded
    @POST("conversation/send-message")
    Call<Message> sendMessage(
            @Field("sender_id") int sender_id, // temporary
            @Field("conversation_id") int conversation_id,
            @Field("message") String message
    );


    @FormUrlEncoded
    @POST("conversation/translate")
    Call<Message> translateMessage(
            @Field("sender_id") int sender_id, // temporary
            @Field("messageId") int messageId,
            @Field("usersLanguage") String usersLanguage
    );

}
