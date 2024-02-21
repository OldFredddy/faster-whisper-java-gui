package org.example;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import javax.sound.sampled.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
public class WavCutter {
    private final double t1;
    private final double t2;
    private final String inputWavPath;
    private final UUID uuid;

    public WavCutter(double t1, double t2, String inputWavPath) {
        this.t1 = t1;
        this.t2 = t2;
        this.inputWavPath = inputWavPath;
        this.uuid = UUID.randomUUID();
    }

    public File cut() throws IOException, UnsupportedAudioFileException {
        File inputFile = new File(inputWavPath);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputFile);
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(inputFile);

        double durationInSeconds = (double) audioInputStream.getFrameLength() / fileFormat.getFormat().getFrameRate();

        int startFrame = (int) Math.round(t1 * fileFormat.getFormat().getFrameRate());
        int endFrame = (int) Math.round(t2 * fileFormat.getFormat().getFrameRate());

        int bytesPerFrame = fileFormat.getFormat().getFrameSize();
        int bufferSize = (endFrame - startFrame) * bytesPerFrame;
        byte[] buffer = new byte[bufferSize];

        audioInputStream.skip(startFrame * bytesPerFrame);
        audioInputStream.read(buffer, 0, bufferSize);

        File outputFile = File.createTempFile("wavcut_" + uuid.toString(), ".wav");
        AudioSystem.write(new AudioInputStream(
                        new java.io.ByteArrayInputStream(buffer),
                        fileFormat.getFormat(),
                        buffer.length / bytesPerFrame),
                AudioFileFormat.Type.WAVE,
                outputFile);

        return outputFile;
    }


}
