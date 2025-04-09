module com.lbrowser.lbrowser {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires javafx.web;

    opens com.lbrowser.lbrowser to javafx.fxml;
    exports com.lbrowser.lbrowser;
}