package org.example;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.URISyntaxException;
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
                         boolean allowCopyWav, boolean isTargetLang, boolean allowDirPipeline, String transcriber,
                         String beam_size, String maxWordsInRow) throws UnsupportedAudioFileException, IOException, InterruptedException, URISyntaxException {
        GetTextFromWav getTextFromWav=new GetTextFromWav(this.contr);
        if (allowDirPipeline){
            if (transcriber.equals("Faster-whisper")){
                Whisper.recognize(60, contr, waveFilesAbsPath.get(0),language,
                        smodel, device, new File(waveFilesAbsPath.get(0)), allowServiseMessages,
                        true, allowDirPipeline, beam_size, maxWordsInRow);
            }
            if (transcriber.equals("WhisperX")){
                WhisperX.recognize(60, contr, waveFilesAbsPath.get(0),language,
                        smodel, device, new File(waveFilesAbsPath.get(0)), allowServiseMessages,
                        true, allowDirPipeline, beam_size, maxWordsInRow);
            }
        } else {
            for (int i = 0; i < waveFilesAbsPath.size(); i++) {
                boolean isSuccess = getTextFromWav.startWhisper(Paths.get(Controller.waveFilesAbsPath.get(i)),
                        device, smodel, language, allowServiseMessages, duratinFilter, isTargetLang, beam_size, maxWordsInRow);
                if (isSuccess) {
                    contr.setToTextArea("\n Обработка файла завершена " + waveFilesAbsPath.get(i));
                } else {
                    contr.setToTextArea("\n Не могу ничего разобрать в файле: " + waveFilesAbsPath.get(i));
                }
            }
        }
    }



}
