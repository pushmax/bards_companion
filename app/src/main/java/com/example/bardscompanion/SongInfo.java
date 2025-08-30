package com.example.bardscompanion;

public class SongInfo {
    private long id;
    private String author;
    private String name;
    private int votes;

    public SongInfo() {}

    public SongInfo(long id, String author, String name) {
        this.id = id;
        this.author = author;
        this.name = name;
        this.votes = 0;
    }

    public SongInfo(long id, String author, String name, int votes) {
        this.id = id;
        this.author = author;
        this.name = name;
        this.votes = votes;
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

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }
}