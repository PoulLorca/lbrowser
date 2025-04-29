package com.lbrowser.lbrowser.modes;

import io.qt.network.QNetworkProxy;
import io.qt.webengine.core.QWebEngineProfile;
import io.qt.webengine.core.QWebEngineSettings;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkModeManager {
    private static final Logger LOGGER = Logger.getLogger(NetworkModeManager.class.getName());

    public enum NetworkMode {
        NORMAL("Normal Mode", "https://startpage.com"),
        TOR("Tor Mode", "https://check.torproject.org/"),
        ZERONET("ZeroNet Mode", "http://127.0.0.1:43111/"),
        I2P("I2P Mode", "http://127.0.0.1:7657/"),
        FREENET("Hyphanet Mode", "http://127.0.0.1:8000/");

        private final String displayName;
        private final String startUrl;

        NetworkMode(String displayName, String startUrl) {
            this.displayName = displayName;
            this.startUrl = startUrl;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getStartUrl() {
            return startUrl;
        }
    }

    private NetworkMode currentMode = NetworkMode.NORMAL;

    public NetworkModeManager(){
        LOGGER.log(Level.INFO, "NetworkModeManager initialized. Current mode: {0}", currentMode);
    }

    public void setMode(NetworkMode mode) {
        if (mode == null || mode == this.currentMode) {
            return;
        }

        LOGGER.log(Level.INFO, "Switching network mode from {0} to {1}", new Object[]{this.currentMode, mode});
        this.currentMode = mode;
    }

    public NetworkMode getCurrentMode() {
        return currentMode;
    }

    public String getModeStartUrl(NetworkMode mode){
        return (mode != null) ? mode.getStartUrl() : NetworkMode.NORMAL.getStartUrl();
    }

    public String getCurrentModeStartUrl(){
        return currentMode.getStartUrl();
    }

    public void configureProxyForProfile(QWebEngineProfile profile) {
        if (profile == null) {
            LOGGER.warning("Cannot configure proxy for null profile");
            return;
        }

        // Security settings
        profile.setHttpCacheType(QWebEngineProfile.HttpCacheType.MemoryHttpCache);
        profile.setPersistentCookiesPolicy(QWebEngineProfile.PersistentCookiesPolicy.NoPersistentCookies);

        QNetworkProxy proxy = new QNetworkProxy();

        switch(currentMode){
            case TOR:
                LOGGER.info("Configuring Tor SOCKS5 proxy: 127.0.0.1:9050");
                proxy.setType(QNetworkProxy.ProxyType.Socks5Proxy);
                proxy.setHostName("127.0.0.1");
                proxy.setPort(9050);
                break;

            case I2P:
                LOGGER.info("Configuring I2P HTTP proxy: 127.0.0.1:4444");
                proxy.setType(QNetworkProxy.ProxyType.HttpProxy);
                proxy.setHostName("127.0.0.1");
                proxy.setPort(4444);
                break;

            default:
                LOGGER.info("Configuring NoProxy");
                proxy.setType(QNetworkProxy.ProxyType.NoProxy);
        }
        QNetworkProxy.setApplicationProxy(proxy);
    }
}
