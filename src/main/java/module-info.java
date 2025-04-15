module com.lbrowser.lbrowser {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires javafx.web;
    requires java.logging;

    opens com.lbrowser.lbrowser to javafx.fxml;
    exports com.lbrowser.lbrowser;
    exports com.lbrowser.lbrowser.media;
    opens com.lbrowser.lbrowser.media to javafx.fxml;
}