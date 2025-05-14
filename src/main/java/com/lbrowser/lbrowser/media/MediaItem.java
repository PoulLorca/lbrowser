package com.lbrowser.lbrowser.media;

import java.nio.charset.StandardCharsets;

/**
 * Represents a media item (like image or video) for display in the browser.
 * Contains URL, type information and selection state for use in media item listings.
 */
public class MediaItem {
    private final String url;
    private final String type;
    private boolean selected = true;

    /**
     * Creates a new media item with the specified URL and type.
     *
     * @param url The URL of the media resource
     * @param type The media type (e.g., "image", "video")
     */
    public MediaItem(String url, String type) {
        this.url = url;
        this.type = type;
    }

    /**
     * Gets the URL of the media resource.
     *
     * @return The media URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the type of media (e.g., "image", "video").
     *
     * @return The media type
     */
    public String getType() {
        return type;
    }

    /**
     * Checks if this media item is currently selected.
     *
     * @return True if the item is selected, false otherwise
     */
    public boolean isSelected() {
        return selected;
    }


    /**
     * Sets the selection state of this media item.
     *
     * @param selected True to select the item, false to deselect
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Extracts a usable filename from the item's URL.
     * Handles URL decoding, removes invalid characters, and truncates if necessary.
     *
     * @return A valid filename derived from the URL
     */
    public String getFilename() {
        String path = getUrl();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            path = path.substring(lastSlash + 1);
        }
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }

        try{
            path = java.net.URLDecoder.decode(path, StandardCharsets.UTF_8.name());
        }catch(Exception e){}

        path = path.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        if(path.length() > 100){
            path = path.substring(0, 100);
        }

        return path.isEmpty() ? "unknown_media" : path;
    }
}
