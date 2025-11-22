package com.shazam.utils;

import javax.sound.sampled.AudioFormat;

/**
 * Utility functions for audio processing.
 */
public class AudioUtils {

    /**
     * Converts byte array from audio input to float array.
     * Assumes 16-bit signed PCM format.
     */
    public static float[] bytesToFloats(byte[] audioBytes, AudioFormat format) {
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        int numSamples = audioBytes.length / bytesPerSample;
        float[] samples = new float[numSamples];

        for (int i = 0; i < numSamples; i++) {
            int offset = i * bytesPerSample;

            // Convert 16-bit signed PCM to float (-1.0 to 1.0)
            if (bytesPerSample == 2) {
                short sample = (short) ((audioBytes[offset + 1] << 8) | (audioBytes[offset] & 0xFF));
                samples[i] = sample / 32768.0f;
            } else if (bytesPerSample == 1) {
                samples[i] = (audioBytes[offset] - 128) / 128.0f;
            }
        }

        return samples;
    }

    /**
     * Normalizes audio samples to the range [-1.0, 1.0].
     */
    public static float[] normalize(float[] samples) {
        float max = 0;
        for (float sample : samples) {
            max = Math.max(max, Math.abs(sample));
        }

        if (max == 0)
            return samples;

        float[] normalized = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            normalized[i] = samples[i] / max;
        }
        return normalized;
    }

    /**
     * Applies a Hamming window to reduce spectral leakage in FFT.
     */
    public static float[] applyHammingWindow(float[] samples) {
        int n = samples.length;
        float[] windowed = new float[n];

        for (int i = 0; i < n; i++) {
            double multiplier = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (n - 1));
            windowed[i] = (float) (samples[i] * multiplier);
        }

        return windowed;
    }

    /**
     * Converts stereo audio to mono by averaging channels.
     */
    public static float[] stereoToMono(float[] stereoSamples) {
        int monoLength = stereoSamples.length / 2;
        float[] mono = new float[monoLength];

        for (int i = 0; i < monoLength; i++) {
            mono[i] = (stereoSamples[i * 2] + stereoSamples[i * 2 + 1]) / 2.0f;
        }

        return mono;
    }

    /**
     * Converts frequency bin index to actual frequency in Hz.
     */
    public static double binToFrequency(int bin, int sampleRate, int fftSize) {
        return (double) bin * sampleRate / fftSize;
    }

    /**
     * Converts frequency in Hz to FFT bin index.
     */
    public static int frequencyToBin(double frequency, int sampleRate, int fftSize) {
        return (int) Math.round(frequency * fftSize / sampleRate);
    }
}
