package org.example;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
                         boolean allowCopyWav, boolean isTargetLang) throws UnsupportedAudioFileException, IOException, InterruptedException {
        GetTextFromWav getTextFromWav=new GetTextFromWav(this.contr);
        for (int i = 0; i < waveFilesAbsPath.size(); i++) {
                CreateTxtWithText(getTextFromWav.startWhisper(Paths.get(Controller.waveFilesAbsPath.get(i)),
                                device,smodel, language,allowServiseMessages, duratinFilter, isTargetLang),
                                String.valueOf(Controller.waveFilesAbsPath.get(i)), allowCopyWav,isTargetLang, language);
        }
    }
    public void CreateTxtWithText(List<String> temp, String nameFile, boolean allowCopyWav, boolean isTargetLang, String language) {
        try {
            try{
            if (temp.get(0).equals("Файл меньше заданного фильтра длительности!") ||
                    temp.get(0).equals("File can't be created.") || temp.get(0).equals("Пропуск файла, превышен timeout ожидания")) {
                contr.setToTextArea(temp.get(0));
                return;
            }
            } catch (IndexOutOfBoundsException e){
                return;
            }
            Path name = Paths.get(nameFile);
            nameFile = name.getFileName().toString();

            // Определение суффикса на основе выбранного языка или использование _en для английского
            String languageSuffix = isTargetLang ? "_" + language.toLowerCase() : "_russian"; // Предполагается, что метод contr.getLanguage() возвращает код выбранного языка
            // Изменение имени файла для включения суффикса языка
            String fileNameWithLanguage = nameFile.substring(0, nameFile.lastIndexOf('.')) + languageSuffix + ".txt";

            FileOutputStream fos = new FileOutputStream(Controller.txtDirPath + "/" + fileNameWithLanguage);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);
            for (int i = 0; i < temp.size(); i++) {
                if (temp.get(i).length() > 0) {
                    bw.write(temp.get(i) + "\n");
                }
            }
            bw.close();

            Path sourceFilePath = name;
            Path destinationPath = Paths.get("txt");
            if (allowCopyWav) {
                moveFileToDirectory(sourceFilePath);
            }
            contr.setToTextArea("\n Обработка файла завершена " + fileNameWithLanguage);
        } catch (IOException | IndexOutOfBoundsException var7) {
            var7.printStackTrace();
            contr.setToTextArea("\n Не могу ничего разобрать в этом файле!" + nameFile);
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
