package com.example.crimereportapp;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<ForumPost> postList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ForumPost post);
    }

    public PostAdapter(List<ForumPost> postList, OnItemClickListener listener) {
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        ForumPost post = postList.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUsername;
        private TextView tvMessage; // Updated from tvPostDescription
        private ImageView ivMedia;
        private TextView tvTimestamp;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvMessage = itemView.findViewById(R.id.tvMessage); // Updated ID
            ivMedia = itemView.findViewById(R.id.ivPostMedia);
            tvTimestamp = itemView.findViewById(R.id.tvPostTimestamp);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(postList.get(position));
                    }
                }
            });
        }

        public void bind(ForumPost post) {
            tvUsername.setText(post.getUserName() != null ? post.getUserName() : "Anonymous");
            tvMessage.setText(post.getMessage() != null ? post.getMessage() : "No message");
            if (post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()) {
                ivMedia.setVisibility(View.VISIBLE);
                ivMedia.setImageURI(Uri.parse(post.getMediaUrl()));
            } else {
                ivMedia.setVisibility(View.GONE);
            }
            tvTimestamp.setText(formatTimestamp(post.getTimestamp()));
        }

        private String formatTimestamp(long timestamp) {
            return "Posted at: " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(timestamp));
        }
    }
}