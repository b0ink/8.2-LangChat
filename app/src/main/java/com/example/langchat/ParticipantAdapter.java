package com.example.langchat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.langchat.API.AuthManager;

import java.util.ArrayList;
import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

    public ArrayList<User> participants;

    private Context context;

    public ParticipantAdapter(Context context, ArrayList<User> participants) {
        this.participants = participants;
        this.context = context;
    }


    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_participant, parent, false);
        return new ParticipantViewHolder(context, view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        User conversation = participants.get(position);
        holder.bind(conversation);
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    public class ParticipantViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUsername;
        private ImageButton btnRemoveUser;
        private ImageView imgProfilePicture;

        private RelativeLayout rlConversationContainer;


        private Context context;

        public ParticipantViewHolder(Context context, @NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            imgProfilePicture = itemView.findViewById(R.id.imgProfilePicture);
//            rlConversationContainer = itemView.findViewById(R.id.rlConversationContainer);
            btnRemoveUser = itemView.findViewById(R.id.btnRemoveUser);
            this.context = context;
        }

        public void bind(User user) {
            tvUsername.setText(user.getUsername());

            //TODO: if logged in user is admin of current converation, display remove user button
            btnRemoveUser.setVisibility(View.GONE);

            AuthManager authManager = new AuthManager(context);
            if(authManager.getJwtProperty("username").equals(user.getUsername())){
                tvUsername.setText(user.getUsername() + " (you)");
            }

            if(getAdapterPosition() == getItemCount()-1){
                // TODO: change image to a plus / '+' button
//                imgProfilePicture.setBackgroundResource();
                imgProfilePicture.setVisibility(View.INVISIBLE);
            }else{
                imgProfilePicture.setVisibility(View.VISIBLE);
            }

//            rlConversationContainer.setOnClickListener(view ->{
//                Intent intent = new Intent(context, MessageActivity.class);
//                intent.putExtra(MessageActivity.EXTRA_CONVERSATION_ID, conversation.getId());
//                intent.putExtra(MessageActivity.EXTRA_USERNAME_DISPLAY, usernameDisplay);
//                context.startActivity(intent);
//                ((Activity)context).finish();
//            });
        }

    }

}