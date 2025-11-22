package com.shazam.audio;

import com.shazam.config.Constants;
import com.shazam.utils.AudioUtils;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Handles audio capture from microphone and file reading.
 */
public class AudioCapture {

    /**
     * Records audio from the system microphone for a specified duration.
     * 
     * @param durationSeconds Duration to record in seconds
     * @return Audio samples as float array
     * @throws LineUnavailableException If microphone is not available
     */
    public static float[] recordFromMicrophone(int durationSeconds) throws LineUnavailableException {
        AudioFormat format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone not supported");
        }

        TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();

        System.out.println("Recording... (" + durationSeconds + " seconds)");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[Constants.RECORDING_BUFFER_SIZE];

        int bytesToRead = (int) (format.getFrameRate() * format.getFrameSize() * durationSeconds);
        int bytesRead = 0;

        while (bytesRead < bytesToRead) {
            int count = microphone.read(buffer, 0, Math.min(buffer.length, bytesToRead - bytesRead));
            if (count > 0) {
                out.write(buffer, 0, count);
                bytesRead += count;
            }
        }

        microphone.stop();
        microphone.close();

        System.out.println("Recording complete!");

        byte[] audioBytes = out.toByteArray();
        return AudioUtils.bytesToFloats(audioBytes, format);
    }

    /**
     * Reads audio from a WAV file.
     * 
     * @param filePath Path to the WAV file
     * @return Audio samples as float array
     * @throws IOException                   If file cannot be read
     * @throws UnsupportedAudioFileException If file format is not supported
     */
    public static float[] readFromFile(String filePath) throws IOException, UnsupportedAudioFileException {
        File audioFile = new File(filePath);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = audioStream.getFormat();

        // Convert to our standard format if needed
        AudioFormat targetFormat = getAudioFormat();
        if (!format.matches(targetFormat)) {
            audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
            format = targetFormat;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[Constants.RECORDING_BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = audioStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }

        audioStream.close();

        byte[] audioBytes = out.toByteArray();
        return AudioUtils.bytesToFloats(audioBytes, format);
    }

    /**
     * Gets the duration of an audio file in seconds.
     */
    public static double getFileDuration(String filePath) throws IOException, UnsupportedAudioFileException {
        File audioFile = new File(filePath);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = audioStream.getFormat();
        long frames = audioStream.getFrameLength();
        double durationInSeconds = (frames + 0.0) / format.getFrameRate();
        audioStream.close();
        return durationInSeconds;
    }

    /**
     * Returns the standard audio format for this application.
     */
    private static AudioFormat getAudioFormat() {
        return new AudioFormat(
                Constants.SAMPLE_RATE,
                Constants.BITS_PER_SAMPLE,
                Constants.CHANNELS,
                true, // signed
                false // little-endian
        );
    }
}
