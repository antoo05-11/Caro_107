module com.example.caro_107 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires de.jensd.fx.glyphs.fontawesome;
    //requires eu.hansolo.tilesfx;

    opens com.example.caro_107 to javafx.fxml;
    exports com.example.caro_107;
}