package com.shazam.config;

/**
 * Configuration constants for the audio fingerprinting system.
 * These values are tuned for optimal performance and accuracy.
 */
public class Constants {
    
    // Audio Processing Constants
    public static final int SAMPLE_RATE = 44100; // Hz - Standard CD quality
    public static final int FFT_WINDOW_SIZE = 4096; // Samples per FFT window
    public static final int OVERLAP_FACTOR = 4; // Overlap between consecutive windows
    public static final int HOP_SIZE = FFT_WINDOW_SIZE / OVERLAP_FACTOR; // 1024 samples
    
    // Frequency Range for Peak Detection (in Hz)
    public static final int MIN_FREQUENCY = 40; // Ignore very low frequencies
    public static final int MAX_FREQUENCY = 5000; // Focus on human-audible range
    
    // Frequency bins for peak detection (divide spectrum into bands)
    public static final int[] FREQUENCY_BANDS = {40, 80, 120, 180, 300, 500, 800, 1200, 2000, 3000, 5000};
    
    // Fingerprinting Parameters
    public static final int PEAK_NEIGHBORHOOD_SIZE = 10; // Radius for local maxima detection
    public static final double PEAK_THRESHOLD = 0.5; // Minimum magnitude for a peak (normalized)
    
    // Target Zone for Anchor-Point Pairing
    public static final int TARGET_ZONE_START = 1; // Time frames after anchor
    public static final int TARGET_ZONE_WIDTH = 10; // Time frames in target zone
    public static final int MAX_PAIRS_PER_ANCHOR = 5; // Limit pairs to avoid explosion
    
    // Matching Parameters
    public static final int MIN_MATCHING_HASHES = 5; // Minimum matches to consider a song
    public static final double MIN_CONFIDENCE_THRESHOLD = 0.1; // Minimum confidence score (0-1)
    public static final int TIME_DELTA_TOLERANCE = 2; // Frames tolerance for time alignment
    
    // Audio Capture
    public static final int RECORDING_BUFFER_SIZE = 4096; // Bytes
    public static final int BITS_PER_SAMPLE = 16; // 16-bit audio
    public static final int CHANNELS = 1; // Mono
    
    // Display
    public static final int TOP_MATCHES_TO_DISPLAY = 5; // Show top N matches
    
    private Constants() {
        // Prevent instantiation
    }
}
