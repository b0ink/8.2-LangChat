package com.example.langchat.API;

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

}
