package com.lbrowser.lbrowser;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;


public class MainViewController implements Initializable {
    @FXML
    public WebView web_view;
    @FXML
    public TextField web_url;
    @FXML
    public Button reload_button;
    @FXML
    public Button prev_button;
    @FXML
    public Button next_button;

    private WebEngine webEngine;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        webEngine = web_view.getEngine();
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/605.1 (KHTML, like Gecko) Lbrowser/1.0";
        webEngine.setUserAgent(userAgent);

        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
           web_url.setText(newValue);
        });

        web_url.setOnAction(event -> {
            String url = web_url.getText();
            if(!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            webEngine.load(url);
        });

        // Load a default page
        webEngine.load("https://www.startpage.com");
    }

    public void goBack(){
        if(webEngine.getHistory().getCurrentIndex() > 0){
            webEngine.getHistory().go(-1);
        }
    }
    public void goForward(){
        if(webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1){
            webEngine.getHistory().go(1);
        }
    }
    public void reload(){
        webEngine.reload();
    }
    public void zoomIn(){web_view.setZoom(web_view.getZoom() + 0.1);}
    public void zoomOut(){web_view.setZoom(web_view.getZoom() - 0.1);}
    public void zoomReset(){web_view.setZoom(1.0);}
    public void showHistory(){
        WebHistory history = webEngine.getHistory();
        ObservableList<WebHistory.Entry> entries = history.getEntries();
        System.out.println(entries);
    }
}
