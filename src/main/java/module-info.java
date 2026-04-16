module tn.esprit.mythoria {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens tn.esprit.mythoria to javafx.fxml;
    opens tn.esprit.mythoria.controller to javafx.fxml;
    opens tn.esprit.mythoria.entity to javafx.base;

    exports tn.esprit.mythoria;
    exports tn.esprit.mythoria.controller;
    exports tn.esprit.mythoria.entity;
}