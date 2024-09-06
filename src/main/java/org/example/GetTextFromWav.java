package org.example;

import javafx.concurrent.Task;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.lang.management.GarbageCollectorMXBean;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class GetTextFromWav  {
    private  Controller contr;
    private UUID uuid;
public static double progress;
    GetTextFromWav(Controller contr){
        this.contr=contr;
    }
    public boolean startWhisper(Path pathToFile, String device, String model,
                                    String language, boolean allowServiceMessages,
                                    long filterLength, boolean isTargetLang,
                                String beam_size, String maxWordsInRow) throws UnsupportedAudioFileException, IOException, InterruptedException {
        uuid = UUID.randomUUID();
        int SampleRate;
        File file = new File(pathToFile.toUri());
        File fileForGetFilename = new File(pathToFile.toUri());
        AudioInputStream in = AudioSystem.getAudioInputStream(file);
        AudioInputStream din = null;
        AudioFormat baseFormat = in.getFormat();
        SampleRate = (int) baseFormat.getSampleRate();
        long t2 = in.getFrameLength();
        t2 = t2 / SampleRate - 1;
        if (t2<filterLength){
            List<String> temp = new ArrayList<>();
                    temp.add("Файл меньше заданного фильтра длительности!");
            return false;
        }
        WavCutter wavCutter = new WavCutter(0, t2, file.getAbsolutePath());
        file = wavCutter.cut();
        if (SampleRate != 16000) {
            Resampler resampler = new Resampler();
            String newPath = String.valueOf(Files.createTempFile("wavcut_" + uuid.toString(), ".wav"));
            resampler.resample(file.getAbsolutePath(), newPath);
            file = new File(newPath);
            pathToFile = Paths.get(newPath);
        }
        boolean isSuccess = Whisper.recognize(t2, contr, pathToFile.toString(),
                language, model, device, fileForGetFilename, allowServiceMessages, isTargetLang, false, beam_size, maxWordsInRow );

        return isSuccess;
    }

    public static List<String> readFileLines(String filePath) {
        File file = new File(filePath);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }



}

