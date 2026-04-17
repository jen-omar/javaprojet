module tn.esprit {
    requires java.logging;
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.google.gson;
    requires itextpdf;

    opens tn.esprit.controllers to javafx.fxml;
    opens tn.esprit.Models to com.google.gson;
    opens tn.esprit.data to com.google.gson;
    opens tn.esprit.util to javafx.fxml;
    opens tn.esprit.services to javafx.fxml;

    exports tn.esprit.controllers;
    exports tn.esprit.Models;
    exports tn.esprit.interfaces;
    exports tn.esprit.services;
    exports tn.esprit.util;
    exports tn.esprit.data;
}
