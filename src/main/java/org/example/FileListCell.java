package org.example;

import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

// Кастомный ListCell для отображения галочки и статуса обработки
public class FileListCell extends ListCell<String> {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(item);
            if (fileIsProcessed(item)) {
                setGraphic(new ImageView(new Image("checked_icon.png"))); // Путь к иконке галочки
            } else if (fileIsProcessing(item)) {
                setTextFill(Color.GRAY);
                setText(item + " (в обработке)");
            } else {
                setGraphic(null);
            }
        }
    }

    private boolean fileIsProcessed(String fileName) {
        // TODO сделать
        return false;
    }

    // Пример метода, определяющего, обрабатывается ли файл
    private boolean fileIsProcessing(String fileName) {
        // TODO сделать
        return false;
    }
}

