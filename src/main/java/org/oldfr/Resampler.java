package org.oldfr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class Resampler {
    public  void resample(String inputPath, String outputPath) {
        String currentDir = Paths.get("").toAbsolutePath().toString();

        String soxPath = Paths.get(currentDir, "SoX", "sox.exe").toString();

        String command = soxPath + " " + inputPath + " -r 16000 " + outputPath;
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

