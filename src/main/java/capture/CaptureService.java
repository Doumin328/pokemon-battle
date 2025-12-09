package capture;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CaptureService {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private VideoCapture capture;
    private Thread captureThread;
    private AtomicBoolean running = new AtomicBoolean(false);

    // フレームが更新されるたびに呼ばれるコールバック
    private Consumer<Image> onFrameCallback;

    public void startCapture(int cameraIndex, Consumer<Image> onFrame) {
        this.onFrameCallback = onFrame;

        capture = new VideoCapture(cameraIndex);
        if (!capture.isOpened()) {
            System.out.println("カメラを開けませんでした");
            return;
        }

        running.set(true);

        captureThread = new Thread(() -> {
            Mat frame = new Mat();

            while (running.get()) {
                if (capture.read(frame)) {

                    Image fxImage = matToImage(frame);

                    Platform.runLater(() -> {
                        if (onFrameCallback != null) {
                            onFrameCallback.accept(fxImage);
                        }
                    });
                }
            }
            frame.release();
        });

        captureThread.setDaemon(true);
        captureThread.start();
    }

    public List<String> getDeviceList() {
        List<String> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            VideoCapture cap = new VideoCapture(i);
            if (cap.isOpened()) {
                devices.add("Device " + i);
                cap.release();
            }
        }
        return devices;
    }

    public void stopCapture() {
        running.set(false);
        if (capture != null) capture.release();
    }

    private Image matToImage(Mat mat) {
        BufferedImage buf = new BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_3BYTE_BGR);
        mat.get(0, 0, ((java.awt.image.DataBufferByte) buf.getRaster().getDataBuffer()).getData());
        return SwingFXUtils.toFXImage(buf, null);
    }
}
