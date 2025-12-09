package service;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageViewUpdater {

    private final ImageView view;

    public ImageViewUpdater(ImageView view) {
        this.view = view;
    }

    public void update(Image img) {
        Platform.runLater(() -> view.setImage(img));
    }
}
