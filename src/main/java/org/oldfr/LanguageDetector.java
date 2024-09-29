package org.oldfr;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageDetector {

    private TextArea outputTextArea;

    public LanguageDetector(TextArea outputTextArea) {
        this.outputTextArea = outputTextArea;
    }

    public void processAllWavFiles() {
        try {
            String currentDir = Paths.get("").toAbsolutePath().toString();
            File wavDir = new File(currentDir, "wav");
            File[] wavFiles = wavDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));

            if (wavFiles == null || wavFiles.length == 0) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Информация");
                    alert.setHeaderText(null);
                    alert.setContentText("Нет WAV-файлов для обработки.");
                    alert.showAndWait();
                });
                return;
            }

            for (File wavFile : wavFiles) {
                System.out.println("Обработка файла: " + wavFile.getName());

                // Проверяем длительность аудио
                double duration = getAudioDuration(wavFile);

                // Извлекаем первые 40 секунд, если длительность больше 40 секунд
                File audioSegment;
                if (duration > 40) {
                    audioSegment = new File("temp_segment.wav");
                    extractFirst40Seconds(wavFile, audioSegment);
                } else {
                    audioSegment = wavFile;
                }

                // Определяем язык
                String detectedLanguage = detectLanguage(audioSegment);

                String message = "Файл " + wavFile.getName() + " имеет язык: " + detectedLanguage;
                System.out.println(message);

                // Обновляем UI
                Platform.runLater(() -> {
                    outputTextArea.appendText(message + "\n");
                });

                // Перемещаем оригинальный файл
                moveFileToLanguageFolder(wavFile, detectedLanguage);

                // Удаляем временный сегмент, если он был создан
                if (duration > 40 && audioSegment.exists()) {
                    audioSegment.delete();
                }
            }

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Готово");
                alert.setHeaderText(null);
                alert.setContentText("Все файлы обработаны и отсортированы по языкам.");
                alert.showAndWait();
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText(null);
                alert.setContentText("Произошла ошибка при обработке файлов: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    private void extractFirst40Seconds(File inputFile, File outputFile) throws IOException, InterruptedException {
        ProcessBuilder ffmpegPb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", inputFile.getAbsolutePath(),
                "-t", "40",
                outputFile.getAbsolutePath()
        );
        Process ffmpegProcess = ffmpegPb.start();

        // Чтение ошибок FFmpeg для диагностики
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()));
        String errorLine;
        while ((errorLine = errorReader.readLine()) != null) {
            System.err.println(errorLine);
        }

        int exitCode = ffmpegProcess.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg завершился с ошибкой, код выхода: " + exitCode);
        }
    }

    private String detectLanguage(File audioSegment) throws IOException, InterruptedException {
        String currentDir = Paths.get("").toAbsolutePath().toString();
        String whisperPath = Paths.get(currentDir, "whisper", "whisper-faster.exe").toString();

        ProcessBuilder builder = new ProcessBuilder(
                whisperPath,
                audioSegment.getAbsolutePath(),
                "--task", "transcribe",
                "--model", "tiny",
                "--output_format", "txt",
                "--verbose", "False"
        );

        Process process = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        String line;
        String detectedLanguage = null;

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            if (line.startsWith("Detected language")) {
                // Извлекаем язык из строки
                Pattern pattern = Pattern.compile("Detected language '(.+)' with probability");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    detectedLanguage = matcher.group(1);
                }
                break;
            }
        }

        process.waitFor();

        if (detectedLanguage == null) {
            throw new IOException("Не удалось определить язык аудиофайла.");
        }

        return detectedLanguage;
    }

    private void moveFileToLanguageFolder(File originalFile, String detectedLanguage) throws IOException {
        String currentDir = Paths.get("").toAbsolutePath().toString();
        Path targetDir = Paths.get(currentDir, "detect_languages", detectedLanguage);

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Path targetPath = targetDir.resolve(originalFile.getName());
        Files.move(originalFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private double getAudioDuration(File audioFile) throws IOException, InterruptedException {
        ProcessBuilder ffprobePb = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                audioFile.getAbsolutePath()
        );
        Process ffprobeProcess = ffprobePb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(ffprobeProcess.getInputStream(), StandardCharsets.UTF_8));
        String durationStr = reader.readLine();
        ffprobeProcess.waitFor();

        return Double.parseDouble(durationStr);
    }
}
