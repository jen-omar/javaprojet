module com.example.mythoriadesktop {
    requires java.logging;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.google.gson;


    opens com.example.mythoriadesktop to javafx.fxml;
    opens com.example.mythoriadesktop.model to com.google.gson;
    exports com.example.mythoriadesktop;
}
