package org.oldfr;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Riffer {
    private Controller contr;
    public Riffer(double WavLen,WavPlayer rec,Controller contr){
        this.WavLen=WavLen;
        this.rec=rec;
        this.contr=contr;
    }
    private double WavLen;
    private WavPlayer rec;
    public  Stage primaryStage = new Stage();

    public void CreateWindow(String PathToWav){
        WaveVisualization waveVisualization = new WaveVisualization(1600, 120,WavLen,rec,this.contr);
        try {
            //Root
            BorderPane root = new BorderPane();
            root.setCenter(waveVisualization);
            root.boundsInLocalProperty().addListener(l -> {
                waveVisualization.setWidth(root.getWidth());
                waveVisualization.setHeight(root.getHeight());
            });
            primaryStage.setTitle(PathToWav);
           // primaryStage.setOnCloseRequest(c -> System.exit(0));

            //Scene
            Scene scene1 = new Scene(root, 883, 120);

            primaryStage.setScene(scene1);
            primaryStage.setX(511);
            primaryStage.setY(780);
            //Show
            primaryStage.show();


            waveVisualization.getWaveService().startService(PathToWav, WaveFormService.WaveFormJob.AMPLITUDES_AND_WAVEFORM);
            scene1.setOnKeyPressed(k->{
                if(k.getCode().toString().toUpperCase()=="SPACE"){
                    if(waveVisualization.TimerStartPosition< waveVisualization.TimerEndPosition){
                        waveVisualization.setTimerXPosition(waveVisualization.TimerStartPosition);
                        Controller.rec.jumpTo(waveVisualization.someShitForPlayOnSpace);
                    } else {
                        waveVisualization.setTimerXPosition(waveVisualization.TimerEndPosition);
                        Controller.rec.jumpTo(waveVisualization.someShitForPlayOnSpace);
                    }

                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
