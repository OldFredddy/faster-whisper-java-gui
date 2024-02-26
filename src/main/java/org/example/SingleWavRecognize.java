package org.example;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Paths;

public class SingleWavRecognize {
    private  Controller contr;
    private String PathToTempWav;
    private boolean allowServiceMessages;
    private long durationFilter;
    SingleWavRecognize(Controller contr, String PathToTempWav, boolean allowServiceMessages, long durationFilter){
        this.contr=contr;
        this.PathToTempWav=PathToTempWav;
        this.allowServiceMessages=allowServiceMessages;
        this.durationFilter=durationFilter;
    }
    public void Recognize() throws UnsupportedAudioFileException, IOException, InterruptedException {
        GetTextFromWav getTextFromWav=new GetTextFromWav(this.contr);
        getTextFromWav.startWhisper(Paths.get(PathToTempWav), contr.whisperDevice,
                contr.whisperModelSize, contr.whisperLanguage, allowServiceMessages,durationFilter, true);
    }
}
