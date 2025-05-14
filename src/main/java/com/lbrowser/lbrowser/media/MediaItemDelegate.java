package com.lbrowser.lbrowser.media;

import io.qt.core.*;
import io.qt.gui.*;
import io.qt.network.QNetworkAccessManager;
import io.qt.network.QNetworkReply;
import io.qt.network.QNetworkRequest;
import io.qt.widgets.*;

import java.util.*;

/**
 * Custom delegate for rendering media items in a view with thumbnails and selection checkboxes.
 * Handles asynchronous downloading and caching of image previews, and custom drawing of list items.
 */
public class MediaItemDelegate extends QStyledItemDelegate {
    // Layout dimensions
    private final int padding = 5;
    private final int checkboxWidth = 20;
    private final int previewWidth = 100;
    private final int previewHeight = 75;

    // Cache for downloaded image previews to avoid redundant requests
    private final Map<String, QPixmap> previewCache = new HashMap<>();
    private QNetworkAccessManager imageDownloader;
    private QPixmap placeholderPixmap;

    /**
     * Creates a new media item delegate with image preview capabilities.
     *
     * @param parent The parent object for this delegate
     */
    public MediaItemDelegate(QObject parent){
        super(parent);

        // Initialize placeholder for images not yet downloaded
        placeholderPixmap = new QPixmap(previewWidth, previewHeight);
        placeholderPixmap.fill(new QColor(Qt.GlobalColor.lightGray));

        // Set up the network manager for downloading image previews
        imageDownloader = new QNetworkAccessManager(this);
        imageDownloader.finished.connect(this::onPreviewDownloaded);
    }

    /**
     * Paints a media item in the view with a checkbox, preview image, and text.
     * Downloads image previews asynchronously if not already cached.
     *
     * @param painter The painter to use for drawing
     * @param option The style options for this item
     * @param index The model index for this item
     */
    @Override
    public void paint(QPainter painter, QStyleOptionViewItem option, QModelIndex index) {
        Object data = index.data(Qt.ItemDataRole.UserRole);
        if (data == null || !(data instanceof MediaItem)){
            super.paint(painter, option, index);
            return;
        }
        MediaItem item = (MediaItem) data;

        painter.save();

        if((option.state().testFlag(QStyle.StateFlag.State_Selected))){
            painter.fillRect(option.rect(), option.palette().highlight());
        }else{
            painter.fillRect(option.rect(), (index.row() % 2 == 0) ? option.palette().base() : option.palette().alternateBase());
        }

        QRect contentRect = option.rect().adjusted(padding, padding, -padding, -padding);

        QStyleOptionButton checkBoxOption = new QStyleOptionButton();
        checkBoxOption.setRect(new QRect(contentRect.left(), contentRect.top(), checkboxWidth, contentRect.height()));
        checkBoxOption.setState(option.state());
        QStyle.State newState = checkBoxOption.state();
        newState.setFlag(QStyle.StateFlag.State_On, item.isSelected());
        checkBoxOption.setState(newState);

        QRect previewRect = new QRect(contentRect.left() + checkboxWidth + padding, contentRect.top(), previewWidth, previewHeight);

        if(contentRect.height() > previewHeight){
            previewRect.moveTop(contentRect.top() + (contentRect.height() - previewHeight) / 2);
        }

        QRect textRect = new QRect(previewRect.right() + padding,
                contentRect.top(),
                contentRect.width() - checkboxWidth - previewWidth - 2 * padding,
                contentRect.height()
        );

        QApplication.style().drawControl(QStyle.ControlElement.CE_CheckBox, checkBoxOption, painter);

        QPixmap previewPixmap = placeholderPixmap;
        if("image".equals(item.getType())){
            if(previewCache.containsKey(item.getUrl())){
                previewPixmap = previewCache.get(item.getUrl());
            }else{
                startPreviewDownload(item.getUrl());
            }
        }

        QPixmap scaledPixmap = previewPixmap.scaled(previewRect.size(), Qt.AspectRatioMode.KeepAspectRatio, Qt.TransformationMode.SmoothTransformation);
        QRect targetPreviewRect = new QRect(previewRect.left() + (previewRect.width() - scaledPixmap.width()) / 2, previewRect.top() + (previewRect.height() - scaledPixmap.height()) / 2, scaledPixmap.width(), scaledPixmap.height());
        painter.drawPixmap(targetPreviewRect, scaledPixmap);

        painter.setPen(option.palette().color(QPalette.ColorGroup.Current, QPalette.ColorRole.Text));
        String filename = item.getFilename();
        QFontMetrics fm = new QFontMetrics(option.font());
        String elidedText = fm.elidedText(filename, Qt.TextElideMode.ElideRight, textRect.width());
        painter.drawText(textRect, Qt.AlignmentFlag.AlignLeft.value() | Qt.AlignmentFlag.AlignVCenter.value(), elidedText);

        painter.restore();
    }

    /**
     * Returns the recommended size for media items in the view.
     *
     * @param option The style options for this item
     * @param index The model index for this item
     * @return The recommended size based on preview dimensions and padding
     */
    @Override
    public QSize sizeHint(QStyleOptionViewItem option, QModelIndex index) {
        int height = previewHeight + 2 * padding;
        int width = checkboxWidth + previewWidth + 100 + 3 * padding;
        return new QSize(width, height);
    }

    /**
     * Handles user interactions with the delegate, particularly checkbox clicks.
     *
     * @param event The event that occurred
     * @param model The item model
     * @param option The style options for this item
     * @param index The model index for this item
     * @return true if the event was handled, false otherwise
     */
    @Override
    public boolean editorEvent(QEvent event, QAbstractItemModel model, QStyleOptionViewItem option, QModelIndex index) {
        if(event.type() == QEvent.Type.MouseButtonRelease){
            QMouseEvent mouseEvent = (QMouseEvent) event;
            QRect contentRect = option.rect().adjusted(padding, padding, -padding, -padding);
            QRect checkboxRect = new QRect(contentRect.left(), contentRect.top(), checkboxWidth, contentRect.height());

            if(checkboxRect.contains(mouseEvent.pos())){
                Object data = index.data(Qt.ItemDataRole.UserRole);
                if(data != null && data instanceof MediaItem){
                    MediaItem item = (MediaItem) data;
                    item.setSelected(!item.isSelected());
                    Qt.CheckState newState = item.isSelected() ? Qt.CheckState.Checked : Qt.CheckState.Unchecked;
                    model.setData(index, newState, Qt.ItemDataRole.CheckStateRole);
                    return true;
                }
            }
        }
        return super.editorEvent(event, model, option, index);
    }

    /**
     * Initiates asynchronous download of an image preview from a URL.
     * Avoids redundant downloads for the same URL.
     *
     * @param urlString URL of the image to download
     */
    private void startPreviewDownload(String urlString){
        if(previewCache.containsKey(urlString)  || isDownloadInProgress(urlString)){
            return;
        }

        QUrl url = new QUrl(urlString);
        if (!url.isValid()) return;

        QNetworkRequest request = new QNetworkRequest(url);
        QNetworkReply reply = imageDownloader.get(request);
        reply.setProperty("previewUrl", urlString);
    }

    /**
     * Checks if a download for the specified URL is already in progress.
     *
     * @param urlString The URL to check
     * @return true if the URL is already being downloaded
     */
    private boolean isDownloadInProgress(String urlString){
        for(QNetworkReply reply : imageDownloader.findChildren(QNetworkReply.class)){
            if(urlString.equals(reply.property("previewUrl"))){
                return true;
            }
        }
        return false;
    }

    /**
     * Callback for when an image preview download completes.
     * Updates the cache and refreshes the view if necessary.
     *
     * @param reply The network reply containing the downloaded image data
     */
    private void onPreviewDownloaded(QNetworkReply reply){
        String urlString = reply.property("previewUrl") != null ? reply.property("previewUrl").toString() : null;

        if(urlString != null && reply.error() == QNetworkReply.NetworkError.NoError){
            QPixmap pixmap = new QPixmap();
            if(pixmap.loadFromData(reply.readAll())){
                previewCache.put(urlString, pixmap);
                if(parent() instanceof QAbstractItemView){
                    QAbstractItemView view = (QAbstractItemView) parent();
                    QAbstractItemModel model = view.model();
                    if (model != null){
                        for(int row = 0; row < model.rowCount(); ++row){
                            QModelIndex index = model.index(row, 0);
                            Object data = index.data(Qt.ItemDataRole.UserRole);
                            if(data != null && data instanceof MediaItem){
                                MediaItem item = (MediaItem) data;
                                if(urlString.equals(item.getUrl())){
                                    view.update(index);
                                }
                            }
                        }
                    }
                }
            }
            else{
                previewCache.put(urlString, placeholderPixmap);
            }
        } else if (urlString != null) {
            previewCache.put(urlString, placeholderPixmap);
            if(parent() instanceof QAbstractItemView){
                QAbstractItemView view = (QAbstractItemView) parent();
            }
        }

        reply.disconnect();

    }
}
