package com.lbrowser.lbrowser;

import com.lbrowser.lbrowser.modes.NetworkModeManager;
import io.qt.core.QUrl;
import io.qt.core.Qt;
import io.qt.gui.*;
import io.qt.webengine.core.QWebEnginePage;
import io.qt.webengine.core.QWebEngineProfile;
import io.qt.webengine.widgets.QWebEngineView;
import io.qt.widgets.*;

import java.util.logging.Logger;

public class LMainWindow  extends QMainWindow {
    private static final Logger LOGGER = Logger.getLogger(LMainWindow.class.getName());

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

    private NetworkModeManager networkModeManager;
    private AdBlocker adBlocker;
    private QActionGroup networkModeActionGroup;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/605.1 (KHTML, like Gecko) Lbrowser/1.0 QtJambi";

    public LMainWindow() {
        super();

        networkModeManager = new NetworkModeManager();
        adBlocker = new AdBlocker();

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
        newTabButton.setIcon(QIcon.fromTheme("new-tab", new QIcon("classpath:icons/plus-circle.svg")));
        newTabButton.setToolTip("New Tab");
        navigationToolBar.addWidget(newTabButton);

        urlLineEdit = new QLineEdit();
        navigationToolBar.addWidget(urlLineEdit);

        optionsButton = new QToolButton();
        optionsButton.setIcon(QIcon.fromTheme("application-menu", new QIcon("classpath:icons/menu-dots-circle.svg")));
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
        QAction zoomInAction = optionsMenu.addAction("Zoom In");
        zoomInAction.setShortcut(new QKeySequence(Qt.Key.Key_Control , Qt.Key.Key_Plus));
        zoomInAction.triggered.connect(this::zoomIn);

        QAction zoomOutAction = optionsMenu.addAction("Zoom Out");
        zoomOutAction.setShortcut(new QKeySequence(Qt.Key.Key_Control , Qt.Key.Key_Minus));
        zoomOutAction.triggered.connect(this::zoomOut);

        QAction resetZoomAction = optionsMenu.addAction("Reset Zoom");
        resetZoomAction.setShortcut(new QKeySequence(Qt.Key.Key_Control , Qt.Key.Key_0));
        resetZoomAction.triggered.connect(this::zoomReset);

        optionsMenu.addSeparator();

        QAction historyAction = optionsMenu.addAction("History");
        historyAction.setShortcut(new QKeySequence(Qt.Key.Key_Control , Qt.Key.Key_H));
        historyAction.triggered.connect(this::showHistory);

        QAction mediaSourcesAction = optionsMenu.addAction("Media Sources");
        mediaSourcesAction.triggered.connect(this::showMediaSources);

        QAction noAdModeAction = optionsMenu.addAction("Mode No-Ad");
        noAdModeAction.setCheckable(true);
        noAdModeAction.setChecked(adBlocker.isEnabled());
        noAdModeAction.toggled.connect(this::toggleAdBlockMode);

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
                urlLineEdit.setText(webView.url().toString());
                // if (ok) applyAdBlockerCssToQtPage(webView.page());
            }
            // if (ok) applyAdBlockerCssToQtPage(webView.page());
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
    }

    private void showHistory(){
        //Needs implementation
    }

    private void showMediaSources(){
        //Needs implementation
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

    @Override
    protected void closeEvent(QCloseEvent event){
        while(tabWidget.count() > 0){
            closeTab(0);
        }
        event.accept();
        super.closeEvent(event);
    }
}
