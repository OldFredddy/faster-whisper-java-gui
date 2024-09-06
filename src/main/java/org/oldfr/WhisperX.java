package org.oldfr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhisperX {
    public static boolean recognize(long durationInSec, Controller controller, String pathToFile,
                                    String language, String model, String device, File file,
                                    boolean allowServiceMessages, boolean isTargetLang, boolean allowDirPipeline,
                                    String beam_size, String maxWordsInRow) throws IOException, InterruptedException, URISyntaxException {
        controller.setPB(0);
        if (isTargetLang) {
            controller.setToTextArea("\nОбработка файла:\n" + file.getName());
        } else {
            controller.setToEnglishTextArea("\nОбработка файла:\n" + file.getName());
        }
        controller.setToTextArea("\nВнимание в области предпросмотра не будет текста!\n" + "\nКонтролируйте обработку по загрузке процессора и выводу сообщений!\n");
        String jarPath = new File(WhisperX.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        String pythonExecutablePath = Paths.get(jarPath, "whisperX", "venv", "Scripts", "python.exe").toString();
        String mainScriptPath = Paths.get(jarPath, "whisperX", "whisperX", "__main__.py").toString();
        String tempDirPath = Paths.get(jarPath, "txt").toString();

        List<String> command = new ArrayList<>();
        command.add(pythonExecutablePath);
        command.add(mainScriptPath);
        command.addAll(controller.waveFilesAbsPath);
        command.add("--model=" + model);
        command.add("--language=" + language);
        command.add("--output_dir=" + tempDirPath);
        command.add("--output_format=vtt");
        if (device.equals("gpu")){
            command.add("--device="+"cuda");
        } else {
            command.add("--device="+device);
        }

        command.add("--diarize");
        if (device.equals("cpu")){
            command.add("--compute_type=float32");
        }

        command.add("--hf_token=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");
      //  command.add("--highlight_words True");
        command.add("--print_progress=True");

        File tempDir = new File(tempDirPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Process[] processHolder = new Process[1];
        Future<?> processFuture = executor.submit(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true); // Объединяем потоки вывода и ошибок
                processHolder[0] = builder.start();

                // Чтение вывода
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(processHolder[0].getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line); // Выводим строки в консоль
                        controller.setToTextArea(line);
                    }
                }

                processHolder[0].waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                controller.setPB(100);
            }
        });

        try {
            processFuture.get(10000, TimeUnit.MINUTES);
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
    static Pattern startPattern = Pattern.compile("Starting transcription on: (.+)");
    static Pattern endPattern = Pattern.compile("Subtitles are written to '(.+)' directory.");


    private static void processOutput(Process process, long durationInSec, Controller controller,
                                      boolean allowServiceMessages, boolean isTargetLang) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            String currentFilePath = "";
            Pattern progressPattern = Pattern.compile("Progress: (\\d+\\.\\d+)%");
            Pattern fileStartPattern = Pattern.compile(">>Performing transcription...");
            Pattern fileEndPattern = Pattern.compile(">>Performing (.+)");

            // Индекс текущего файла в списке
            int currentFileIndex = 0;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);

                // Обработка начала обработки файла
                Matcher startMatcher = fileStartPattern.matcher(line);
                if (startMatcher.find()) {
                    if (currentFileIndex < controller.waveFilesAbsPath.size()) {
                        currentFilePath = controller.waveFilesAbsPath.get(currentFileIndex++);
                        controller.updateFileStatus(currentFilePath, Controller.FileStatus.PROCESSING);
                    }
                }

                // Обновление прогресса по строкам "Progress: X%"
                Matcher progressMatcher = progressPattern.matcher(line);
                if (progressMatcher.find()) {
                    double progress = Double.parseDouble(progressMatcher.group(1));
                    controller.setPB(progress);
                    if (progress == 100.0 && !currentFilePath.isEmpty()) {
                        controller.updateFileStatus(currentFilePath, Controller.FileStatus.PROCESSED);
                        currentFilePath = "";
                    }
                }

                // Обработка конца обработки файла
                Matcher endMatcher = fileEndPattern.matcher(line);
                if (endMatcher.find()) {
                    if (!currentFilePath.isEmpty()) {
                        controller.updateFileStatus(currentFilePath, Controller.FileStatus.PROCESSED);
                        currentFilePath = "";
                    }
                }

                // Логирование
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
