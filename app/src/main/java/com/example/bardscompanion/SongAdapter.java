package com.example.bardscompanion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private List<Song> songs;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
        void onEditClick(Song song);
        void onDeleteClick(Song song);
    }

    public SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.songNameText.setText(song.getName());
        holder.authorText.setText(song.getAuthor());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(song);
            }
        });

        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(song);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(song);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void updateSongs(List<Song> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView songNameText;
        TextView authorText;
        ImageButton editButton;
        ImageButton deleteButton;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songNameText = itemView.findViewById(R.id.songNameText);
            authorText = itemView.findViewById(R.id.authorText);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}