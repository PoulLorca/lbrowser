package com.lbrowser.lbrowser.modes;

import io.qt.network.QNetworkProxy;
import io.qt.webengine.core.QWebEngineProfile;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkModeManager {
    private static final Logger LOGGER = Logger.getLogger(NetworkModeManager.class.getName());

    public enum NetworkMode {
        NORMAL("Normal Mode", "https://startpage.com"),
        TOR("Tor Mode", "https://check.torproject.org/"),
        ZERONET("ZeroNet Mode", "http://127.0.0.1:43110/"),
        FREENET("Freenet Mode", "http://127.0.0.1:8888/");

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

        QNetworkProxy proxy = new QNetworkProxy();

        if(currentMode == NetworkMode.TOR){
            LOGGER.info("Configuring Tor SOCKS5 proxy for profile: 127.0.0.1:9050");
            proxy.setType(QNetworkProxy.ProxyType.Socks5Proxy);
            proxy.setHostName("127.0.0.1");
            proxy.setPort(9050);
        }else{
            LOGGER.info("Configuring NoProxy for profile");
            proxy.setType(QNetworkProxy.ProxyType.NoProxy);
        }
        QNetworkProxy.setApplicationProxy(proxy);
    }
}
