package com.example.bardscompanion;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class AddEditSongActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private TextInputEditText authorEditText;
    private TextInputEditText nameEditText;
    private TextInputEditText lyricsEditText;
    private Button saveButton;
    private Button cancelButton;
    
    private Song currentSong;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_song);

        initViews();
        setupDatabase();
        checkEditMode();
        setupClickListeners();
    }

    private void initViews() {
        authorEditText = findViewById(R.id.authorEditText);
        nameEditText = findViewById(R.id.nameEditText);
        lyricsEditText = findViewById(R.id.lyricsEditText);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    private void setupDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }

    private void checkEditMode() {
        long songId = getIntent().getLongExtra("song_id", -1);
        if (songId != -1) {
            isEditMode = true;
            currentSong = databaseHelper.getSong(songId);
            if (currentSong != null) {
                setTitle(getString(R.string.edit_song));
                populateFields();
            }
        } else {
            setTitle(getString(R.string.add_song));
        }
    }

    private void populateFields() {
        authorEditText.setText(currentSong.getAuthor());
        nameEditText.setText(currentSong.getName());
        lyricsEditText.setText(currentSong.getLyrics());
    }

    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> saveSong());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void saveSong() {
        String author = authorEditText.getText() != null ? 
                authorEditText.getText().toString().trim() : "";
        String name = nameEditText.getText() != null ? 
                nameEditText.getText().toString().trim() : "";
        String lyrics = lyricsEditText.getText() != null ? 
                lyricsEditText.getText().toString().trim() : "";

        if (author.isEmpty() || name.isEmpty() || lyrics.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditMode && currentSong != null) {
            currentSong.setAuthor(author);
            currentSong.setName(name);
            currentSong.setLyrics(lyrics);
            databaseHelper.updateSong(currentSong);
        } else {
            Song newSong = new Song(author, name, lyrics);
            databaseHelper.addSong(newSong);
        }

        Toast.makeText(this, getString(R.string.song_saved), Toast.LENGTH_SHORT).show();
        finish();
    }
}