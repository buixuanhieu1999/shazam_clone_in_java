package com.shazam.fingerprint;

import com.shazam.config.Constants;
import com.shazam.utils.AudioUtils;
import com.shazam.utils.FFT;

import java.util.*;

/**
 * Core audio fingerprinting engine.
 * Performs FFT analysis, detects spectral peaks, and generates fingerprint
 * hashes.
 */
public class AudioFingerprinter {

    /**
     * Represents a spectral peak in the time-frequency domain.
     */
    private static class Peak {
        int time; // Frame index
        int frequency; // Frequency bin

        Peak(int time, int frequency) {
            this.time = time;
            this.frequency = frequency;
        }
    }

    /**
     * Generates fingerprint hashes from audio samples.
     * 
     * @param audioSamples Audio samples as float array
     * @param songId       Song identifier (null for query audio)
     * @return List of fingerprint hashes
     */
    public static List<FingerprintHash> generateFingerprints(float[] audioSamples, String songId) {
        // Step 1: Compute spectrogram using FFT
        double[][] spectrogram = computeSpectrogram(audioSamples);

        // Step 2: Detect spectral peaks
        List<Peak> peaks = detectPeaks(spectrogram);

        // Step 3: Generate hashes from peak pairs (anchor + target zone)
        List<FingerprintHash> hashes = generateHashes(peaks, songId);

        return hashes;
    }

    /**
     * Computes the spectrogram using Short-Time Fourier Transform (STFT).
     */
    private static double[][] computeSpectrogram(float[] samples) {
        int windowSize = Constants.FFT_WINDOW_SIZE;
        int hopSize = Constants.HOP_SIZE;
        int numFrames = (samples.length - windowSize) / hopSize + 1;

        double[][] spectrogram = new double[numFrames][windowSize / 2];

        for (int frame = 0; frame < numFrames; frame++) {
            int offset = frame * hopSize;

            // Extract window
            float[] window = new float[windowSize];
            System.arraycopy(samples, offset, window, 0, windowSize);

            // Apply Hamming window
            window = AudioUtils.applyHammingWindow(window);

            // Prepare FFT input
            double[] real = new double[windowSize];
            double[] imag = new double[windowSize];
            for (int i = 0; i < windowSize; i++) {
                real[i] = window[i];
                imag[i] = 0;
            }

            // Perform FFT
            FFT.fft(real, imag);

            // Compute magnitude spectrum (only first half due to symmetry)
            for (int i = 0; i < windowSize / 2; i++) {
                spectrogram[frame][i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            }
        }

        return spectrogram;
    }

    /**
     * Detects spectral peaks in the spectrogram.
     * Uses frequency bands to ensure peaks are distributed across the spectrum.
     */
    private static List<Peak> detectPeaks(double[][] spectrogram) {
        List<Peak> peaks = new ArrayList<>();
        int numFrames = spectrogram.length;
        int numBins = spectrogram[0].length;

        // Convert frequency bands to bin indices
        int[] bandBins = new int[Constants.FREQUENCY_BANDS.length];
        for (int i = 0; i < Constants.FREQUENCY_BANDS.length; i++) {
            bandBins[i] = AudioUtils.frequencyToBin(
                    Constants.FREQUENCY_BANDS[i],
                    Constants.SAMPLE_RATE,
                    Constants.FFT_WINDOW_SIZE);
        }

        // For each time frame
        for (int t = 0; t < numFrames; t++) {
            // For each frequency band
            for (int b = 0; b < bandBins.length - 1; b++) {
                int startBin = bandBins[b];
                int endBin = Math.min(bandBins[b + 1], numBins);

                // Find local maxima in this band
                for (int f = startBin; f < endBin; f++) {
                    if (isLocalMaximum(spectrogram, t, f, numFrames, numBins)) {
                        double magnitude = spectrogram[t][f];
                        if (magnitude > Constants.PEAK_THRESHOLD) {
                            peaks.add(new Peak(t, f));
                        }
                    }
                }
            }
        }

        return peaks;
    }

    /**
     * Checks if a point is a local maximum in its neighborhood.
     */
    private static boolean isLocalMaximum(double[][] spectrogram, int t, int f, int numFrames, int numBins) {
        double value = spectrogram[t][f];
        int radius = Constants.PEAK_NEIGHBORHOOD_SIZE;

        for (int dt = -radius; dt <= radius; dt++) {
            for (int df = -radius; df <= radius; df++) {
                if (dt == 0 && df == 0)
                    continue;

                int nt = t + dt;
                int nf = f + df;

                if (nt >= 0 && nt < numFrames && nf >= 0 && nf < numBins) {
                    if (spectrogram[nt][nf] > value) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Generates hashes from peak pairs using anchor-point + target zone approach.
     */
    private static List<FingerprintHash> generateHashes(List<Peak> peaks, String songId) {
        List<FingerprintHash> hashes = new ArrayList<>();

        // Sort peaks by time
        peaks.sort(Comparator.comparingInt(p -> p.time));

        // For each anchor point
        for (int i = 0; i < peaks.size(); i++) {
            Peak anchor = peaks.get(i);
            int pairsForAnchor = 0;

            // Look for target points in the target zone
            for (int j = i + 1; j < peaks.size() && pairsForAnchor < Constants.MAX_PAIRS_PER_ANCHOR; j++) {
                Peak target = peaks.get(j);

                int timeDelta = target.time - anchor.time;

                // Check if target is in the target zone
                if (timeDelta >= Constants.TARGET_ZONE_START &&
                        timeDelta <= Constants.TARGET_ZONE_START + Constants.TARGET_ZONE_WIDTH) {

                    // Create hash from anchor frequency, target frequency, and time delta
                    long hash = FingerprintHash.createHash(anchor.frequency, target.frequency, timeDelta);

                    // Time offset is the anchor's time
                    FingerprintHash fingerprintHash = new FingerprintHash(hash, anchor.time, songId);
                    hashes.add(fingerprintHash);

                    pairsForAnchor++;
                }
            }
        }

        return hashes;
    }
}
