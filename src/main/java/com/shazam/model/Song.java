package com.shazam.model;

import java.util.UUID;

/**
 * Represents a song with metadata.
 */
public class Song implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String title;
    private final String artist;
    private final String filePath;
    private final double duration; // in seconds

    public Song(String title, String artist, String filePath, double duration) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.artist = artist;
        this.filePath = filePath;
        this.duration = duration;
    }

    public Song(String id, String title, String artist, String filePath, double duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.filePath = filePath;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getFilePath() {
        return filePath;
    }

    public double getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%.1fs)", artist, title, duration);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Song song = (Song) o;
        return id.equals(song.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
