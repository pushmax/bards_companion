package com.example.bardscompanion;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SongDisplayActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private TextView displaySongNameText;
    private TextView displayAuthorText;
    private TextView displayLyricsText;
    private Button finishSongButton;
    
    // Reactions display components
    private CardView reactionsCard;
    private LinearLayout reactionsContainer;
    private Handler handler;
    private Runnable reactionsUpdateRunnable;
    
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
        setupReactionsDisplay();
        loadSong();
        setupClickListeners();
    }

    private void initViews() {
        displaySongNameText = findViewById(R.id.displaySongNameText);
        displayAuthorText = findViewById(R.id.displayAuthorText);
        displayLyricsText = findViewById(R.id.displayLyricsText);
        finishSongButton = findViewById(R.id.finishSongButton);
        
        // Reactions display views
        reactionsCard = findViewById(R.id.reactionsCard);
        reactionsContainer = findViewById(R.id.reactionsContainer);
    }

    private void setupDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }

    private void setupReactionsDisplay() {
        handler = new Handler(Looper.getMainLooper());
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
            reactionsCard.setVisibility(android.view.View.VISIBLE);
            startReactionsUpdates();
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

    private void startReactionsUpdates() {
        reactionsUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateReactionsDisplay();
                handler.postDelayed(this, 2000); // Update every 2 seconds
            }
        };
        handler.post(reactionsUpdateRunnable);
    }

    private void stopReactionsUpdates() {
        if (handler != null && reactionsUpdateRunnable != null) {
            handler.removeCallbacks(reactionsUpdateRunnable);
        }
    }

    private void updateReactionsDisplay() {
        SimpleHttpServer server = getServerInstance();
        if (server != null) {
            Map<String, Integer> reactionTotals = server.getReactionTotalsForDisplay();
            List<ReactionResult> results = new ArrayList<>();
            
            for (Map.Entry<String, Integer> entry : reactionTotals.entrySet()) {
                results.add(new ReactionResult(entry.getKey(), entry.getValue()));
            }
            
            // Sort by reaction count descending
            results.sort((a, b) -> Integer.compare(b.reactionCount, a.reactionCount));
            
            updateReactionsContainer(results);
        }
    }
    
    private void updateReactionsContainer(List<ReactionResult> results) {
        // Clear existing views
        reactionsContainer.removeAllViews();
        
        // Hide reactions card if no reactions, show if there are reactions
        if (results.isEmpty()) {
            reactionsCard.setVisibility(android.view.View.GONE);
            return;
        } else {
            reactionsCard.setVisibility(android.view.View.VISIBLE);
        }
        
        // Create layout params with equal weight for even distribution
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            0, // width = 0dp for weight distribution  
            LinearLayout.LayoutParams.WRAP_CONTENT, // height wrap_content
            1.0f // equal weight for even distribution
        );
        layoutParams.setMargins(4, 0, 4, 0); // 4dp margins on sides
        
        LayoutInflater inflater = LayoutInflater.from(this);
        
        // Add each reaction item
        for (ReactionResult result : results) {
            LinearLayout reactionItem = (LinearLayout) inflater.inflate(
                R.layout.item_reaction_horizontal, reactionsContainer, false);
            
            TextView emojiText = reactionItem.findViewById(R.id.reactionEmojiText);
            TextView countText = reactionItem.findViewById(R.id.reactionCountText);
            
            emojiText.setText(result.emoji);
            countText.setText(String.valueOf(result.reactionCount));
            
            reactionItem.setLayoutParams(layoutParams);
            reactionsContainer.addView(reactionItem);
        }
    }
    
    private static class ReactionResult {
        public String reactionType;
        public int reactionCount;
        public String emoji;

        public ReactionResult(String reactionType, int reactionCount) {
            this.reactionType = reactionType;
            this.reactionCount = reactionCount;
            
            switch (reactionType) {
                case "panties":
                    this.emoji = "👙";
                    break;
                case "heart":
                    this.emoji = "❤️";
                    break;
                case "tomato":
                    this.emoji = "🍅";
                    break;
                case "vomit":
                    this.emoji = "🤮";
                    break;
                default:
                    this.emoji = "🎭";
                    break;
            }
        }
    }

    private SimpleHttpServer getServerInstance() {
        return SimpleHttpServer.getInstance();
    }

    private void clearCurrentSongOnServer() {
        SimpleHttpServer.clearCurrentSongStatic();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear keep screen on flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        stopReactionsUpdates();
        
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