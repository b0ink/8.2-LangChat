package com.example.langchat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.langchat.API.AuthManager;
import com.example.langchat.API.RetrofitClient;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";
    public static final String EXTRA_USERNAME_DISPLAY = "extra_username_display";

    private EditText etMessage;
    private ImageButton btnSend;

    private ImageButton btnProfile;
    private ImageButton btnGoBack;
    private ImageButton btnSettings;
    private TextView tvUsername;

    private LocalDatabaseHelper databaseHelper;
    public ArrayList<Message> messages;
    public MessageAdapter adapter;
    public RecyclerView recycler;

    private AuthManager authManager;

    private Thread messageThread;
    private Channel channel;
    private Connection connection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_message);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnProfile = findViewById(R.id.btnProfile);

        btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(view -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });


        Intent intent = getIntent();
        int conversationId = intent.getIntExtra(EXTRA_CONVERSATION_ID, -1);
        if (intent == null || !intent.hasExtra(EXTRA_CONVERSATION_ID) || conversationId == -1) {
            Intent homeActivity = new Intent(this, MainActivity.class);
            startActivity(homeActivity);
            finish();
            return;
        }


        btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(view -> {
            Intent settings = new Intent(this, ConversationSettings.class);
            settings.putExtra(ConversationSettings.EXTRA_CONVERSATION_ID, conversationId);
            startActivity(settings);
            finish();
        });

        authManager = new AuthManager(this);
        databaseHelper = LocalDatabaseHelper.getInstance(this);

        tvUsername = findViewById(R.id.tvUsername);
//        String usernameDisplay = intent.getStringExtra(EXTRA_USERNAME_DISPLAY);
//        tvUsername.setText(usernameDisplay);

        messages = new ArrayList<>();

        recycler = findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new MessageAdapter(this, messages);
        recycler.setAdapter(adapter);

        // Set recipients username
        getParticipants(conversationId);

        getAllMessages(conversationId);

        messageThread = new Thread(() -> {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("10.0.2.2");
                factory.setPort(5672);
                connection = factory.newConnection();
                channel = connection.createChannel();
                String QueueName = "messages_" + conversationId + "_" + authManager.getJwtProperty("id");
                System.out.println("Queeue name: " + QueueName);
                channel.queueDeclare(QueueName, false, false, false, null);
                Log.d("ADF", "Waiting for messages. To exit press CTRL+C");

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");
                    Log.d("ADF", "Received message: " + message);
                    try {

                        // New message has been detected, pull new messages
                        runOnUiThread(() -> {
//                            Toast.makeText(this, "DEBUG: Incoming new message", Toast.LENGTH_SHORT).show();
                            getAllMessages(conversationId);
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                };
                channel.basicConsume(QueueName, true, deliverCallback, consumerTag -> {
                });
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        });
        messageThread.start();

        btnSend.setOnClickListener(view -> {
            String message = etMessage.getText().toString();
            if (message.isEmpty()) {
                return;
            }

            etMessage.setText("");
            sendNewMessage(conversationId, message);
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (messageThread != null && messageThread.isAlive()) {
            messageThread.interrupt();
        }

        try {
            if (channel != null) {
                channel.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }


    }

    private void getParticipants(int conversationId) {
        Call<List<User>> call = RetrofitClient.getInstance()
                .getAPI().getParticipants(authManager.getToken(), conversationId);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                String username = "";
                List<User> participantList = response.body();
                participantList.remove(0); // Remove calling user

                if (participantList.size() == 1) {
                    username = participantList.get(0).getUsername();
                    //TODO: profile pic
                    btnProfile.setImageResource(R.drawable.pfp_placeholder);
                } else {
                    ArrayList<String> usernames = new ArrayList<>();
                    for (User p : participantList) {
                        usernames.add(p.getUsername());
                    }
                    if (usernames.size() <= 2) {
                        username = String.join(", ", usernames);
                    } else {
                        int otherUserCount = usernames.size() - 2;
                        username = usernames.get(0) + ", " + usernames.get(1) + " +" + otherUserCount + " more";
                    }
                    btnProfile.setImageResource(R.drawable.pfp_group_placeholder);
                }

                tvUsername.setText(username);
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable throwable) {

            }
        });
    }

    private void getAllMessages(int conversationId) {
        int lastMessageId = -1;
        if (!messages.isEmpty()) {
            lastMessageId = messages.get(messages.size() - 1).getId();
        }

        Call<List<Message>> call = RetrofitClient.getInstance()
                .getAPI().getMessages(authManager.getToken(), conversationId, lastMessageId);

        call.enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("Invalid response from getAllMessages");
                    return;
                }
                System.out.println("Received all messages: ");

                for (Message msg : response.body()) {
                    if (!containsMessage(messages, msg)) {
                        System.out.println("new msg received: " + msg);
                        messages.add(msg);
                        runOnUiThread(() -> {
//                            Toast.makeText(MessageActivity.this, msg.getMessage(), Toast.LENGTH_SHORT).show();
                            adapter.notifyItemInserted(messages.size() - 1);
                            recycler.scrollToPosition(messages.size() - 1);
                        });
                    } else {
                        System.out.println("ignoring old msg received: " + msg.getMessage());
                    }
                }

                if (!messages.isEmpty()) {
                    int mostRecentMessage = messages.get(messages.size() - 1).getId();
                    runOnUiThread(() -> {
                        databaseHelper.saveLastReadMessage(conversationId, mostRecentMessage);
                    });
                }

            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable throwable) {

            }
        });
    }

    private boolean containsMessage(List<Message> messages, Message msg) {
        for (Message m : messages) {
            if (m.getId() == msg.getId()) {
                return true;
            }
        }
        return false;
    }

    private void sendNewMessage(int conversationId, String message) {
        Call<Message> newMsgCall = RetrofitClient.getInstance()
                .getAPI().sendMessage(authManager.getToken(), conversationId, message);

        newMsgCall.enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> newMsgCall, Response<Message> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                //TODO: insert the new message in an "undelivered" state, then rabbitMQ will update the state as delivered
//                Toast.makeText(MessageActivity.this, "New message id :" + response.body().getId(), Toast.LENGTH_SHORT).show();
                messages.add(response.body());
                runOnUiThread(() -> {
                    adapter.notifyItemInserted(messages.size() - 1);
                    recycler.scrollToPosition(messages.size() - 1);
                });

            }

            @Override
            public void onFailure(Call<Message> newMsgCall, Throwable throwable) {

            }
        });
    }
}