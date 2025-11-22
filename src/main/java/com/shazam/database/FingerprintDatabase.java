package com.shazam.database;

import com.shazam.fingerprint.FingerprintHash;
import com.shazam.model.Song;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.*;

/**
 * Database implementation using MySQL.
 */
public class FingerprintDatabase {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPass;

    public FingerprintDatabase() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.dbUrl = dotenv.get("DB_URL", "jdbc:mysql://localhost:3306/shazam_db?createDatabaseIfNotExist=true");
        this.dbUser = dotenv.get("DB_USER", "root");
        this.dbPass = dotenv.get("DB_PASS", "");

        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            // Create songs table
            stmt.execute("CREATE TABLE IF NOT EXISTS songs (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "title VARCHAR(255), " +
                    "artist VARCHAR(255), " +
                    "file_path VARCHAR(500), " +
                    "duration DOUBLE)");

            // Create fingerprints table
            stmt.execute("CREATE TABLE IF NOT EXISTS fingerprints (" +
                    "hash BIGINT, " +
                    "time_offset INT, " +
                    "song_id VARCHAR(36), " +
                    "FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE, " +
                    "INDEX idx_hash (hash))");

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }

    public void addSong(Song song, List<FingerprintHash> fingerprints) {
        String insertSongSQL = "INSERT INTO songs (id, title, artist, file_path, duration) VALUES (?, ?, ?, ?, ?)";
        String insertFingerprintSQL = "INSERT INTO fingerprints (hash, time_offset, song_id) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(insertSongSQL)) {
                pstmt.setString(1, song.getId());
                pstmt.setString(2, song.getTitle());
                pstmt.setString(3, song.getArtist());
                pstmt.setString(4, song.getFilePath());
                pstmt.setDouble(5, song.getDuration());
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(insertFingerprintSQL)) {
                int batchSize = 0;
                for (FingerprintHash hash : fingerprints) {
                    pstmt.setLong(1, hash.getHash());
                    pstmt.setInt(2, hash.getTimeOffset());
                    pstmt.setString(3, song.getId());
                    pstmt.addBatch();
                    batchSize++;

                    if (batchSize % 1000 == 0) {
                        pstmt.executeBatch();
                    }
                }
                pstmt.executeBatch();
            }

            conn.commit();
            System.out.println("Added song: " + song + " with " + fingerprints.size() + " fingerprints");

        } catch (SQLException e) {
            System.err.println("Error adding song: " + e.getMessage());
        }
    }

    public Map<String, List<FingerprintHash>> query(List<FingerprintHash> queryHashes) {
        Map<String, List<FingerprintHash>> matches = new HashMap<>();
        String querySQL = "SELECT hash, time_offset, song_id FROM fingerprints WHERE hash = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(querySQL)) {

            for (FingerprintHash queryHash : queryHashes) {
                pstmt.setLong(1, queryHash.getHash());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        long hash = rs.getLong("hash");
                        int timeOffset = rs.getInt("time_offset");
                        String songId = rs.getString("song_id");

                        FingerprintHash match = new FingerprintHash(hash, timeOffset, songId);
                        matches.computeIfAbsent(songId, k -> new ArrayList<>()).add(match);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error querying database: " + e.getMessage());
        }

        return matches;
    }

    public Song getSong(String songId) {
        String sql = "SELECT * FROM songs WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, songId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Song(
                            rs.getString("id"),
                            rs.getString("title"),
                            rs.getString("artist"),
                            rs.getString("file_path"),
                            rs.getDouble("duration"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting song: " + e.getMessage());
        }
        return null;
    }

    public Collection<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT * FROM songs";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                songs.add(new Song(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getString("file_path"),
                        rs.getDouble("duration")));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all songs: " + e.getMessage());
        }
        return songs;
    }

    public int getTotalFingerprints() {
        String sql = "SELECT COUNT(*) FROM fingerprints";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting fingerprints: " + e.getMessage());
        }
        return 0;
    }

    public int getSongCount() {
        String sql = "SELECT COUNT(*) FROM songs";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting songs: " + e.getMessage());
        }
        return 0;
    }

    public void clear() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM fingerprints");
            stmt.execute("DELETE FROM songs");
        } catch (SQLException e) {
            System.err.println("Error clearing database: " + e.getMessage());
        }
    }
}
