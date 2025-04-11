package com.lbrowser.lbrowser;

import com.lbrowser.lbrowser.media.MediaItem;
import com.lbrowser.lbrowser.media.MediaListCell;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MediaSelectorController implements Initializable {
    @FXML private ListView<MediaItem> mediaListView;
    @FXML private CheckBox selectAllCheckBox;
    @FXML private Label counterLabel;
    private final ObservableList<MediaItem> mediaItems = FXCollections.observableArrayList(
            item -> new Observable[]{item.selectedProperty()}
    );

    private final ExecutorService downloadExecutor = Executors.newFixedThreadPool(5);
    private boolean updatingSelectAllFromItems = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mediaListView.setCellFactory(lv -> new MediaListCell());
        mediaListView.setItems(mediaItems);

        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingSelectAllFromItems) {
                // ...entonces actualiza todos los items
                boolean selected = newVal;
                mediaItems.forEach(item -> item.setSelected(selected));
            }
        });

        mediaItems.addListener((ListChangeListener<MediaItem>) c -> {
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasUpdated()) {
                    updateSelectAllCheckBoxState();
                    updateCounter();
                    break;
                }
            }

            if (mediaItems.isEmpty()) {
                updateSelectAllCheckBoxState();
                updateCounter();
            }
        });

        updateCounter();
        updateSelectAllCheckBoxState();
    }

    public void setMediaItems(List<MediaItem> items){
        Platform.runLater(() -> {
            mediaItems.setAll(items);
        });
    }

    private void updateCounter() {
        long selectedCount = mediaItems.stream().filter(MediaItem::isSelected).count();
        long totalCount = mediaItems.size();
        Platform.runLater(() -> counterLabel.setText(String.format("%d/%d selected", selectedCount, totalCount)));
    }

    private void updateSelectAllCheckBoxState() {
        Platform.runLater(() -> {
            long totalItems = mediaItems.size();
            if (totalItems == 0) {
                selectAllCheckBox.setDisable(true);
                selectAllCheckBox.setIndeterminate(false);
                setSelectAllCheckedInternal(false);
                return;
            }

            selectAllCheckBox.setDisable(false);
            long selectedCount = mediaItems.stream().filter(MediaItem::isSelected).count();

            try {
                updatingSelectAllFromItems = true;

                if (selectedCount == totalItems) {
                    selectAllCheckBox.setIndeterminate(false);
                    setSelectAllCheckedInternal(true);
                } else if (selectedCount == 0) {
                    selectAllCheckBox.setIndeterminate(false);
                    setSelectAllCheckedInternal(false);
                } else {
                    selectAllCheckBox.setIndeterminate(true);
                    // setSelectAllCheckedInternal(false); // Let as not checked
                }
            } finally {
                updatingSelectAllFromItems = false;
            }
        });
    }

    private void setSelectAllCheckedInternal(boolean checked) {
        if (selectAllCheckBox.isSelected() != checked) {
            selectAllCheckBox.setSelected(checked);
        }

        if (selectAllCheckBox.isIndeterminate() && (checked || mediaItems.stream().noneMatch(MediaItem::isSelected))) {
            selectAllCheckBox.setIndeterminate(false);
        }
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
        directoryChooser.setTitle("Select Download Location");
        javafx.stage.Window ownerWindow = (mediaListView != null && mediaListView.getScene() != null)
                ? mediaListView.getScene().getWindow()
                : null;

        File dir = directoryChooser.showDialog(ownerWindow);

        if (dir != null) {
            final String targetDir = dir.getAbsolutePath(); // Guardar ruta como final para usar en lambda
            final int totalItemsToDownload = items.size();

            Platform.runLater(() -> {
                Notifications.create()
                        .title("Downloads Started")
                        .text("Starting download of " + totalItemsToDownload + " items to:\n" + targetDir)
                        .owner(ownerWindow)
                        .position(Pos.BOTTOM_RIGHT)
                        .hideAfter(Duration.seconds(6))
                        .showInformation();
            });


            items.forEach(item -> {
                downloadExecutor.submit(() -> {
                    Path outputPath = null;
                    String finalFilename = item.getFilename();
                    try {
                        URL website = new URL(item.getUrl());
                        finalFilename = item.getFilename().replaceAll("[^a-zA-Z0-9.\\-_]+", "_"); // Sanitizar un poco más permisivo
                        outputPath = Paths.get(targetDir, finalFilename);
                        int attempt = 1;
                        while (Files.exists(outputPath) && attempt < 100) {
                            String baseName;
                            String extension;
                            int dotIndex = finalFilename.lastIndexOf('.');
                            if (dotIndex > 0 && dotIndex < finalFilename.length() - 1) {
                                baseName = finalFilename.substring(0, dotIndex);
                                extension = finalFilename.substring(dotIndex);
                            } else {
                                baseName = finalFilename;
                                extension = "";
                            }
                            outputPath = Paths.get(targetDir, baseName + "_" + attempt + extension);
                            attempt++;
                        }
                        finalFilename = outputPath.getFileName().toString();

                        // System.out.println("Downloading: " + item.getUrl() + " -> " + finalFilename);

                        try (InputStream in = website.openStream()) {
                            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);


                            final String successFilename = finalFilename;
                            Platform.runLater(() -> {
                                Notifications.create()
                                        .title("Download Complete")
                                        .text(successFilename + " downloaded.")
                                        .owner(ownerWindow)
                                        .position(Pos.BOTTOM_RIGHT)
                                        .hideAfter(Duration.seconds(4))
                                        .showInformation();
                            });

                        } catch (IOException e) {
                            final String errorMsg = e.getMessage();
                            final String failedFilenameIO = finalFilename;
                            Platform.runLater(() -> {
                                Notifications.create()
                                        .title("Download Error")
                                        .text("Failed I/O for " + failedFilenameIO + ":\n" + errorMsg)
                                        .owner(ownerWindow)
                                        .position(Pos.BOTTOM_RIGHT)
                                        // .hideAfter(Duration.seconds(10))
                                        .showError();
                            });
                            e.printStackTrace();
                        }
                    } catch (MalformedURLException e) {
                        final String invalidUrl = item.getUrl();
                        Platform.runLater(() -> {
                            Notifications.create()
                                    .title("Invalid URL")
                                    .text("Skipping invalid URL:\n" + invalidUrl)
                                    .owner(ownerWindow)
                                    .position(Pos.BOTTOM_RIGHT)
                                    .showWarning();
                        });
                    } catch (Exception e) {
                        final String errorMsg = e.getMessage();
                        final String failedFilenameGeneric = finalFilename;
                        Platform.runLater(() -> {
                            Notifications.create()
                                    .title("Download Error")
                                    .text("Failed " + failedFilenameGeneric + ":\n" + errorMsg)
                                    .owner(ownerWindow)
                                    .position(Pos.BOTTOM_RIGHT)
                                    .showError();
                        });
                        e.printStackTrace();
                    }
                });
            });
        } else {
            Platform.runLater(() -> {
                Notifications.create()
                        .title("Download Cancelled")
                        .text("No directory was selected.")
                        .owner(ownerWindow)
                        .position(Pos.BOTTOM_RIGHT)
                        .showWarning();
            });
        }
    }
}
