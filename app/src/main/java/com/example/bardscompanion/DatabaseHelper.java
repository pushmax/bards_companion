package com.example.bardscompanion;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "songs.db";
    private static final int DATABASE_VERSION = 1;
    
    private static final String TABLE_SONGS = "songs";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_AUTHOR = "author";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_LYRICS = "lyrics";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SONGS_TABLE = "CREATE TABLE " + TABLE_SONGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_AUTHOR + " TEXT NOT NULL,"
                + COLUMN_NAME + " TEXT NOT NULL,"
                + COLUMN_LYRICS + " TEXT NOT NULL" + ")";
        db.execSQL(CREATE_SONGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        onCreate(db);
    }

    public long addSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AUTHOR, song.getAuthor());
        values.put(COLUMN_NAME, song.getName());
        values.put(COLUMN_LYRICS, song.getLyrics());
        
        long id = db.insert(TABLE_SONGS, null, values);
        db.close();
        return id;
    }

    public Song getSong(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SONGS, null, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        Song song = null;
        if (cursor != null && cursor.moveToFirst()) {
            song = new Song(
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LYRICS))
            );
            cursor.close();
        }
        db.close();
        return song;
    }

    public List<Song> getAllSongs() {
        List<Song> songList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SONGS, null, null, null, null, null, 
                COLUMN_AUTHOR + ", " + COLUMN_NAME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LYRICS))
                );
                songList.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return songList;
    }

    public Song getSongById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SONGS, null, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        
        Song song = null;
        if (cursor.moveToFirst()) {
            song = new Song(
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LYRICS))
            );
        }
        cursor.close();
        db.close();
        return song;
    }

    public int updateSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AUTHOR, song.getAuthor());
        values.put(COLUMN_NAME, song.getName());
        values.put(COLUMN_LYRICS, song.getLyrics());

        int result = db.update(TABLE_SONGS, values, COLUMN_ID + "=?",
                new String[]{String.valueOf(song.getId())});
        db.close();
        return result;
    }

    public void deleteSong(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SONGS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void prepopulateIfEmpty() {
        List<Song> existingSongs = getAllSongs();
        if (existingSongs.isEmpty()) {
            // Prepopulate with sample songs (lyrics are intentionally left empty)
            String[][] sampleSongs = {
                {"Daft Punk", "Get lucky"},
                {"Foster the people", "Pumped up kicks"},
                {"Ricky Martin", "La vida loca"},
                {"Robin Shultz", "Sugar"},
                {"Imagine Dragons", "Radioactive"},
                {"Oasis", "Wonderwall"},
                {"Pretty Reckless", "You make me wanna die"},
                {"Theory of a Deadman", "Angel"},
                {"Nirvana", "Come as you are"},
                {"Nirvana", "Smells like teen spirit"},
                {"Green day", "Wake me up when September ends"},
                {"Depeche mode", "Personal Jesus"},
                {"Nickelback", "How you remind me"},
                {"Nickelback", "If everyone cared"},
                {"Metallica", "Fade to black"},
                {"Metallica", "Unforgiven"},
                {"Limp Bizkit", "Behind blue eyes"},
                {"Green day", "Boulevard of broken dreams"},
                {"Sunrise avenue", "Fairytale gone bad"},
                {"Aerosmith", "Dream on"},
                {"Scorpions", "Still loving you"},
                {"Sting", "Shape of my heart"},
                {"Cranberries", "Zombie"},
                {"Poets of the fall", "Carnival of Rust"},
                {"Slipknot", "Snuff"},
                {"Slipknot", "Dead memories"},
                {"Stone Sour", "Through glass"},
                {"Depeche mode", "Precious"},
                {"Megadeth", "A tout le monde"},
                {"RHCP", "Otherside"},
                {"RHCP", "Californication"},
                {"Браво", "Я то что надо"},
                {"Звери", "Рома извини"},
                {"Звери", "Районы Кварталы"},
                {"А-студио", "Ещё люблю"},
                {"Агата Кристи", "Я на тебе как на войне"},
                {"После 11 / Хелависа", "Рядом быть"},
                {"Ария", "Грязь"},
                {"Ария", "Небо тебя найдет"},
                {"Мармеладзе", "Стоша-говнозад"},
                {"Сектор газа", "Колхозный панк"},
                {"Сектор газа", "Ява"},
                {"Машина времени", "Не стоит прогибаться"},
                {"Машина времени", "Мой друг лучше всех играет Блюз"},
                {"Сплин", "Орбит без сахара"},
                {"Леприконсы", "Хали-Гали"},
                {"Ария", "Беспечный ангел"},
                {"Ария", "Потерянный рай"},
                {"Ария", "Там высоко"},
                {"Николай Носков", "Это здорово"},
                {"Люмен", "Гореть"},
                {"Сплин", "Выхода нет"}
            };

            for (String[] songData : sampleSongs) {
                Song song = new Song(0, songData[0], songData[1], ""); // Empty lyrics
                addSong(song);
            }
        }
    }
}