package service;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;

public class JavaCVUtils {

    private static final Java2DFrameConverter converter = new Java2DFrameConverter();

    public static Image toFXImage(Frame frame) {
        BufferedImage bimg = converter.convert(frame);
        return SwingFXUtils.toFXImage(bimg, null);
    }
}

