package com.lbrowser.lbrowser.media;

import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class MediaListCell extends ListCell<MediaItem> {
    private final ImageView imageView = new ImageView();
    private final CheckBox checkBox = new CheckBox();
    private final Label label = new Label();
    private final HBox hbox;

    public MediaListCell(){
        hbox = new HBox(15, checkBox, imageView, label);
        imageView.setFitWidth(100);
        imageView.setPreserveRatio(true);

        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            getItem().setSelected(newValue);
        });
    }

    @Override
    protected void updateItem(MediaItem item, boolean empty){
        super.updateItem(item, empty);
        if (empty || item == null){
            setText(null);
            setGraphic(null);
        }else{
            label.setText(item.getFilename());
            checkBox.setSelected(item.isSelected());
            loadPreviewAsync(item);
            setGraphic(hbox);
        }
    }

    private void loadPreviewAsync(MediaItem item){
        if(item.getType().equals("image")){
            new Thread(() -> {
                Image img = new Image(item.getUrl(), 100, 0, true, true, true);
                Platform.runLater(() -> imageView.setImage(img));
            }).start();
        }else{
            Rectangle fallback = new Rectangle(100, 75);
            fallback.setFill(Color.GRAY);
            imageView.setImage(null);
            hbox.getChildren().set(1, fallback);
        }
    }


}
