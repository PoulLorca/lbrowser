package com.lbrowser.lbrowser;

import com.lbrowser.lbrowser.media.MediaItem;
import com.lbrowser.lbrowser.modes.NetworkModeManager;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;


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
    @FXML
    public CheckMenuItem noAddModeMenuItem;
    @FXML
    public MenuButton options_menu;

    private NetworkModeManager networkModeManager;
    private ToggleGroup networkModeToggleGroup;

    private AdBlocker adBlocker;

    private static final String DEFAULT_URL = "https://www.startpage.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/605.1 (KHTML, like Gecko) Lbrowser/1.0";
    private static final Logger LOGGER = Logger.getLogger(MainViewController.class.getName());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        networkModeManager = new NetworkModeManager();
        networkModeToggleGroup = new ToggleGroup();

        createNetworkModeMenu();

        hideLoadingIndicator();
        adBlocker = new AdBlocker();
        if (noAddModeMenuItem != null){
            noAddModeMenuItem.setSelected(adBlocker.isEnabled());
        }else{
            LOGGER.warning("noAddModeMenuItem is null");
        }

        createNewTab(networkModeManager.getCurrentModeStartUrl());

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

    private void createNetworkModeMenu(){
        Menu modesMenu = new Menu("Net modes");

        for (NetworkModeManager.NetworkMode mode : NetworkModeManager.NetworkMode.values()){
            RadioMenuItem modeItem = new RadioMenuItem(mode.getDisplayName());
            modeItem.setToggleGroup(networkModeToggleGroup);
            modeItem.setUserData(mode);

            if(mode == networkModeManager.getCurrentMode()){
                modeItem.setSelected(true);
            }

            modeItem.setOnAction(event -> {
                NetworkModeManager.NetworkMode selectedMode = (NetworkModeManager.NetworkMode) modeItem.getUserData();
                LOGGER.info("Selected network mode: " + selectedMode.getDisplayName());

                networkModeManager.setMode(selectedMode);

                String startUrl = networkModeManager.getModeStartUrl(selectedMode);

                LOGGER.info("Opening new tab for mode " + selectedMode + " with URL: " + startUrl);
                createNewTab(startUrl);
            });
            modesMenu.getItems().add(modeItem);
        }

        if(options_menu != null && options_menu.getItems().size() >= 8){
            options_menu.getItems().add(8, modesMenu);
        } else if (options_menu != null) {
            options_menu.getItems().add(modesMenu);
        }else{
            LOGGER.warning("options_menu is null. Cannot add Net Modes menu.");
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

    private WebEngine getWebEngineFromTab(Tab tab) {
        if (tab != null && tab.getContent() instanceof WebView) {
            return ((WebView) tab.getContent()).getEngine();
        }
        return null;
    }

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
                updateUrlBarFromTab(newTab);
                if (newState == Worker.State.SUCCEEDED) {
                    applyAdBlockCssToEngine(newWebEngine);
                }
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

        String finalUrlToLoad = urlToLoad;
        if (finalUrlToLoad == null || finalUrlToLoad.trim().isEmpty()){
            finalUrlToLoad = networkModeManager.getCurrentModeStartUrl();
            LOGGER.info("No URL provided for new tab, using default for current mode: " + finalUrlToLoad);
        }

        loadUrl(newWebEngine, formatUrl(finalUrlToLoad));

        if(finalUrlToLoad.equals(networkModeManager.getModeStartUrl(NetworkModeManager.NetworkMode.NORMAL)) && newTab.getText().equals("Loading...")){
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

    public void noAddMode(ActionEvent actionEvent) {
        if (adBlocker == null || noAddModeMenuItem == null) {
            return;
        }
        boolean newState = noAddModeMenuItem.isSelected();
        adBlocker.setEnabled(newState);
        applyAdBlockCssToCurrentTab();
    }

    private void applyAdBlockCssToEngine(WebEngine engine){
        if (engine == null) return;

        Platform.runLater(() -> {
            try {
                if (adBlocker != null && adBlocker.isEnabled() && adBlocker.areRulesLoaded()) {
                    String currentUrl = engine.getLocation();
                    String cssContent = adBlocker.getEffectiveCssForDomain(currentUrl);
                    String dataUrl = adBlocker.createDataUrlForCss(cssContent);
                    if (dataUrl != null) {
                        engine.setUserStyleSheetLocation(dataUrl);
                        System.out.println("Applied AdBlockCSS for: " + currentUrl);
                    }
                }
            }catch (Exception e){
                System.err.println("Error applying CSS: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void applyAdBlockCssToCurrentTab(){
        applyAdBlockCssToEngine(getCurrentWebEngine());
    }

    public void showMediaSources(){
        WebEngine engine = getCurrentWebEngine();
        Document dom = engine.getDocument();
        String baseUrl = engine.getLocation();
        Set<String> uniqueImageUrls = new HashSet<>();
        Set<String> uniqueVideoUrls = new HashSet<>();
        NodeList imageNodes = dom.getElementsByTagName("img");
        NodeList videoNodes = dom.getElementsByTagName("video");
        NodeList sourceNodes = dom.getElementsByTagName("source");

        for (int i = 0; i < imageNodes.getLength(); i++) {
            Element img = (Element) imageNodes.item(i);
            String src = img.getAttribute("src");

            if(src == null || src.isEmpty() || src.startsWith("data:")){
                continue;
            }

            try{
                URL absoluteUrl = new URL(new URL(baseUrl), src);
                String absoluteSrc = absoluteUrl.toString();

                if(absoluteSrc.startsWith("http")){
                    uniqueImageUrls.add(absoluteSrc);
                }
            }catch (MalformedURLException e){
                System.out.println("Invalid URL");
            }

        }

        for (int i = 0; i < videoNodes.getLength(); i++) {
            Element video = (Element) videoNodes.item(i);
            String src = video.getAttribute("src");

            if (src != null && !src.isEmpty() && !src.startsWith("data:")) {
                try {
                    URL absoluteUrl = new URL(new URL(baseUrl), src);
                    String absoluteSrc = absoluteUrl.toString();

                    if (absoluteSrc.startsWith("http")) {
                        uniqueVideoUrls.add(absoluteSrc);
                    }
                } catch (MalformedURLException e) {
                    // Ignorar URLs malformadas silenciosamente
                }
            }

            NodeList videoSourceNodes = video.getElementsByTagName("source");
            for (int j = 0; j < videoSourceNodes.getLength(); j++) {
                Element source = (Element) videoSourceNodes.item(j);
                String sourceSrc = source.getAttribute("src");

                if (sourceSrc != null && !sourceSrc.isEmpty() && !sourceSrc.startsWith("data:")) {
                    try {
                        URL absoluteUrl = new URL(new URL(baseUrl), sourceSrc);
                        String absoluteSrc = absoluteUrl.toString();

                        if (absoluteSrc.startsWith("http")) {
                            uniqueVideoUrls.add(absoluteSrc);
                        }
                    } catch (MalformedURLException e) {

                    }
                }
            }
        }

        for (int j = 0; j < sourceNodes.getLength(); j++) {
            Element source = (Element) sourceNodes.item(j);
            String sourceSrc = source.getAttribute("src");
            String type = source.getAttribute("type");

            if (sourceSrc != null && !sourceSrc.isEmpty() && !sourceSrc.startsWith("data:")) {
                try {
                    URL absoluteUrl = new URL(new URL(baseUrl), sourceSrc);
                    String absoluteSrc = absoluteUrl.toString();

                    if (absoluteSrc.startsWith("http")) {
                        uniqueVideoUrls.add(absoluteSrc);
                    }
                } catch (MalformedURLException e) {
                    System.out.println("URL source malformed: " + sourceSrc);
                }
            }
        }


        List<MediaItem> items = new ArrayList<>();
        uniqueImageUrls.forEach(url -> items.add(new MediaItem(url, "image")));
        uniqueVideoUrls.forEach(url -> items.add(new MediaItem(url, "video")));

        Platform.runLater(() -> {
            try{
                FXMLLoader loader = new FXMLLoader(getClass().getResource("media-selector.fxml"));
                Parent root = loader.load();
                MediaSelectorController controller = loader.getController();
                controller.setMediaItems(items);

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Select Media");
                stage.show();
            }catch(IOException e){
                e.printStackTrace();
            }
        });
    }
}
