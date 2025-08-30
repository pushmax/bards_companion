package com.example.bardscompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {
    private DatabaseHelper databaseHelper;
    private SongAdapter songAdapter;
    private RecyclerView songsRecyclerView;
    private TextView noSongsText;
    private Button startPerformanceButton;
    private FloatingActionButton addSongFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupDatabase();
        setupRecyclerView();
        setupClickListeners();
        loadSongs();
    }

    private void initViews() {
        songsRecyclerView = findViewById(R.id.songsRecyclerView);
        noSongsText = findViewById(R.id.noSongsText);
        startPerformanceButton = findViewById(R.id.startPerformanceButton);
        addSongFab = findViewById(R.id.addSongFab);
    }

    private void setupDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }

    private void setupRecyclerView() {
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(databaseHelper.getAllSongs(), this);
        songsRecyclerView.setAdapter(songAdapter);
    }

    private void setupClickListeners() {
        addSongFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditSongActivity.class);
            startActivity(intent);
        });

        startPerformanceButton.setOnClickListener(v -> {
            List<Song> songs = databaseHelper.getAllSongs();
            if (songs.isEmpty()) {
                Toast.makeText(this, "Add some songs first!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, PerformanceActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSongs();
    }

    private void loadSongs() {
        List<Song> songs = databaseHelper.getAllSongs();
        songAdapter.updateSongs(songs);
        
        if (songs.isEmpty()) {
            noSongsText.setVisibility(View.VISIBLE);
            songsRecyclerView.setVisibility(View.GONE);
        } else {
            noSongsText.setVisibility(View.GONE);
            songsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSongClick(Song song) {
        Intent intent = new Intent(this, SongDisplayActivity.class);
        intent.putExtra("song_id", song.getId());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Song song) {
        Intent intent = new Intent(this, AddEditSongActivity.class);
        intent.putExtra("song_id", song.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Song")
                .setMessage("Are you sure you want to delete \"" + song.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseHelper.deleteSong(song.getId());
                    loadSongs();
                    Toast.makeText(this, getString(R.string.song_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}