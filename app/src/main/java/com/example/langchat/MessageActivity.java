package com.example.langchat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.core.app.ActivityCompat;
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

import java.io.File;
import java.io.IOException;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";
    public static final String EXTRA_USERNAME_DISPLAY = "extra_username_display";

    private EditText etMessage;
    private ImageButton btnSend;

    private ImageFilterView btnProfile;

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

    private RelativeLayout rlAvatarGroup;
    private ImageFilterView imgGroupAvatar1;
    private ImageFilterView imgGroupAvatar2;



    private MediaRecorder mediaRecorder;
    //    private String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";
    private Handler voiceMessageHandler = new Handler();
    private ImageButton btnMicrophone;
    public Boolean voiceRecording = false;
    AudioMessageWaveformView audioMessageWaveform;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private final String[] permissions = {android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private Runnable updateVisualizer;
    private String audioMessageFilename;
    int conversationId = -1;

    private ImageView gifSpinner;

    private boolean sendingAudioMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_message);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        audioMessageFilename = "";

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnProfile = findViewById(R.id.btnProfile);

        rlAvatarGroup = findViewById(R.id.rlAvatarGroup);
        rlAvatarGroup.setVisibility(View.GONE);
        btnProfile.setVisibility(View.VISIBLE);

        imgGroupAvatar1 = findViewById(R.id.imgGroupAvatar1);
        imgGroupAvatar2 = findViewById(R.id.imgGroupAvatar2);

        btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(view -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });


        Intent intent = getIntent();
        conversationId = intent.getIntExtra(EXTRA_CONVERSATION_ID, -1);
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

        gifSpinner  = findViewById(R.id.gifSpinner);
        gifSpinner.setVisibility(View.GONE);
        btnSend.setVisibility(View.VISIBLE);

        btnSend.setOnClickListener(view -> {
            if (!audioMessageFilename.isEmpty()) {
                btnSend.setVisibility(View.GONE);
                gifSpinner.setVisibility(View.VISIBLE);
                stopRecording();
                stopVisualizer();
                btnMicrophone.setImageResource(R.drawable.microphone_off);

                sendAudioMessage();
                //TODO: loading spinner
                return;
            }


            String message = etMessage.getText().toString();
            if (message.isEmpty()) {
                return;
            }

            etMessage.setText("");
            sendNewMessage(conversationId, message);
        });

        audioMessageWaveform = findViewById(R.id.audioMessageWaveformView);
        audioMessageWaveform.setVisibility(View.GONE);

        btnMicrophone = findViewById(R.id.btnMicrophone);
        btnMicrophone.setOnClickListener(view -> {
            if(sendingAudioMessage){
                return;
            }

            if (!audioMessageFilename.isEmpty()) {
                //TODO: check to see if the file exists
                if (!voiceRecording) {
                    //TODO: delete the file
                    audioMessageWaveform.reset();
                }
            }

            if (!voiceRecording) {
                String filename = conversationId + "_" + authManager.getJwtProperty("username") + "_" + System.currentTimeMillis();
                boolean recording = startRecording(filename);

                if (!recording) {
                    return;
                }

                startVisualizer();
                etMessage.setVisibility(View.GONE);
                audioMessageWaveform.setVisibility(View.VISIBLE);
                btnMicrophone.setImageResource(R.drawable.microphone_on);
                return;
            }

            btnMicrophone.setImageResource(R.drawable.microphone_off);
            voiceRecording = false;
//            etMessage.setVisibility(View.VISIBLE);
//            audioMessageWaveform.setVisibility(View.GONE);
            stopRecording();
            stopVisualizer();
        });

        audioMessageWaveform.setOnClickListener(view -> {
            if(sendingAudioMessage){
                return;
            }
            //TODO: message that says "tap to delete" above/under the waveform
            resetAudioMessageRecording();
        });

        updateVisualizer = new Runnable() {
            @Override
            public void run() {
                int amplitude = getAmplitude();
                // Pass amplitude to the custom view to render waveform
                audioMessageWaveform.addAmplitude(amplitude);
                voiceMessageHandler.postDelayed(this, 100);
            }
        };
    }

    private void resetAudioMessageRecording(){
        if(sendingAudioMessage){
            return;
        }
        voiceRecording = false;
        stopRecording();
        stopVisualizer();
        audioMessageWaveform.reset();
        etMessage.setVisibility(View.VISIBLE);
        audioMessageWaveform.setVisibility(View.GONE);
        btnMicrophone.setImageResource(R.drawable.microphone_off);
        audioMessageFilename = "";
        btnSend.setVisibility(View.VISIBLE);
        gifSpinner.setVisibility(View.GONE);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionToRecordAccepted = requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    private void sendAudioMessage() {
        if(audioMessageFilename.isEmpty()){
            return;
        }
        File audio = new File(audioMessageFilename);
        if(!audio.exists() || !audio.isFile()){
            audioMessageFilename = "";
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/3gp"), audio);
        MultipartBody.Part body = MultipartBody.Part.createFormData("audio", audioMessageFilename, requestFile);

        Call<Message> call = RetrofitClient.getInstance()
                .getAPI().sendAudioMessage(authManager.getToken(), conversationId, body);

        sendingAudioMessage = true;

        call.enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("Upload", "Success");
                    insertNewMessage(response.body());
                } else {
                    Log.e("Upload", "Error: " + response.message());
                }

                sendingAudioMessage = false;
                resetAudioMessageRecording();
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                Log.e("Upload", "Failure: " + t.getMessage());
                sendingAudioMessage = false;
            }
        });
    }

    private boolean startRecording(String fileName) {
        if (fileName.isEmpty()) {
            return false;
        }
        mediaRecorder = new MediaRecorder();
        audioMessageFilename = getExternalCacheDir().getAbsolutePath() + "/" + fileName + ".3gp";
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioMessageFilename);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(320000);
            mediaRecorder.setAudioSamplingRate(48000);
            mediaRecorder.prepare();
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            return false;
        }
        mediaRecorder.start();
        voiceRecording = true;
        return true;
    }

    private void stopRecording() {
        if (mediaRecorder == null) {
            return;
        }

        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
    }

    private int getAmplitude() {
        if (mediaRecorder != null) {
            return mediaRecorder.getMaxAmplitude();
        }
        return 0;
    }


    private void startVisualizer() {
        voiceMessageHandler.post(updateVisualizer);
    }

    private void stopVisualizer() {
        voiceMessageHandler.removeCallbacks(updateVisualizer);
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
                    String avatarBase64 = participantList.get(0).getAvatar();
                    if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                        Bitmap avatar = ImageUtil.convert(avatarBase64);
                        btnProfile.setImageBitmap(avatar);
                    }
                } else {
                    ArrayList<String> usernames = new ArrayList<>();
                    rlAvatarGroup.setVisibility(View.VISIBLE);
                    btnProfile.setVisibility(View.INVISIBLE);
                    int count = 0;
                    for (User p : participantList) {
                        count++;
                        String avatarBase64 = p.getAvatar();
                        if(avatarBase64 != null && !avatarBase64.isEmpty()){
                            Bitmap avatar = ImageUtil.convert(avatarBase64);
                            if(count == 1){
                                imgGroupAvatar1.setImageBitmap(avatar);
                            }else if(count == 2){
                                imgGroupAvatar2.setImageBitmap(avatar);
                            }
                        }


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
                insertNewMessage(response.body());
            }

            @Override
            public void onFailure(Call<Message> newMsgCall, Throwable throwable) {

            }
        });
    }

    private void insertNewMessage(Message msg){
        messages.add(msg);
        databaseHelper.saveLastReadMessage(conversationId,msg.getId());
        runOnUiThread(() -> {
            adapter.notifyItemInserted(messages.size() - 1);
            recycler.scrollToPosition(messages.size() - 1);
        });
    }
}