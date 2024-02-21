package org.example;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class CtrlRecService extends Service<Void> {
    private double t1;
    private double t2;
    private Controller contr;
    private String PathToWav;

    public CtrlRecService(double t1, double t2, Controller contr, String PathToWav) {
        this.t1 = t1;
        this.t2 = t2;
        if (t2 < t1) {
            double temp = t1;
            t1 = t2;
            t2 = temp;
        }
        this.contr = contr;
        this.PathToWav = PathToWav;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    WavCutter wavCutter1 = new WavCutter(t1, t2, PathToWav);
                    File outputFile1 = wavCutter1.cut();
                    SingleWavRecognize NewRecon = new SingleWavRecognize(contr, outputFile1.getAbsolutePath(),contr.getServiceMessagesStatus(),contr.getDurationFilter());
                    NewRecon.Recognize();
                    System.out.println("Output file 1: " + outputFile1.getAbsolutePath());
                    outputFile1.delete();
                } catch (IOException | UnsupportedAudioFileException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }
}