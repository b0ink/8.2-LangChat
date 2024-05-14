package com.example.langchat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    private EditText etMessage;
    private ImageButton btnSend;

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

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);


        Intent intent = getIntent();
        if(intent == null || !intent.hasExtra(EXTRA_CONVERSATION_ID)){
            Intent homeActivity = new Intent(this, MainActivity.class);
            startActivity(homeActivity);
            finish();
            return;
        }


        int conversationId = intent.getIntExtra(EXTRA_CONVERSATION_ID, -1);

        ArrayList<Message> messages = new ArrayList<>();

        RecyclerView recycler = findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        MessageAdapter adapter = new MessageAdapter(this, messages);
        recycler.setAdapter(adapter);

        Call<List<Message>> call = RetrofitClient.getInstance()
                .getAPI().getMessages(conversationId);

        call.enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                messages.addAll(response.body());
                adapter.notifyDataSetChanged();
                recycler.scrollToPosition(messages.size()-1);

                for(Message msg : messages){
                    if(msg.getSender_id() == 1){
                        // dont translate messages from self
                        continue;
                    }
                    // -> if local translation not found:
                    Call<Message> callTranslation = RetrofitClient.getInstance()
                            .getAPI().translateMessage(1, msg.getId(), "german");

                    callTranslation.enqueue(new Callback<Message>() {
                        @Override
                        public void onResponse(Call<Message> callTranslation, Response<Message> response) {
                            if (!response.isSuccessful()) {
                                return;
                            }
                            if(response.body() == null || response.body().getMessage() == null){
                                return;
                            }
                            System.out.println(response.body());
                            msg.setMessage(response.body().getMessage());
                            adapter.notifyItemChanged(messages.indexOf(msg));

                        }

                        @Override
                        public void onFailure(Call<Message> callTranslation, Throwable throwable) {

                        }
                    });
                }

            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable throwable) {

            }
        });


        // TODO: instead, re-query the database for new messages that we dont yet have instead of manually inserting a new message item
        new Thread(() -> {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("10.0.2.2");
                factory.setPort(5672);
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                channel.queueDeclare("messages_"+conversationId, false, false, false, null);
                Log.d("ADF", "Waiting for messages. To exit press CTRL+C");

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");
                    Log.d("ADF", "Received message: " + message);
                    try{
                        JSONObject object = new JSONObject(message);
                        String newMessage = object.getString("message");
                        int convId = object.getInt("conversation_id");
                        int sender_id = object.getInt("sender_id");
                        int msgId = object.getInt("id");
                        JSONObject user = object.getJSONObject("user");
                        String username = user.getString("username");

                        String createdAt = object.getString("createdAt");
                        String updatedAt = object.getString("updatedAt");

                        runOnUiThread(() -> {
                            Message newMsg = new Message(msgId, convId, sender_id, newMessage, createdAt, updatedAt, new User(username));
                            messages.add(newMsg);
                            adapter.notifyItemInserted(messages.indexOf(newMsg));
                            recycler.scrollToPosition(messages.indexOf(newMsg)); // TODO: unless user has scrolled far enough above the start to prevent going back to the start

                        });

                    }catch (Exception e){
                        e.printStackTrace();
                    }

                };
                channel.basicConsume("messages_"+conversationId, true, deliverCallback, consumerTag -> {
                });
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }).start();

        btnSend.setOnClickListener(view ->{
            String message = etMessage.getText().toString();
            if(message.isEmpty()){
                return;
            }

            etMessage.setText("");
            Call<Message> newMsgCall = RetrofitClient.getInstance()
                    .getAPI().sendMessage(1, conversationId, message);

            newMsgCall.enqueue(new Callback<Message>() {
                @Override
                public void onResponse(Call<Message> newMsgCall, Response<Message> response) {
                    if(!response.isSuccessful()){
                        return;
                    }
                    //TODO: insert the new message in an "undelivered" state, then rabbitMQ will update the state as delivered
//                    messages.add(response.body());
//                    adapter.notifyItemInserted(messages.size()-1);

                }

                @Override
                public void onFailure(Call<Message> newMsgCall, Throwable throwable) {

                }
            });



        });

    }
}