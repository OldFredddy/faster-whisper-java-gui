package org.oldfr;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class AudioNormalizer {

    public static void normalizeDirectory(String directoryPath, double gainFactor) throws IOException, UnsupportedAudioFileException {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Provided path is not a directory.");
        }

        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
        if (files == null || files.length == 0) {
            System.out.println("No WAV files found in the directory.");
            return;
        }

        for (File file : files) {
            normalizeWavFile(file.getAbsolutePath(), gainFactor);
        }
    }

    public static void normalizeWavFile(String filePath, double gainFactor) throws UnsupportedAudioFileException, IOException {
        File inputFile = new File(filePath);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputFile);
        AudioFormat format = audioInputStream.getFormat();
        byte[] audioBytes = audioInputStream.readAllBytes();

        try {
            int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
            boolean isBigEndian = format.isBigEndian();
            int[] samples = new int[audioBytes.length / sampleSizeInBytes];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = 0;
                for (int j = 0; j < sampleSizeInBytes; j++) {
                    int byteIndex = isBigEndian ? (sampleSizeInBytes - 1 - j) : j;
                    samples[i] |= (audioBytes[i * sampleSizeInBytes + byteIndex] & 0xFF) << (j * 8);
                }
            }

            double maxAmplitude = 0;
            for (int sample : samples) {
                maxAmplitude = Math.max(maxAmplitude, Math.abs(sample));
            }

            double compressionThreshold = 32767 * 0.5; // 50% of max amplitude for 16-bit audio
            double compressionRatio = 4.0;

            for (int i = 0; i < samples.length; i++) {
                double normalizedSample = samples[i] / maxAmplitude;
                if (Math.abs(normalizedSample) > compressionThreshold / 32767) {
                    normalizedSample = Math.signum(normalizedSample) * (compressionThreshold / 32767 + (Math.abs(normalizedSample) - compressionThreshold / 32767) / compressionRatio);
                }
                normalizedSample *= gainFactor;
                samples[i] = (int) (normalizedSample * maxAmplitude);
            }

            for (int i = 0; i < samples.length; i++) {
                for (int j = 0; j < sampleSizeInBytes; j++) {
                    int byteIndex = isBigEndian ? (sampleSizeInBytes - 1 - j) : j;
                    audioBytes[i * sampleSizeInBytes + byteIndex] = (byte) ((samples[i] >> (j * 8)) & 0xFF);
                }
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
            AudioInputStream processedAudioInputStream = new AudioInputStream(bais, format, audioBytes.length / sampleSizeInBytes);
            AudioSystem.write(processedAudioInputStream, AudioFileFormat.Type.WAVE, new File(filePath));
        } finally {
            audioInputStream.close();
        }
    }
}
