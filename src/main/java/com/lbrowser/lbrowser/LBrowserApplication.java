package com.lbrowser.lbrowser;

import io.qt.core.Qt;
import io.qt.widgets.QApplication;
import io.qt.widgets.QMainWindow;

public class LBrowserApplication {

    public static void main(String[] args) {
        QApplication.initialize(args);

        QApplication.setAttribute(Qt.ApplicationAttribute.AA_EnableHighDpiScaling);
        QApplication.setAttribute(Qt.ApplicationAttribute.AA_UseHighDpiPixmaps);

        LMainWindow mainWindow = new LMainWindow();

        mainWindow.show();

        QApplication.exec();
    }
}
