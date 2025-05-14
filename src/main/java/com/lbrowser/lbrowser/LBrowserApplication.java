package com.lbrowser.lbrowser;

import io.qt.core.Qt;
import io.qt.gui.QIcon;
import io.qt.widgets.QApplication;
import io.qt.widgets.QMainWindow;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LBrowserApplication {

    /**
     * Entry point for the LBrowser application.
     * Initializes the Qt framework with web debugging capabilities and launches the browser.
     *
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        // Configure web engine with debugging capabilities
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        argsList.add("--webEngineArgs");
        argsList.add("--remote-debugging-port=9222");
        argsList.add("--remote-allow-origins=*");

        // Initialize the Qt application with the configured arguments
        QApplication.initialize(argsList.toArray(new String[0]));

        // Set application metadata
        QApplication.setApplicationName("LBrowser");
        QApplication.setApplicationDisplayName("LBrowser");
        QApplication.setApplicationVersion("1.0");
        QApplication.setWindowIcon(QIcon.fromTheme("web-browser", new QIcon("classpath:icons/submarine.svg")));

        // Enable high DPI support
        QApplication.setAttribute(Qt.ApplicationAttribute.AA_EnableHighDpiScaling);
        QApplication.setAttribute(Qt.ApplicationAttribute.AA_UseHighDpiPixmaps);

        // Create and display the main browser window
        LMainWindow mainWindow = new LMainWindow();
        mainWindow.show();

        // Start the Qt event loop
        QApplication.exec();
    }
}
