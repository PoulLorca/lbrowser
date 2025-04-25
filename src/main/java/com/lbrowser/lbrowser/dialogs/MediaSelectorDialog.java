package com.lbrowser.lbrowser.dialogs;

import com.lbrowser.lbrowser.media.MediaItem;
import com.lbrowser.lbrowser.media.MediaItemDelegate;
import io.qt.core.*;
import io.qt.gui.QColor;
import io.qt.gui.QPalette;
import io.qt.network.QNetworkAccessManager;
import io.qt.network.QNetworkReply;
import io.qt.network.QNetworkRequest;
import io.qt.widgets.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MediaSelectorDialog extends QDialog {

    private static final Logger LOGGER = Logger.getLogger(MediaSelectorDialog.class.getName());

    private QListWidget mediaListWidget;
    private QCheckBox selectAllCheckBox;
    private QLabel counterLabel;
    private QPushButton downloadSelectedButton;
    private QPushButton downloadAllButton;
    private QPushButton cancelButton;
    private QProgressBar downloadProgressBar;

    private final List<MediaItem> allMediaItems;

    private QNetworkAccessManager networkManager;
    private int downloadsInProgress = 0;
    private int totalDownloadsInBatch = 0;
    private int successfulDownloads = 0;
    private int failedDownloads = 0;
    private List<String> failedUrls;

    public MediaSelectorDialog(List<MediaItem> items, QWidget parent){
        super(parent);
        this.allMediaItems = new ArrayList<>(items);

        setWindowTitle("Media Sources");
        setMinimumSize(600, 450);

        setupUI();
        populateList();
        setupConnections();
        updateCounter();
        updateSelectAllState();
    }

    private void setupUI(){
        QVBoxLayout mainLayout = new QVBoxLayout(this);

        QHBoxLayout topLayout = new QHBoxLayout();
        selectAllCheckBox = new QCheckBox("Select All");
        selectAllCheckBox.setChecked(true);
        counterLabel = new QLabel("0/0 selected");
        topLayout.addWidget(selectAllCheckBox);
        topLayout.addStretch(1);
        topLayout.addWidget(counterLabel);
        mainLayout.addLayout(topLayout);

        mediaListWidget = new QListWidget();
        mediaListWidget.setItemDelegate(new MediaItemDelegate(this));
        mainLayout.addWidget(mediaListWidget, 1);

        downloadProgressBar = new QProgressBar();
        downloadProgressBar.setRange(0, 100);
        downloadProgressBar.setValue(0);
        downloadProgressBar.setTextVisible(true);
        downloadProgressBar.setFormat("Downloading: %p%");
        downloadProgressBar.setVisible(false);
        QPalette palette = downloadProgressBar.palette();
        palette.setColor(QPalette.ColorRole.Highlight, new QColor("#3498db"));
        downloadProgressBar.setPalette(palette);
        mainLayout.addWidget(downloadProgressBar);

        QHBoxLayout bottomLayout = new QHBoxLayout();
        downloadSelectedButton = new QPushButton("Download Selected");
        downloadAllButton = new QPushButton("Download All");
        cancelButton = new QPushButton("Cancel");
        bottomLayout.addStretch(1);
        bottomLayout.addWidget(downloadSelectedButton);
        bottomLayout.addWidget(downloadAllButton);
        bottomLayout.addWidget(cancelButton);
        mainLayout.addLayout(bottomLayout);
    }

    private void populateList(){
        mediaListWidget.clear();
        for (MediaItem item : allMediaItems){
            QListWidgetItem listItem = new QListWidgetItem();
            listItem.setData(Qt.ItemDataRole.UserRole, item);
            listItem.setCheckState(item.isSelected() ? Qt.CheckState.Checked : Qt.CheckState.Unchecked);
            mediaListWidget.addItem(listItem);
        }
        if(!allMediaItems.isEmpty()){
            mediaListWidget.scrollToItem(mediaListWidget.item(0), QAbstractItemView.ScrollHint.PositionAtTop);
        }
    }

    private void setupConnections(){
        cancelButton.clicked.connect(this::reject);
        downloadSelectedButton.clicked.connect(this::downloadSelected);
        downloadAllButton.clicked.connect(this::downloadAll);

        selectAllCheckBox.stateChanged.connect(this::onSelectAllChanged);

        mediaListWidget.itemChanged.connect(this::onItemChanged);
    }

    private void onSelectAllChanged(int state){
        if(selectAllCheckBox.isTristate() && state == Qt.CheckState.PartiallyChecked.value()){
            selectAllCheckBox.setCheckState(Qt.CheckState.Checked);
            return;
        }

        boolean checked = (state == Qt.CheckState.Checked.value());
        Qt.CheckState checkState = checked ? Qt.CheckState.Checked : Qt.CheckState.Unchecked;

        mediaListWidget.itemChanged.disconnect(this::onItemChanged);
        for (int i = 0; i < mediaListWidget.count(); i++) {
            QListWidgetItem listItem = mediaListWidget.item(i);
            listItem.setCheckState(checkState);
            MediaItem mediaItem = getMediaItemFromListItem(listItem);
            if (mediaItem != null){
                mediaItem.setSelected(checked);
            }
        }

        mediaListWidget.itemChanged.connect(this::onItemChanged);

        updateCounter();

        if (selectAllCheckBox.isTristate()){
            selectAllCheckBox.setTristate(false);
        }
    }

    private void onItemChanged(QListWidgetItem listItem){
        MediaItem mediaItem = getMediaItemFromListItem(listItem);
        if (mediaItem != null){
            mediaItem.setSelected(listItem.checkState() == Qt.CheckState.Checked);
        }
        updateCounter();
        updateSelectAllState();
    }

    private void updateCounter(){
        long selectedCount = 0;
        for (int i = 0; i < mediaListWidget.count(); i++) {
            if (mediaListWidget.item(i).checkState() == Qt.CheckState.Checked){
                selectedCount++;
            }
        }
        long totalCount = mediaListWidget.count();
        counterLabel.setText(String.format("%d/%d selected", selectedCount, totalCount));
    }

    private void updateSelectAllState(){
        long selectedCount = 0;
        long totalCount = mediaListWidget.count();

        if(totalCount == 0){
            selectAllCheckBox.setEnabled(false);
            selectAllCheckBox.setTristate(false);
            selectAllCheckBox.setChecked(false);
            return;
        }

        selectAllCheckBox.setEnabled(true);
        for (int i = 0; i < mediaListWidget.count(); i++) {
            if(mediaListWidget.item(i).checkState() == Qt.CheckState.Checked){
                selectedCount++;
            }
        }

        selectAllCheckBox.stateChanged.disconnect(this::onSelectAllChanged);
        if(selectedCount == totalCount){
            selectAllCheckBox.setTristate(false);
            selectAllCheckBox.setChecked(true);
        } else if (selectedCount == 0) {
            selectAllCheckBox.setTristate(false);
            selectAllCheckBox.setChecked(false);
        }else{
            selectAllCheckBox.setTristate(true);
            selectAllCheckBox.setCheckState(Qt.CheckState.PartiallyChecked);
        }
        selectAllCheckBox.stateChanged.connect(this::onSelectAllChanged);
    }

    private void downloadSelected(){
        List<MediaItem> itemsToDownload = new ArrayList<>();
        for (int i = 0; i < mediaListWidget.count(); i++) {
            QListWidgetItem listItem = mediaListWidget.item(i);
            if(listItem.checkState() == Qt.CheckState.Checked){
                MediaItem mediaItem = getMediaItemFromListItem(listItem);
                if (mediaItem != null){
                    itemsToDownload.add(mediaItem);
                }
            }
        }
        if (!itemsToDownload.isEmpty()){
            startDownloadProcess(itemsToDownload);
        }else{
            QMessageBox.information(this, "Download", "No items selected for download.");
        }
    }

    private void downloadAll(){
        if(!allMediaItems.isEmpty()){
            startDownloadProcess(allMediaItems);
        }else{
            QMessageBox.information(this, "Download", "No items to download.");
        }
    }

    private void startDownloadProcess(List<MediaItem> items){
        if (downloadsInProgress > 0){
            QMessageBox.warning(this, "Download", "Downloads are already in progress.");
            return;
        }

        String targetDirectory = QFileDialog.getExistingDirectory(this, "Select Download Directory", QDir.homePath());
        if(targetDirectory == null || targetDirectory.isEmpty()){
            return;
        }

        if(networkManager == null){
            networkManager = new QNetworkAccessManager(this);
            networkManager.finished.connect(this::onNetworkReplyFinished);
        }

        totalDownloadsInBatch = items.size();
        downloadsInProgress = items.size();
        successfulDownloads = 0;
        failedDownloads = 0;
        failedUrls = new ArrayList<>();

        downloadProgressBar.setRange(0, totalDownloadsInBatch);
        downloadProgressBar.setValue(0);
        downloadProgressBar.setFormat(String.format("Downloading: %d/%d", 0, totalDownloadsInBatch));
        downloadProgressBar.setVisible(true);


        disableDownloadButtons();

        for(MediaItem item : items){
            downloadFile(item, targetDirectory);
        }

        if(downloadsInProgress == 0 && totalDownloadsInBatch > 0){
            checkDownloadsComplete();
        }
    }

    private void downloadFile(MediaItem item, String targetDir){
        QUrl url = new QUrl(item.getUrl());
        if(!url.isValid()){
            downloadsInProgress--;
            failedDownloads++;
            failedUrls.add(item.getUrl() + " (Invalid URL)");
            return;
        }

        String baseFilename = item.getFilename();
        String filePath = targetDir + QDir.separator() + baseFilename;
        QFile file = new QFile(filePath);

        int attempt = 1;
        while (file.exists() && attempt < 100){
            String nameWithoutExt;
            String extension;
            int dotIndex = baseFilename.lastIndexOf('.');
            if(dotIndex > 0){
                nameWithoutExt = baseFilename.substring(0, dotIndex);
                extension = baseFilename.substring(dotIndex);
            }else{
                nameWithoutExt = baseFilename;
                extension = "";
            }
            filePath = targetDir + QDir.separator() + nameWithoutExt + "_" + attempt + extension;
            file = new QFile(filePath);
            attempt++;
        }
        if(file.exists()){
            downloadsInProgress--;
            checkDownloadsComplete();
            return;
        }

        QNetworkRequest request = new QNetworkRequest(url);
        request.setRawHeader("User-Agent".getBytes(), "LBrowser-Download-Client/1.0".getBytes());

        QNetworkReply reply = networkManager.get(request);

        reply.setProperty("downloadFilePath", filePath);
        reply.setProperty("originalUrl", item.getUrl());
    }

    private void onNetworkReplyFinished(QNetworkReply reply){
        downloadsInProgress--;

        QUrl url = reply.url();
        QNetworkReply.NetworkError error = reply.error();
        String filePath = reply.property("downloadFilePath") != null ? reply.property("downloadFilePath").toString() : null;
        String originalUrl = reply.property("originalUrl") != null ? reply.property("originalUrl").toString() : null;

        boolean success = false;
        if(error == QNetworkReply.NetworkError.NoError){
            if(filePath != null){
                QFile file = new QFile(filePath);
                if(file.open(QIODevice.OpenModeFlag.WriteOnly)){
                    long bytesWritten = file.write(reply.readAll());
                    file.close();
                    if(bytesWritten > 0) {
                        successfulDownloads++;
                        success = true;
                    }else{
                        failedDownloads++;
                        failedUrls.add(originalUrl + " (Write Error)");
                        if (file.exists()) file.remove();
                    }
                }else{
                    failedDownloads++;
                    failedUrls.add(originalUrl + " (Cannot save file: " + file.errorString() + ")");
                }
            }else{
                LOGGER.log(Level.WARNING, "No file path provided. Skipping.");
                failedDownloads++;
                failedUrls.add(originalUrl + " (Internal error: file path lost)");
            }
        }else{
            failedDownloads++;
            failedUrls.add(originalUrl + " (" + error.toString() + ")");
        }

        int finishedCount = successfulDownloads + failedDownloads;
        downloadProgressBar.setValue(finishedCount);

        reply.disconnect();

        checkDownloadsComplete();
    }


    private void checkDownloadsComplete(){
        if(downloadsInProgress <= 0){
            downloadsInProgress = 0;
            downloadProgressBar.setVisible(false);

            enableDownloadButtons();

            String summaryTitle = "Downloads Complete";
            String summaryMessage;
            QMessageBox.Icon icon = QMessageBox.Icon.Information;

            if(failedDownloads == 0){
                summaryMessage = String.format("%d files downloaded successfully.", successfulDownloads);
            }else{
                summaryTitle = "Downloads Finished with Errors";
                summaryMessage = String.format("%d files downloaded successfully.\n%d files failed to download.", successfulDownloads, failedDownloads);
                icon = QMessageBox.Icon.Warning;

                if(!failedUrls.isEmpty()){
                    summaryMessage += "\n\nFailed URLs:\n" + String.join("\n", failedUrls);
                }
            }

            QMessageBox.information(this, summaryTitle, summaryMessage);

            successfulDownloads = 0;
            failedDownloads = 0;
            failedUrls = null;
        }
    }

    private void disableDownloadButtons(){
        downloadSelectedButton.setEnabled(false);
        downloadAllButton.setEnabled(false);
    }

    private void enableDownloadButtons(){
        downloadSelectedButton.setEnabled(true);
        downloadAllButton.setEnabled(true);
    }

    private MediaItem getMediaItemFromListItem(QListWidgetItem listItem){
        if(listItem != null){
            Object data = listItem.data(Qt.ItemDataRole.UserRole);
            if(data instanceof MediaItem){
                return (MediaItem) data;
            }
        }
        return null;
    }
}
