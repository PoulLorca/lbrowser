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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaListCell extends ListCell<MediaItem> {
    private final ImageView imageView = new ImageView();
    private final CheckBox checkBox = new CheckBox();
    private final Label label = new Label();
    private final HBox hbox;

    public MediaListCell(){
        hbox = new HBox(15);
        imageView.setFitWidth(100);
        imageView.setPreserveRatio(true);
        hbox.getChildren().addAll(checkBox, imageView, label);

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
        } else {
            label.setText(item.getFilename());
            checkBox.setSelected(item.isSelected());
            loadPreviewAsync(item);
            setGraphic(hbox);
        }
    }

    private void loadPreviewAsync(MediaItem item){
        if (item == null) return; // Seguridad extra

        // Resetear a placeholder por defecto antes de cargar
        Rectangle placeholder = new Rectangle(100, 75); // Ajusta tamaño si es necesario
        placeholder.setFill(Color.LIGHTGRAY); // Un color más suave
        // Asegurar que siempre haya una ImageView o un Placeholder en la posición 1
        if (hbox.getChildren().get(1) != imageView) {
            hbox.getChildren().set(1, imageView);
        }
        imageView.setImage(null); // Limpiar imagen anterior

        if("image".equals(item.getType())){
            ExecutorService imageLoadExecutor = Executors.newSingleThreadExecutor();
            imageLoadExecutor.submit(() -> {
                try {
                    Image img = new Image(item.getUrl(), 100, 0, true, true, true);
                    if (img.isError()) {
                        System.err.println("Error loading image preview: " + item.getUrl() + " - " + img.getException().getMessage());
                        Platform.runLater(() -> hbox.getChildren().set(1, placeholder));
                    } else {
                        Platform.runLater(() -> imageView.setImage(img));
                    }
                } catch (Exception e) {
                    System.err.println("Exception loading image preview: " + item.getUrl() + " - " + e.getMessage());
                    Platform.runLater(() -> hbox.getChildren().set(1, placeholder));
                } finally {
                    imageLoadExecutor.shutdown();
                }
            });
        } else {
            hbox.getChildren().set(1, placeholder);
        }
    }


}
