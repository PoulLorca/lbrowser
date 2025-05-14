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

/**
 * Main application window for the LBrowser web browser.
 * Provides tab-based browsing with navigation controls, ad blocking capabilities,
 * network mode management, and media source extraction.
 */
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
    private static final String PROJECT_WEBSITE = "https://poullorca.github.io/lbrowser-site/";

    /**
     * Creates a new main window with browser functionality and initializes components.
     */
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

    /**
     * Sets up the user interface components for the browser window.
     * Initializes toolbars, buttons, address bar, and tab container.
     */
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
        newTabButton.setIcon(QIcon.fromTheme("add", new QIcon("classpath:icons/plus-circle.svg")));
        newTabButton.setToolTip("New Tab");
        navigationToolBar.addWidget(newTabButton);

        urlLineEdit = new QLineEdit();
        navigationToolBar.addWidget(urlLineEdit);

        optionsButton = new QToolButton();
        optionsButton.setIcon(QIcon.fromTheme("system-run", new QIcon("classpath:icons/menu-dots-circle.svg")));
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

    /**
     * Sets up the options menu with zoom controls, history, media extraction,
     * ad blocking, network configuration, and developer tools.
     */
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

        QAction aboutAction = optionsMenu.addAction("About");
        aboutAction.triggered.connect(this::showAboutDialog);
    }

    /**
     * Shows the about dialog displaying the project website.
     */
    private void showAboutDialog() {
        createNewTab(PROJECT_WEBSITE);
    }

    /**
     * Sets up signal and slot connections for UI elements.
     */
    private void setupConnections(){
        backButton.clicked.connect(this::goBack);
        forwardButton.clicked.connect(this::goForward);
        reloadButton.clicked.connect(this::reloadPage);
        newTabButton.clicked.connect(this::handleNewTabButton);

        urlLineEdit.returnPressed.connect(this::loadUrlInCurrentTab);
        tabWidget.currentChanged.connect(this::onTabChanged);
        tabWidget.tabCloseRequested.connect(this::closeTab);
    }

    /**
     * Gets the web view widget from the currently active tab.
     *
     * @return The current QWebEngineView, or null if none exists
     */
    private QWebEngineView getCurrentQtView(){
        QWidget currentWidget = tabWidget.currentWidget();
        if(currentWidget instanceof QWebEngineView){
            return (QWebEngineView) currentWidget;
        }
        return null;
    }

    /**
     * Gets the web page from the currently active tab.
     *
     * @return The current QWebEnginePage, or null if none exists
     */
    private QWebEnginePage getCurrentQtPage(){
        QWebEngineView view = getCurrentQtView();
        return (view != null) ? view.page() : null;
    }

    /**
     * Creates a new browser tab with the specified URL.
     * Configures the web view with appropriate settings and loads the URL.
     *
     * @param urlToLoad The URL to load in the new tab
     */
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

    /**
     * Sets up signal connections for a web view to handle URL changes,
     * page loading events, and new window requests.
     *
     * @param webView The web view to configure
     */
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

    /**
     * Closes the tab at the specified index.
     * Properly disconnects signals and disposes of resources.
     *
     * @param index The index of the tab to close
     */
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

    /**
     * Handles tab change events by updating the UI to reflect the current tab.
     *
     * @param index The index of the newly selected tab
     */
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

    /**
     * Creates a new tab when the new tab button is clicked.
     */
    private void handleNewTabButton(){
        createNewTab(networkModeManager.getCurrentModeStartUrl());
    }

    /**
     * Loads the URL from the address bar in the current tab.
     */
    private void loadUrlInCurrentTab(){
        QWebEngineView view = getCurrentQtView();
        if(view != null){
            String url = formatUrl(urlLineEdit.text());
            view.load(new QUrl(url));
        }else{

        }
    }

    /**
     * Formats a URL string by adding appropriate protocol prefixes if needed.
     *
     * @param url The URL to format
     * @return The properly formatted URL
     */
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

    /**
     * Navigates back in the current web view.
     */
    private void goBack(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) view.back();
    }

    /**
     * Navigates forward in the current web view.
     */
    private void goForward(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) view.forward();
    }

    /**
     * Reloads the current page.
     */
    private void reloadPage(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) {
            view.reload();
        }
    }

    /**
     * Increases the zoom level of the current web view.
     */
    private void zoomIn(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) view.setZoomFactor(view.getZoomFactor() + 0.1);
    }

    /**
     * Decreases the zoom level of the current web view.
     */
    private void zoomOut(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) view.setZoomFactor(view.getZoomFactor() - 0.1);
    }

    /**
     * Resets the zoom level of the current web view to 100%.
     */
    private void zoomReset(){
        QWebEngineView view = getCurrentQtView();
        if(view != null) view.setZoomFactor(1.0);
    }

    /**
     * Shows the network configuration dialog.
     * Initiates Docker setup process with progress feedback.
     */
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

    /**
     * Updates the progress dialog during Docker setup.
     *
     * @param value The progress percentage
     * @param message The progress message to display
     */
    private void updateProgressDialog(int value, String message) {
        if (progressDialog != null) {
            progressDialog.setValue(value);
            progressDialog.setLabelText(message);
        }
    }

    /**
     * Handles completion of the Docker setup task.
     * Shows appropriate messages and opens Portainer if successful.
     *
     * @param success Whether the setup was successful
     * @param finalMessage The final status message
     */
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

    /**
     * Handles changes to the network mode.
     * Updates network settings and opens a new tab with the appropriate start URL.
     */
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

    /**
     * Toggles the ad blocker on or off.
     * Applies ad blocking CSS to all open tabs when enabled.
     *
     * @param checked Whether ad blocking should be enabled
     */
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

    /**
     * Shows the browsing history in a new tab.
     * Currently displays a privacy notice since history is not persisted.
     */
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

    /**
     * Shows the media sources dialog for downloading images and videos.
     * Extracts media sources from the current page using JavaScript.
     */
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
                dialog.setWindowIcon(QIcon.fromTheme("media-playback-start", new QIcon("classpath:icons/compass.svg")));
                dialog.exec();
            }
        });
    }

    /**
     * Parses JSON data containing media sources extracted from a web page.
     *
     * @param jsonString The JSON string containing media URLs
     * @return A list of MediaItem objects representing extractable media
     */
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

    /**
     * Updates the UI elements to reflect the current state of the web view.
     *
     * @param view The web view to get state from
     */
    private void updateUiForView(QWebEngineView view){
        if(view == null){
            clearUiState();
            return;
        }
        urlLineEdit.setText(view.url().toString());
        updateNavigationButtons();

    }

    /**
     * Updates the navigation buttons based on the current page state.
     * Enables or disables back, forward, and reload buttons as appropriate.
     */
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

    /**
     * Clears the UI state when no tab is active.
     */
    private void clearUiState(){
        urlLineEdit.clear();
        backButton.setEnabled(false);
        forwardButton.setEnabled(false);
        reloadButton.setEnabled(false);
    }

    /**
     * Toggles the visibility of the developer tools window.
     */
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

    /**
     * Creates the developer tools window.
     */
    private void createDevToolsWindow() {
        devToolsDialog = new QDialog(this);
        devToolsDialog.setWindowTitle("Developer Tools");
        devToolsDialog.resize(800, 600);

        devToolsWebView = new QWebEngineView();
        QVBoxLayout layout = new QVBoxLayout(devToolsDialog);
        layout.addWidget(devToolsWebView);
    }

    /**
     * Loads developer tools for the current tab.
     */
    private void loadCurrentTabDevTools() {
        QWebEngineView currentView = getCurrentQtView();
        if (currentView != null) {
            String debugUrl = "http://localhost:9222/devtools/inspector.html?ws=localhost:9222/devtools/page/"
                    + currentView.page().devToolsId();
            devToolsWebView.load(new QUrl(debugUrl));
        }
    }


    /**
     * Handles window close events by closing all tabs properly.
     *
     * @param event The close event
     */
    @Override
    protected void closeEvent(QCloseEvent event){
        while(tabWidget.count() > 0){
            closeTab(0);
        }
        event.accept();
        super.closeEvent(event);
    }

    /**
     * Applies ad blocking CSS to a web page if ad blocking is enabled.
     * Injects CSS via JavaScript to hide ad elements.
     *
     * @param page The web page to apply ad blocking to
     */
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
