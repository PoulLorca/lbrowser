package com.lbrowser.lbrowser.modes;

import io.qt.network.QNetworkProxy;
import io.qt.webengine.core.QWebEngineProfile;
import io.qt.webengine.core.QWebEngineSettings;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages different network modes for the browser, including normal browsing,
 * Tor, ZeroNet, I2P and Freenet (Hyphanet) connections. Each mode can have different
 * proxy settings and starting URLs.
 */
public class NetworkModeManager {
    private static final Logger LOGGER = Logger.getLogger(NetworkModeManager.class.getName());

    /**
     * Represents the available network modes with their display names and start URLs.
     * Each mode can have specific proxy configurations applied when activated.
     */
    public enum NetworkMode {
        NORMAL("Normal Mode", "https://startpage.com"),
        TOR("Tor Mode", "https://check.torproject.org/"),
        ZERONET("ZeroNet Mode", "http://127.0.0.1:43111/"),
        I2P("I2P Mode", "http://127.0.0.1:7657/"),
        FREENET("Hyphanet Mode", "http://127.0.0.1:8123/");

        private final String displayName;
        private final String startUrl;

        /**
         * Creates a new network mode with display name and start URL.
         *
         * @param displayName Human-readable name for the mode
         * @param startUrl URL to load when this mode is activated
         */
        NetworkMode(String displayName, String startUrl) {
            this.displayName = displayName;
            this.startUrl = startUrl;
        }

        /**
         * Gets the display name for this network mode.
         *
         * @return The human-readable name
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Gets the starting URL for this network mode.
         *
         * @return The URL to load when this mode is activated
         */
        public String getStartUrl() {
            return startUrl;
        }
    }

    private NetworkMode currentMode = NetworkMode.NORMAL;

    /**
     * Creates a new NetworkModeManager with the default mode (NORMAL).
     */
    public NetworkModeManager(){
        LOGGER.log(Level.INFO, "NetworkModeManager initialized. Current mode: {0}", currentMode);
    }

    /**
     * Sets the current network mode and logs the change.
     * Does nothing if the requested mode is the same as the current mode.
     *
     * @param mode The network mode to set
     */
    public void setMode(NetworkMode mode) {
        if (mode == null || mode == this.currentMode) {
            return;
        }

        LOGGER.log(Level.INFO, "Switching network mode from {0} to {1}", new Object[]{this.currentMode, mode});
        this.currentMode = mode;
    }

    /**
     * Gets the current network mode.
     *
     * @return The current network mode
     */
    public NetworkMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Gets the start URL for the specified network mode.
     *
     * @param mode The network mode to get the start URL for
     * @return The start URL for the specified mode, or the default URL if mode is null
     */
    public String getModeStartUrl(NetworkMode mode){
        return (mode != null) ? mode.getStartUrl() : NetworkMode.NORMAL.getStartUrl();
    }

    /**
     * Gets the start URL for the current network mode.
     *
     * @return The start URL for the current mode
     */
    public String getCurrentModeStartUrl(){
        return currentMode.getStartUrl();
    }

    /**
     * Configures the network proxy settings based on the current mode.
     * Sets the appropriate proxy type, host and port according to the selected mode.
     * Also configures security settings like cache type and cookie policy.
     *
     * @param profile The web engine profile to configure
     */
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
