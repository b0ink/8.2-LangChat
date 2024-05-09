package com.example.langchat.API;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import com.example.langchat.API.models.ConversationResponse;
import com.example.langchat.API.models.ResponsePost;

import java.util.List;

public interface API {
    @FormUrlEncoded
    @POST("users/conversations")
    Call<List<ConversationResponse>> getUsersConversations(
            @Field("user_id") int user_id
    );

}
