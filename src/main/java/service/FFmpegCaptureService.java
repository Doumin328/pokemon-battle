package service;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.Frame;

public class FFmpegCaptureService {

    private FFmpegFrameGrabber grabber;
    private boolean running = false;

    private final ImageView imageView;

    public FFmpegCaptureService(ImageView imageView) {
        this.imageView = imageView;
    }

    /** 映像＋音声を同時キャプチャ開始 */
    public void start(String videoDeviceName, String audioDeviceName) {
        if (running) return;

        try {
            String deviceInput = "video=" + videoDeviceName;
            if (audioDeviceName != null && !audioDeviceName.isBlank()) {
                deviceInput += ":audio=" + audioDeviceName;
            }

            grabber = new FFmpegFrameGrabber(deviceInput);
            grabber.setFormat("dshow");

            // 利用するサンプルの指定（音声）
            grabber.setSampleRate(44100);
            grabber.setAudioChannels(2);

            grabber.start();
            running = true;

            // キャプチャスレッド
            Thread thread = new Thread(() -> {
                Java2DFrameConverter converter = new Java2DFrameConverter();

                while (running) {
                    try {
                        Frame frame = grabber.grab();

                        if (frame == null) continue;

                        // 映像フレーム
                        if (frame.image != null) {
                            var bufferedImage = converter.convert(frame);

                            Platform.runLater(() -> {
                                imageView.setImage(
                                        javafx.embed.swing.SwingFXUtils.toFXImage(bufferedImage, null)
                                );
                            });
                        }

                        // 音声フレームも来る（frame.samples）
                        if (frame.samples != null) {
                            // 音声データは後で AudioPlayerService に渡す想定
                            // short[][] pcm = (short[][]) frame.samples; ← 変換は別途やる
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    grabber.stop();
                    grabber.release();
                } catch (Exception ignored) {}
            });

            thread.setDaemon(true);
            thread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }
}
