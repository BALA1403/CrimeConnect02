package com.example.crimereportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<ForumComment> commentList;

    public CommentAdapter(List<ForumComment> commentList) {
        this.commentList = new ArrayList<>(commentList);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        ForumComment comment = commentList.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public void updateComments(List<ForumComment> newComments) {
        commentList.clear();
        commentList.addAll(newComments);
        notifyDataSetChanged();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCommentUser;
        private TextView tvCommentText;
        private TextView tvCommentTimestamp;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentUser = itemView.findViewById(R.id.tvCommentUser);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
            tvCommentTimestamp = itemView.findViewById(R.id.tvCommentTimestamp);
        }

        void bind(ForumComment comment) {
            tvCommentUser.setText(comment.getUserName() != null ? comment.getUserName() : "Anonymous");
            tvCommentText.setText(comment.getComment() != null ? comment.getComment() : "No comment");
            tvCommentTimestamp.setText(formatTimestamp(comment.getTimestamp()));
        }

        private String formatTimestamp(long timestamp) {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(timestamp));
        }
    }
}