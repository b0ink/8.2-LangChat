package com.example.langchat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

    private Spinner spnLanguage;

    private AuthManager authManager;


    private ArrayList<String> availableLanguages;
    private ArrayAdapter<String> languageAdapter;

    private ArrayList<User> participants;
    private ParticipantAdapter participantAdapter;

    private Button btnAddUser;
    private ImageButton btnGoBack;
    private Button btnLeaveConversation;


    private String selectedLanguage = "";

    private int conversationId = -1;

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
        conversationId = intent.getIntExtra(EXTRA_CONVERSATION_ID, -1);
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


        participants = new ArrayList<>();

        RecyclerView recycler = findViewById(R.id.participantRecyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        participantAdapter = new ParticipantAdapter(this, participants, conversationId);
        recycler.setAdapter(participantAdapter);

        // Set the listener to detect item selection
        spnLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();

                if (selectedLanguage.isEmpty()) {
                    selectedLanguage = selectedItem;
                    return;
                }

                if (selectedItem.equals(selectedLanguage)) {
                    return;
                }

                saveLanguage(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do something when nothing is selected, if needed
            }
        });

        getLanguages();


        btnAddUser = findViewById(R.id.btnAddUser);
        btnAddUser.setOnClickListener(view -> {
            showAddUserDialog();
        });

        btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(view -> {
            startActivity(new Intent(this, MessageActivity.class).putExtra(MessageActivity.EXTRA_CONVERSATION_ID, conversationId));
            finish();
        });

        btnLeaveConversation = findViewById(R.id.btnLeaveConversation);
        btnLeaveConversation.setOnClickListener(view -> {
            showLeaveConversationConfirmationBox();
        });

    }

    private void showLeaveConversationConfirmationBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you really want to leave the conversation?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked Yes button
                        leaveConversation();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked No button
                        dialog.dismiss(); // close the dialog
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void leaveConversation() {
        int removingUserid = Integer.valueOf(authManager.getJwtProperty("id"));

        Call<String> call = RetrofitClient.getInstance()
                .getAPI().removeUser(authManager.getToken(), conversationId, removingUserid);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("Invalid response from leaveConversation");
                    return;
                }

                if (response.code() == 203) {
                    Toast.makeText(ConversationSettings.this, "You have left the conversation", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ConversationSettings.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {

            }
        });
    }

    public void showAddUserDialog() {
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
                addUserToConversation(username);
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

    private void addUserToConversation(String username) {
        Call<NewConversationResponse> call = RetrofitClient.getInstance()
                .getAPI().addParticipant(authManager.getToken(), conversationId, username);

        call.enqueue(new Callback<NewConversationResponse>() {
            @Override
            public void onResponse(Call<NewConversationResponse> call, Response<NewConversationResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (response.code() == 404) {
                        Toast.makeText(ConversationSettings.this, "User does not exist", Toast.LENGTH_SHORT).show();
                    } else if (response.code() == 401) {
                        Toast.makeText(ConversationSettings.this, "User is already part of this conversation", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ConversationSettings.this, "Error adding user", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                final int newConversationId = response.body().getConversationId();
                if (newConversationId != conversationId) {
                    // new conversation has been created, start new settings activity
                    Toast.makeText(ConversationSettings.this, "A new group chat has been created!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ConversationSettings.this, ConversationSettings.class);
                    intent.putExtra(EXTRA_CONVERSATION_ID, newConversationId);
                    startActivity(intent);
                    finish();
                    return;
                }

                User newUser = new User(-1, username, null, false, null);
                participants.add(newUser);
                participantAdapter.notifyItemInserted(participants.indexOf(newUser));
                if (response.body() != null) {
                    Toast.makeText(ConversationSettings.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<NewConversationResponse> call, Throwable throwable) {
                Toast.makeText(ConversationSettings.this, "Unable to add user, please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void getParticipants() {
        Call<List<User>> call = RetrofitClient.getInstance()
                .getAPI().getParticipants(authManager.getToken(), conversationId);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("Invalid response from getParticipants");
                    return;
                }

                participants.addAll(response.body());

                // If user object contains preferred language, set spinner item
                // First participant will be the calling/logged in user

                String language = participants.get(0).getPreferredLanguage();
                if (language != null) {
                    spnLanguage.setSelection(availableLanguages.indexOf(language), false);
                    selectedLanguage = language;
                }
                if (participants.get(0).isAdmin()) {
                    participantAdapter.viewAdminControls = true;
                }
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
                availableLanguages.addAll(response.body());
                languageAdapter.notifyDataSetChanged();

                // retrieve users in conversation (conversationId)
                getParticipants();
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable throwable) {

            }
        });
    }

    private void saveLanguage(String language) {
        Call<Boolean> call = RetrofitClient.getInstance()
                .getAPI().saveConversationsLanguage(authManager.getToken(), conversationId, language);

        call.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("Invalid response from saveLanguage");
                    return;
                }

                if (response.body() == true) {
                    Toast.makeText(ConversationSettings.this, "Successfully saved language to: " + language, Toast.LENGTH_SHORT).show();
                    selectedLanguage = language;
                }

            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable throwable) {
                Toast.makeText(ConversationSettings.this, "Unable to save language, please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}