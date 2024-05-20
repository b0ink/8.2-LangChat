package com.example.langchat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.langchat.API.AuthManager;
import com.example.langchat.API.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationSettings extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";
//    public final String EXTRA = "extra_conversation_id";

    private Spinner spnLanguage;

    private AuthManager authManager;


    private ArrayList<String> availableLanguages;
    private ArrayAdapter<String> languageAdapter;

    private ArrayList<User> participants;
    private ParticipantAdapter participantAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_conversation_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authManager = new AuthManager(this);

        Intent intent = getIntent();
        int conversationId = intent.getIntExtra(EXTRA_CONVERSATION_ID, -1);
        if (intent == null || !intent.hasExtra(EXTRA_CONVERSATION_ID) || conversationId == -1) {
            Intent homeActivity = new Intent(this, MainActivity.class);
            startActivity(homeActivity);
            finish();
            return;
        }

        spnLanguage = findViewById(R.id.spnLanguage);
        availableLanguages = new ArrayList<>();

        languageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableLanguages);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnLanguage.setAdapter(languageAdapter);


        getLanguages();


        participants = new ArrayList<>();

        RecyclerView recycler = findViewById(R.id.participantRecyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        participantAdapter = new ParticipantAdapter(this, participants);
        recycler.setAdapter(participantAdapter);

        // retrieve users in conversation (conversationId)
        getParticipants(conversationId);

    }

    private void getParticipants(int conversationId){
        Call<List<User>> call = RetrofitClient.getInstance()
                .getAPI().getParticipants(authManager.getToken(), conversationId);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("Invalid response from getParticipants");
                    return;
                }
                System.out.println("participants" + response.body());
                participants.addAll(response.body());

                participants.add(new User("Add user to conversation"));

                participantAdapter.notifyDataSetChanged();

            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable throwable) {

            }
        });
    }


    private void getLanguages() {
        Call<List<String>> call = RetrofitClient.getInstance()
                .getAPI().getAvailableLanguages(authManager.getToken());

        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("Invalid response from getAvailableLanguages");
                    return;
                }
                System.out.println(response.body());
                availableLanguages.addAll(response.body());
                languageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable throwable) {

            }
        });
    }
}