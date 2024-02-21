package org.example;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Whisper {
    public static String recognize(long durationInSec, Controller controller, String pathToFile, String language, String model, String device, File file, boolean allowServiceMessages) throws IOException, InterruptedException {
        controller.setPB(0);
        File forExtractFilename = new File(pathToFile);
        String namefile = forExtractFilename.getName();
        controller.setToTextArea("\nОбработка файла:\n" + file.getName());
        String currentDir = Paths.get("").toAbsolutePath().toString();
        String whisperPath = Paths.get(currentDir, "whisper", "whisper-faster.exe").toString();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Process[] processHolder = new Process[1]; // Хранилище для процесса

        Future<?> processFuture = executor.submit(() -> {
            try {
                ProcessBuilder builder;
                if (language.equals("None")){
                    builder = new ProcessBuilder(whisperPath, pathToFile, "--model=" + model, "--device=" + device,"--beep_off");
                } else {
                    builder = new ProcessBuilder(whisperPath, pathToFile, "--model=" + model, "--device=" + device, "--language=" + language,"--beep_off");
                }
                processHolder[0] = builder.start();
                processOutput(processHolder[0], durationInSec, controller, allowServiceMessages);
                processHolder[0].waitFor();
            } catch (IOException | InterruptedException e) {
                // Здесь можно логировать ошибку, но не выбрасывать её повторно
            } finally {
                controller.setPB(100);
            }
        });

        try {
            processFuture.get(10, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            if (processHolder[0] != null) processHolder[0].destroyForcibly(); // Уничтожаем процесс при таймауте
            System.out.println("Процесс прерван из-за истечения времени ожидания");
            controller.setToTextArea("Пропуск файла, превышен timeout ожидания");
            return "Пропуск файла, превышен timeout ожидания";
        } catch (Exception e) {
            e.printStackTrace();
            return "Произошла ошибка";
        } finally {
            if (processHolder[0] != null) processHolder[0].destroyForcibly(); // Убедитесь, что процесс уничтожен в любом случае
            executor.shutdownNow();
        }

        String baseName = namefile.contains(".") ? namefile.substring(0, namefile.lastIndexOf('.')) : namefile;
        File srtFile = new File(currentDir + "/whisper/" + baseName + ".srt");

        if (srtFile.exists()) {
            return srtFile.getAbsolutePath();
        } else {
            controller.setToTextArea("File can't be created.");
            return "File can't be created.";
        }
    }


    private static double convertTimeToSeconds(String time) {
        String[] parts = time.split(":");
        double minutes = Double.parseDouble(parts[0]);
        double seconds = Double.parseDouble(parts[1]);
        return minutes * 60 + seconds;
    }

    private static void processOutput(Process process, long durationInSec, Controller controller, boolean allowServiceMessages) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            Pattern timePattern = Pattern.compile("\\[(\\d{2}:\\d{2}\\.\\d{3}) --> ");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                Matcher matcher = timePattern.matcher(line);
                if (matcher.find()) {
                    String time = matcher.group(1);
                    double currentTimeInSeconds = convertTimeToSeconds(time);
                    double progress = (currentTimeInSeconds / durationInSec) * 100;
                    controller.setPB(progress);
                }
                if (allowServiceMessages) {
                    controller.setToTextArea("\n" + line);
                } else {
                    if (line.matches(".*\\[.*\\].*")) {
                        controller.setToTextArea("\n" + line);
                    }
                }
            }
        }
    }
}
