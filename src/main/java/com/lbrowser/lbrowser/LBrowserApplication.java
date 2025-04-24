package com.lbrowser.lbrowser;

import io.qt.core.Qt;
import io.qt.widgets.QApplication;
import io.qt.widgets.QMainWindow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LBrowserApplication {

    public static void main(String[] args) {
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        argsList.add("--webEngineArgs");
        argsList.add("--remote-debugging-port=9222");
        argsList.add("--remote-allow-origins=*");

        QApplication.initialize(argsList.toArray(new String[0]));
        //QApplication.initialize(args);

        QApplication.setAttribute(Qt.ApplicationAttribute.AA_EnableHighDpiScaling);
        QApplication.setAttribute(Qt.ApplicationAttribute.AA_UseHighDpiPixmaps);

        LMainWindow mainWindow = new LMainWindow();

        mainWindow.show();

        QApplication.exec();
    }
}
