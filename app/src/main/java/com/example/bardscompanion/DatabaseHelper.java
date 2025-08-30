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
}