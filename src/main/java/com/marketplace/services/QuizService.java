package com.marketplace.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class QuizService {
    private static final String DATA_FILE = "quiz_win.dat";

    /**
     * Checks if the user has already won the quiz on this device.
     */
    public boolean hasAlreadyWon() {
        return new File(DATA_FILE).exists();
    }

    /**
     * Marks the quiz as won locally.
     */
    public void markAsWon() {
        try {
            Files.write(Paths.get(DATA_FILE), "WON".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Resets the quiz (optional, for testing).
     */
    public void reset() {
        File f = new File(DATA_FILE);
        if (f.exists()) f.delete();
    }
}
