package com.example.bardscompanion;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class PerformanceActivity extends AppCompatActivity implements PerformanceSongAdapter.OnSongSelectListener {
    private SimpleHttpServer httpServer;
    private DatabaseHelper databaseHelper;
    private TextView ipAddressText;
    private TextView serverStatusText;
    private Button openHotspotSettingsButton;
    private Button stopServerButton;
    private Button selectSongButton;
    private Button closeSongListButton;
    private View overlayBackground;
    private androidx.cardview.widget.CardView songListOverlay;
    private RecyclerView performanceSongsRecyclerView;
    private PerformanceSongAdapter performanceSongAdapter;
    private Handler handler;
    private Runnable ipUpdateRunnable;
    
    // Voting display components
    private CardView votingInfoCard;
    private TextView votingStatusText;
    private RecyclerView votingRecyclerView;
    private VotingResultAdapter votingResultAdapter;
    private Runnable votingUpdateRunnable;
    
    // Reactions display components
    private CardView reactionsInfoCard;
    private TextView reactionsStatusText;
    private RecyclerView reactionsRecyclerView;
    private ReactionResultAdapter reactionResultAdapter;
    private Runnable reactionsUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);

        initViews();
        setupDatabase();
        setupServer();
        setupRecyclerView();
        setupVotingDisplay();
        setupReactionsDisplay();
        setupClickListeners();
        loadSongs();
        startIpUpdates();
        startVotingUpdates();
        startReactionsUpdates();
    }

    private void initViews() {
        ipAddressText = findViewById(R.id.ipAddressText);
        serverStatusText = findViewById(R.id.serverStatusText);
        openHotspotSettingsButton = findViewById(R.id.openHotspotSettingsButton);
        stopServerButton = findViewById(R.id.stopServerButton);
        selectSongButton = findViewById(R.id.selectSongButton);
        closeSongListButton = findViewById(R.id.closeSongListButton);
        overlayBackground = findViewById(R.id.overlayBackground);
        songListOverlay = findViewById(R.id.songListOverlay);
        performanceSongsRecyclerView = findViewById(R.id.performanceSongsRecyclerView);
        
        // Voting display views
        votingInfoCard = findViewById(R.id.votingInfoCard);
        votingStatusText = findViewById(R.id.votingStatusText);
        votingRecyclerView = findViewById(R.id.votingRecyclerView);
        
        // Reactions display views
        reactionsInfoCard = findViewById(R.id.reactionsInfoCard);
        reactionsStatusText = findViewById(R.id.reactionsStatusText);
        reactionsRecyclerView = findViewById(R.id.reactionsRecyclerView);
    }

    private void setupDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }

    private void setupServer() {
        httpServer = new SimpleHttpServer(this);
        if (httpServer.start()) {
            Toast.makeText(this, "Server started successfully", Toast.LENGTH_SHORT).show();
            updateServerInfo();
        } else {
            Toast.makeText(this, getString(R.string.server_error), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        performanceSongsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<Song> songs = databaseHelper.getAllSongs();
        performanceSongAdapter = new PerformanceSongAdapter(songs, this);
        performanceSongsRecyclerView.setAdapter(performanceSongAdapter);
    }

    private void setupVotingDisplay() {
        votingRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        votingResultAdapter = new VotingResultAdapter();
        votingRecyclerView.setAdapter(votingResultAdapter);
    }

    private void setupReactionsDisplay() {
        reactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reactionResultAdapter = new ReactionResultAdapter();
        reactionsRecyclerView.setAdapter(reactionResultAdapter);
    }

    private void setupClickListeners() {
        openHotspotSettingsButton.setOnClickListener(v -> {
            try {
                // Try to open WiFi hotspot settings
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                // Fallback to general settings
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
            }
        });

        stopServerButton.setOnClickListener(v -> {
            stopServer();
            finish();
        });

        selectSongButton.setOnClickListener(v -> showSongListOverlay());

        closeSongListButton.setOnClickListener(v -> hideSongListOverlay());

        overlayBackground.setOnClickListener(v -> hideSongListOverlay());
    }

    private void loadSongs() {
        List<Song> songs = databaseHelper.getAllSongs();
        if (performanceSongAdapter != null) {
            performanceSongAdapter = new PerformanceSongAdapter(songs, this);
            performanceSongsRecyclerView.setAdapter(performanceSongAdapter);
        }
    }

    private void updateServerInfo() {
        String ipAddress = NetworkUtils.getLocalIpAddress(this);
        ipAddressText.setText(ipAddress + ":8080");
    }

    private void startIpUpdates() {
        handler = new Handler(Looper.getMainLooper());
        ipUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateServerInfo();
                handler.postDelayed(this, 5000); // Update every 5 seconds
            }
        };
        handler.post(ipUpdateRunnable);
    }

    private void stopIpUpdates() {
        if (handler != null && ipUpdateRunnable != null) {
            handler.removeCallbacks(ipUpdateRunnable);
        }
    }

    private void startVotingUpdates() {
        votingUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateVotingDisplay();
                handler.postDelayed(this, 3000); // Update every 3 seconds
            }
        };
        handler.post(votingUpdateRunnable);
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

    private void stopVotingUpdates() {
        if (handler != null && votingUpdateRunnable != null) {
            handler.removeCallbacks(votingUpdateRunnable);
        }
    }

    private void stopReactionsUpdates() {
        if (handler != null && reactionsUpdateRunnable != null) {
            handler.removeCallbacks(reactionsUpdateRunnable);
        }
    }

    private void updateVotingDisplay() {
        if (httpServer != null) {
            Map<Long, Integer> votingResults = httpServer.getVoteCountsForDisplay();
            if (votingResults.isEmpty()) {
                votingInfoCard.setVisibility(android.view.View.GONE);
                votingStatusText.setText("No votes yet");
            } else {
                votingInfoCard.setVisibility(android.view.View.VISIBLE);
                List<VotingResultAdapter.VotingResult> results = new ArrayList<>();
                
                for (Map.Entry<Long, Integer> entry : votingResults.entrySet()) {
                    Song song = databaseHelper.getSongById(entry.getKey());
                    if (song != null) {
                        results.add(new VotingResultAdapter.VotingResult(song, entry.getValue()));
                    }
                }
                
                // Sort by vote count descending
                results.sort((a, b) -> Integer.compare(b.voteCount, a.voteCount));
                
                votingResultAdapter.updateVotingResults(results);
                
                if (results.size() == 1) {
                    votingStatusText.setText("1 vote received");
                } else {
                    votingStatusText.setText(results.size() + " votes received");
                }
            }
        }
    }

    private void updateReactionsDisplay() {
        if (httpServer != null) {
            Song currentSong = httpServer.getCurrentSong();
            
            // Only show reactions when a song is currently being performed
            if (currentSong == null) {
                reactionsInfoCard.setVisibility(android.view.View.GONE);
                return;
            }
            
            Map<String, Integer> reactionTotals = httpServer.getReactionTotalsForDisplay();
            if (reactionTotals.isEmpty()) {
                reactionsInfoCard.setVisibility(android.view.View.VISIBLE);
                reactionsStatusText.setText("No reactions yet");
                reactionResultAdapter.updateReactionResults(new ArrayList<>());
            } else {
                reactionsInfoCard.setVisibility(android.view.View.VISIBLE);
                List<ReactionResultAdapter.ReactionResult> results = new ArrayList<>();
                
                for (Map.Entry<String, Integer> entry : reactionTotals.entrySet()) {
                    results.add(new ReactionResultAdapter.ReactionResult(entry.getKey(), entry.getValue()));
                }
                
                // Sort by reaction count descending
                results.sort((a, b) -> Integer.compare(b.reactionCount, a.reactionCount));
                
                reactionResultAdapter.updateReactionResults(results);
                
                int totalReactions = results.stream().mapToInt(r -> r.reactionCount).sum();
                if (totalReactions == 1) {
                    reactionsStatusText.setText("1 reaction");
                } else {
                    reactionsStatusText.setText(totalReactions + " reactions");
                }
            }
        }
    }

    private void showSongListOverlay() {
        overlayBackground.setVisibility(android.view.View.VISIBLE);
        songListOverlay.setVisibility(android.view.View.VISIBLE);
    }

    private void hideSongListOverlay() {
        overlayBackground.setVisibility(android.view.View.GONE);
        songListOverlay.setVisibility(android.view.View.GONE);
    }

    @Override
    public void onSongSelected(Song song) {
        hideSongListOverlay(); // Hide overlay when song is selected
        
        if (httpServer != null) {
            httpServer.setCurrentSong(song);
        }
        
        Intent intent = new Intent(this, SongDisplayActivity.class);
        intent.putExtra("song_id", song.getId());
        intent.putExtra("is_performance", true);
        startActivity(intent);
    }

    private void stopServer() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
        stopIpUpdates();
        stopVotingUpdates();
        stopReactionsUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }

    @Override
    public void onBackPressed() {
        if (songListOverlay.getVisibility() == android.view.View.VISIBLE) {
            hideSongListOverlay();
        } else {
            stopServer();
            super.onBackPressed();
        }
    }
}