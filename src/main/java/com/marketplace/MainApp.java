package com.marketplace;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("Welcome to the Marketplace Template!");
        StackPane root = new StackPane(label);
        
        Scene scene = new Scene(root, 800, 600);
        try {
            String css = getClass().getResource("/styles/dark-gold.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.out.println("No style found.");
        }
        
        stage.setTitle("Marketplace Template");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
