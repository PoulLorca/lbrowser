package com.lbrowser.lbrowser;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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
    @FXML
    public ProgressIndicator loading_indicator;

    private static final String DEFAULT_URL = "https://www.startpage.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/605.1 (KHTML, like Gecko) Lbrowser/1.0";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideLoadingIndicator();
        createNewTab(DEFAULT_URL);

        tab_pane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if(newTab != null) {
                updateUrlBarFromTab(newTab);
                updateNavigationButtons(newTab);
                updateLoadingIndicatorVisibility(newTab);
            }else{
                web_url.clear();
                prev_button.setDisable(true);
                next_button.setDisable(true);
                reload_button.setDisable(true);
                hideLoadingIndicator();
            }
        });

        web_url.setOnAction(event -> loadUrlInCurrentTab());

        web_url.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER){
                loadUrlInCurrentTab();
            }
        });
    }

    private void showLoadingIndicator(){
        if (loading_indicator != null) {
            loading_indicator.setVisible(true);
            loading_indicator.setManaged(true);
        }
    }

    private void hideLoadingIndicator() {
        if (loading_indicator != null) {
            loading_indicator.setVisible(false);
            loading_indicator.setManaged(false);
        }
    }

    private void updateLoadingIndicatorVisibility(Tab tab){
        if (tab != null && tab.getContent() instanceof WebView){
            WebEngine engine = ((WebView) tab.getContent()).getEngine();
            Worker.State state = engine.getLoadWorker().getState();
            if (state == Worker.State.RUNNING || state == Worker.State.SCHEDULED) {
                showLoadingIndicator();
            } else {
                hideLoadingIndicator();
            }
        }else{
            hideLoadingIndicator();
        }
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
            Worker.State state = newWebEngine.getLoadWorker().getState();
            if(state != Worker.State.RUNNING && state != Worker.State.SCHEDULED){
                if (newTitle != null && !newTitle.trim().isEmpty()){
                    newTab.setText(newTitle);
                }else{
                    String loc = newWebEngine.getLocation();
                    newTab.setText(loc != null && !loc.trim().isEmpty() ? loc : "New Tab");
                }
            }
        });

        newWebEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            boolean isSelectedTab = (tab_pane.getSelectionModel().getSelectedItem() == newTab);

            if(isSelectedTab){
                if (newState == Worker.State.RUNNING || newState == Worker.State.SCHEDULED){
                    showLoadingIndicator();
                    newTab.setText("Loading...");
                }else{
                    hideLoadingIndicator();

                    String title = newWebEngine.getTitle();
                    String loc = newWebEngine.getLocation();

                    if (title != null && !title.trim().isEmpty()) {
                        newTab.setText(title);
                    }else if (loc != null && !loc.trim().isEmpty()) {
                        newTab.setText(loc);
                    }else if (newState == Worker.State.FAILED){
                        newTab.setText("Load failed");
                    }else{
                        newTab.setText(newWebEngine.getLocation() != null ? newWebEngine.getLocation() : "Loaded Tab");
                    }
                }
                updateNavigationButtons(newTab);
            }else{
                if(newState == Worker.State.RUNNING || newState == Worker.State.SCHEDULED){showLoadingIndicator();
                    newTab.setText("Loading...");
                } else if (newState == Worker.State.FAILED) {
                    newTab.setText("Load failed");
                } else if (newState == Worker.State.SUCCEEDED) {
                    String title = newWebEngine.getTitle();
                    String loc = newWebEngine.getLocation();
                    if(title != null && !title.trim().isEmpty()) {
                        newTab.setText(title);
                    }else if (loc != null && !loc.trim().isEmpty()) {
                        newTab.setText(loc);
                    }else {
                        newTab.setText("Loaded Tab");
                    }
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
            WebView webViewToClose = (WebView) newTab.getContent();
            if (webViewToClose != null) {
                webViewToClose.getEngine().getLoadWorker().cancel();
                webViewToClose.getEngine().load(null);
            }

            if (tab_pane.getTabs().size() == 1) {
                Platform.exit();
            }
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

            Tab historyTab = new Tab("History");
            WebView historyView = new WebView();
            historyTab.setContent(historyView);

            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html><head>");
            htmlBuilder.append("<style>");
            htmlBuilder.append("body { font-family: Arial, sans-serif; margin: 20px; }");
            htmlBuilder.append("h1 { color: #333; }");
            htmlBuilder.append(".history-item { border-bottom: 1px solid #eee; padding: 10px; margin: 5px 0; }");
            htmlBuilder.append(".history-item:hover { background-color: #f5f5f5; }");
            htmlBuilder.append(".history-title { font-weight: bold; font-size: 16px; color: #1a73e8; cursor: pointer; }");
            htmlBuilder.append(".history-url { color: #666; font-size: 14px; margin-top: 5px; }");
            htmlBuilder.append(".history-date { color: #999; font-size: 12px; margin-top: 5px; }");
            htmlBuilder.append("</style>");
            htmlBuilder.append("</head><body>");
            htmlBuilder.append("<h1>Browsing History</h1>");
            htmlBuilder.append("<small><i>This browsing history is limited to the current tab and will not be saved or stored anywhere. It will be lost once the tab is closed.</i></small>");

            for (WebHistory.Entry entry : entries){
                String title = entry.getTitle();
                String url = entry.getUrl();
                String visitDate = entry.getLastVisitedDate().toString();

                htmlBuilder.append("<div class='history-item'>");
                htmlBuilder.append("<div class='history-title' onclick='window.location.href=\"" + url + "\"'>" +
                        (title != null && !title.isEmpty() ? title : url) + "</div>");
                htmlBuilder.append("<div class='history-url'>" + url + "</div>");
                htmlBuilder.append("<div class='history-date'>Seen: " + visitDate + "</div>");
                htmlBuilder.append("</div>");
            }

            htmlBuilder.append("</body></html>");

            historyView.getEngine().loadContent(htmlBuilder.toString());
            historyView.getEngine().setCreatePopupHandler(request -> getCurrentWebEngine());

            tab_pane.getTabs().add(historyTab);
            tab_pane.getSelectionModel().select(historyTab);
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
            Worker.State state = engine.getLoadWorker().getState();
            boolean isLoading = (state == Worker.State.RUNNING || state == Worker.State.SCHEDULED);

            prev_button.setDisable(history.getCurrentIndex() <= 0 );
            next_button.setDisable(history.getCurrentIndex() >= history.getEntries().size() - 1 );
            reload_button.setDisable(isLoading);
        } else {
            prev_button.setDisable(true);
            next_button.setDisable(true);
            reload_button.setDisable(true);
        }
    }
}
