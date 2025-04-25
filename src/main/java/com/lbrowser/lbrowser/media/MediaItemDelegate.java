package com.lbrowser.lbrowser.media;

import io.qt.core.*;
import io.qt.gui.*;
import io.qt.network.QNetworkAccessManager;
import io.qt.network.QNetworkReply;
import io.qt.network.QNetworkRequest;
import io.qt.widgets.*;

import java.util.*;

public class MediaItemDelegate extends QStyledItemDelegate {
    private final int padding = 5;
    private final int checkboxWidth = 20;
    private final int previewWidth = 100;
    private final int previewHeight = 75;

    private final Map<String, QPixmap> previewCache = new HashMap<>();
    private final Set<String> downloadsInProgressSet = Collections.synchronizedSet(new HashSet<>());
    private QNetworkAccessManager imageDownloader;
    private QPixmap placeholderPixmap;

    public MediaItemDelegate(QObject parent){
        super(parent);

        placeholderPixmap = new QPixmap(previewWidth, previewHeight);
        placeholderPixmap.fill(new QColor(Qt.GlobalColor.lightGray));

        imageDownloader = new QNetworkAccessManager(this);
        imageDownloader.finished.connect(this::onPreviewDownloaded);
    }

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

    @Override
    public QSize sizeHint(QStyleOptionViewItem option, QModelIndex index) {
        int height = previewHeight + 2 * padding;
        int width = checkboxWidth + previewWidth + 100 + 3 * padding;
        return new QSize(width, height);
    }

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

    private boolean isDownloadInProgress(String urlString){
        for(QNetworkReply reply : imageDownloader.findChildren(QNetworkReply.class)){
            if(urlString.equals(reply.property("previewUrl"))){
                return true;
            }
        }
        return false;
    }

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
