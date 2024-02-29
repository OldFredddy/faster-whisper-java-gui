package org.example;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Whisper {
    public static String recognize(long durationInSec, Controller controller, String pathToFile,
                                   String language, String model, String device, File file,
                                   boolean allowServiceMessages, boolean isTargetLang) throws IOException, InterruptedException {
        controller.setPB(0);
        String namefile = file.getName();
        if (isTargetLang) {
            controller.setToTextArea("\nОбработка файла:\n" + file.getName());
        } else {
            controller.setToEnglishTextArea("\nОбработка файла:\n" + file.getName());
        }
        String currentDir = Paths.get("").toAbsolutePath().toString();
        String whisperPath = Paths.get(currentDir, "whisper", "whisper-faster.exe").toString();

        // Определение суффикса и пути для временной директории
        String languageSuffix = isTargetLang ? "_" + language.toLowerCase() : "_en";
        String tempDirPath = currentDir + "/txt/";
        File tempDir = new File(tempDirPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs(); // Создаем временную директорию, если не существует
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Process[] processHolder = new Process[1];
        Future<?> processFuture = executor.submit(() -> {
            try {
                ProcessBuilder builder;
               if (device.equals("gpu")){
                    builder = new ProcessBuilder(whisperPath, pathToFile, "--model=" + model,
                           "--language=" + language, "--output_dir", tempDirPath,"--output_format=txt", "--beep_off");
               } else {
                    builder = new ProcessBuilder(whisperPath, pathToFile, "--model=" + model, "--device=" + device,
                           "--language=" + language, "--output_dir", tempDirPath,"--output_format=txt", "--beep_off");
               }
                processHolder[0] = builder.start();
                processOutput(processHolder[0], durationInSec, controller, allowServiceMessages, isTargetLang);
                processHolder[0].waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                controller.setPB(100);
            }
        });

        try {
            processFuture.get(10, TimeUnit.MINUTES);
            // После завершения процесса, ищем .srt файл во временной директории
            File[] files = tempDir.listFiles((dir, name) -> name.endsWith(".srt"));
            if (files != null && files.length > 0) {
                File srtFile = files[0]; // Предполагаем, что есть только один .srt файл
                Path targetPath = Paths.get(currentDir, "whisper", namefile.replace(".wav", "") + languageSuffix + ".srt");
                Files.move(srtFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                return targetPath.toString();
            } else {
                controller.setToTextArea("File can't be created or not found.");
                return "File can't be created or not found.";
            }
        } catch (TimeoutException e) {
            processHolder[0].destroyForcibly(); // Уничтожаем процесс при таймауте
            controller.setToTextArea("Процесс прерван из-за истечения времени ожидания");
            return "Процесс прерван из-за истечения времени ожидания";
        } catch (Exception e) {
            e.printStackTrace();
            return "Произошла ошибка";
        } finally {
            executor.shutdownNow();
            // Очистка временной директории
            for (File f : tempDir.listFiles()) {
                f.delete();
            }
            tempDir.delete();
        }
    }




    private static double convertTimeToSeconds(String time) {
        String[] parts = time.split(":");
        double minutes = Double.parseDouble(parts[0]);
        double seconds = Double.parseDouble(parts[1]);
        return minutes * 60 + seconds;
    }

    private static void processOutput(Process process, long durationInSec, Controller controller,
                                      boolean allowServiceMessages, boolean isTargetLang) throws IOException {
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
                    if (isTargetLang){
                        controller.setPB(progress);
                    }

                }
                if (allowServiceMessages) {
                    if (isTargetLang){
                        controller.setToTextArea("\n" + line);
                    } else {
                        controller.setToEnglishTextArea("\n" + line);
                    }
                } else {
                    if (line.matches(".*\\[.*\\].*")) {
                        if (isTargetLang){
                            controller.setToTextArea("\n" + line);
                        } else {
                            controller.setToEnglishTextArea("\n" + line);
                        }
                    }
                }
            }
        }
    }
}
