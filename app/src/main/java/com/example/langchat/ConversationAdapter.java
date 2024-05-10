package com.example.langchat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.langchat.API.AuthManager;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    public ArrayList<ConversationResponse> conversations;
    private ArrayList<String> selectedInterests = new ArrayList<>();

    private Context context;

    public ConversationAdapter(Context context, ArrayList<ConversationResponse> conversations) {
        this.conversations = conversations;
        this.context = context;
    }


    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_conversation, parent, false);
        return new ConversationViewHolder(context, view, selectedInterests);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationAdapter.ConversationViewHolder holder, int position) {
        ConversationResponse conversation = conversations.get(position);
        holder.bind(conversation);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public class ConversationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUsername;
        private TextView tvLastMessageTime;
        private TextView tvRecentMessage;
        private ImageView imgProfilePicture;

        private RelativeLayout rlConversationContainer;

        ArrayList<String> selectedInterests;

        private Context context;

        public ConversationViewHolder(Context context, @NonNull View itemView, ArrayList<String> selectedInterests) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMessageTime);
            tvRecentMessage = itemView.findViewById(R.id.tvRecentMessage);
            imgProfilePicture = itemView.findViewById(R.id.imgProfilePicture);
            rlConversationContainer = itemView.findViewById(R.id.rlConversationContainer);
            this.context = context;
            this.selectedInterests = selectedInterests;
        }

        public void bind(ConversationResponse conversation) {
            String username = "";
            List<Participant> participantList = conversation.getParticipants();
            if(participantList.size() == 1){
                username = participantList.get(0).getUser().getUsername();
                imgProfilePicture.setBackgroundResource(R.drawable.pfp_placeholder);
            }else{
                ArrayList<String> usernames = new ArrayList<>();
                for(Participant p : participantList){
                    usernames.add(p.getUser().getUsername());
                }
                if(usernames.size() <= 2){
                    username = String.join(", ", usernames);
                }else{
                    int otherUserCount = usernames.size()-2;
                    username = usernames.get(0) + ", " + usernames.get(1) + " +" + otherUserCount + " more";
                }
                imgProfilePicture.setBackgroundResource(R.drawable.pfp_group_placeholder);
            }


            tvUsername.setText(username);
            Message lastMsg = conversation.getLastMessage();
            if(lastMsg == null){
                tvRecentMessage.setText("Tap to send a message...");
            }else{
                tvRecentMessage.setText(lastMsg.getMessage());
            }

            rlConversationContainer.setOnClickListener(view ->{
                Intent intent = new Intent(context, MessageActivity.class);
                intent.putExtra(MessageActivity.EXTRA_CONVERSATION_ID, conversation.getId());
                context.startActivity(intent);
                ((Activity)context).finish();
            });
        }

    }

}