package com.example.langchat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import androidx.constraintlayout.utils.widget.ImageFilterView;
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
        private ImageFilterView imgProfilePicture;

        private ImageFilterView imgGroupAvatar1;
        private ImageFilterView imgGroupAvatar2;

        private ImageView imgNewMessageIcon;

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
            imgNewMessageIcon = itemView.findViewById(R.id.imgNewMessageIcon);
            imgGroupAvatar1 = itemView.findViewById(R.id.imgGroupAvatar1);
            imgGroupAvatar2 = itemView.findViewById(R.id.imgGroupAvatar2);

            this.context = context;
            this.selectedInterests = selectedInterests;
        }

        public void bind(ConversationResponse conversation) {
            String username = "";
            List<Participant> participantList = conversation.getParticipants();
            imgNewMessageIcon.setVisibility(View.GONE);

            imgProfilePicture.setVisibility(View.VISIBLE);
            imgGroupAvatar1.setVisibility(View.GONE);
            imgGroupAvatar2.setVisibility(View.GONE);

            imgGroupAvatar1.setImageResource(R.drawable.pfp_placeholder);
            imgGroupAvatar2.setImageResource(R.drawable.pfp_placeholder);

            //TODO: put into static util class
            if (!conversation.isGroupChat()) {
                username = participantList.get(0).getUser().getUsername();
                imgProfilePicture.setImageResource(R.drawable.pfp_placeholder);
                String avatarBase64 = participantList.get(0).getUser().getAvatar();
                if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                    Bitmap avatar = ImageUtil.convert(avatarBase64);
                    imgProfilePicture.setImageBitmap(avatar);
                }
            } else {
                ArrayList<String> usernames = new ArrayList<>();
                imgGroupAvatar1.setVisibility(View.VISIBLE);
                imgGroupAvatar2.setVisibility(View.VISIBLE);
                int count = 0;
                for (Participant p : participantList) {
                    count++;

                    String avatarBase64 = p.getUser().getAvatar();
                    if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                        Bitmap avatar = ImageUtil.convert(avatarBase64);
                        if (count == 1) {
                            imgGroupAvatar1.setImageBitmap(avatar);
                        } else if (count == 2) {
                            imgGroupAvatar2.setImageBitmap(avatar);
                        }
                    }
                    usernames.add(p.getUser().getUsername());
                }
                if (usernames.size() <= 2) {
                    username = String.join(", ", usernames);
                } else {
                    int otherUserCount = usernames.size() - 2;
                    username = usernames.get(0) + ", " + usernames.get(1) + " +" + otherUserCount + " more";
                }
                imgProfilePicture.setImageResource(R.drawable.pfp_group_placeholder);
                imgProfilePicture.setVisibility(View.INVISIBLE);


            }

            tvLastMessageTime.setText(conversation.getLastUpdatedDisplay());

            tvUsername.setText(username);
            Message lastMsg = conversation.getLastMessage();
            if (lastMsg == null) {
                tvRecentMessage.setText("Tap to send a message...");
                imgNewMessageIcon.setVisibility(View.VISIBLE);
            } else {

                List<Translation> translations = lastMsg.getTranslations();
                tvRecentMessage.setText(lastMsg.getMessage());
                if (translations != null && translations.size() > 0) {
                    tvRecentMessage.setText(translations.get(0).getMessage());
                }

                // Check if message has been opened yet (notification)
                int recentMessageId = conversation.getLastMessage().getId();
                int lastReadMessageId = LocalDatabaseHelper.getInstance(context).getLastReadMessage(conversation.getId());

                if (recentMessageId > lastReadMessageId) {
                    imgNewMessageIcon.setVisibility(View.VISIBLE);
                }
            }


            final String usernameDisplay = username;
            rlConversationContainer.setOnClickListener(view -> {
                Intent intent = new Intent(context, MessageActivity.class);
                intent.putExtra(MessageActivity.EXTRA_CONVERSATION_ID, conversation.getId());
                intent.putExtra(MessageActivity.EXTRA_USERNAME_DISPLAY, usernameDisplay);
                context.startActivity(intent);
                ((Activity) context).finish();
            });
        }

    }

}