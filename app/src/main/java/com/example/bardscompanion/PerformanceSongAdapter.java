package com.example.bardscompanion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PerformanceSongAdapter extends RecyclerView.Adapter<PerformanceSongAdapter.PerformanceSongViewHolder> {
    private List<Song> songs;
    private OnSongSelectListener listener;

    public interface OnSongSelectListener {
        void onSongSelected(Song song);
    }

    public PerformanceSongAdapter(List<Song> songs, OnSongSelectListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PerformanceSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_performance_song, parent, false);
        return new PerformanceSongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PerformanceSongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.songNameText.setText(song.getName());
        holder.authorText.setText(song.getAuthor());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongSelected(song);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class PerformanceSongViewHolder extends RecyclerView.ViewHolder {
        TextView songNameText;
        TextView authorText;

        PerformanceSongViewHolder(@NonNull View itemView) {
            super(itemView);
            songNameText = itemView.findViewById(R.id.performanceSongNameText);
            authorText = itemView.findViewById(R.id.performanceAuthorText);
        }
    }
}