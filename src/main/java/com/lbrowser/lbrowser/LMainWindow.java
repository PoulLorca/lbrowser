package com.lbrowser.lbrowser;

import com.lbrowser.lbrowser.dialogs.DockerSetupTask;
import com.lbrowser.lbrowser.dialogs.MediaSelectorDialog;
import com.lbrowser.lbrowser.media.MediaItem;
import com.lbrowser.lbrowser.modes.DockerManager;
import com.lbrowser.lbrowser.modes.NetworkModeManager;
import io.qt.core.*;
import io.qt.gui.*;
import io.qt.webengine.core.QWebEnginePage;
import io.qt.webengine.core.QWebEngineProfile;
import io.qt.webengine.widgets.QWebEngineView;
import io.qt.widgets.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LMainWindow  extends QMainWindow {
    private QToolBar navigationToolBar;
    private QLineEdit urlLineEdit;
    private QToolButton backButton;
    private QToolButton forwardButton;
    private QToolButton reloadButton;
    private QToolButton newTabButton;
    private QProgressBar loadingProgressBar;
    private QToolButton optionsButton;
    private QMenu optionsMenu;
    private QTabWidget tabWidget;
    private QDialog devToolsDialog;
    private QWebEngineView devToolsWebView;
    private NetworkModeManager networkModeManager;
    private AdBlocker adBlocker;
    private QActionGroup networkModeActionGroup;
    private DockerManager dockerManager;
    private DockerSetupTask dockerTask;
    private QProgressDialog progressDialog;


    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/605.1 (KHTML, like Gecko) Lbrowser/1.0 QtJambi";
    private static final String DOCKER_COMPOSE_FILE = "docker-compose.yml";

    public LMainWindow() {
        super();

        networkModeManager = new NetworkModeManager();
        adBlocker = new AdBlocker();
        dockerManager = new DockerManager(DOCKER_COMPOSE_FILE);

        setWindowTitle("LBrowser");
        resize(1024, 768);

        setupUI();
        setupConnections();

        createNewTab(networkModeManager.getCurrentModeStartUrl());
    }

    private void setupUI() {
        navigationToolBar = new QToolBar("Navigation");
        addToolBar(navigationToolBar);

        backButton = new QToolButton();
        backButton.setIcon(QIcon.fromTheme("go-previous", new QIcon("classpath:icons/go-previous.png")));
        backButton.setToolTip("Go Back");
        navigationToolBar.addWidget(backButton);

        forwardButton = new QToolButton();
        forwardButton.setIcon(QIcon.fromTheme("go-next", new QIcon("classpath:icons/go-next.png")));
        forwardButton.setToolTip("Go Forward");
        navigationToolBar.addWidget(forwardButton);

        reloadButton = new QToolButton();
        reloadButton.setIcon(QIcon.fromTheme("view-refresh", new QIcon("classpath:icons/view-refresh.png")));
        reloadButton.setToolTip("Reload");
        navigationToolBar.addWidget(reloadButton);

        newTabButton = new QToolButton();
        newTabButton.setIcon(QIcon.fromTheme("tab-new-symbolic", new QIcon("classpath:icons/plus-circle.svg")));
        newTabButton.setToolTip("New Tab");
        navigationToolBar.addWidget(newTabButton);

        urlLineEdit = new QLineEdit();
        navigationToolBar.addWidget(urlLineEdit);

        optionsButton = new QToolButton();
        optionsButton.setIcon(QIcon.fromTheme("open-menu-symbolic", new QIcon("classpath:icons/menu-dots-circle.svg")));
        optionsButton.setToolTip("Options");
        optionsButton.setPopupMode(QToolButton.ToolButtonPopupMode.InstantPopup);
        optionsMenu = new QMenu(this);
        optionsButton.setMenu(optionsMenu);
        navigationToolBar.addWidget(optionsButton);

        setupOptionsMenu();

        loadingProgressBar = new QProgressBar();
        loadingProgressBar.setRange(0, 100);
        loadingProgressBar.setValue(0);
        loadingProgressBar.setTextVisible(false);
        loadingProgressBar.setFixedHeight(3);
        loadingProgressBar.setVisible(false);
        QPalette palette = loadingProgressBar.palette();
        palette.setColor(QPalette.ColorRole.Highlight, new QColor("#3498db"));
        loadingProgressBar.setPalette(palette);


        tabWidget = new QTabWidget();
        tabWidget.setTabsClosable(true);
        tabWidget.setMovable(true);

        QVBoxLayout mainLayout = new QVBoxLayout();
        mainLayout.setContentsMargins(0, 0, 0, 0);
        mainLayout.setSpacing(0);

        mainLayout.addWidget(loadingProgressBar);
        mainLayout.addWidget(tabWidget);

        QWidget centralContainer = new QWidget();
        centralContainer.setLayout(mainLayout);

        setCentralWidget(centralContainer);

        updateNavigationButtons();
    }

    private void setupOptionsMenu(){
        optionsMenu.clear();

        QAction zoomInAction = optionsMenu.addAction("Zoom In");
        zoomInAction.setShortcut(QKeySequence.fromString("Ctrl++"));
        zoomInAction.triggered.connect(this::zoomIn);

        QAction zoomOutAction = optionsMenu.addAction("Zoom Out");
        zoomOutAction.setShortcut(QKeySequence.fromString("Ctrl+-"));
        zoomOutAction.triggered.connect(this::zoomOut);

        QAction resetZoomAction = optionsMenu.addAction("Reset Zoom");
        resetZoomAction.setShortcut(QKeySequence.fromString("Ctrl+0"));
        resetZoomAction.triggered.connect(this::zoomReset);

        optionsMenu.addSeparator();

        QAction historyAction = optionsMenu.addAction("History");
        historyAction.setShortcut(QKeySequence.fromString("Ctrl+H"));
        historyAction.triggered.connect(this::showHistory);

        QAction mediaSourcesAction = optionsMenu.addAction("Media Sources");
        mediaSourcesAction.triggered.connect(this::showMediaSources);

        QAction noAdModeAction = optionsMenu.addAction("Mode No-Ad");
        noAdModeAction.setCheckable(true);
        noAdModeAction.setChecked(adBlocker.isEnabled());
        noAdModeAction.toggled.connect(this::toggleAdBlockMode);

        optionsMenu.addSeparator();

        QAction netConfigAction = optionsMenu.addAction("Net Config");
        netConfigAction.triggered.connect(this::showNetConfig);

        QMenu modesMenu = optionsMenu.addMenu("Net Modes");
        networkModeActionGroup = new QActionGroup(this);
        networkModeActionGroup.setExclusive(true);

        for(NetworkModeManager.NetworkMode mode : NetworkModeManager.NetworkMode.values()) {
            QAction modeAction = modesMenu.addAction(mode.getDisplayName());
            modeAction.setCheckable(true);
            modeAction.setData(mode);
            networkModeActionGroup.addAction(modeAction);

            if(mode == networkModeManager.getCurrentMode()) {
                modeAction.setChecked(true);
            }

            modeAction.triggered.connect(this::handleModeChange);
        }

        optionsMenu.addSeparator();

        QAction devToolsAction = optionsMenu.addAction("Dev Tools");
        devToolsAction.setShortcut(QKeySequence.fromString("Ctrl+Shift+I"));
        devToolsAction.triggered.connect(this::toggleDevTools);

        optionsMenu.addSeparator();
        optionsMenu.addAction("About");
    }

    private void setupConnections(){
        backButton.clicked.connect(this::goBack);
        forwardButton.clicked.connect(this::goForward);
        reloadButton.clicked.connect(this::reloadPage);
        newTabButton.clicked.connect(this::handleNewTabButton);

        urlLineEdit.returnPressed.connect(this::loadUrlInCurrentTab);
        tabWidget.currentChanged.connect(this::onTabChanged);
        tabWidget.tabCloseRequested.connect(this::closeTab);
    }

    private QWebEngineView getCurrentQtView(){
        QWidget currentWidget = tabWidget.currentWidget();
        if(currentWidget instanceof QWebEngineView){
            return (QWebEngineView) currentWidget;
        }
        return null;
    }

    private QWebEnginePage getCurrentQtPage(){
        QWebEngineView view = getCurrentQtView();
        return (view != null) ? view.page() : null;
    }

    private void createNewTab(String urlToLoad){
        QWebEngineView qtWebView = new QWebEngineView();
        QWebEnginePage qtPage = qtWebView.page();
        QWebEngineProfile qtProfile = qtPage.profile();

        qtProfile.setHttpUserAgent(USER_AGENT);
        networkModeManager.configureProxyForProfile(qtProfile);

        setupWebViewSignals(qtWebView);

        int index = tabWidget.addTab(qtWebView, "Loading...");
        tabWidget.setCurrentIndex(index);

        qtWebView.load(new QUrl(formatUrl(urlToLoad)));

        urlLineEdit.setFocus();
    }

    private void setupWebViewSignals(QWebEngineView webView){
        webView.urlChanged.connect(qUrl -> {
            if(webView == getCurrentQtView()){
                urlLineEdit.setText(qUrl.toString());
                updateNavigationButtons();
            }
        });

        webView.titleChanged.connect(title -> {
            int index = tabWidget.indexOf(webView);
            if(index != -1){
                if (loadingProgressBar.isVisible() && webView == getCurrentQtView()) {

                }else{
                    tabWidget.setTabText(index, title != null && !title.trim().isEmpty() ? title : webView.url().toString());
                }
            }
        });

        webView.loadStarted.connect(() -> {
            webView.page().setProperty("isLoading", true);

            int index = tabWidget.indexOf(webView);
            if(index != -1){
                tabWidget.setTabText(index, "Loading...");
            }
            if(webView == getCurrentQtView()){
                loadingProgressBar.setValue(0);
                loadingProgressBar.setVisible(true);
                updateNavigationButtons();
            }
        });

        webView.loadProgress.connect(progress -> {
            if(webView == getCurrentQtView()){
                loadingProgressBar.setValue(progress);
            }
        });

        webView.loadFinished.connect(ok -> {
            webView.page().setProperty("isLoading", false);

            int index = tabWidget.indexOf(webView);
            if(index != -1){
                String title = webView.title();
                String loc = webView.url().toString();
                if(!ok){
                    tabWidget.setTabText(index, "Load Failed");
                } else if (title != null && !title.trim().isEmpty()) {
                    tabWidget.setTabText(index, title);
                }else{
                    tabWidget.setTabText(index, loc);
                }
            }
            if(webView == getCurrentQtView()){
                loadingProgressBar.setVisible(false);
                updateNavigationButtons();
                if(ok) urlLineEdit.setText(webView.url().toString());
            }
            if (ok) {
                applyAdBlockerCssToQtPage(webView.page());
            }
        });

        webView.page().newWindowRequested.connect(request -> {
            createNewTab(networkModeManager.getCurrentModeStartUrl());
            int newIndex = tabWidget.count() - 1;
            QWidget newWidget = tabWidget.widget(newIndex);
            if (newWidget instanceof QWebEngineView) {
                request.openIn(((QWebEngineView) newWidget).page());
            }
        });
    }

    private void closeTab(int index){
        if (index < 0 || index >= tabWidget.count()) {
            return;
        }

        QWidget widget = tabWidget.widget(index);
        if(widget instanceof QWebEngineView){
            QWebEngineView view = (QWebEngineView) widget;
            view.stop();

            view.loadStarted.disconnect();
            view.loadProgress.disconnect();
            view.loadFinished.disconnect();
            view.titleChanged.disconnect();
            view.urlChanged.disconnect();
            view.page().newWindowRequested.disconnect();

            view.stop();
            tabWidget.removeTab(index);
            view.dispose();
        }else{
            tabWidget.removeTab(index);
        }
    }

    private void onTabChanged(int index){
        if(index >= 0 && index < tabWidget.count()){
            QWebEngineView currentView = getCurrentQtView();
            if(currentView != null){
                updateUiForView(currentView);
                QWebEnginePage currentPage = currentView.page();
                boolean isLoading = currentPage.property("isLoading") != null && (Boolean) currentPage.property("isLoading");
                if(isLoading) {
                    loadingProgressBar.setVisible(true);
                }else{
                    loadingProgressBar.setVisible(false);
                }
            }else{
                clearUiState();
            }
        }else{
            clearUiState();
        }
    }

    private void handleNewTabButton(){
        createNewTab(networkModeManager.getCurrentModeStartUrl());
    }

    private void loadUrlInCurrentTab(){
        QWebEngineView view = getCurrentQtView();
        if(view != null){
            String url = formatUrl(urlLineEdit.text());
            view.load(new QUrl(url));
        }else{

        }
    }

    private String formatUrl(String url){
        if(url == null) return "";
        url = url.trim();
        if(!url.isEmpty() && !url.matches("^[a-zA-Z]+://.*") && !url.startsWith("about:") && !url.startsWith("file:")){
            if(!url.startsWith("localhost:") && !url.startsWith("127.0.0.1")){
                return "https://" + url;
            }
        }
        return url;
    }

    private void goBack(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) view.back();
    }

    private void goForward(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) view.forward();
    }

    private void reloadPage(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) {
            view.reload();
        }
    }

    private void zoomIn(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) view.setZoomFactor(view.getZoomFactor() + 0.1);
    }

    private void zoomOut(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) view.setZoomFactor(view.getZoomFactor() - 0.1);
    }

    private void zoomReset(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) view.setZoomFactor(1.0);
    }

    private void showNetConfig() {
        if (dockerManager == null){
            QMessageBox.critical(this, "Error", "DockerManager is not initialized.");
            return;
        }

        if(dockerTask != null && dockerTask.isRunning()){
            if(progressDialog != null && progressDialog.isVisible()){
                progressDialog.raise();
                progressDialog.activateWindow();
            }else{
                QMessageBox.information(this, "In progress", "The network configuration is already in progress.");
            }
            return;
        }

        progressDialog = new QProgressDialog("Setting up Network configuration...", "Cancel", 0, 100, this);
        progressDialog.setWindowTitle("Network Configuration");
        progressDialog.setMinimumDuration(0);
        progressDialog.setValue(0);
        progressDialog.setModal(true);

        dockerTask = new DockerSetupTask(dockerManager);

        dockerTask.progressUpdated.connect(this::updateProgressDialog);
        dockerTask.taskFinished.connect(this::onDockerTaskFinished);

        dockerTask.start();

        progressDialog.show();
    }

    private void updateProgressDialog(int value, String message) {
        if (progressDialog != null) {
            progressDialog.setValue(value);
            progressDialog.setLabelText(message);
        }
    }

    private void onDockerTaskFinished(boolean success, String finalMessage) {
        if (progressDialog != null) {
            progressDialog.setValue(100);
            progressDialog.setLabelText(finalMessage + (success ? "" : " (failed)"));
            progressDialog.close();
        }


        if (success) {
            QMessageBox.information(this, "Success", "Network configuration completed successfully.");
            createNewTab("http://localhost:9000");
        } else {
            QMessageBox.critical(this, "Error", "Network configuration failed: " + finalMessage);
        }

        dockerTask = null;
        progressDialog = null;
    }

    private void handleModeChange(){
        QAction triggeredAction = (QAction) sender();
        if (triggeredAction != null && triggeredAction.isChecked()) {
            NetworkModeManager.NetworkMode selectedMode = (NetworkModeManager.NetworkMode) triggeredAction.data();
            if (selectedMode != null) {
                networkModeManager.setMode(selectedMode);
                String startUrl  = networkModeManager.getModeStartUrl(selectedMode);
                createNewTab(startUrl);
            }
        }
    }

    private void toggleAdBlockMode(boolean checked){
        if (adBlocker == null) return;
        adBlocker.setEnabled(checked);

        for(int i = 0; i < tabWidget.count(); i++){
            QWidget widget = tabWidget.widget(i);
            if(widget instanceof QWebEngineView){
                QWebEnginePage page = ((QWebEngineView) widget).page();
                Object isLoadingProp = page.property("isLoading");
                boolean isLoading = isLoadingProp != null && (Boolean) isLoadingProp;
                if(!isLoading){
                    applyAdBlockerCssToQtPage(page);
                }
            }
        }
    }

    private void showHistory() {
        QWebEngineView originalView = getCurrentQtView();
        if (originalView == null) return;

        try {
            QWebEngineView historyView = new QWebEngineView();
            int tabIndex = tabWidget.addTab(historyView, "History");
            tabWidget.setCurrentIndex(tabIndex);

            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html><head>")
                    .append("<style>")
                    .append("body { font-family: Arial, sans-serif; margin: 50px; line-height: 1.6; }")
                    .append("h1 { color: #333; }")
                    .append(".container { max-width: 800px; margin: 0 auto; }")
                    .append(".info { background-color: #f8f9fa; border-left: 4px solid #4285f4; padding: 15px; margin: 20px 0; }")
                    .append(".note { color: #666; font-style: italic; margin-top: 20px; }")
                    .append("</style>")
                    .append("</head><body>")
                    .append("<div class='container'>")
                    .append("<h1>Browsing History</h1>")
                    .append("<div class='info'>")
                    .append("<p><strong>No browsing history available</strong></p>")
                    .append("<p>LBrowser is currently configured to prioritize your privacy by using:")
                    .append("<ul>")
                    .append("<li><code>NoPersistentCookies</code> - prevents websites from storing persistent cookies</li>")
                    .append("<li><code>MemoryHttpCache</code> - keeps cache only in RAM, not on disk</li>")
                    .append("</ul>")
                    .append("<p>With these privacy-focused settings, browsing history is not maintained.</p>")
                    .append("</div>")
                    .append("<p class='note'>This is an intentional design choice to enhance privacy and security. ")
                    .append("Future versions may include options for less restrictive modes, but the current focus ")
                    .append("is on minimizing data persistence and maximizing privacy.</p>")
                    .append("</div>")
                    .append("</body></html>");

            historyView.setHtml(htmlBuilder.toString(), new QUrl("about:blank"));
        } catch (Exception e) {
            System.err.println("Error showing the history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showMediaSources(){
        QWebEnginePage page = getCurrentQtPage();

        if(page == null){
            QMessageBox.warning(this, "Media Sources", "No media sources available.");
            return;
        }

        //Try to extract media sources with JavaScript
        String script = """
            (function() {
                const baseUrl = document.baseURI;
                const images = new Set();
                const videos = new Set();
                
                const resolveUrl = (relativeUrl) => {
                    try {                        
                        if (relativeUrl.startsWith('data:') || relativeUrl.startsWith('javascript:')) {
                             return null;
                        }
                        return new URL(relativeUrl, baseUrl).toString();
                    } catch (e) {                        
                        return null; 
                    }
                };
                
                document.querySelectorAll('img[src]').forEach(img => {
                    const absUrl = resolveUrl(img.getAttribute('src'));
                    if (absUrl && absUrl.startsWith('http')) {
                        images.add(absUrl);
                    }
                    // Support for srcset (experimental)
                });
                
                document.querySelectorAll('video').forEach(vid => {
                    const vidSrc = vid.getAttribute('src');
                    if (vidSrc) {
                        const absUrl = resolveUrl(vidSrc);
                        if (absUrl && absUrl.startsWith('http')) {
                            videos.add(absUrl);
                        }
                    }
                    vid.querySelectorAll('source[src]').forEach(source => {
                        const sourceSrc = source.getAttribute('src');
                        const absUrl = resolveUrl(sourceSrc);
                         if (absUrl && absUrl.startsWith('http')) {
                            videos.add(absUrl);
                        }
                    });
                });
                
                document.querySelectorAll('source[src]').forEach(source => {                     
                     if (!source.closest('video')) {
                         const sourceSrc = source.getAttribute('src');
                         const absUrl = resolveUrl(sourceSrc);
                         if (absUrl && absUrl.startsWith('http')) {                             
                             videos.add(absUrl);
                         }
                     }
                });

                return JSON.stringify({
                    images: Array.from(images),
                    videos: Array.from(videos)
                });
            })();
        """;

        page.runJavaScript(script, result -> {
            if (result == null || !(result instanceof String)){
                QMessageBox.warning(this, "Media Sources", "Could not extract media sources.");
                return;
            }

            String jsonResult = (String) result;
            List<MediaItem> mediaItems = parseMediaJson(jsonResult);

            if(mediaItems.isEmpty()){
                QMessageBox.information(this, "Media Sources", "No downloadable media sources found.");
            } else {
                MediaSelectorDialog dialog = new MediaSelectorDialog(mediaItems, this);
                dialog.exec();
            }
        });
    }

    private List<MediaItem> parseMediaJson(String jsonString) {
        List<MediaItem> items = new ArrayList<>();
        try{
            QJsonDocument.FromJsonResult doc = QJsonDocument.fromJson(jsonString.getBytes(StandardCharsets.UTF_8));

            if(doc == null || !doc.document.isObject()){
                return items;
            }
            QJsonObject obj = doc.document.object();

            if (obj.contains("images")) {
                QJsonValue imagesValue = obj.value("images");
                if (imagesValue.isArray()) {
                    QJsonArray imagesArray = imagesValue.toArray();
                    for (int i = 0; i < imagesArray.size(); i++) {
                        QJsonValue imageValue = imagesArray.at(i);
                        if (imageValue.isString()) {
                            String url = imageValue.toString();
                            if (url != null && !url.isEmpty()) {
                                items.add(new MediaItem(url, "image"));
                            }
                        }
                    }
                }
            }


            if (obj.contains("videos")) {
                QJsonValue videosValue = obj.value("videos");
                if (videosValue.isArray()) {
                    QJsonArray videosArray = videosValue.toArray();
                    for (int i = 0; i < videosArray.size(); i++) {
                        QJsonValue videoValue = videosArray.at(i);
                        if (videoValue.isString()) {
                            String url = videoValue.toString();
                            if (url != null && !url.isEmpty()) {
                                items.add(new MediaItem(url, "video"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return items;
    }

    private void updateUiForView(QWebEngineView view){
        if(view == null){
            clearUiState();
            return;
        }
        urlLineEdit.setText(view.url().toString());
        updateNavigationButtons();

    }

    private void updateNavigationButtons(){
        QWebEngineView view = getCurrentQtView();
        if(view != null){
            boolean isLoading = loadingProgressBar.isVisible();
            backButton.setEnabled(view.page().action(QWebEnginePage.WebAction.Back).isEnabled());
            forwardButton.setEnabled(view.page().action(QWebEnginePage.WebAction.Forward).isEnabled());
            reloadButton.setEnabled(view.page().action(QWebEnginePage.WebAction.Reload).isEnabled());
        }else{
            backButton.setEnabled(false);
            forwardButton.setEnabled(false);
            reloadButton.setEnabled(false);
        }
    }

    private void clearUiState(){
        urlLineEdit.clear();
        backButton.setEnabled(false);
        forwardButton.setEnabled(false);
        reloadButton.setEnabled(false);
    }

    private void toggleDevTools() {
        if (devToolsDialog == null) {
            createDevToolsWindow();
        }

        if (devToolsDialog.isVisible()) {
            devToolsDialog.hide();
        } else {
            devToolsDialog.show();
            loadCurrentTabDevTools();
        }
    }

    private void createDevToolsWindow() {
        devToolsDialog = new QDialog(this);
        devToolsDialog.setWindowTitle("Developer Tools");
        devToolsDialog.resize(800, 600);

        devToolsWebView = new QWebEngineView();
        QVBoxLayout layout = new QVBoxLayout(devToolsDialog);
        layout.addWidget(devToolsWebView);
    }

    private void loadCurrentTabDevTools() {
        QWebEngineView currentView = getCurrentQtView();
        if (currentView != null) {
            String debugUrl = "http://localhost:9222/devtools/inspector.html?ws=localhost:9222/devtools/page/"
                    + currentView.page().devToolsId();
            devToolsWebView.load(new QUrl(debugUrl));
        }
    }


    @Override
    protected void closeEvent(QCloseEvent event){
        while(tabWidget.count() > 0){
            closeTab(0);
        }
        event.accept();
        super.closeEvent(event);
    }

    private void applyAdBlockerCssToQtPage(QWebEnginePage page){
        if(page == null || adBlocker == null) return;

        String currentUrl = page.url().toString();
        String cssToInject = "";

        if(adBlocker.isEnabled() && adBlocker.areRulesLoaded()){
            cssToInject = adBlocker.getEffectiveCssForDomain(currentUrl);
            if(cssToInject == null ) cssToInject = "";
        }

        String escapedCss = cssToInject
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("${", "\\${");

        String script = String.format(
                "(function() {" +
                        "  var css = `%s`;" +
                        "  var styleId = 'lbrowser-adblock-style';" +
                        "  var styleElement = document.getElementById(styleId);" +
                        "  if (!styleElement) {" +
                        "    styleElement = document.createElement('style');" +
                        "    styleElement.id = styleId;" +
                        "    (document.head || document.documentElement).appendChild(styleElement);" +
                        "  }" +
                        "  /* console.log('Applying AdBlock CSS (' + (css.length > 0 ? css.length + ' bytes' : 'empty') + ')'); */" +
                        "  styleElement.textContent = css;" +
                        "})();",
                escapedCss
        );

        page.runJavaScript(script);
    }
}
