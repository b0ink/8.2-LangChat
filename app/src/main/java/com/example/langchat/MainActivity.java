package com.example.langchat;

import android.os.Bundle;
import android.widget.Adapter;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.langchat.API.RetrofitClient;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ArrayList<ConversationResponse> conversations = new ArrayList<>();

        RecyclerView recycler = findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        ConversationAdapter adapter = new ConversationAdapter(this, conversations);
        recycler.setAdapter(adapter);

        Call<List<ConversationResponse>> call = RetrofitClient.getInstance()
                .getAPI().getUsersConversations(1);

        call.enqueue(new Callback<List<ConversationResponse>>() {
            @Override
            public void onResponse(Call<List<ConversationResponse>> call, Response<List<ConversationResponse>> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                conversations.addAll(response.body());
                adapter.notifyDataSetChanged();

                for (ConversationResponse convo : conversations) {
                    System.out.println("---- CONVO -----");
                    for (Participant user : convo.getParticipants()) {
                        System.out.println("Participants: " + user.getUser().getUsername());
                    }
                    LastMessage lastMessage = convo.getLastMessage();
                    if (lastMessage != null) {
                        // TODO; check local cache for lastMessage.getID() and the translation
                        System.out.println("Last message: " + lastMessage.getMessage());
                    } else {
                        System.out.println("NULL LAST MESAGES");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ConversationResponse>> call, Throwable throwable) {

            }
        });
    }
}