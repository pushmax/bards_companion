package com.example.bardscompanion;

public class Song {
    private long id;
    private String author;
    private String name;
    private String lyrics;

    public Song() {}

    public Song(String author, String name, String lyrics) {
        this.author = author;
        this.name = name;
        this.lyrics = lyrics;
    }

    public Song(long id, String author, String name, String lyrics) {
        this.id = id;
        this.author = author;
        this.name = name;
        this.lyrics = lyrics;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }
}