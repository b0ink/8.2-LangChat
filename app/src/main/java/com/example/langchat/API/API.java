package com.example.langchat.API;

import com.example.langchat.API.models.ResponsePost;
import com.example.langchat.ConversationResponse;

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
            @Field("confirmPassword") String confirmPassword,
            @Field("mobile") String mobile
    );

}
