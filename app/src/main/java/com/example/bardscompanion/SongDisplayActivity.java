package com.example.bardscompanion;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SongDisplayActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private TextView displaySongNameText;
    private TextView displayAuthorText;
    private TextView displayLyricsText;
    private Button finishSongButton;
    
    private Song currentSong;
    private boolean isPerformanceMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Keep screen on during performance
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_song_display);

        initViews();
        setupDatabase();
        loadSong();
        setupClickListeners();
    }

    private void initViews() {
        displaySongNameText = findViewById(R.id.displaySongNameText);
        displayAuthorText = findViewById(R.id.displayAuthorText);
        displayLyricsText = findViewById(R.id.displayLyricsText);
        finishSongButton = findViewById(R.id.finishSongButton);
    }

    private void setupDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }

    private void loadSong() {
        long songId = getIntent().getLongExtra("song_id", -1);
        isPerformanceMode = getIntent().getBooleanExtra("is_performance", false);
        
        if (songId != -1) {
            currentSong = databaseHelper.getSong(songId);
            if (currentSong != null) {
                displaySong();
            }
        }
        
        if (isPerformanceMode) {
            finishSongButton.setVisibility(android.view.View.VISIBLE);
            // Hide action bar for full screen experience
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
        }
    }

    private void displaySong() {
        displaySongNameText.setText(currentSong.getName());
        displayAuthorText.setText("by " + currentSong.getAuthor());
        displayLyricsText.setText(currentSong.getLyrics());
    }

    private void setupClickListeners() {
        finishSongButton.setOnClickListener(v -> {
            // Clear the current song on the server
            if (isPerformanceMode) {
                clearCurrentSongOnServer();
            }
            finish();
        });
    }

    private void clearCurrentSongOnServer() {
        SimpleHttpServer.clearCurrentSongStatic();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear keep screen on flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        if (isPerformanceMode) {
            clearCurrentSongOnServer();
        }
    }

    @Override
    public void onBackPressed() {
        if (isPerformanceMode) {
            clearCurrentSongOnServer();
        }
        super.onBackPressed();
    }
}