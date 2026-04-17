module com.example.mythoriadesktop {
    requires java.sql;
    requires java.logging;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.google.gson;
    requires jbcrypt;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;
    requires org.apache.pdfbox;


    opens com.example.mythoriadesktop to javafx.fxml;
    opens com.example.mythoriadesktop.model to com.google.gson;
    exports com.example.mythoriadesktop;
}
