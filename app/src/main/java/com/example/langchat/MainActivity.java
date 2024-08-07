package com.example.langchat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.langchat.API.AuthManager;
import com.example.langchat.API.RetrofitClient;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private ArrayList<ConversationResponse> conversations;
    private ConversationAdapter adapter;

    private AuthManager authManager;
    private Button btnLogout;
    private ImageButton btnNewMessage;

    private ImageFilterView btnProfile;

    private Handler handler;
    private Runnable runnable;

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
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        authManager = new AuthManager(this);
        if (authManager.getToken() == null || !authManager.isTokenValid()) {
            startActivity(new Intent(this, LoginActivity.class));
            System.out.println("Invalid token, logging out");
            authManager.logout();
            finish();
            return;
        }

        btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(view -> {
            startActivity(new Intent(this, ProfileSettings.class));
            finish();
            return;
        });


        btnNewMessage = findViewById(R.id.btnNewMessage);
        btnNewMessage.setOnClickListener(view -> {
            showAddUserDialog("Enter a username to start a conversation!");
        });


        conversations = new ArrayList<>();

        RecyclerView recycler = findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new ConversationAdapter(this, conversations);
        recycler.setAdapter(adapter);

        retrieveConversations();

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                retrieveConversations();
                handler.postDelayed(this, 5000); // 5000 milliseconds = 5 seconds
            }
        };

        // Start the repeated task
        handler.post(runnable);

        getAvatar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
    }

    public void showAddUserDialog(String prompt) {
        // Inflate the custom layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_user, null);

        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        // Find the EditText in the custom layout
        EditText usernameEditText = dialogView.findViewById(R.id.etUsername);
        TextView tvDialogPrompt = dialogView.findViewById(R.id.tvDialogPrompt);
        tvDialogPrompt.setText(prompt);

        // Set up the dialog buttons
        builder.setPositiveButton("Add", (dialog, which) -> {
            // Retrieve the inputted username
            String username = usernameEditText.getText().toString().trim();

            // Work with the username (e.g., display a toast or save it)
            if (!username.isEmpty()) {
                startNewConversation(username);
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

    private void startNewConversation(String username) {
        Call<NewConversationResponse> call = RetrofitClient.getInstance()
                .getAPI().createConversation(authManager.getToken(), username);

        call.enqueue(new Callback<NewConversationResponse>() {
            @Override
            public void onResponse(Call<NewConversationResponse> call, Response<NewConversationResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (response.code() == 404) {
                        Toast.makeText(MainActivity.this, "User does not exist", Toast.LENGTH_SHORT).show();
                    }
//                    else if (response.code() == 401) {
//                        Toast.makeText(MainActivity.this, "User is already part of this conversation", Toast.LENGTH_SHORT).show();
//                    }
                    else {
                        Toast.makeText(MainActivity.this, "Error starting new conversation", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                int conversationId = response.body().getConversationId();

                if (conversationId != -1) {
//                    Toast.makeText(MainActivity.this, response.body().toString(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, MessageActivity.class);
                    intent.putExtra(MessageActivity.EXTRA_CONVERSATION_ID, conversationId);
                    intent.putExtra(MessageActivity.EXTRA_USERNAME_DISPLAY, username);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "Could not start conversation. Please try again later.", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<NewConversationResponse> call, Throwable throwable) {
                Toast.makeText(MainActivity.this, "Unable to add user, please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAvatar() {
        Call<String> call = RetrofitClient.getInstance()
                .getAPI().getAvatar(authManager.getToken());

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                if (response.body() == null || response.body().isEmpty()) {
                    return;
                }

                Bitmap avatar = ImageUtil.convert(response.body());
                btnProfile.setImageBitmap(avatar);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e("Upload", "Failure: " + t.getMessage());
            }
        });
    }

    private void retrieveConversations() {
        Call<List<ConversationResponse>> call = RetrofitClient.getInstance()
                .getAPI().getUsersConversations(authManager.getToken());

        call.enqueue(new Callback<List<ConversationResponse>>() {
            @Override
            public void onResponse(Call<List<ConversationResponse>> call, Response<List<ConversationResponse>> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                conversations.clear();
                conversations.addAll(response.body());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<List<ConversationResponse>> call, Throwable throwable) {

            }
        });
    }
}