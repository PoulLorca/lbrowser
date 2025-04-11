package com.lbrowser.lbrowser;

import com.lbrowser.lbrowser.media.MediaItem;
import com.lbrowser.lbrowser.media.MediaListCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MediaSelectorController implements Initializable {
    @FXML private ListView<MediaItem> mediaListView;
    @FXML private CheckBox selectAllCheckBox;
    @FXML private Label counterLabel;
    private ObservableList<MediaItem> mediaItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mediaListView.setCellFactory(lv -> new MediaListCell());
        mediaListView.setItems(mediaItems);

        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            mediaItems.forEach(item -> item.setSelected(newVal));
        });

        mediaItems.addListener((ListChangeListener<MediaItem>) c -> {
            updateCounter();
        });
    }

    public void setMediaItems(List<MediaItem> items){
        mediaItems.setAll(items);
        updateCounter();
    }

    private void updateCounter() {
        long selectedCount = mediaItems.stream().filter(MediaItem::isSelected).count();
        counterLabel.setText(String.format("%d/%d selected", selectedCount, mediaItems.size()));
    }

    public void downloadSelected(ActionEvent actionEvent) {
        List<MediaItem> selectedItems = mediaItems.stream()
                .filter(MediaItem::isSelected)
                .collect(Collectors.toList());
        downloadMedia(selectedItems);
    }

    public void downloadAll(ActionEvent actionEvent) {
        downloadMedia(mediaItems);
    }

    private void downloadMedia(List<MediaItem> items) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File dir = directoryChooser.showDialog(null);

        if(dir != null) {
            items.forEach(item -> {
                try{
                    URL website = new URL(item.getUrl());
                    Path outputPath = Paths.get(dir.toString(), item.getFilename());

                    new Thread(()-> {
                        try (InputStream in = website.openStream()){
                            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e){
                            Platform.runLater(() -> System.out.println("Error downloading " + item.getFilename() + ": " + e.getMessage()));
                        }
                    }).start();
                }catch(MalformedURLException e){
                    System.out.println("Invalid URL: " + item.getUrl());
                }
            });
        }
    }
}
