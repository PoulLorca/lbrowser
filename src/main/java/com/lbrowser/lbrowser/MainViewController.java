package com.lbrowser.lbrowser;

import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;


public class MainViewController implements Initializable {
    @FXML
    public TextField web_url;
    @FXML
    public Button reload_button;
    @FXML
    public Button prev_button;
    @FXML
    public Button next_button;
    @FXML
    public Button new_tab_button;
    @FXML
    public TabPane tab_pane;

    private static final String DEFAULT_URL = "https://www.startpage.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/605.1 (KHTML, like Gecko) Lbrowser/1.0";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        createNewTab(DEFAULT_URL);

        tab_pane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if(newTab != null) {
                updateUrlBarFromTab(newTab);
                updateNavigationButtons(newTab);
            }else{
                web_url.clear();
                prev_button.setDisable(true);
                next_button.setDisable(true);
            }
        });

        web_url.setOnAction(event -> loadUrlInCurrentTab());

        web_url.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER){
                loadUrlInCurrentTab();
            }
        });
    }

    private Tab getCurrentTab() {
        return tab_pane.getSelectionModel().getSelectedItem();
    }

    private WebView getCurrentWebView() {
        Tab currentTab = getCurrentTab();
        if (currentTab != null && currentTab.getContent() instanceof WebView){
            return (WebView) currentTab.getContent();
        }
        return null;
    }
    private WebEngine getCurrentWebEngine() {
        WebView currentWebView = getCurrentWebView();
        return (currentWebView != null) ? currentWebView.getEngine() : null;}

    private Tab createNewTab(String urlToLoad){
        Tab newTab = new Tab("Loading...");
        WebView newWebView = new WebView();
        WebEngine newWebEngine = newWebView.getEngine();

        newWebEngine.setUserAgent(USER_AGENT);

        newWebEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            if (tab_pane.getSelectionModel().getSelectedItem() == newTab){
                web_url.setText(newValue);
                updateNavigationButtons(newTab);
            }
        });

        newWebEngine.titleProperty().addListener((observable, oldTitle, newTitle) -> {
            if (newTitle != null && !newTitle.trim().isEmpty()){
                newTab.setText(newTitle);
            }else{
                String loc = newWebEngine.getLocation();
                newTab.setText(loc != null && !loc.trim().isEmpty() ? loc : "New Tab");
            }
        });

        newWebEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED){
                if(tab_pane.getSelectionModel().getSelectedItem() == newTab){
                    updateNavigationButtons(newTab);
                    String title = newWebEngine.getTitle();
                    if (title != null && !title.trim().isEmpty()){
                        newTab.setText(title);
                    }else{
                        newTab.setText(newWebEngine.getLocation() != null ? newWebEngine.getLocation() : "Loaded Tab");
                    }
                }
            }else if (newState == Worker.State.FAILED){
                newTab.setText("Error Loading");
                if (tab_pane.getSelectionModel().getSelectedItem() == newTab){
                    updateNavigationButtons(newTab);
                }
            }
        });

        newWebEngine.setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {
            @Override
            public WebEngine call(PopupFeatures param){
                Tab popupTab = createNewTab(null);
                return ((WebView) popupTab.getContent()).getEngine();
            }
        });

        newTab.setContent(newWebView);
        newTab.setClosable(true);

        newTab.setOnCloseRequest(event -> {
            System.out.println("Tab closed: " + newTab.getText());
        });

        tab_pane.getTabs().add(newTab);
        tab_pane.getSelectionModel().select(newTab);

        if (urlToLoad != null && !urlToLoad.trim().isEmpty()){
            loadUrl(newWebEngine, formatUrl(urlToLoad));
        }else{
            loadUrl(newWebEngine, formatUrl(DEFAULT_URL));
            newTab.setText("New Tab");
        }

        return newTab;
    }
    public void handleNewTabButton(ActionEvent actionEvent) {
        createNewTab(DEFAULT_URL);
    }

    private void loadUrlInCurrentTab(){
        WebEngine engine = getCurrentWebEngine();
        if (engine != null ){
            String url = formatUrl(web_url.getText());
            loadUrl(engine, url);
        }else{
            System.out.println("No current WebEngine found.");
        }
    }

    private String formatUrl(String url){
        if (url == null) return "";
        url = url.trim();
        if(!url.isEmpty() && !url.matches("^[a-zA-Z]+://.*")){
            if(!url.startsWith("about:") && !url.startsWith("file:")){
                return "https://" + url;
            }
        }
        return url;
    }

    private void loadUrl(WebEngine engine, String url){
        engine.load(url);
    }

    public void goBack(){
        WebEngine engine =getCurrentWebEngine();
        if(engine != null){
            WebHistory history = engine.getHistory();
            if (history.getCurrentIndex() > 0){
                history.go(-1);
            }
        }
    }
    public void goForward(){
        WebEngine engine =getCurrentWebEngine();
        if(engine != null){
            WebHistory history = engine.getHistory();
            if (history.getCurrentIndex() < history.getEntries().size() - 1){
                history.go(1);
            }
        }
    }
    public void reload(){
        WebEngine engine =getCurrentWebEngine();
        if(engine != null){
            engine.reload();
        }
    }
    public void zoomIn(){
        WebView currentWebView = getCurrentWebView();
        if (currentWebView != null){
            currentWebView.setZoom(currentWebView.getZoom() + 0.1);
        }
    }
    public void zoomOut(){
        WebView currentWebView = getCurrentWebView();
        if (currentWebView != null){
            currentWebView.setZoom(currentWebView.getZoom() - 0.1);
        }
    }
    public void zoomReset(){
        WebView currentWebView = getCurrentWebView();
        if (currentWebView != null){
            currentWebView.setZoom(1.0);
        }
    }
    public void showHistory(){
        WebEngine engine = getCurrentWebEngine();
        if (engine != null){
            WebHistory history = engine.getHistory();
            ObservableList<WebHistory.Entry> entries = history.getEntries();
            System.out.println("--Browser History of the current tab");
            for (WebHistory.Entry entry : entries){
                System.out.println("Title: " + entry.getTitle() + ", URL: " + entry.getUrl() + ", Visited: " + entry.getLastVisitedDate());
            }
            System.out.println("Current Index: " + history.getCurrentIndex());
            System.out.println("-----------------");
        }else{
            System.out.println("No current WebEngine found.");
        }
    }

    private void updateUrlBarFromTab(Tab tab){
        if (tab != null && tab.getContent() instanceof WebView){
            WebView view = (WebView) tab.getContent();
            WebEngine engine = view.getEngine();
            String location = engine.getLocation();
            if(location != null && !location.isEmpty() && !location.equals("about:blank")){
                web_url.setText(location);
            }else if(engine.getLoadWorker().isRunning()){
                web_url.setText("Loading...");
            }else{
                web_url.clear();
            }
        }else{
            web_url.clear();
        }
    }

    private void updateNavigationButtons(Tab tab) {
        if (tab != null && tab.getContent() instanceof WebView) {
            WebEngine engine = ((WebView) tab.getContent()).getEngine();
            WebHistory history = engine.getHistory();
            boolean isLoading = engine.getLoadWorker().isRunning();

            prev_button.setDisable(history.getCurrentIndex() <= 0 );
            next_button.setDisable(history.getCurrentIndex() >= history.getEntries().size() - 1);
            reload_button.setDisable(!isLoading);
        } else {
            prev_button.setDisable(true);
            next_button.setDisable(true);
            reload_button.setDisable(true);
        }
    }
}
