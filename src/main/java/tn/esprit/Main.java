package tn.esprit;

import javafx.application.Application;
import tn.esprit.controllers.HelloApplication;

/**
 * Main entry point for the MythoriaDesktop application.
 * Launches the JavaFX HelloApplication.
 */
public class Main {
    public static void main(String[] args) {
        // JVM Flag to allow media access (Camera/Mic) in JavaFX WebView
        System.setProperty("javafx.web.allow.media.access", "true");
        Application.launch(HelloApplication.class, args);
    }
}
