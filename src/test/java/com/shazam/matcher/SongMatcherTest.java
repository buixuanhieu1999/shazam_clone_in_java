package com.shazam.matcher;

import com.shazam.database.FingerprintDatabase;
import com.shazam.fingerprint.AudioFingerprinter;
import com.shazam.fingerprint.FingerprintHash;
import com.shazam.model.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SongMatcher.
 */
public class SongMatcherTest {

    private FingerprintDatabase database;
    private SongMatcher matcher;

    @BeforeEach
    public void setUp() {
        database = new FingerprintDatabase();
        matcher = new SongMatcher(database);
    }

    @Test
    public void testNoMatchesInEmptyDatabase() {
        float[] query = generateSineWave(440.0, 44100, 1);
        List<FingerprintHash> queryHashes = AudioFingerprinter.generateFingerprints(query, null);

        List<SongMatcher.MatchResult> matches = matcher.findMatches(queryHashes);

        assertTrue(matches.isEmpty(), "Empty database should return no matches");
    }

    @Test
    public void testExactMatch() {
        // Add a song to database
        float[] audio = generateSineWave(440.0, 44100, 2);
        Song song = new Song("Test Song", "Test Artist", "test.wav", 2.0);
        List<FingerprintHash> fingerprints = AudioFingerprinter.generateFingerprints(audio, song.getId());
        database.addSong(song, fingerprints);

        // Query with the same audio
        List<FingerprintHash> queryHashes = AudioFingerprinter.generateFingerprints(audio, null);
        List<SongMatcher.MatchResult> matches = matcher.findMatches(queryHashes);

        assertFalse(matches.isEmpty(), "Should find at least one match");

        SongMatcher.MatchResult topMatch = matches.get(0);
        assertEquals(song.getId(), topMatch.getSong().getId(), "Should match the correct song");
        assertTrue(topMatch.getConfidence() > 0.5, "Confidence should be high for exact match");
    }

    @Test
    public void testNoMatchForDifferentSong() {
        // Add a 440 Hz tone
        float[] audio1 = generateSineWave(440.0, 44100, 2);
        Song song1 = new Song("Song A", "Artist A", "a.wav", 2.0);
        List<FingerprintHash> fingerprints1 = AudioFingerprinter.generateFingerprints(audio1, song1.getId());
        database.addSong(song1, fingerprints1);

        // Query with a different frequency (880 Hz)
        float[] audio2 = generateSineWave(880.0, 44100, 2);
        List<FingerprintHash> queryHashes = AudioFingerprinter.generateFingerprints(audio2, null);
        List<SongMatcher.MatchResult> matches = matcher.findMatches(queryHashes);

        // Different tones should not match well
        if (!matches.isEmpty()) {
            assertTrue(matches.get(0).getConfidence() < 0.3,
                    "Different songs should have low confidence");
        }
    }

    /**
     * Generates a sine wave for testing.
     */
    private float[] generateSineWave(double frequency, int sampleRate, int durationSeconds) {
        int numSamples = sampleRate * durationSeconds;
        float[] samples = new float[numSamples];

        for (int i = 0; i < numSamples; i++) {
            double time = (double) i / sampleRate;
            samples[i] = (float) Math.sin(2 * Math.PI * frequency * time);
        }

        return samples;
    }
}
