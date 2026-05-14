package tn.esprit.util;

import javax.sound.sampled.*;
import java.io.*;

public class AudioRecorder {
    private TargetDataLine targetLine;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    private File audioFile;

    public void startRecording(String filePath) {
        try {
            audioFile = new File(filePath);
            File parent = audioFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            AudioFormat format = new AudioFormat(16000, 16, 2, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Line not supported");
                return;
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();

            Thread recordingThread = new Thread(() -> {
                AudioInputStream inputStream = new AudioInputStream(targetLine);
                try {
                    AudioSystem.write(inputStream, fileType, audioFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            recordingThread.start();
            System.out.println("Recording started...");

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
            System.out.println("Recording stopped.");
        }
    }

    public static Clip playAudio(String filePath, Runnable onFinished) {
        try {
            System.out.println("Attempting to play: " + filePath);
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("Audio file not found: " + filePath);
                return null;
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    if (onFinished != null) onFinished.run();
                }
            });
            clip.start();
            System.out.println("Playback started.");
            return clip;
        } catch (Exception e) {
            System.err.println("Error during playback: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static String getFormattedDuration(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return "0:00";
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            double durationInSeconds = (double) frames / format.getFrameRate();
            int minutes = (int) (durationInSeconds / 60);
            int seconds = (int) (durationInSeconds % 60);
            return String.format("%d:%02d", minutes, seconds);
        } catch (Exception e) {
            return "0:00";
        }
    }
}
