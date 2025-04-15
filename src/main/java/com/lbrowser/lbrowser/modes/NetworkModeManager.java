package com.lbrowser.lbrowser.modes;

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
        clearTorProxySettings();
        LOGGER.log(Level.INFO, "NetworkModeManager initialized. Current mode: {0}", currentMode);
    }

    public void setMode(NetworkMode mode) {
        if (mode == null || mode == this.currentMode) {
            return;
        }

        LOGGER.log(Level.INFO, "Switching network mode from {0} to {1}", new Object[]{this.currentMode, mode});
        this.currentMode = mode;

        if(mode == NetworkMode.TOR){
            applyTorProxySettings();
        }else{
            clearTorProxySettings();
        }
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

    private void applyTorProxySettings() {
        LOGGER.info("Applying Tor SOCKS proxy settings (127.0.0.1:9050");
        System.setProperty("java.net.useSystemProxies", "false");
        System.setProperty("socksProxyHost", "127.0.0.1");
        System.setProperty("socksProxyPort", "9050");
    }

    private void clearTorProxySettings() {
        LOGGER.info("Clearing Tor SOCKS proxy settings");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        //System.setProperty("java.net.useSystemProxies", "true"); Restore the system proxies if needed
    }
}
