package com.shazam.fingerprint;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Unit tests for AudioFingerprinter.
 */
public class AudioFingerprintTest {

    @Test
    public void testGenerateFingerprintsFromSilence() {
        // Silent audio should produce few or no fingerprints
        float[] silence = new float[44100]; // 1 second of silence
        List<FingerprintHash> hashes = AudioFingerprinter.generateFingerprints(silence, "test-song");

        // Silence should produce very few peaks
        assertTrue(hashes.size() < 100, "Silence should not produce many fingerprints");
    }

    @Test
    public void testGenerateFingerprintsFromTone() {
        // Generate a simple sine wave
        int sampleRate = 44100;
        int duration = 2; // seconds
        float[] samples = generateSineWave(440.0, sampleRate, duration); // A4 note

        List<FingerprintHash> hashes = AudioFingerprinter.generateFingerprints(samples, "test-tone");

        // A pure tone should produce some fingerprints
        assertTrue(hashes.size() > 0, "Tone should produce fingerprints");
    }

    @Test
    public void testConsistentFingerprints() {
        // Same audio should produce same fingerprints
        float[] samples = generateSineWave(440.0, 44100, 2);

        List<FingerprintHash> hashes1 = AudioFingerprinter.generateFingerprints(samples, "song1");
        List<FingerprintHash> hashes2 = AudioFingerprinter.generateFingerprints(samples, "song2");

        // Should produce same number of hashes
        assertEquals(hashes1.size(), hashes2.size(), "Same audio should produce same number of hashes");

        // Hash values should match (ignoring song ID)
        for (int i = 0; i < hashes1.size(); i++) {
            assertEquals(hashes1.get(i).getHash(), hashes2.get(i).getHash(),
                    "Hash values should match for same audio");
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
