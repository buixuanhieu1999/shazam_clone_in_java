package com.shazam.utils;

/**
 * Simple FFT implementation using Cooley-Tukey algorithm.
 * This is a self-contained implementation to avoid external dependencies.
 */
public class FFT {

    /**
     * Computes the Fast Fourier Transform of the input data.
     * The input array length must be a power of 2.
     * 
     * @param real Real part of the input (will be modified in-place)
     * @param imag Imaginary part of the input (will be modified in-place)
     */
    public static void fft(double[] real, double[] imag) {
        int n = real.length;

        if (n == 1)
            return;

        // Check if n is a power of 2
        if ((n & (n - 1)) != 0) {
            throw new IllegalArgumentException("FFT size must be a power of 2");
        }

        // Bit-reversal permutation
        int bits = (int) (Math.log(n) / Math.log(2));
        for (int i = 0; i < n; i++) {
            int j = reverseBits(i, bits);
            if (j > i) {
                double tempReal = real[i];
                double tempImag = imag[i];
                real[i] = real[j];
                imag[i] = imag[j];
                real[j] = tempReal;
                imag[j] = tempImag;
            }
        }

        // Cooley-Tukey FFT
        for (int len = 2; len <= n; len *= 2) {
            double angle = -2 * Math.PI / len;
            double wlenReal = Math.cos(angle);
            double wlenImag = Math.sin(angle);

            for (int i = 0; i < n; i += len) {
                double wReal = 1;
                double wImag = 0;

                for (int j = 0; j < len / 2; j++) {
                    int idx1 = i + j;
                    int idx2 = i + j + len / 2;

                    double tReal = wReal * real[idx2] - wImag * imag[idx2];
                    double tImag = wReal * imag[idx2] + wImag * real[idx2];

                    real[idx2] = real[idx1] - tReal;
                    imag[idx2] = imag[idx1] - tImag;
                    real[idx1] = real[idx1] + tReal;
                    imag[idx1] = imag[idx1] + tImag;

                    double tempReal = wReal * wlenReal - wImag * wlenImag;
                    wImag = wReal * wlenImag + wImag * wlenReal;
                    wReal = tempReal;
                }
            }
        }
    }

    /**
     * Reverses the bits of an integer.
     */
    private static int reverseBits(int x, int bits) {
        int result = 0;
        for (int i = 0; i < bits; i++) {
            result = (result << 1) | (x & 1);
            x >>= 1;
        }
        return result;
    }

    /**
     * Computes the magnitude spectrum from FFT output.
     * 
     * @param real Real part of FFT output
     * @param imag Imaginary part of FFT output
     * @return Magnitude spectrum
     */
    public static double[] getMagnitude(double[] real, double[] imag) {
        int n = real.length;
        double[] magnitude = new double[n];

        for (int i = 0; i < n; i++) {
            magnitude[i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
        }

        return magnitude;
    }
}
