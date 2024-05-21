package com.example.langchat;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

    private ImageButton btnProfile;

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
        if(authManager.getToken() == null || !authManager.isTokenValid()){
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(view ->{
            authManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        });


        // Run the message receiving logic on a background thread
        new Thread(() -> {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("10.0.2.2");
                factory.setPort(5672);
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                channel.queueDeclare("my_messages", false, false, false, null);
                Log.d("ADF", "Waiting for messages. To exit press CTRL+C");

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");
                    Log.d("ADF", "Received message: " + message);
                    try {
                        JSONObject object = new JSONObject(message);
                        String newMessage = object.getString("message");
                        int convId = object.getInt("conversation_id");
                        int sender_id = object.getInt("sender_id");

                        runOnUiThread(() -> {
//                            retrieveConversations(1);

                            int index = 0;
                            for (ConversationResponse convo : conversations) {
                                System.out.println("checking" + convo.getId() + " with " + convId);
                                //TODO: if no conversation exists, create a new one
                                if (convo.getId() == convId) {
                                    ConversationResponse item = conversations.remove(index);
                                    conversations.add(0, item);
//                                    adapter.notifyItemChanged(index);
//                                    conversations.add(0, convo);
//                                    conversations.remove(index);
                                    adapter.notifyItemMoved(index, 0);
                                    item.getLastMessage().setMessage(newMessage);
                                    adapter.notifyItemChanged(0);

//                                    conversations.
                                    break;
                                }
                                index++;
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                };
                channel.basicConsume("my_messages", true, deliverCallback, consumerTag -> {
                });
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }).start();


        if (false) {
            startActivity(new Intent(this, MessageActivity.class));
            finish();
            return;
        }

        conversations = new ArrayList<>();

        RecyclerView recycler = findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new ConversationAdapter(this, conversations);
        recycler.setAdapter(adapter);

        retrieveConversations(1);
    }

    private void retrieveConversations(int userId) {
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

                for (ConversationResponse convo : conversations) {
                    System.out.println("---- CONVO -----");
                    for (Participant user : convo.getParticipants()) {
                        System.out.println("Participants: " + user.getUser().getUsername());
                    }
                    Message lastMessage = convo.getLastMessage();
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