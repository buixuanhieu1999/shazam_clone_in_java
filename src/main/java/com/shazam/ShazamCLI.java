package com.shazam;

import com.shazam.audio.AudioCapture;
import com.shazam.config.Constants;
import com.shazam.database.FingerprintDatabase;
import com.shazam.fingerprint.AudioFingerprinter;
import com.shazam.fingerprint.FingerprintHash;
import com.shazam.matcher.SongMatcher;
import com.shazam.matcher.SongMatcher.MatchResult;
import com.shazam.model.Song;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Command-line interface for the Shazam clone.
 * 
 * Usage:
 * java -jar shazam-clone.jar index <directory|file>
 * java -jar shazam-clone.jar listen <duration_seconds>
 * java -jar shazam-clone.jar identify <file>
 * java -jar shazam-clone.jar list
 */
public class ShazamCLI {

    private static final FingerprintDatabase database = new FingerprintDatabase();
    private static final SongMatcher matcher = new SongMatcher(database);

    public static void main(String[] args) {
        printBanner();

        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();

        try {
            switch (command) {
                case "index":
                    if (args.length < 2) {
                        System.out.println("Error: Please specify a directory or file to index");
                        printUsage();
                        return;
                    }
                    indexCommand(args[1]);
                    break;

                case "add":
                    if (args.length < 4) {
                        System.out.println("Error: Usage: add <file> <artist> <title>");
                        printUsage();
                        return;
                    }
                    addSongCommand(args[1], args[2], args[3]);
                    break;

                case "listen":
                    int duration = args.length >= 2 ? Integer.parseInt(args[1]) : 10;
                    listenCommand(duration);
                    break;

                case "identify":
                    if (args.length < 2) {
                        System.out.println("Error: Please specify a file to identify");
                        printUsage();
                        return;
                    }
                    identifyCommand(args[1]);
                    break;

                case "list":
                    listCommand();
                    break;

                default:
                    System.out.println("Error: Unknown command '" + command + "'");
                    printUsage();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Indexes audio files from a directory or single file.
     */
    private static void indexCommand(String path) {
        File file = new File(path);

        if (!file.exists()) {
            System.out.println("Error: Path does not exist: " + path);
            return;
        }

        int indexed = 0;

        if (file.isDirectory()) {
            System.out.println("Indexing directory: " + path);
            File[] files = file.listFiles(
                    (dir, name) -> name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".mp3"));

            if (files == null || files.length == 0) {
                System.out.println("No audio files found in directory");
                return;
            }

            for (File audioFile : files) {
                if (indexFile(audioFile)) {
                    indexed++;
                }
            }
        } else {
            if (indexFile(file)) {
                indexed++;
            }
        }

        System.out.println("\n✓ Successfully indexed " + indexed + " song(s)");
        System.out.println("Database now contains " + database.getSongCount() + " songs with " +
                database.getTotalFingerprints() + " fingerprints");
    }

    /**
     * Adds a single song with custom metadata.
     */
    private static void addSongCommand(String filePath, String artist, String title) {
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Error: File does not exist: " + filePath);
            return;
        }

        if (indexFileWithMetadata(file, artist, title)) {
            System.out.println("\n✓ Successfully added song: " + artist + " - " + title);
            System.out.println("Database now contains " + database.getSongCount() + " songs with " +
                    database.getTotalFingerprints() + " fingerprints");
        }
    }

    /**
     * Indexes a single audio file with custom metadata.
     */
    private static boolean indexFileWithMetadata(File file, String artist, String title) {
        try {
            System.out.println("\nProcessing: " + file.getName());

            // Read audio
            float[] samples = AudioCapture.readFromFile(file.getAbsolutePath());
            double duration = AudioCapture.getFileDuration(file.getAbsolutePath());

            // Generate fingerprints with custom metadata
            Song song = new Song(title, artist, file.getAbsolutePath(), duration);

            List<FingerprintHash> fingerprints = AudioFingerprinter.generateFingerprints(samples, song.getId());

            // Add to database
            database.addSong(song, fingerprints);

            return true;

        } catch (IOException | UnsupportedAudioFileException e) {
            System.err.println("  Error processing file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Indexes a single audio file (auto-detects metadata from filename).
     * Supports format: "Artist - Song Title.wav"
     */
    private static boolean indexFile(File file) {
        try {
            System.out.println("\nProcessing: " + file.getName());

            // Read audio
            float[] samples = AudioCapture.readFromFile(file.getAbsolutePath());
            double duration = AudioCapture.getFileDuration(file.getAbsolutePath());

            // Parse artist and title from filename
            String fileNameWithoutExt = getFileNameWithoutExtension(file.getName());
            String artist = "Unknown Artist";
            String title = fileNameWithoutExt;

            // Check if filename contains " - " separator
            if (fileNameWithoutExt.contains(" - ")) {
                String[] parts = fileNameWithoutExt.split(" - ", 2); // Split into max 2 parts
                if (parts.length == 2) {
                    artist = parts[0].trim();
                    title = parts[1].trim();
                }
            }

            // Generate fingerprints
            Song song = new Song(title, artist, file.getAbsolutePath(), duration);

            List<FingerprintHash> fingerprints = AudioFingerprinter.generateFingerprints(samples, song.getId());

            // Add to database
            database.addSong(song, fingerprints);

            System.out.println("  ✓ Indexed: " + artist + " - " + title);

            return true;

        } catch (IOException | UnsupportedAudioFileException e) {
            System.err.println("  Error processing file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Records audio from microphone and identifies the song.
     */
    private static void listenCommand(int durationSeconds) {
        if (database.getSongCount() == 0) {
            System.out.println("Error: No songs in database. Please index some songs first.");
            return;
        }

        try {
            System.out.println("\n🎤 Listening for " + durationSeconds + " seconds...");
            System.out.println("Play a song near your microphone!");

            // Record audio
            float[] samples = AudioCapture.recordFromMicrophone(durationSeconds);

            // Identify
            identifySamples(samples);

        } catch (LineUnavailableException e) {
            System.err.println("Error: Microphone not available - " + e.getMessage());
        }
    }

    /**
     * Identifies a song from an audio file.
     */
    private static void identifyCommand(String filePath) {
        if (database.getSongCount() == 0) {
            System.out.println("Error: No songs in database. Please index some songs first.");
            return;
        }

        try {
            System.out.println("\n🔍 Analyzing: " + filePath);

            // Read audio
            float[] samples = AudioCapture.readFromFile(filePath);

            // Identify
            identifySamples(samples);

        } catch (IOException | UnsupportedAudioFileException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Identifies audio samples against the database.
     */
    private static void identifySamples(float[] samples) {
        System.out.println("Generating fingerprints...");
        List<FingerprintHash> queryHashes = AudioFingerprinter.generateFingerprints(samples, null);
        System.out.println("Generated " + queryHashes.size() + " fingerprints");

        System.out.println("Searching database...");
        List<MatchResult> matches = matcher.getTopMatches(queryHashes, Constants.TOP_MATCHES_TO_DISPLAY);

        if (matches.isEmpty()) {
            System.out.println("\n❌ No matches found");
            System.out.println("The song may not be in the database, or the audio quality is too poor.");
        } else {
            System.out.println("\n✓ Found " + matches.size() + " match(es):\n");

            for (int i = 0; i < matches.size(); i++) {
                MatchResult match = matches.get(i);
                String rank = (i == 0) ? "🎵 BEST MATCH" : "  #" + (i + 1);
                System.out.println(rank + ": " + match);
            }
        }
    }

    /**
     * Lists all songs in the database.
     */
    private static void listCommand() {
        if (database.getSongCount() == 0) {
            System.out.println("Database is empty. Use 'index' command to add songs.");
            return;
        }

        System.out.println("\n📚 Songs in database (" + database.getSongCount() + " total):\n");

        int i = 1;
        for (Song song : database.getAllSongs()) {
            System.out.println(i + ". " + song);
            System.out.println("   Path: " + song.getFilePath());
            i++;
        }

        System.out.println("\nTotal fingerprints: " + database.getTotalFingerprints());
    }

    /**
     * Prints the application banner.
     */
    private static void printBanner() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║     🎵  SHAZAM CLONE v1.0  🎵         ║");
        System.out.println("║   Audio Fingerprinting System          ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Prints usage information.
     */
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar shazam-clone.jar index <directory|file>");
        System.out.println("      Index audio files (auto-detects metadata from filename)");
        System.out.println();
        System.out.println("  java -jar shazam-clone.jar add <file> <artist> <title>");
        System.out.println("      Add a single song with custom artist and title");
        System.out.println();
        System.out.println("  java -jar shazam-clone.jar listen [duration_seconds]");
        System.out.println("      Record from microphone and identify the song (default: 10s)");
        System.out.println();
        System.out.println("  java -jar shazam-clone.jar identify <file>");
        System.out.println("      Identify a song from an audio file");
        System.out.println();
        System.out.println("  java -jar shazam-clone.jar list");
        System.out.println("      List all indexed songs");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar shazam-clone.jar index ./music");
        System.out.println("  java -jar shazam-clone.jar add song.wav \"The Beatles\" \"Hey Jude\"");
        System.out.println("  java -jar shazam-clone.jar listen 15");
        System.out.println("  java -jar shazam-clone.jar identify song.wav");
    }

    /**
     * Gets filename without extension.
     */
    private static String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}
