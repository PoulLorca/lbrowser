package com.lbrowser.lbrowser.media;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;

public class MediaItem {
    private final String url;
    private final String type;
    private final BooleanProperty selected = new SimpleBooleanProperty(true);
    private Image preview;

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
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public Image getPreview() {
        if(preview == null && "image".equals(type)) {
            loadPreviewAsync();
        }
        return preview;
    }

    public void setPreview(Image preview) {
        this.preview = preview;
    }


    public String getFilename() {
        String url = getUrl();
        return url.substring(url.lastIndexOf("/") + 1);
    }

    private void loadPreviewAsync() {
        new Thread(() -> {
            Image img = new Image(url, 100, 0, true, true, true);
            Platform.runLater(() -> preview = img);
        }).start();
    }
}
