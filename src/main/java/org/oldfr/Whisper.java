package org.oldfr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Whisper {
    public static boolean recognize(long durationInSec, Controller controller, String pathToFile,
                                   String language, String model, String device, File file,
                                   boolean allowServiceMessages, boolean isTargetLang, boolean allowDirPipeline,
                                    String beam_size, String maxWordsInRow) throws IOException, InterruptedException {
        controller.setPB(0);
        // controller.updateFileStatus(pathToFile, Controller.FileStatus.PROCESSING);
        if (isTargetLang) {
            controller.setToTextArea("\nОбработка файла:\n" + file.getName());
        } else {
            controller.setToEnglishTextArea("\nОбработка файла:\n" + file.getName());
        }
        String currentDir = Paths.get("").toAbsolutePath().toString();
        String whisperPath = Paths.get(currentDir, "whisper", "whisper-faster.exe").toString();
        String tempDirPath = currentDir + "/txt/";
        String wavesDirPath = currentDir + "/wav/";
        File tempDir = new File(tempDirPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Process[] processHolder = new Process[1];
        Future<?> processFuture = executor.submit(() -> {
            try {
                ProcessBuilder builder = null;
                if (device.equals("gpu")) {
                    if (allowDirPipeline) {
                        builder = new ProcessBuilder(whisperPath, wavesDirPath, "--model=" + model,
                                "--language=" + language, "--output_dir", tempDirPath, "--output_format=txt", "--beep_off",
                                "--beam_size=" + beam_size);
                    }
                } else {
                    if (allowDirPipeline) {
                        builder = new ProcessBuilder(whisperPath, wavesDirPath, "--model=" + model, "--device=" + device,
                                "--language=" + language, "--output_dir", tempDirPath, "--output_format=txt", "--beep_off",
                                "--beam_size=" + beam_size);
                    }
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
            processFuture.get(10000, TimeUnit.MINUTES);
            renameTxtFile(file, pathToFile, tempDirPath);
            return true;
        } catch (TimeoutException e) {
            processHolder[0].destroyForcibly(); // Уничтожаем процесс при таймауте
            controller.setToTextArea("Процесс прерван из-за истечения времени ожидания");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            executor.shutdownNow();

        }
    }




    private static double convertTimeToSeconds(String time) {
        String[] parts = time.split(":");
        double minutes = Double.parseDouble(parts[0]);
        double seconds = Double.parseDouble(parts[1]);
        return minutes * 60 + seconds;
    }
    static Pattern startPattern = Pattern.compile("Starting work on: (.+)");
    static Pattern endPattern = Pattern.compile("Subtitles are written to '(.+)' directory.");


    private static void processOutput(Process process, long durationInSec, Controller controller,
                                      boolean allowServiceMessages, boolean isTargetLang) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            String filePath = "";
            Pattern timePattern = Pattern.compile("\\[(\\d{2}:\\d{2}\\.\\d{3}) --> ");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);

                Matcher startMatcher = startPattern.matcher(line);
                if (startMatcher.matches()) {
                    filePath = Paths.get(startMatcher.group(1)).normalize().toAbsolutePath().toString();
                    controller.updateFileStatus(filePath, Controller.FileStatus.PROCESSING);
                }

                Matcher endMatcher = endPattern.matcher(line);
                if (endMatcher.matches()) {
                    controller.updateFileStatus(filePath, Controller.FileStatus.PROCESSED);
                }

                Matcher timeMatcher = timePattern.matcher(line);
                if (timeMatcher.find()) {
                    String time = timeMatcher.group(1);
                    double currentTimeInSeconds = convertTimeToSeconds(time);
                    double progress = (currentTimeInSeconds / durationInSec) * 100;
                    if (isTargetLang) {
                        controller.setPB(progress);
                    }
                }

                if (allowServiceMessages) {
                    if (isTargetLang) {
                        controller.setToTextArea("\n" + line);
                    } else {
                        controller.setToEnglishTextArea("\n" + line);
                    }
                } else {
                    if (line.matches(".*\\[.*\\].*")) {
                        if (isTargetLang) {
                            controller.setToTextArea("\n" + line);
                        } else {
                            controller.setToEnglishTextArea("\n" + line);
                        }
                    }
                }
            }
        }
    }



    /**
     * Переименовывает текстовый файл, соответствующий временному файлу, в исходное имя файла.
     * @param originalFile Путь к исходному файлу.
     * @param tempFilePath Путь к временному файлу.
     * @param txtDirPath Путь к директории с текстовыми файлами.
     */
    public static void renameTxtFile(File originalFile, String tempFilePath, String txtDirPath) {
        // Получаем имя временного файла без расширения
        String baseName = Paths.get(tempFilePath).getFileName().toString();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = baseName.substring(0, dotIndex);
        }

        Path tempDirPath = Paths.get(txtDirPath);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDirPath, baseName + "*.txt")) {
            for (Path entry : stream) {
                // Найденный файл с расширением .txt, соответствующий baseName
                File foundTxtFile = entry.toFile();
                String newFileName = originalFile.getName();
                if (newFileName.contains(".")) {
                    newFileName = newFileName.substring(0, newFileName.lastIndexOf('.')) + ".txt";
                } else {
                    newFileName += ".txt"; // На случай, если в оригинальном имени файла нет расширения
                }

                File newFile = new File(tempDirPath.toFile(), newFileName);
                if (foundTxtFile.renameTo(newFile)) {
                    System.out.println("Файл успешно переименован в: " + newFile.getName());
                } else {
                    System.out.println("Не удалось переименовать файл.");
                }
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
