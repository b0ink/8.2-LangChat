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

        btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(view ->{
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
        btnSettings.setOnClickListener(view ->{
            Intent settings = new Intent(this, ConversationSettings.class);
            settings.putExtra(ConversationSettings.EXTRA_CONVERSATION_ID, conversationId);
            startActivity(settings);
            finish();
        });

        authManager = new AuthManager(this);
        databaseHelper = LocalDatabaseHelper.getInstance(this);

        tvUsername = findViewById(R.id.tvUsername);
        String usernameDisplay = intent.getStringExtra(EXTRA_USERNAME_DISPLAY);
        tvUsername.setText(usernameDisplay);

        messages = new ArrayList<>();

        recycler = findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new MessageAdapter(this, messages);
        recycler.setAdapter(adapter);

        getAllMessages(conversationId);


        // TODO: instead, re-query the database for new messages that we dont yet have instead of manually inserting a new message item
        new Thread(() -> {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("10.0.2.2");
                factory.setPort(5672);
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
                String QueueName = "messages_" + conversationId +"_"+authManager.getJwtProperty("id");
                System.out.println("Queeue name: "+QueueName);
                channel.queueDeclare(QueueName, false, false, false, null);
                Log.d("ADF", "Waiting for messages. To exit press CTRL+C");

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");
                    Log.d("ADF", "Received message: " + message);
                    try {
//                        JSONObject object = new JSONObject(message);
//                        String newMessage = object.getString("message");
//                        int convId = object.getInt("conversation_id");
//                        int sender_id = object.getInt("sender_id");
//                        int msgId = object.getInt("id");
//                        JSONObject user = object.getJSONObject("user");
//                        String username = user.getString("username");
//
//                        String createdAt = object.getString("createdAt");
//                        String updatedAt = object.getString("updatedAt");
//
//                        runOnUiThread(() -> {
//                            Message newMsg = new Message(msgId, convId, sender_id, newMessage, createdAt, updatedAt, new User(username));
//                            messages.add(newMsg);
//                            adapter.notifyItemInserted(messages.indexOf(newMsg));
//                            recycler.scrollToPosition(messages.indexOf(newMsg)); // TODO: unless user has scrolled far enough above the start to prevent going back to the start
//                        });

                        // New message has been detected, pull new messages
                        runOnUiThread(()->{
                            Toast.makeText(this, "DEBUG: Incoming new message", Toast.LENGTH_SHORT).show();
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
        }).start();

        btnSend.setOnClickListener(view -> {
            String message = etMessage.getText().toString();
            if (message.isEmpty()) {
                return;
            }

            etMessage.setText("");
            sendNewMessage(conversationId, message);
        });

    }

    private void getAllMessages(int conversationId) {
        int lastMessageId = -1;
        if(!messages.isEmpty()){
            lastMessageId = messages.get(messages.size()-1).getId();
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
                for(Message msg : response.body()){
                    if(!containsMessage(messages, msg)){
                        messages.add(msg);
                        adapter.notifyItemInserted(messages.indexOf(msg));
                        recycler.scrollToPosition(messages.indexOf(msg));
                    }
                }
//                messages.addAll(response.body());
//                adapter.notifyDataSetChanged();


//                for (Message msg : messages) {
//                    if (msg.getSender_id() == 1) {
//                        // dont translate messages from self
//                        continue;
//                    }
//                    String translatedMessage = databaseHelper.retrieveTranslatedMessage(msg.getId(), "german");
//                    if (translatedMessage != null) {
//                        msg.setMessage(translatedMessage);
//                        adapter.notifyItemChanged(messages.indexOf(msg));
//                        continue;
//                    }
//
//                    translateMessage(msg);
//                }

            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable throwable) {

            }
        });
    }

    private boolean containsMessage(List<Message> messages, Message msg) {
        for(Message m : messages) {
            if(m.getId() == msg.getId()) {
                return true;
            }
        }
        return false;
    }

    private void translateMessage(Message msg) {
        // -> if local translation not found:
        final String preferredLanguage = "german"; // TODO: to be replaced with user's preference for this specific conversation
        final int senderId = 1; // TODO: to be replaced with logged in user

        Call<Message> callTranslation = RetrofitClient.getInstance()
                .getAPI().translateMessage(authManager.getToken(), msg.getId(), "german");

        callTranslation.enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> callTranslation, Response<Message> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                if (response.body() == null || response.body().getMessage() == null) {
                    return;
                }
                databaseHelper.saveTranslation(msg.getId(), preferredLanguage, response.body().getMessage());
                System.out.println(response.body());
                msg.setMessage(response.body().getMessage());
                adapter.notifyItemChanged(messages.indexOf(msg));

            }

            @Override
            public void onFailure(Call<Message> callTranslation, Throwable throwable) {

            }
        });
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
                messages.add(response.body());
                adapter.notifyItemInserted(messages.size() - 1);
                recycler.scrollToPosition(messages.size() - 1);

            }

            @Override
            public void onFailure(Call<Message> newMsgCall, Throwable throwable) {

            }
        });
    }
}