package com.lbrowser.lbrowser.media;

import java.nio.charset.StandardCharsets;

public class MediaItem {
    private final String url;
    private final String type;
    private boolean selected = true;

    public MediaItem(String url, String type) {
        this.url = url;
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public boolean isSelected() {
        return selected;
    }


    public void setSelected(boolean selected) {
        this.selected = selected;
    }

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
