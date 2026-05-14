package com.marketplace.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

public class VoiceService {

    public CompletableFuture<String> listenAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String scriptPath = new File("ChatBotAI/voice_recognizer.py").getAbsolutePath();
                
                ProcessBuilder pb = new ProcessBuilder("python", scriptPath);
                pb.redirectErrorStream(true);
                Process p = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = reader.readLine();
                
                if (line == null) return "UNKNOWN";
                return line.trim();

            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR";
            }
        });
    }
}
