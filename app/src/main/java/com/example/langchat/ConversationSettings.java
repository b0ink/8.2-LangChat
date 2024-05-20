package com.example.langchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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

    private Button btnAddUser;

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


        // Set the listener to detect item selection
        spnLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();

                saveLanguage(conversationId, selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do something when nothing is selected, if needed
            }
        });


        participants = new ArrayList<>();

        RecyclerView recycler = findViewById(R.id.participantRecyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        participantAdapter = new ParticipantAdapter(this, participants);
        recycler.setAdapter(participantAdapter);

        // retrieve users in conversation (conversationId)
        getParticipants(conversationId);

        getLanguages();


        btnAddUser = findViewById(R.id.btnAddUser);
        btnAddUser.setOnClickListener(view -> {
            showAddUserDialog(conversationId);
        });

    }

    public void showAddUserDialog(int conversationId) {
        // Inflate the custom layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_user, null);

        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        // Find the EditText in the custom layout
        EditText usernameEditText = dialogView.findViewById(R.id.etUsername);

        // Set up the dialog buttons
        builder.setPositiveButton("Add", (dialog, which) -> {
            // Retrieve the inputted username
            String username = usernameEditText.getText().toString().trim();

            // Work with the username (e.g., display a toast or save it)
            if (!username.isEmpty()) {
                // Example: Display the username using a Toast
//                Toast.makeText(this, "Username: " + username, Toast.LENGTH_SHORT).show();
                addUserToConversation(conversationId, username);

                // TODO: Add your code here to handle the username (e.g., save to a database)
            } else {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Cancel the dialog
            dialog.dismiss();
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addUserToConversation(int conversationId, String username) {
        Call<String> call = RetrofitClient.getInstance()
                .getAPI().addParticipant(authManager.getToken(), conversationId, username);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if(response.code() == 404){
                        Toast.makeText(ConversationSettings.this, "User does not exist", Toast.LENGTH_SHORT).show();
                    }else if(response.code() == 401){
                        Toast.makeText(ConversationSettings.this, "User is already part of this conversation", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(ConversationSettings.this, "Error adding user", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                User newUser = new User(username, null);
                participants.add(newUser);
                participantAdapter.notifyItemInserted(participants.indexOf(newUser));
                if (response.body() != null) {
                    Toast.makeText(ConversationSettings.this, response.body(), Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {
                Toast.makeText(ConversationSettings.this, "Unable to add user, please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void getParticipants(int conversationId) {
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

                // If user object contains preferred language, set spinner item
                // First participant will be the calling/logged in user
                if (participants.get(0).getPreferredLanguage() != null) {
                    spnLanguage.setSelection(availableLanguages.indexOf(participants.get(0).getPreferredLanguage()));
                }

//                participants.add(new User("Add user to conversation", null));


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

    private void saveLanguage(int conversationId, String language) {
        Call<Boolean> call = RetrofitClient.getInstance()
                .getAPI().saveConversationsLanguage(authManager.getToken(), conversationId, language);

        call.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("Invalid response from saveLanguage");
                    return;
                }
                System.out.println("Success save lang?: " + response.body());

                if (response.body() == true) {
                    Toast.makeText(ConversationSettings.this, "Successfully saved language to: " + language, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable throwable) {
                Toast.makeText(ConversationSettings.this, "Unable to save language, please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}