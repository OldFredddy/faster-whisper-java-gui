package org.example;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class WavPlayer {
    private Media media;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    public String PathToWav;
    public WavPlayer(String path,Controller contr) {
        PathToWav=path;
        media = new Media(new File(path).toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnReady(() -> {
            // media has finished loading, ready to play
            isPlaying = true;
            mediaPlayer.play();
        });
    }

    public void play() {
        if (mediaPlayer.getStatus() == Status.PAUSED || mediaPlayer.getStatus() == Status.STOPPED) {
            mediaPlayer.play();
            isPlaying = true;
        }
    }
    public  double getDurationInSeconds() {
        double duration = 0.0;
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(PathToWav))) {
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            duration = (frames+0.0) / format.getFrameRate();
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
        return duration;
    }
    public double getCurrentTimeInSeconds() {
        Duration duration = mediaPlayer.getCurrentTime();
        return duration.toSeconds();
    }
    public void stop() {
        if (isPlaying) {
            mediaPlayer.stop();
            isPlaying = false;
        }
    }
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }
   public void close(){
        stop();
        mediaPlayer=null;
        media=null;
   }
    public void jumpTo(double time) {
        if (mediaPlayer.getStatus() == Status.UNKNOWN || mediaPlayer.getStatus() == Status.HALTED) {
            return;
        }

        Duration duration = media.getDuration();
        double millis = duration.toMillis();
        double jumpMillis = millis * time;
        mediaPlayer.seek(Duration.millis(jumpMillis));

        if (mediaPlayer.getStatus() != Status.PLAYING) {
            mediaPlayer.play();
        }
    }
}