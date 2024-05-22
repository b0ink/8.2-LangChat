package com.example.langchat;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.langchat.API.AuthManager;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    public ArrayList<Message> chatMessages;

    private Context context;

    public MessageAdapter(Context context, ArrayList<Message> items) {
        this.chatMessages = items;
        this.context = context;
    }


    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_message, parent, false);
        return new MessageViewHolder(context, view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.MessageViewHolder holder, int position) {
        Message Message = chatMessages.get(position);
        holder.bind(Message);
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {


        private Context context;
        private TextView tvMessageText;
        private TextView tvSenderUsername;
        private LinearLayout llMessageContainer;

        private AuthManager authManager;

        public MessageViewHolder(Context context, @NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            llMessageContainer = itemView.findViewById(R.id.llMessageContainer);
            tvSenderUsername = itemView.findViewById(R.id.tvSenderUsername);
            authManager = new AuthManager(context);
            this.context = context;
        }

        public void bind(Message chatMessage) {
            // set the original text of the message
            tvMessageText.setText(chatMessage.getMessage());

            tvSenderUsername.setText(chatMessage.getUser().getUsername());

            // if a translation is available, update it to translated version:
            if(chatMessage.getTranslations() != null){
                List<Translation> translations = chatMessage.getTranslations();
                if(!translations.isEmpty()){
                    tvMessageText.setText(translations.get(0).getMessage());
                }
            }

            System.out.println("username: " + chatMessage.getUser().getUsername());

            //TODO: check if authed user matches this username
            if(chatMessage.getUser().getUsername().equals(authManager.getJwtProperty("username"))){
                llMessageContainer.setGravity(Gravity.RIGHT);

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tvMessageText.getLayoutParams();
                params.gravity = Gravity.RIGHT; // or any other gravity
                tvMessageText.setLayoutParams(params);

                tvMessageText.setBackgroundResource(R.drawable.text_view_background_user);
                tvSenderUsername.setVisibility(View.GONE);


            }else{
                llMessageContainer.setGravity(Gravity.LEFT);
//                tvMessageText.setGravity(Gravity.LEFT);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tvMessageText.getLayoutParams();
                params.gravity = Gravity.LEFT; // or any other gravity
                tvMessageText.setLayoutParams(params);


                tvMessageText.setBackgroundResource(R.drawable.text_view_background_ai);
                tvSenderUsername.setVisibility(View.VISIBLE);

                int position = getAdapterPosition();
                if(position >= 1){
                    Message previousMessage = chatMessages.get(position-1);
                    if(previousMessage.getUser().equals(chatMessage.getUser())){
                        tvSenderUsername.setVisibility(View.GONE);
                    }
                }
            }
        }

    }

}