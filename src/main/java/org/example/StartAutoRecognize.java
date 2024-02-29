package org.example;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.List;

public class StartAutoRecognize
{
    private  Controller contr;

    StartAutoRecognize(Controller contr){
    this.contr=contr;
}

    public void startRec(List <String> waveFilesAbsPath, String language, String device, String smodel,
                         boolean allowServiseMessages, long duratinFilter,
                         boolean allowCopyWav, boolean isTargetLang, boolean allowDirPipeline) throws UnsupportedAudioFileException, IOException, InterruptedException {
        GetTextFromWav getTextFromWav=new GetTextFromWav(this.contr);
        if (allowDirPipeline){
            Whisper.recognize(60, contr, waveFilesAbsPath.get(0),language, smodel, device, new File(waveFilesAbsPath.get(0)), allowServiseMessages, true, allowDirPipeline);
        } else {
            for (int i = 0; i < waveFilesAbsPath.size(); i++) {
                boolean isSuccess = getTextFromWav.startWhisper(Paths.get(Controller.waveFilesAbsPath.get(i)),
                        device, smodel, language, allowServiseMessages, duratinFilter, isTargetLang);
                if (isSuccess) {
                    contr.setToTextArea("\n Обработка файла завершена " + waveFilesAbsPath.get(i));
                } else {
                    contr.setToTextArea("\n Не могу ничего разобрать в файле: " + waveFilesAbsPath.get(i));
                }
            }
        }
    }


    public static void moveFileToDirectory(Path sourceFilePath) {
        try {
            // Определение директории, где находится JAR
            CodeSource codeSource = Controller.class.getProtectionDomain().getCodeSource();
            Path jarDirPath = Paths.get(codeSource.getLocation().toURI()).getParent();

            // Создание пути к папке txt в той же директории, что и JAR
            Path destinationDirPath = jarDirPath.resolve("txt");

            // Создание папки txt, если она не существует
            if (!Files.exists(destinationDirPath)) {
                Files.createDirectories(destinationDirPath);
            }

            // Формирование пути назначения с сохранением имени файла
            String fileName = sourceFilePath.getFileName().toString();
            Path destinationPath = destinationDirPath.resolve(fileName);

            // Копирование (или перемещение) файла в папку txt
            Files.copy(sourceFilePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Файл успешно перемещен в " + destinationPath);
        } catch (Exception e) {
            System.err.println("Ошибка при перемещении файла: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
