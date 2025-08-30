package com.example.bardscompanion;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PerformanceActivity extends AppCompatActivity implements PerformanceSongAdapter.OnSongSelectListener {
    private SimpleHttpServer httpServer;
    private DatabaseHelper databaseHelper;
    private TextView ipAddressText;
    private TextView serverStatusText;
    private Button openHotspotSettingsButton;
    private Button stopServerButton;
    private RecyclerView performanceSongsRecyclerView;
    private PerformanceSongAdapter performanceSongAdapter;
    private Handler handler;
    private Runnable ipUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);

        initViews();
        setupDatabase();
        setupServer();
        setupRecyclerView();
        setupClickListeners();
        loadSongs();
        startIpUpdates();
    }

    private void initViews() {
        ipAddressText = findViewById(R.id.ipAddressText);
        serverStatusText = findViewById(R.id.serverStatusText);
        openHotspotSettingsButton = findViewById(R.id.openHotspotSettingsButton);
        stopServerButton = findViewById(R.id.stopServerButton);
        performanceSongsRecyclerView = findViewById(R.id.performanceSongsRecyclerView);
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

    @Override
    public void onSongSelected(Song song) {
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }

    @Override
    public void onBackPressed() {
        stopServer();
        super.onBackPressed();
    }
}